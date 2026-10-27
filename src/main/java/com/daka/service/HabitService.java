package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.UserContext;
import com.daka.dto.HabitDto;
import com.daka.entity.Habit;
import com.daka.entity.HabitRecord;
import com.daka.mapper.HabitMapper;
import com.daka.mapper.HabitRecordMapper;
import com.daka.vo.HabitVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HabitService {

    /** 连续天数最多回溯的天数 */
    private static final int LOOKBACK_DAYS = 400;

    private final HabitMapper habitMapper;
    private final HabitRecordMapper recordMapper;

    public HabitService(HabitMapper habitMapper, HabitRecordMapper recordMapper) {
        this.habitMapper = habitMapper;
        this.recordMapper = recordMapper;
    }

    // ------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------

    public List<HabitVo.Item> list() {
        Long uid = UserContext.requireView();
        List<Habit> habits = activeHabits(uid);
        Map<Long, Set<LocalDate>> doneMap = doneDatesByHabit(uid, LocalDate.now().minusDays(LOOKBACK_DAYS), LocalDate.now());
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        List<HabitVo.Item> items = new ArrayList<>();
        for (Habit h : habits) {
            Set<LocalDate> done = doneMap.getOrDefault(h.getId(), Set.of());
            List<Boolean> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                week.add(done.contains(monday.plusDays(i)));
            }
            items.add(new HabitVo.Item(
                    h.getId(), h.getName(), h.getColor(), h.getIconText(),
                    streak(done), done.contains(today), week));
        }
        return items;
    }

    public HabitVo.Calendar calendar(String month) {
        Long uid = UserContext.requireView();
        YearMonth ym = parseMonth(month);
        List<Habit> habits = activeHabits(uid);
        int totalHabits = habits.size();

        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        List<HabitRecord> records = recordMapper.selectList(
                Wrappers.<HabitRecord>lambdaQuery()
                        .eq(HabitRecord::getUserId, uid)
                        .eq(HabitRecord::getDone, 1)
                        .between(HabitRecord::getRecordDate, from, to));

        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (HabitRecord r : records) {
            countByDate.merge(r.getRecordDate(), 1, Integer::sum);
        }

        LocalDate today = LocalDate.now();
        List<HabitVo.DayCell> days = new ArrayList<>();
        int activeDays = 0;
        int doneTotal = 0;
        int elapsed = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            int count = countByDate.getOrDefault(date, 0);
            boolean future = date.isAfter(today);
            days.add(new HabitVo.DayCell(
                    date, d, count,
                    totalHabits == 0 ? 0 : Math.round(count * 1000.0 / totalHabits) / 1000.0,
                    future));
            if (!future) {
                elapsed++;
                doneTotal += count;
                if (count > 0) {
                    activeDays++;
                }
            }
        }

        double rate = (totalHabits == 0 || elapsed == 0)
                ? 0
                : Math.round(doneTotal * 1000.0 / (totalHabits * elapsed)) / 10.0;

        return new HabitVo.Calendar(ym.toString(), totalHabits, activeDays, rate, days);
    }

    public HabitVo.Stats stats() {
        Long uid = UserContext.requireView();
        List<Habit> habits = activeHabits(uid);
        LocalDate today = LocalDate.now();
        Map<Long, Set<LocalDate>> doneMap = doneDatesByHabit(uid, today.minusDays(LOOKBACK_DAYS), today);

        int maxStreak = 0;
        String maxStreakHabit = null;
        for (Habit h : habits) {
            int s = streak(doneMap.getOrDefault(h.getId(), Set.of()));
            if (s > maxStreak) {
                maxStreak = s;
                maxStreakHabit = h.getName();
            }
        }

        // 本月完成率
        YearMonth ym = YearMonth.from(today);
        int[] monthCount = new int[1];
        int todayDone = 0;
        Map<Long, Integer> monthCountByHabit = new HashMap<>();
        List<HabitRecord> monthRecords = recordMapper.selectList(
                Wrappers.<HabitRecord>lambdaQuery()
                        .eq(HabitRecord::getUserId, uid)
                        .eq(HabitRecord::getDone, 1)
                        .between(HabitRecord::getRecordDate, ym.atDay(1), ym.atEndOfMonth()));
        for (HabitRecord r : monthRecords) {
            monthCount[0]++;
            monthCountByHabit.merge(r.getHabitId(), 1, Integer::sum);
            if (r.getRecordDate().equals(today)) {
                todayDone++;
            }
        }
        int elapsed = today.getDayOfMonth();
        double monthRate = (habits.isEmpty() || elapsed == 0)
                ? 0
                : Math.round(monthCount[0] * 1000.0 / (habits.size() * elapsed)) / 10.0;

        int maxMonthCount = Math.max(1, habits.stream()
                .mapToInt(h -> monthCountByHabit.getOrDefault(h.getId(), 0))
                .max().orElse(1));
        List<HabitVo.RankItem> ranking = habits.stream()
                .map(h -> new HabitVo.RankItem(
                        h.getName(), h.getColor(),
                        monthCountByHabit.getOrDefault(h.getId(), 0),
                        Math.round(monthCountByHabit.getOrDefault(h.getId(), 0) * 100.0 / maxMonthCount) / 100.0))
                .sorted(Comparator.comparingInt(HabitVo.RankItem::count).reversed())
                .toList();

        return new HabitVo.Stats(
                monthRate, todayDone, habits.size(),
                maxStreak, maxStreakHabit, ranking);
    }

    /** 今日习惯清单（首页用） */
    public List<HabitVo.TodayItem> todayItems(Long uid) {
        List<Habit> habits = activeHabits(uid);
        LocalDate today = LocalDate.now();
        Map<Long, Set<LocalDate>> doneMap = doneDatesByHabit(uid, today, today);
        List<HabitVo.TodayItem> items = new ArrayList<>();
        for (Habit h : habits) {
            items.add(new HabitVo.TodayItem(
                    h.getId(), h.getName(), h.getColor(),
                    doneMap.getOrDefault(h.getId(), Set.of()).contains(today)));
        }
        return items;
    }

    /** 近 N 天每天完成的习惯数（趋势图用） */
    public List<HabitVo.DailyPoint> trend(Long uid, int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        int totalHabits = activeHabits(uid).size();

        List<HabitRecord> records = recordMapper.selectList(
                Wrappers.<HabitRecord>lambdaQuery()
                        .eq(HabitRecord::getUserId, uid)
                        .eq(HabitRecord::getDone, 1)
                        .between(HabitRecord::getRecordDate, from, today));
        Map<LocalDate, Integer> countByDate = new HashMap<>();
        for (HabitRecord r : records) {
            countByDate.merge(r.getRecordDate(), 1, Integer::sum);
        }

        List<HabitVo.DailyPoint> points = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            points.add(new HabitVo.DailyPoint(d, countByDate.getOrDefault(d, 0), totalHabits));
        }
        return points;
    }

    // ------------------------------------------------------------
    // 写操作
    // ------------------------------------------------------------

    public Habit create(HabitDto.SaveReq req) {
        Long uid = UserContext.require();
        Habit h = new Habit();
        h.setUserId(uid);
        h.setName(req.name().trim());
        h.setColor(req.color() == null ? "violet" : req.color());
        h.setIconText(req.iconText());
        h.setFrequency("daily");
        h.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        h.setArchived(0);
        habitMapper.insert(h);
        return h;
    }

    public Habit update(Long id, HabitDto.SaveReq req) {
        Long uid = UserContext.require();
        Habit h = requireOwned(uid, id);
        h.setName(req.name().trim());
        if (req.color() != null) {
            h.setColor(req.color());
        }
        if (req.iconText() != null) {
            h.setIconText(req.iconText());
        }
        if (req.sortOrder() != null) {
            h.setSortOrder(req.sortOrder());
        }
        if (req.archived() != null) {
            h.setArchived(req.archived());
        }
        habitMapper.updateById(h);
        return h;
    }

    @Transactional
    public void delete(Long id) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        recordMapper.delete(Wrappers.<HabitRecord>lambdaQuery()
                .eq(HabitRecord::getUserId, uid)
                .eq(HabitRecord::getHabitId, id));
        habitMapper.deleteById(id);
    }

    /** 打卡 / 取消打卡 / 补打卡（date 可指定过去日期） */
    @Transactional
    public boolean check(Long id, HabitDto.CheckReq req) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        LocalDate date = req.date() == null ? LocalDate.now() : req.date();
        boolean done = req.done() == null || req.done();

        HabitRecord existing = recordMapper.selectOne(
                Wrappers.<HabitRecord>lambdaQuery()
                        .eq(HabitRecord::getUserId, uid)
                        .eq(HabitRecord::getHabitId, id)
                        .eq(HabitRecord::getRecordDate, date));

        if (existing == null) {
            if (!done) {
                return false;
            }
            HabitRecord r = new HabitRecord();
            r.setUserId(uid);
            r.setHabitId(id);
            r.setRecordDate(date);
            r.setDone(1);
            recordMapper.insert(r);
            return true;
        }

        existing.setDone(done ? 1 : 0);
        recordMapper.updateById(existing);
        return done;
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------

    public List<Habit> activeHabits(Long uid) {
        return habitMapper.selectList(Wrappers.<Habit>lambdaQuery()
                .eq(Habit::getUserId, uid)
                .eq(Habit::getArchived, 0)
                .orderByAsc(Habit::getSortOrder)
                .orderByAsc(Habit::getId));
    }

    private Map<Long, Set<LocalDate>> doneDatesByHabit(Long uid, LocalDate from, LocalDate to) {
        List<HabitRecord> records = recordMapper.selectList(
                Wrappers.<HabitRecord>lambdaQuery()
                        .eq(HabitRecord::getUserId, uid)
                        .eq(HabitRecord::getDone, 1)
                        .between(HabitRecord::getRecordDate, from, to));
        Map<Long, Set<LocalDate>> map = new LinkedHashMap<>();
        for (HabitRecord r : records) {
            map.computeIfAbsent(r.getHabitId(), k -> new HashSet<>()).add(r.getRecordDate());
        }
        return map;
    }

    /** 从今天（或昨天）往前连续打卡天数 */
    private int streak(Set<LocalDate> done) {
        if (done == null || done.isEmpty()) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        LocalDate cursor = done.contains(today) ? today : today.minusDays(1);
        int n = 0;
        while (done.contains(cursor)) {
            n++;
            cursor = cursor.minusDays(1);
        }
        return n;
    }

    private Habit requireOwned(Long uid, Long id) {
        Habit h = habitMapper.selectById(id);
        if (h == null || !uid.equals(h.getUserId())) {
            throw new BizException("习惯不存在");
        }
        return h;
    }

    private YearMonth parseMonth(String month) {
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
