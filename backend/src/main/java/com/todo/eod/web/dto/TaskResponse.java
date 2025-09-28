package com.todo.eod.web.dto;

import com.todo.eod.domain.TaskState;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TaskResponse {
    private String id;
    private String key;
    private String title;
    private TaskState state;
    private String dodPolicyId;
    private String assignee;
    private List<String> labels;
    private UUID correlationId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private DodSummary dod;

    @Data @Builder
    public static class DodSummary {
        private String policyId;
        private List<Progress> progress;
        private boolean complete;

        @Data @Builder
        public static class Progress {
            private String req;
            private boolean ok;
            private String details;
        }
    }
}
