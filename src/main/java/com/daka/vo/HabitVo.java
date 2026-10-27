package com.daka.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class HabitVo {

    /** 习惯卡片：含连续天数、本周打点 */
    public record Item(
            Long id,
            String name,
            String color,
            String iconText,
            int streak,
            boolean todayDone,
            List<Boolean> weekDots
    ) {
    }

    /** 月历单元格 */
    public record DayCell(
            LocalDate date,
            int day,
            int count,
            double ratio,
            boolean future
    ) {
    }

    public record Calendar(
            String month,
            int totalHabits,
            int activeDays,
            double rate,
            List<DayCell> days
    ) {
    }

    public record RankItem(String name, String color, int count, double ratio) {
    }

    public record Stats(
            double monthRate,
            int todayDone,
            int todayTotal,
            int maxStreak,
            String maxStreakHabit,
            List<RankItem> ranking
    ) {
    }

    /** 首页概览用 */
    public record TodayItem(Long id, String name, String color, boolean done) {
    }

    public record DailyPoint(LocalDate date, int done, int total) {
    }

    public record MonthlyBill(String month, BigDecimal income, BigDecimal expense) {
    }
}
