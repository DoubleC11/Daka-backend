package com.daka.vo;

import java.math.BigDecimal;
import java.util.List;

public class BillVo {

    public record Summary(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal balance,
            int count,
            double saveRate
    ) {
    }

    public record CategoryStat(
            Long categoryId,
            String name,
            String color,
            String iconText,
            BigDecimal amount,
            double ratio
    ) {
    }

    public record MonthStat(String month, BigDecimal income, BigDecimal expense) {
    }

    public record DailyStat(String date, BigDecimal income, BigDecimal expense) {
    }

    /**
     * 余额趋势的一个数据点（按天）
     *
     * @param date    日期 yyyy-MM-dd
     * @param income  当天收入合计（无数据为 0）
     * @param expense 当天支出合计（无数据为 0）
     * @param net     当天净额 = income - expense
     * @param balance 累计结余 = 期初结余 + 窗口内逐日 net 累加
     */
    public record BalancePoint(
            String date,
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            BigDecimal balance
    ) {
    }

    public record StatBundle(
            Summary summary,
            List<CategoryStat> expenseByCategory,
            List<CategoryStat> incomeByCategory,
            List<MonthStat> monthly,
            List<DailyStat> daily
    ) {
    }
}
