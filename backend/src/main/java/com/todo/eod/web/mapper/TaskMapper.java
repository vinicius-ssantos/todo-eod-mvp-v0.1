package com.todo.eod.web.mapper;

import com.todo.eod.domain.Task;
import com.todo.eod.web.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "dod", ignore = true)
    @Mapping(target = "id", expression = "java(task.getId() != null ? task.getId().toString() : null)")
    @Mapping(target = "dodPolicyId", expression = "java(task.getDodPolicy() != null && task.getDodPolicy().getId() != null ? task.getDodPolicy().getId().toString() : null)")
    TaskResponse toResponse(Task task);
}
