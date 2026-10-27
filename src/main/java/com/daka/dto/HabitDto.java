package com.daka.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class HabitDto {

    public record SaveReq(
            @NotBlank(message = "习惯名称不能为空") String name,
            String color,
            String iconText,
            Integer sortOrder,
            Integer archived
    ) {
    }

    /** date 为空时默认今天 */
    public record CheckReq(LocalDate date, Boolean done) {
    }
}
