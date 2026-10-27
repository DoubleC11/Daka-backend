package com.daka.controller;

import com.daka.common.Result;
import com.daka.service.StatsService;
import com.daka.vo.BillVo;
import com.daka.vo.HabitVo;
import com.daka.vo.StatsVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 首页概览聚合 */
    @GetMapping("/overview")
    public Result<StatsVo.Overview> overview() {
        return Result.ok(statsService.overview());
    }

    /** 近 N 天打卡趋势 */
    @GetMapping("/trend")
    public Result<List<HabitVo.DailyPoint>> trend(@RequestParam(defaultValue = "14") int days) {
        return Result.ok(statsService.trend(days));
    }

    /** 近 N 个月收支对比 */
    @GetMapping("/monthly-bills")
    public Result<List<BillVo.MonthStat>> monthlyBills(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsService.monthlyBills(months));
    }

    /** 记账页整包统计 */
    @GetMapping("/bills")
    public Result<BillVo.StatBundle> bills(@RequestParam(required = false) String month) {
        return Result.ok(statsService.billStats(month));
    }

    /** 近 N 天累计结余曲线（记账页余额趋势卡） */
    @GetMapping("/bill-trend")
    public Result<List<BillVo.BalancePoint>> billTrend(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.billTrend(days));
    }
}
