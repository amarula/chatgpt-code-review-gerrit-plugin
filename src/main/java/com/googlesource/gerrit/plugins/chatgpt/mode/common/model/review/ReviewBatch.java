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

package com.googlesource.gerrit.plugins.chatgpt.mode.common.model.review;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import static com.googlesource.gerrit.plugins.chatgpt.settings.Settings.GERRIT_PATCH_SET_FILENAME;

@Data
@RequiredArgsConstructor
public class ReviewBatch {
    private String id;
    @NonNull
    private String content;
    private String filename;
    private Integer line;
    private GerritCodeRange range;

    public String getFilename() {
        return filename == null ? GERRIT_PATCH_SET_FILENAME : filename;
    }
}
