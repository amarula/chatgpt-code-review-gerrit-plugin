package com.googlesource.gerrit.plugins.chatgpt.mode.common.client.api.gerrit;

import com.google.gerrit.entities.BranchNameKey;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Getter
public class GerritChange {
    private Event event;
    private String eventType;
    private long eventTimeStamp;
    private PatchSetEvent patchSetEvent;
    private Project.NameKey projectNameKey;
    private BranchNameKey branchNameKey;
    private Change.Key changeKey;
    private String fullChangeId;
    @Setter
    private Boolean isCommentEvent;

    public GerritChange(Project.NameKey projectNameKey, BranchNameKey branchNameKey, Change.Key changeKey) {
        this.projectNameKey = projectNameKey;
        this.branchNameKey = branchNameKey;
        this.changeKey = changeKey;
        buildFullChangeId();
    }

    public GerritChange(Event event) {
        this(
                ((PatchSetEvent) event).getProjectNameKey(),
                ((PatchSetEvent) event).getBranchNameKey(),
                ((PatchSetEvent) event).getChangeKey()
        );
        this.event = event;
        eventType = event.getType();
        eventTimeStamp = event.eventCreatedOn;
        patchSetEvent = (PatchSetEvent) event;
    }

    // Incomplete initialization used by CodeReviewPluginIT
    public GerritChange(String fullChangeId) {
        this.fullChangeId = fullChangeId;
    }

    public Optional<PatchSetAttribute> getPatchSetAttribute() {
        try {
            return Optional.ofNullable(patchSetEvent.patchSet.get());
        }
        catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    private void buildFullChangeId() {
        fullChangeId = String.join("~", URLEncoder.encode(projectNameKey.get(), StandardCharsets.UTF_8),
                branchNameKey.shortName(), changeKey.get());
    }

}
