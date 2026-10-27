package com.daka.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.DbClock;
import com.daka.common.UserContext;
import com.daka.dto.TodoDto;
import com.daka.entity.Todo;
import com.daka.mapper.GoalMapper;
import com.daka.mapper.TodoMapper;
import com.daka.entity.Goal;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final GoalMapper goalMapper;
    private final DbClock dbClock;

    public TodoService(TodoMapper todoMapper, GoalMapper goalMapper, DbClock dbClock) {
        this.todoMapper = todoMapper;
        this.goalMapper = goalMapper;
        this.dbClock = dbClock;
    }

    /**
     * scope: today / week / all（未完成） / done（已完成）
     */
    public List<Todo> list(String scope) {
        Long uid = UserContext.requireView();
        LocalDate today = LocalDate.now();
        String s = scope == null ? "today" : scope;

        LambdaQueryWrapper<Todo> q = Wrappers.<Todo>lambdaQuery().eq(Todo::getUserId, uid);
        switch (s) {
            case "done" -> q.eq(Todo::getDone, 1).orderByDesc(Todo::getDoneTime).last("limit 50");
            case "all" -> q.eq(Todo::getDone, 0).orderByAsc(Todo::getSortOrder).orderByAsc(Todo::getId);
            case "week" -> {
                LocalDate sunday = today.with(DayOfWeek.SUNDAY);
                q.eq(Todo::getDone, 0)
                        .isNotNull(Todo::getDueDate)
                        .between(Todo::getDueDate, today, sunday)
                        .orderByAsc(Todo::getDueDate)
                        .orderByAsc(Todo::getId);
            }
            default -> q.eq(Todo::getDone, 0)
                    // 今天到期的，以及没设期的
                    .and(w -> w.le(Todo::getDueDate, today).or().isNull(Todo::getDueDate))
                    .orderByAsc(Todo::getSortOrder)
                    .orderByAsc(Todo::getId);
        }
        return todoMapper.selectList(q);
    }

    /** 今日 / 本周的完成情况统计 */
    public Map<String, Object> counts(Long uid) {
        LocalDate today = LocalDate.now();
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        long todayTotal = count(uid, Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getUserId, uid)
                .and(w -> w.le(Todo::getDueDate, today).or().isNull(Todo::getDueDate)));
        long todayDone = count(uid, Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getUserId, uid)
                .eq(Todo::getDone, 1)
                .and(w -> w.le(Todo::getDueDate, today).or().isNull(Todo::getDueDate)));
        long weekTotal = count(uid, Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getUserId, uid)
                .isNotNull(Todo::getDueDate)
                .between(Todo::getDueDate, today, sunday));
        long weekDone = count(uid, Wrappers.<Todo>lambdaQuery()
                .eq(Todo::getUserId, uid)
                .eq(Todo::getDone, 1)
                .isNotNull(Todo::getDueDate)
                .between(Todo::getDueDate, today, sunday));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayTotal", todayTotal);
        m.put("todayDone", todayDone);
        m.put("weekTotal", weekTotal);
        m.put("weekDone", weekDone);
        return m;
    }

    public Todo create(TodoDto.SaveReq req) {
        Long uid = UserContext.require();
        Todo t = new Todo();
        t.setUserId(uid);
        t.setTitle(req.title().trim());
        t.setTag(req.tag());
        t.setDueDate(req.dueDate() == null ? LocalDate.now() : req.dueDate());
        t.setGoalId(checkGoal(uid, req.goalId()));
        t.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        t.setDone(0);
        todoMapper.insert(t);
        return t;
    }

    /** goalId 归属校验，避免脏 id 触发外键 500 */
    private Long checkGoal(Long uid, Long goalId) {
        if (goalId == null) {
            return null;
        }
        Goal g = goalMapper.selectById(goalId);
        if (g == null || (g.getUserId() != null && !g.getUserId().equals(uid))) {
            throw new BizException("关联的目标不存在，请重新选择");
        }
        return goalId;
    }

    public Todo update(Long id, TodoDto.SaveReq req) {
        Long uid = UserContext.require();
        Todo t = requireOwned(uid, id);
        t.setTitle(req.title().trim());
        if (req.tag() != null) {
            t.setTag(req.tag());
        }
        if (req.dueDate() != null) {
            t.setDueDate(req.dueDate());
        }
        if (req.goalId() != null) {
            t.setGoalId(checkGoal(uid, req.goalId()));
        }
        if (req.sortOrder() != null) {
            t.setSortOrder(req.sortOrder());
        }
        todoMapper.updateById(t);
        return t;
    }

    /** 勾选 / 取消勾选 */
    public Todo toggle(Long id) {
        Long uid = UserContext.require();
        Todo t = requireOwned(uid, id);
        boolean nowDone = t.getDone() == null || t.getDone() == 0;
        t.setDone(nowDone ? 1 : 0);
        t.setDoneTime(nowDone ? dbClock.now() : null);
        todoMapper.updateById(t);
        return t;
    }

    public void delete(Long id) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        todoMapper.deleteById(id);
    }

    private long count(Long uid, LambdaQueryWrapper<Todo> q) {
        Long c = todoMapper.selectCount(q);
        return c == null ? 0 : c;
    }

    private Todo requireOwned(Long uid, Long id) {
        Todo t = todoMapper.selectById(id);
        if (t == null || !uid.equals(t.getUserId())) {
            throw new BizException("待办不存在");
        }
        return t;
    }
}
