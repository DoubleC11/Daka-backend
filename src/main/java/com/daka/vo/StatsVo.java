package com.daka.vo;

import com.daka.entity.Bill;
import com.daka.entity.Goal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StatsVo {

    /** 首页概览聚合 */
    public record Overview(
            String date,
            String weekday,
            String greeting,
            int todayDone,
            int todayTotal,
            int maxStreak,
            String maxStreakHabit,
            double monthRate,
            BigDecimal todayIncome,
            BigDecimal todayExpense,
            BigDecimal todayBalance,
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal monthBalance,
            List<HabitVo.TodayItem> todayHabits,
            String todayMood,
            int todayDiaryCount,
            List<Bill> todayBills,
            Goal topGoal,
            double topGoalPercent,
            Map<String, Object> todoCounts
    ) {
    }
}
