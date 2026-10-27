package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.entity.Bill;
import com.daka.entity.Category;
import com.daka.entity.Diary;
import com.daka.entity.Goal;
import com.daka.entity.Habit;
import com.daka.entity.HabitRecord;
import com.daka.entity.Todo;
import com.daka.mapper.BillMapper;
import com.daka.mapper.CategoryMapper;
import com.daka.mapper.DiaryMapper;
import com.daka.mapper.GoalMapper;
import com.daka.mapper.HabitMapper;
import com.daka.mapper.HabitRecordMapper;
import com.daka.mapper.TodoMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出：生成 Excel 友好的 CSV（UTF-8 带 BOM，逗号分隔）。
 */
@Service
public class ExportService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;
    private final TodoMapper todoMapper;
    private final GoalMapper goalMapper;
    private final DiaryMapper diaryMapper;
    private final HabitMapper habitMapper;
    private final HabitRecordMapper recordMapper;

    public ExportService(BillMapper billMapper, CategoryMapper categoryMapper, TodoMapper todoMapper,
                         GoalMapper goalMapper, DiaryMapper diaryMapper, HabitMapper habitMapper,
                         HabitRecordMapper recordMapper) {
        this.billMapper = billMapper;
        this.categoryMapper = categoryMapper;
        this.todoMapper = todoMapper;
        this.goalMapper = goalMapper;
        this.diaryMapper = diaryMapper;
        this.habitMapper = habitMapper;
        this.recordMapper = recordMapper;
    }

    /* ---------------- 各模块数据集 ---------------- */

    public List<List<String>> bills(Long uid, String month, Integer type) {
        YearMonth ym = parseMonth(month);
        List<Category> cats = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().eq(Category::getUserId, uid));
        Map<Long, Category> catMap = new HashMap<>();
        cats.forEach((c) -> catMap.put(c.getId(), c));

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("日期", "类型", "分类", "标题", "金额", "备注", "记录时间"));
        for (Bill b : billMapper.selectList(Wrappers.<Bill>lambdaQuery()
                .eq(Bill::getUserId, uid)
                .between(Bill::getBillDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByDesc(Bill::getBillDate).orderByDesc(Bill::getId))) {
            if (type != null && b.getType() != null && b.getType() != type) {
                continue;
            }
            Category c = catMap.get(b.getCategoryId());
            rows.add(List.of(
                    str(b.getBillDate()),
                    b.getType() != null && b.getType() == BillService.INCOME ? "收入" : "支出",
                    c == null ? "未分类" : c.getName(),
                    str(b.getTitle()),
                    str(b.getAmount()),
                    str(b.getNote()),
                    str(b.getCreateTime())));
        }
        return rows;
    }

    public List<List<String>> todos(Long uid) {
        Map<Long, Goal> goals = new HashMap<>();
        goalMapper.selectList(Wrappers.<Goal>lambdaQuery().eq(Goal::getUserId, uid))
                .forEach((g) -> goals.put(g.getId(), g));

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("内容", "标签", "截止日期", "状态", "完成时间", "关联目标", "创建时间"));
        for (Todo t : todoMapper.selectList(Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getUserId, uid)
                .orderByAsc(Todo::getDone).orderByAsc(Todo::getDueDate).orderByAsc(Todo::getId))) {
            Goal g = t.getGoalId() == null ? null : goals.get(t.getGoalId());
            rows.add(List.of(
                    str(t.getTitle()),
                    str(t.getTag()),
                    str(t.getDueDate()),
                    t.getDone() != null && t.getDone() == 1 ? "已完成" : "未完成",
                    str(t.getDoneTime()),
                    g == null ? "" : g.getTitle(),
                    str(t.getCreateTime())));
        }
        return rows;
    }

    public List<List<String>> diaries(Long uid, String month) {
        YearMonth ym = parseMonth(month);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("日期", "心情", "标签", "内容", "创建时间"));
        for (Diary d : diaryMapper.selectList(Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getUserId, uid)
                .between(Diary::getDiaryDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByDesc(Diary::getDiaryDate).orderByDesc(Diary::getId))) {
            rows.add(List.of(
                    str(d.getDiaryDate()),
                    str(d.getMood()),
                    str(d.getTags()),
                    str(d.getContent()),
                    str(d.getCreateTime())));
        }
        return rows;
    }

    public List<List<String>> habitRecords(Long uid, String month) {
        YearMonth ym = parseMonth(month);
        Map<Long, Habit> habits = new HashMap<>();
        habitMapper.selectList(Wrappers.<Habit>lambdaQuery().eq(Habit::getUserId, uid))
                .forEach((h) -> habits.put(h.getId(), h));

        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("习惯", "日期", "状态", "备注"));
        for (HabitRecord r : recordMapper.selectList(Wrappers.<HabitRecord>lambdaQuery()
                .eq(HabitRecord::getUserId, uid)
                .between(HabitRecord::getRecordDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByDesc(HabitRecord::getRecordDate))) {
            Habit h = habits.get(r.getHabitId());
            rows.add(List.of(
                    h == null ? "已删除习惯" : h.getName(),
                    str(r.getRecordDate()),
                    r.getDone() != null && r.getDone() == 1 ? "已打卡" : "未打卡",
                    str(r.getNote())));
        }
        return rows;
    }

    public List<List<String>> goals(Long uid) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("目标", "目标值", "当前值", "进度%", "单位", "截止日期", "状态", "创建时间"));
        for (Goal g : goalMapper.selectList(Wrappers.<Goal>lambdaQuery()
                .eq(Goal::getUserId, uid)
                .orderByAsc(Goal::getStatus).orderByAsc(Goal::getDeadline))) {
            BigDecimal tv = g.getTargetValue() == null ? BigDecimal.ZERO : g.getTargetValue();
            BigDecimal cv = g.getCurrentValue() == null ? BigDecimal.ZERO : g.getCurrentValue();
            double pct = tv.compareTo(BigDecimal.ZERO) > 0
                    ? cv.multiply(BigDecimal.valueOf(100)).divide(tv, 1, java.math.RoundingMode.HALF_UP).doubleValue()
                    : 0;
            rows.add(List.of(
                    str(g.getTitle()),
                    str(g.getTargetValue()),
                    str(g.getCurrentValue()),
                    String.valueOf(pct),
                    str(g.getUnit()),
                    str(g.getDeadline()),
                    goalStatus(g.getStatus()),
                    str(g.getCreateTime())));
        }
        return rows;
    }

    private String goalStatus(Integer s) {
        if (s == null) {
            return "进行中";
        }
        return switch (s) {
            case 2 -> "已完成";
            case 3 -> "已放弃";
            default -> "进行中";
        };
    }

    /* ---------------- CSV 组装 ---------------- */

    /** 生成 CSV 字节（UTF-8 带 BOM，Excel 直接打开不乱码） */
    public byte[] toCsvBytes(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escape(row.get(i)));
            }
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 下载响应头（中文文件名 RFC 5987） */
    public void setDownloadHeaders(jakarta.servlet.http.HttpServletResponse res, String filename) {
        res.setContentType("text/csv;charset=UTF-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        res.setHeader("Content-Disposition",
                "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
    }

    private String escape(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new com.daka.common.BizException("月份格式应为 yyyy-MM");
        }
    }
}
