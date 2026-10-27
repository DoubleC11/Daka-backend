package com.daka.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class TodoDto {

    public record SaveReq(
            @NotBlank(message = "待办内容不能为空") String title,
            String tag,
            LocalDate dueDate,
            Long goalId,
            Integer sortOrder
    ) {
    }
}
