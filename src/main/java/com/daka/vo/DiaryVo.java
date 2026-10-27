package com.daka.vo;

import java.time.LocalDate;
import java.util.List;

public class DiaryVo {

    public record MoodStat(String mood, int count) {
    }

    public record Stats(
            int monthCount,
            int totalCount,
            String topMood,
            List<MoodStat> moods,
            List<LocalDate> recordedDays
    ) {
    }
}
