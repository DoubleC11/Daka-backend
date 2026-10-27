package com.daka.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class DiaryDto {

    /** diaryDate 为空时默认今天 */
    public record SaveReq(
            LocalDate diaryDate,
            String mood,
            @NotBlank(message = "日记内容不能为空") String content,
            String tags
    ) {
    }
}
