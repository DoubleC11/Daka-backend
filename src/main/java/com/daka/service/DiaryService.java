package com.daka.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.UserContext;
import com.daka.dto.DiaryDto;
import com.daka.entity.Diary;
import com.daka.mapper.DiaryMapper;
import com.daka.vo.DiaryVo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiaryService {

    public static final List<String> MOODS = List.of("开心", "平静", "疲惫", "低落");

    private final DiaryMapper diaryMapper;

    public DiaryService(DiaryMapper diaryMapper) {
        this.diaryMapper = diaryMapper;
    }

    /**
     * 时间轴查询。month 为空则取最近 100 条；keyword 模糊匹配内容与标签。
     */
    public List<Diary> list(String month, String keyword) {
        Long uid = UserContext.requireView();
        LambdaQueryWrapper<Diary> q = Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getUserId, uid);

        YearMonth ym = parseMonth(month);
        if (ym != null) {
            q.between(Diary::getDiaryDate, ym.atDay(1), ym.atEndOfMonth());
        }
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            q.and(w -> w.like(Diary::getContent, k).or().like(Diary::getTags, k));
        }
        q.orderByDesc(Diary::getDiaryDate).orderByDesc(Diary::getId);

        if (ym == null) {
            q.last("limit 100");
        }
        return diaryMapper.selectList(q);
    }

    public Diary create(DiaryDto.SaveReq req) {
        Long uid = UserContext.require();
        Diary d = new Diary();
        d.setUserId(uid);
        d.setDiaryDate(req.diaryDate() == null ? LocalDate.now() : req.diaryDate());
        d.setMood(normalizeMood(req.mood()));
        d.setContent(req.content().trim());
        d.setTags(req.tags());
        diaryMapper.insert(d);
        return d;
    }

    public Diary update(Long id, DiaryDto.SaveReq req) {
        Long uid = UserContext.require();
        Diary d = requireOwned(uid, id);
        if (req.diaryDate() != null) {
            d.setDiaryDate(req.diaryDate());
        }
        d.setMood(normalizeMood(req.mood()));
        d.setContent(req.content().trim());
        d.setTags(req.tags());
        diaryMapper.updateById(d);
        return d;
    }

    public void delete(Long id) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        diaryMapper.deleteById(id);
    }

    /** 某月心情分布 + 有记录的日子 */
    public DiaryVo.Stats stats(String month) {
        Long uid = UserContext.requireView();
        YearMonth ym = parseMonth(month) == null ? YearMonth.now() : parseMonth(month);

        List<Diary> monthList = diaryMapper.selectList(Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getUserId, uid)
                .between(Diary::getDiaryDate, ym.atDay(1), ym.atEndOfMonth()));

        Map<String, Integer> moodCount = new LinkedHashMap<>();
        for (String m : MOODS) {
            moodCount.put(m, 0);
        }
        List<LocalDate> recordedDays = new ArrayList<>();
        for (Diary d : monthList) {
            String mood = d.getMood() == null ? "平静" : d.getMood();
            moodCount.merge(mood, 1, Integer::sum);
            if (!recordedDays.contains(d.getDiaryDate())) {
                recordedDays.add(d.getDiaryDate());
            }
        }

        int max = moodCount.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String topMood = moodCount.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("平静");

        List<DiaryVo.MoodStat> moods = moodCount.entrySet().stream()
                .map(e -> new DiaryVo.MoodStat(e.getKey(), e.getValue()))
                .toList();

        Long total = diaryMapper.selectCount(Wrappers.<Diary>lambdaQuery().eq(Diary::getUserId, uid));

        return new DiaryVo.Stats(monthList.size(), total == null ? 0 : total.intValue(),
                topMood, moods, recordedDays);
    }

    /** 今日最新一条日记（首页用），没有则返回 null */
    public Diary todayLatest(Long uid) {
        return diaryMapper.selectOne(Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getUserId, uid)
                .eq(Diary::getDiaryDate, LocalDate.now())
                .orderByDesc(Diary::getId)
                .last("limit 1"));
    }

    public int countByDate(Long uid, LocalDate date) {
        Long c = diaryMapper.selectCount(Wrappers.<Diary>lambdaQuery()
                .eq(Diary::getUserId, uid)
                .eq(Diary::getDiaryDate, date));
        return c == null ? 0 : c.intValue();
    }

    private String normalizeMood(String mood) {
        if (mood == null || mood.isBlank()) {
            return "平静";
        }
        String m = mood.trim();
        if (!MOODS.contains(m)) {
            throw new BizException("心情只能是：" + String.join(" / ", MOODS));
        }
        return m;
    }

    private Diary requireOwned(Long uid, Long id) {
        Diary d = diaryMapper.selectById(id);
        if (d == null || !uid.equals(d.getUserId())) {
            throw new BizException("日记不存在");
        }
        return d;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
    }
}
