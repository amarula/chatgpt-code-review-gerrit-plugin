package com.googlesource.gerrit.plugins.chatgpt.client.patch.code;

import com.googlesource.gerrit.plugins.chatgpt.client.patch.diff.FileDiffProcessed;
import com.googlesource.gerrit.plugins.chatgpt.model.api.chatgpt.ChatGptReplyItem;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritCodeRange;
import com.googlesource.gerrit.plugins.chatgpt.model.api.gerrit.GerritComment;
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
            return getLine(commentProperty);
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
        String line = newContent.get(line_num);
        try {
            if (line_num == range.endLine) {
                line = line.substring(0, range.endCharacter);
            }
            if (line_num == range.startLine) {
                line = line.substring(range.startCharacter);
            }
        }
        catch (StringIndexOutOfBoundsException e) {
            log.info("Could not extract a slice from line \"{}\". The whole line is returned", line, e);
        }
        return line;
    }

    private String getLine(GerritComment commentProperty) {
        try {
            return newContent.get(commentProperty.getLine());
        }
        catch (IndexOutOfBoundsException e) {
            log.warn("Could not extract line #{} from the code", commentProperty.getLine(), e);
            return null;
        }
    }

}
