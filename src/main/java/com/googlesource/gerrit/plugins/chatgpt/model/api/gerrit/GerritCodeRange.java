package com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class GerritCodeRange {
    @SerializedName("start_line")
    public int startLine;
    @SerializedName("end_line")
    public int endLine;
    @SerializedName("start_character")
    public int startCharacter;
    @SerializedName("end_character")
    public int endCharacter;
}
