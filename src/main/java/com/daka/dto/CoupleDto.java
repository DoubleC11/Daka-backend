package com.daka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CoupleDto {

    public record BindReq(
            @NotBlank(message = "请输入邀请码")
            @Size(max = 10, message = "邀请码格式不正确")
            String code
    ) {
    }

    /**
     * 共同日历：给某一天写/改内容。
     * content 传空字符串表示把这一天清空。
     */
    public record CalendarSaveReq(
            @NotNull(message = "请选择日期") LocalDate date,
            @Size(max = 300, message = "最多 300 字，精简一下再保存") String content
    ) {
    }
}
