package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gson.JsonArray;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.UriResourceLocator;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class GerritClientAccount extends GerritClientBase {

    public GerritClientAccount(Configuration config) {
        super(config);
    }

    public boolean isDisabledUser(String authorUsername) {
        List<String> enabledUsers = config.getEnabledUsers();
        List<String> disabledUsers = config.getDisabledUsers();
        return !enabledUsers.contains(Configuration.ENABLED_USERS_ALL)
                && !enabledUsers.contains(authorUsername)
                || disabledUsers.contains(authorUsername)
                || isDisabledUserGroup(authorUsername);
    }

    public boolean isDisabledTopic(String topic) {
        List<String> enabledTopicFilter = config.getEnabledTopicFilter();
        List<String> disabledTopicFilter = config.getDisabledTopicFilter();
        return !enabledTopicFilter.contains(Configuration.ENABLED_TOPICS_ALL)
                && enabledTopicFilter.stream().noneMatch(topic::contains)
                || !topic.isEmpty() && disabledTopicFilter.stream().anyMatch(topic::contains);
    }

    protected Optional<Integer> getAccountId(String authorUsername) {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritAccountIdUri(authorUsername));
        try {
            JsonArray responseArray = forwardGetRequestReturnJsonArray(uri);
            return Optional.of(responseArray.get(0).getAsJsonObject().get("_account_id").getAsInt());
        }
        catch (Exception e) {
            log.error("Could not find account ID for username '{}'", authorUsername);
            return Optional.empty();
        }
    }

    protected Integer getNotNullAccountId(String authorUsername) {
        return getAccountId(authorUsername).orElseThrow(() -> new NoSuchElementException(
                String.format("Error retrieving '%s' account ID in Gerrit", authorUsername)));
    }

    private List<String> getAccountGroups(Integer accountId) {
        URI uri = URI.create(config.getGerritAuthBaseUrl()
                + UriResourceLocator.gerritAccountsUri()
                + UriResourceLocator.gerritGroupPostfixUri(accountId));
        try {
            JsonArray responseArray = forwardGetRequestReturnJsonArray(uri);
            return StreamSupport.stream(responseArray.spliterator(), false)
                    .map(jsonElement -> jsonElement.getAsJsonObject().get("name").getAsString())
                    .collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("Could not find groups for account ID {}", accountId);
            return null;
        }
    }

    private boolean isDisabledUserGroup(String authorUsername) {
        List<String> enabledGroups = config.getEnabledGroups();
        List<String> disabledGroups = config.getDisabledGroups();
        if (enabledGroups.isEmpty() && disabledGroups.isEmpty()) {
            return false;
        }
        Optional<Integer> accountId = getAccountId(authorUsername);
        if (accountId.isEmpty()) {
            return false;
        }
        List<String> accountGroups = getAccountGroups(accountId.orElse(-1));
        if (accountGroups == null || accountGroups.isEmpty()) {
            return false;
        }
        return !enabledGroups.contains(Configuration.ENABLED_GROUPS_ALL)
                && enabledGroups.stream().noneMatch(accountGroups::contains)
                || disabledGroups.stream().anyMatch(accountGroups::contains);
    }

}
