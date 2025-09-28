package com.todo.eod.web.dto;

import com.todo.eod.domain.TaskState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskStatePatchRequest {
    @NotNull
    private TaskState state;
}
