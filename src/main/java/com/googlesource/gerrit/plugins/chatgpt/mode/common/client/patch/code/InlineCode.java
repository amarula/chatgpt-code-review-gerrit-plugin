package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.code;

import com.googlesource.gerrit.plugins.chatgpt.mode.common.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.mode.common.model.api.gerrit.GerritComment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.googlesource.gerrit.plugins.chatgpt.utils.TextUtils.joinWithNewLine;

@Slf4j
public class InlineCode {
    private final CodeFinder codeFinder;
    private final List<String> newContent;
    private GerritCodeRange range;

    public InlineCode(FileDiffProcessed fileDiffProcessed) {
        codeFinder = new CodeFinder(fileDiffProcessed.getCodeFinderDiffs(), fileDiffProcessed.getRandomPlaceholder());
        newContent = fileDiffProcessed.getNewContent();
    }

    public String getInlineCode(GerritComment commentProperty) {
        if (commentProperty.getRange() != null) {
            List<String> codeByRange = new ArrayList<>();
            range = commentProperty.getRange();
            for (int line_num = range.startLine; line_num <= range.endLine; line_num++) {
                codeByRange.add(getLineSlice(line_num));
            }
            return joinWithNewLine(codeByRange);
        }
        else {
            return getLineFromLineNumber(commentProperty.getLine());
        }
    }

    public Optional<GerritCodeRange> findCommentRange(ChatGptReplyItem replyItem) {
        int commentedLine;
        try {
            commentedLine = replyItem.getLineNumber();
        }
        catch (NumberFormatException ex){
            // If the line number is not passed, a line in the middle of the code is used as best guess
            commentedLine = newContent.size() / 2;
        }

        return Optional.ofNullable(codeFinder.findCommentedCode(replyItem, commentedLine));
    }

    private String getLineSlice(int line_num) {
        String line = getLineFromLineNumber(line_num);
        if (line == null) {
            throw new RuntimeException("Error retrieving line number from content");
        }
        try {
            if (line_num == range.endLine) {
                line = line.substring(0, range.endCharacter);
            }
            if (line_num == range.startLine) {
                line = line.substring(range.startCharacter);
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            log.info("Could not extract a slice from line \"{}\". The whole line is returned", line);
        }
        return line;
    }

    private String getLineFromLineNumber(int line_num) {
        String line = null;
        try {
            line = newContent.get(line_num);
        }
        catch (IndexOutOfBoundsException e) {
            // If the line number returned by ChatGPT exceeds the actual number of lines, return the last line
            int lastLine = newContent.size() - 1;
            if (line_num > lastLine) {
                line = newContent.get(lastLine);
            }
            else {
                log.warn("Could not extract line #{} from the code", line_num);
            }
        }
        return line;
    }

}
