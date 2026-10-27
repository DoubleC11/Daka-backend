package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.UserContext;
import com.daka.dto.BillDto;
import com.daka.entity.Bill;
import com.daka.entity.Category;
import com.daka.mapper.BillMapper;
import com.daka.mapper.CategoryMapper;
import com.daka.vo.BillVo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BillService {

    public static final int INCOME = 1;
    public static final int EXPENSE = 2;

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    public BillService(BillMapper billMapper, CategoryMapper categoryMapper) {
        this.billMapper = billMapper;
        this.categoryMapper = categoryMapper;
    }

    // ------------------------------------------------------------
    // 流水
    // ------------------------------------------------------------

    public List<Bill> list(String month, Integer type) {
        Long uid = UserContext.requireView();
        YearMonth ym = parseMonthOrNow(month);
        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .between(Bill::getBillDate, ym.atDay(1), ym.atEndOfMonth())
                .eq(type != null, Bill::getType, type)
                .orderByDesc(Bill::getBillDate)
                .orderByDesc(Bill::getId));
        fillCategory(bills);
        return bills;
    }

    public Bill create(BillDto.SaveReq req) {
        Long uid = UserContext.require();
        Category c = requireCategory(uid, req.categoryId());

        Bill b = new Bill();
        b.setUserId(uid);
        b.setCategoryId(c.getId());
        b.setType(req.type() == null ? c.getType() : req.type());
        b.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        b.setBillDate(req.billDate() == null ? LocalDate.now() : req.billDate());
        b.setTitle(req.title().trim());
        b.setNote(req.note());
        billMapper.insert(b);
        fillCategory(List.of(b));
        return b;
    }

    public Bill update(Long id, BillDto.SaveReq req) {
        Long uid = UserContext.require();
        Bill b = requireOwned(uid, id);
        Category c = requireCategory(uid, req.categoryId());

        b.setCategoryId(c.getId());
        b.setType(req.type() == null ? c.getType() : req.type());
        b.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        if (req.billDate() != null) {
            b.setBillDate(req.billDate());
        }
        b.setTitle(req.title().trim());
        b.setNote(req.note());
        billMapper.updateById(b);
        fillCategory(List.of(b));
        return b;
    }

    public void delete(Long id) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        billMapper.deleteById(id);
    }

    // ------------------------------------------------------------
    // 统计
    // ------------------------------------------------------------

    public BillVo.Summary summary(String month) {
        Long uid = UserContext.requireView();
        List<Bill> bills = monthBills(uid, parseMonthOrNow(month));

        BigDecimal income = sum(bills, INCOME);
        BigDecimal expense = sum(bills, EXPENSE);
        BigDecimal balance = income.subtract(expense);
        double saveRate = income.compareTo(BigDecimal.ZERO) > 0
                ? balance.multiply(BigDecimal.valueOf(100))
                .divide(income, 1, RoundingMode.HALF_UP).doubleValue()
                : 0;
        return new BillVo.Summary(income, expense, balance, bills.size(), saveRate);
    }

    public List<BillVo.CategoryStat> byCategory(String month, Integer type) {
        Long uid = UserContext.requireView();
        int t = type == null ? EXPENSE : type;
        List<Bill> bills = monthBills(uid, parseMonthOrNow(month)).stream()
                .filter(b -> b.getType() != null && b.getType() == t)
                .toList();

        Map<Long, BigDecimal> byCat = new LinkedHashMap<>();
        for (Bill b : bills) {
            byCat.merge(b.getCategoryId(), b.getAmount(), BigDecimal::add);
        }
        if (byCat.isEmpty()) {
            return List.of();
        }
        BigDecimal total = byCat.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<Long, Category> catMap = categoryMapper.selectList(
                        Wrappers.<Category>lambdaQuery().in(Category::getId, byCat.keySet()))
                .stream().collect(Collectors.toMap(Category::getId, c -> c));

        return byCat.entrySet().stream()
                .map(e -> {
                    Category c = catMap.get(e.getKey());
                    double ratio = total.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(total, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0;
                    return new BillVo.CategoryStat(
                            e.getKey(),
                            c == null ? "未分类" : c.getName(),
                            c == null ? "gray" : c.getColor(),
                            c == null ? "?" : c.getIconText(),
                            e.getValue(), ratio);
                })
                .sorted(Comparator.comparing(BillVo.CategoryStat::amount).reversed())
                .toList();
    }

    /** 近 N 个月收支（含当月） */
    public List<BillVo.MonthStat> monthly(int months) {
        Long uid = UserContext.requireView();
        YearMonth current = YearMonth.now();
        YearMonth from = current.minusMonths(Math.max(0, months - 1L));

        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .between(Bill::getBillDate, from.atDay(1), current.atEndOfMonth()));

        List<BillVo.MonthStat> list = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            YearMonth ym = from.plusMonths(i);
            List<Bill> monthBills = bills.stream()
                    .filter(b -> YearMonth.from(b.getBillDate()).equals(ym))
                    .toList();
            list.add(new BillVo.MonthStat(
                    ym.getYear() + "-" + String.format("%02d", ym.getMonthValue()),
                    sum(monthBills, INCOME),
                    sum(monthBills, EXPENSE)));
        }
        return list;
    }

    /** 某月每天的收支（柱状/折线用） */
    public List<BillVo.DailyStat> daily(String month) {
        Long uid = UserContext.requireView();
        YearMonth ym = parseMonthOrNow(month);
        List<Bill> bills = monthBills(uid, ym);

        Map<Integer, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (Bill b : bills) {
            BigDecimal[] arr = byDay.computeIfAbsent(b.getBillDate().getDayOfMonth(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (b.getType() != null && b.getType() == INCOME) {
                arr[0] = arr[0].add(b.getAmount());
            } else {
                arr[1] = arr[1].add(b.getAmount());
            }
        }

        List<BillVo.DailyStat> list = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            BigDecimal[] arr = byDay.get(d);
            list.add(new BillVo.DailyStat(
                    ym.getYear() + "-" + String.format("%02d", ym.getMonthValue()) + "-" + String.format("%02d", d),
                    arr == null ? BigDecimal.ZERO : arr[0],
                    arr == null ? BigDecimal.ZERO : arr[1]));
        }
        return list;
    }

    /**
     * 近 N 天的累计结余曲线（记账页「余额趋势」卡片用）
     * <p>
     * 期初结余 = 窗口首日之前的全部历史收支差；窗口内按天零填充，逐日累加 net。
     * 返回长度恒等于 days（越界钳制到 1..365）。
     */
    public List<BillVo.BalancePoint> balanceTrend(int days) {
        Long uid = UserContext.requireView();
        int n = Math.min(Math.max(days, 1), 365);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(n - 1L);

        // 期初结余：窗口之前的历史收支差
        List<Bill> before = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .lt(Bill::getBillDate, from));
        BigDecimal balance = sum(before, INCOME).subtract(sum(before, EXPENSE));

        // 窗口内按天聚合
        List<Bill> window = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .between(Bill::getBillDate, from, today));

        Map<LocalDate, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (Bill b : window) {
            BigDecimal[] arr = byDay.computeIfAbsent(b.getBillDate(),
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (b.getType() != null && b.getType() == INCOME) {
                arr[0] = arr[0].add(b.getAmount());
            } else {
                arr[1] = arr[1].add(b.getAmount());
            }
        }

        List<BillVo.BalancePoint> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate d = from.plusDays(i);
            BigDecimal[] arr = byDay.get(d);
            BigDecimal income = arr == null ? BigDecimal.ZERO : arr[0];
            BigDecimal expense = arr == null ? BigDecimal.ZERO : arr[1];
            BigDecimal net = income.subtract(expense);
            balance = balance.add(net);
            list.add(new BillVo.BalancePoint(d.toString(), income, expense, net,
                    balance.setScale(2, RoundingMode.HALF_UP)));
        }
        return list;
    }

    public List<Bill> todayBills(Long uid) {        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .eq(Bill::getBillDate, LocalDate.now())
                .orderByDesc(Bill::getId));
        fillCategory(bills);
        return bills;
    }

    public BigDecimal[] sumByType(Long uid, LocalDate date) {
        List<Bill> bills = billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .eq(Bill::getBillDate, date));
        return new BigDecimal[]{sum(bills, INCOME), sum(bills, EXPENSE)};
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------

    private List<Bill> monthBills(Long uid, YearMonth ym) {
        return billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .between(Bill::getBillDate, ym.atDay(1), ym.atEndOfMonth()));
    }

    private BigDecimal sum(List<Bill> bills, int type) {
        return bills.stream()
                .filter(b -> b.getType() != null && b.getType() == type)
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void fillCategory(List<Bill> bills) {
        if (bills == null || bills.isEmpty()) {
            return;
        }
        List<Long> ids = bills.stream().map(Bill::getCategoryId).distinct().toList();
        Map<Long, Category> map = categoryMapper.selectList(
                        Wrappers.<Category>lambdaQuery().in(Category::getId, ids))
                .stream().collect(Collectors.toMap(Category::getId, c -> c));
        for (Bill b : bills) {
            Category c = map.get(b.getCategoryId());
            if (c != null) {
                b.setCategoryName(c.getName());
                b.setCategoryColor(c.getColor());
                b.setCategoryIcon(c.getIconText());
            }
        }
    }

    private Category requireCategory(Long uid, Long categoryId) {
        Category c = categoryMapper.selectById(categoryId);
        if (c == null || (c.getUserId() != null && !c.getUserId().equals(uid))) {
            throw new BizException("分类不存在或无权使用");
        }
        return c;
    }

    private Bill requireOwned(Long uid, Long id) {
        Bill b = billMapper.selectById(id);
        if (b == null || !uid.equals(b.getUserId())) {
            throw new BizException("记账记录不存在");
        }
        return b;
    }

    private YearMonth parseMonthOrNow(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
    }
}
