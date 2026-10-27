package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.DbClock;
import com.daka.common.UserContext;
import com.daka.dto.CoupleDto;
import com.daka.entity.CoupleCalendar;
import com.daka.entity.User;
import com.daka.mapper.CoupleCalendarMapper;
import com.daka.mapper.UserMapper;
import com.daka.vo.CoupleVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 共同日历：绑定后右侧栏那块「哪天干了什么」。
 * <p>
 * 数据按「用户对」共享（较小 id + 较大 id），因此双方读到的是同一份，
 * 谁都能改谁写的条目——这是本模块和只读视图其它数据的关键区别。
 * 查询、修改都基于 {@link UserContext#require()}，不受 viewUserId 影响。
 */
@Service
public class CoupleCalendarService {

    /** 单日内容上限，与库字段长度一致 */
    private static final int MAX_CONTENT = 300;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CoupleCalendarMapper calendarMapper;
    private final CoupleService coupleService;
    private final UserMapper userMapper;
    private final DbClock dbClock;

    public CoupleCalendarService(CoupleCalendarMapper calendarMapper,
                                 CoupleService coupleService,
                                 UserMapper userMapper,
                                 DbClock dbClock) {
        this.calendarMapper = calendarMapper;
        this.coupleService = coupleService;
        this.userMapper = userMapper;
        this.dbClock = dbClock;
    }

    /** 某个月的记录；month 为空时取当前月 */
    public CoupleVo.CalendarMonth month(String month) {
        Long me = UserContext.require();
        Long other = coupleService.requirePartnerId(me);
        long a = Math.min(me, other);
        long b = Math.max(me, other);
        YearMonth ym = parseMonth(month);

        List<CoupleCalendar> rows = calendarMapper.selectList(Wrappers.<CoupleCalendar>lambdaQuery()
                .eq(CoupleCalendar::getUserAId, a)
                .eq(CoupleCalendar::getUserBId, b)
                .between(CoupleCalendar::getEventDate, ym.atDay(1), ym.atEndOfMonth())
                .orderByAsc(CoupleCalendar::getEventDate));

        return new CoupleVo.CalendarMonth(ym.toString(), rows.size(), toVos(rows));
    }

    /**
     * 写/改某一天。content 为空表示清空这一天（等同于删除）。
     * 返回保存后的记录；清空时返回 null。
     */
    public CoupleVo.CalendarDay save(CoupleDto.CalendarSaveReq req) {
        Long me = UserContext.require();
        Long other = coupleService.requirePartnerId(me);
        long a = Math.min(me, other);
        long b = Math.max(me, other);
        LocalDate day = req.date();

        String content = req.content() == null ? "" : req.content().trim();
        CoupleCalendar exist = findOne(a, b, day);

        if (content.isEmpty()) {
            if (exist != null) {
                calendarMapper.deleteById(exist.getId());
            }
            return null;
        }
        if (content.length() > MAX_CONTENT) {
            throw new BizException("最多 " + MAX_CONTENT + " 字，精简一下再保存");
        }

        if (exist == null) {
            CoupleCalendar row = new CoupleCalendar();
            row.setUserAId(a);
            row.setUserBId(b);
            row.setEventDate(day);
            row.setContent(content);
            row.setEditorId(me);
            LocalDateTime now = dbClock.now();
            row.setCreateTime(now);
            row.setUpdateTime(now);
            try {
                calendarMapper.insert(row);
            } catch (DuplicateKeyException e) {
                // 两边几乎同时写同一天：唯一索引挡住，退化成更新
                CoupleCalendar again = findOne(a, b, day);
                if (again == null) {
                    throw new BizException("保存失败，请重试");
                }
                again.setContent(content);
                again.setEditorId(me);
                again.setUpdateTime(dbClock.now());
                calendarMapper.updateById(again);
                return toVo(again);
            }
            return toVo(row);
        }

        exist.setContent(content);
        exist.setEditorId(me);
        exist.setUpdateTime(dbClock.now());
        calendarMapper.updateById(exist);
        return toVo(exist);
    }

    /** 删掉某一天；没有记录也不报错 */
    public void delete(LocalDate day) {
        if (day == null) {
            throw new BizException("请选择日期");
        }
        Long me = UserContext.require();
        Long other = coupleService.requirePartnerId(me);
        calendarMapper.delete(Wrappers.<CoupleCalendar>lambdaQuery()
                .eq(CoupleCalendar::getUserAId, Math.min(me, other))
                .eq(CoupleCalendar::getUserBId, Math.max(me, other))
                .eq(CoupleCalendar::getEventDate, day));
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------

    private CoupleCalendar findOne(long a, long b, LocalDate day) {
        return calendarMapper.selectOne(Wrappers.<CoupleCalendar>lambdaQuery()
                .eq(CoupleCalendar::getUserAId, a)
                .eq(CoupleCalendar::getUserBId, b)
                .eq(CoupleCalendar::getEventDate, day)
                .last("limit 1"));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new BizException("月份格式应为 yyyy-MM");
        }
    }

    /** 批量补上编辑人名字，避免逐条查库 */
    private List<CoupleVo.CalendarDay> toVos(List<CoupleCalendar> rows) {
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = rows.stream().map(CoupleCalendar::getEditorId).distinct().collect(Collectors.toList());
        Map<Long, String> names = new HashMap<>();
        for (User u : userMapper.selectBatchIds(ids)) {
            String name = u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname();
            names.put(u.getId(), name);
        }
        List<CoupleVo.CalendarDay> list = new ArrayList<>(rows.size());
        for (CoupleCalendar c : rows) {
            list.add(toVo(c, names));
        }
        return list;
    }

    private CoupleVo.CalendarDay toVo(CoupleCalendar c) {
        return toVo(c, Map.of());
    }

    private CoupleVo.CalendarDay toVo(CoupleCalendar c, Map<Long, String> names) {
        String editor = names.get(c.getEditorId());
        if (editor == null && c.getEditorId() != null) {
            User u = userMapper.selectById(c.getEditorId());
            if (u != null) {
                editor = u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname();
            }
        }
        String stamp = c.getUpdateTime() == null ? "" : c.getUpdateTime().format(STAMP);
        return new CoupleVo.CalendarDay(
                c.getEventDate().toString(),
                c.getContent(),
                c.getEditorId(),
                editor,
                stamp);
    }
}
