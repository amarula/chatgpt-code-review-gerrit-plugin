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

package com.googlesource.gerrit.plugins.reviewai.data;

import com.google.gerrit.server.account.AccountCache;
import com.google.gerrit.server.account.AccountState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.reviewai.config.Configuration;
import com.googlesource.gerrit.plugins.reviewai.aibackend.common.model.data.ChangeSetData;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ChangeSetDataProvider implements Provider<ChangeSetData> {
  private final int aiAccountId;
  private final Configuration config;

  @Inject
  ChangeSetDataProvider(Configuration config, AccountCache accountCache) {
    this.config = config;
    Optional<AccountState> accountState = accountCache.getByUsername(config.gerritUserName());
    if (accountState.isPresent()) {
      aiAccountId = accountState.get().account().id().get();
      log.debug("AI account ID set to {}", aiAccountId);
    } else {
      log.error(
          "Failed to retrieve AI account ID for username {}",
          config.gerritUserName());
      throw new IllegalStateException("AI account not found for given username");
    }
  }

  @Override
  public ChangeSetData get() {
    log.debug(
        "Providing ChangeSetData with accountId: {}, minScore: {}, maxScore: {}",
        aiAccountId,
        config.votingMinScore(),
        config.votingMaxScore());
    return new ChangeSetData(
        aiAccountId,
        config.votingMinScore(),
        config.votingMaxScore());
  }
}
