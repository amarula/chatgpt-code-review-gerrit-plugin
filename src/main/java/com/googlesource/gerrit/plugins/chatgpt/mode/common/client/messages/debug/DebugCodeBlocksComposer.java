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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.messages.debug;

import com.googlesource.gerrit.plugins.chatgpt.localization.Localizer;

import java.util.ArrayList;
import java.util.List;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.*;

public abstract class DebugCodeBlocksComposer {
  private static final String TITLE_PREFIX = "### ";

  protected final Localizer localizer;
  protected final String commentOpening;

  public DebugCodeBlocksComposer(Localizer localizer, String openingTitleKey) {
    this.localizer = localizer;
    String openingTitle = localizer.getText(openingTitleKey);
    commentOpening = CODE_DELIMITER_BEGIN + openingTitle + "\n";
  }

  protected String getDebugCodeBlock(List<String> panelItems) {
    return joinWithNewLine(
        new ArrayList<>() {
          {
            add(commentOpening);
            addAll(panelItems);
            add(CODE_DELIMITER);
          }
        });
  }

  protected String getAsTitle(String title) {
    return TITLE_PREFIX + convertPascalCaseToWords(title);
  }
}
