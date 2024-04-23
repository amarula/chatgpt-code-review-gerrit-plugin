package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            List<AccountInfo> accounts = config.getGerritApi().accounts().query(authorUsername).get();
            if (accounts.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(accounts.get(0)).map(a -> a._accountId);
        }
        catch (Exception e) {
            log.error("Could not find account ID for username '{}'", authorUsername);
            return Optional.empty();
        }
    }

    public Integer getNotNullAccountId(String authorUsername) {
        return getAccountId(authorUsername).orElseThrow(() -> new NoSuchElementException(
                String.format("Error retrieving '%s' account ID in Gerrit", authorUsername)));
    }

    private List<String> getAccountGroups(Integer accountId) {
        try (ManualRequestContext requestContext = config.openRequestContext()) {
            List<GroupInfo> groups = config.getGerritApi().accounts().id(accountId).getGroups();
            return groups.stream().map(g -> g.name).collect(toList());
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
