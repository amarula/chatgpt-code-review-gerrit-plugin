/*
 * Copyright (c) 2025. The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlesource.gerrit.plugins.chatgpt.client.api.gerrit;

import com.google.gerrit.extensions.common.GroupInfo;
import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.util.ManualRequestContext;
import com.googlesource.gerrit.plugins.chatgpt.config.Configuration;
import lombok.extern.slf4j.Slf4j;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
public class GerritClientAccount extends GerritClientBase {
  private final AccountCache accountCache;

  public GerritClientAccount(Configuration config, AccountCache accountCache) {
    super(config);
    this.accountCache = accountCache;
    log.debug("GerritClientAccount initialized.");
  }

  public boolean isDisabledUser(String authorUsername) {
    List<String> enabledUsers = config.getEnabledUsers();
    List<String> disabledUsers = config.getDisabledUsers();
    boolean isDisabled =
        !enabledUsers.contains(Configuration.ENABLED_USERS_ALL)
                && !enabledUsers.contains(authorUsername)
            || disabledUsers.contains(authorUsername)
            || isDisabledUserGroup(authorUsername);
    log.debug("Checking if user '{}' is disabled: {}", authorUsername, isDisabled);
    return isDisabled;
  }

  public boolean isDisabledTopic(String topic) {
    List<String> enabledTopicFilter = config.getEnabledTopicFilter();
    List<String> disabledTopicFilter = config.getDisabledTopicFilter();
    boolean isDisabled =
        !enabledTopicFilter.contains(Configuration.ENABLED_TOPICS_ALL)
                && enabledTopicFilter.stream().noneMatch(topic::contains)
            || !topic.isEmpty() && disabledTopicFilter.stream().anyMatch(topic::contains);
    log.debug("Checking if topic '{}' is disabled: {}", topic, isDisabled);
    return isDisabled;
  }

  protected Optional<Integer> getAccountId(String authorUsername) {
    try {
      Optional<Integer> accountId =
          accountCache
              .getByUsername(authorUsername)
              .map(accountState -> accountState.account().id().get());
      log.debug("Retrieved account ID for username '{}': {}", authorUsername, accountId);
      return accountId;
    } catch (Exception e) {
      log.error("Could not find account ID for username '{}'", authorUsername);
      return Optional.empty();
    }
  }

  public Integer getNotNullAccountId(String authorUsername) {
    log.debug("Getting not null account ID for username '{}'", authorUsername);
    return getAccountId(authorUsername)
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    String.format("Error retrieving '%s' account ID in Gerrit", authorUsername)));
  }

  private List<String> getAccountGroups(Integer accountId) {
    try (ManualRequestContext requestContext = config.openRequestContext()) {
      List<GroupInfo> groups = config.getGerritApi().accounts().id(accountId).getGroups();
      log.debug("Retrieved groups for account ID {}: {}", accountId, groups);
      return groups.stream().map(g -> g.name).collect(toList());
    } catch (Exception e) {
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
      log.debug(
          "No account ID found for username '{}', cannot determine group status.", authorUsername);
      return false;
    }
    List<String> accountGroups = getAccountGroups(accountId.orElse(-1));
    if (accountGroups == null || accountGroups.isEmpty()) {
      log.debug(
          "No groups found for account ID of username '{}', assuming not disabled.",
          authorUsername);
      return false;
    }
    boolean isDisabled =
        !enabledGroups.contains(Configuration.ENABLED_GROUPS_ALL)
                && enabledGroups.stream().noneMatch(accountGroups::contains)
            || disabledGroups.stream().anyMatch(accountGroups::contains);
    log.debug("User group status for username '{}': disabled={}", authorUsername, isDisabled);
    return isDisabled;
  }
}
