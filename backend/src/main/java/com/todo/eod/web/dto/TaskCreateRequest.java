package com.todo.eod.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TaskCreateRequest {
    @NotBlank
    private String key;
    @NotBlank
    private String title;
    @NotBlank
    private String dodPolicyId; // UUID string
    private String assignee;
    private List<String> labels;
    @NotNull
    private UUID correlationId;
}
