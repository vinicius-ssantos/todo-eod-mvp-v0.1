package com.todo.eod.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebhookPayload {
    @NotBlank
    private String eventId;

    @NotBlank
    private String type;

    private String repo;
    private String branch;
    private Integer pr;

    private String workflow;
    private String sha;

    private String url;
    private String query;
    private Integer count;

    private String flagKey;
    private Integer percentage;

    private String taskKey;
}
