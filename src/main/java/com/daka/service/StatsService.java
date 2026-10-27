package com.daka.service;

import com.daka.common.UserContext;
import com.daka.entity.Bill;
import com.daka.entity.Diary;
import com.daka.entity.Goal;
import com.daka.vo.BillVo;
import com.daka.vo.HabitVo;
import com.daka.vo.StatsVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class StatsService {

    private final HabitService habitService;
    private final DiaryService diaryService;
    private final GoalService goalService;
    private final TodoService todoService;
    private final BillService billService;

    public StatsService(HabitService habitService, DiaryService diaryService,
                        GoalService goalService, TodoService todoService, BillService billService) {
        this.habitService = habitService;
        this.diaryService = diaryService;
        this.goalService = goalService;
        this.todoService = todoService;
        this.billService = billService;
    }

    /** 首页概览 */
    public StatsVo.Overview overview() {
        Long uid = UserContext.requireView();
        LocalDate today = LocalDate.now();

        HabitVo.Stats habitStats = habitService.stats();
        List<HabitVo.TodayItem> todayHabits = habitService.todayItems(uid);
        int todayDone = (int) todayHabits.stream().filter(HabitVo.TodayItem::done).count();

        BigDecimal[] todaySum = billService.sumByType(uid, today);
        BillVo.Summary monthSummary = billService.summary(YearMonth.from(today).toString());

        Diary diary = diaryService.todayLatest(uid);

        Goal topGoal = goalService.topInProgress(uid);
        double topGoalPercent = 0;
        if (topGoal != null && topGoal.getTargetValue() != null
                && topGoal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            topGoalPercent = topGoal.getCurrentValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(topGoal.getTargetValue(), 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new StatsVo.Overview(
                today.toString(),
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA),
                greeting(),
                todayDone,
                todayHabits.size(),
                habitStats.maxStreak(),
                habitStats.maxStreakHabit(),
                habitStats.monthRate(),
                todaySum[0],
                todaySum[1],
                todaySum[0].subtract(todaySum[1]),
                monthSummary.income(),
                monthSummary.expense(),
                monthSummary.balance(),
                todayHabits,
                diary == null ? null : diary.getMood(),
                diaryService.countByDate(uid, today),
                billService.todayBills(uid),
                topGoal,
                topGoalPercent,
                todoService.counts(uid)
        );
    }

    public List<HabitVo.DailyPoint> trend(int days) {
        Long uid = UserContext.requireView();
        int d = Math.min(Math.max(days, 1), 365);
        return habitService.trend(uid, d);
    }

    public List<BillVo.MonthStat> monthlyBills(int months) {
        int m = Math.min(Math.max(months, 1), 36);
        return billService.monthly(m);
    }

    /** 近 N 天累计结余曲线 */
    public List<BillVo.BalancePoint> billTrend(int days) {
        return billService.balanceTrend(days);
    }

    /** 记账页所需的全部统计 */
    public BillVo.StatBundle billStats(String month) {
        return new BillVo.StatBundle(
                billService.summary(month),
                billService.byCategory(month, BillService.EXPENSE),
                billService.byCategory(month, BillService.INCOME),
                billService.monthly(6),
                billService.daily(month)
        );
    }

    private String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 6) {
            return "夜深了";
        }
        if (hour < 11) {
            return "早上好";
        }
        if (hour < 14) {
            return "中午好";
        }
        if (hour < 18) {
            return "下午好";
        }
        return "晚上好";
    }
}
