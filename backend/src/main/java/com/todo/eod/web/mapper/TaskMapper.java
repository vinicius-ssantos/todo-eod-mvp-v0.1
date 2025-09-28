package com.todo.eod.web.mapper;

import com.todo.eod.domain.Task;
import com.todo.eod.web.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "dod", ignore = true)
    TaskResponse toResponse(Task task);
}
