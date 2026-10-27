package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.UserContext;
import com.daka.dto.GoalDto;
import com.daka.entity.Goal;
import com.daka.mapper.GoalMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GoalService {

    private final GoalMapper goalMapper;

    public GoalService(GoalMapper goalMapper) {
        this.goalMapper = goalMapper;
    }

    public List<Goal> list() {
        Long uid = UserContext.requireView();
        return goalMapper.selectList(Wrappers.<Goal>lambdaQuery()
                .eq(Goal::getUserId, uid)
                .ne(Goal::getStatus, 3)
                .orderByAsc(Goal::getStatus)
                .orderByAsc(Goal::getId));
    }

    public Goal create(GoalDto.SaveReq req) {
        Long uid = UserContext.require();
        Goal g = new Goal();
        g.setUserId(uid);
        g.setTitle(req.title().trim());
        g.setTargetValue(req.targetValue() == null ? new BigDecimal("100") : req.targetValue());
        g.setCurrentValue(req.currentValue() == null ? BigDecimal.ZERO : req.currentValue());
        g.setUnit(req.unit());
        g.setColor(req.color() == null ? "violet" : req.color());
        g.setDeadline(req.deadline());
        g.setStatus(1);
        goalMapper.insert(g);
        return g;
    }

    public Goal update(Long id, GoalDto.SaveReq req) {
        Long uid = UserContext.require();
        Goal g = requireOwned(uid, id);
        g.setTitle(req.title().trim());
        if (req.targetValue() != null) {
            g.setTargetValue(req.targetValue());
        }
        if (req.currentValue() != null) {
            g.setCurrentValue(req.currentValue());
            // 进度达到目标自动标记完成
            if (g.getTargetValue() != null
                    && g.getCurrentValue().compareTo(g.getTargetValue()) >= 0
                    && g.getStatus() == 1) {
                g.setStatus(2);
            }
        }
        if (req.unit() != null) {
            g.setUnit(req.unit());
        }
        if (req.color() != null) {
            g.setColor(req.color());
        }
        if (req.deadline() != null) {
            g.setDeadline(req.deadline());
        }
        if (req.status() != null) {
            g.setStatus(req.status());
        }
        goalMapper.updateById(g);
        return g;
    }

    public void delete(Long id) {
        Long uid = UserContext.require();
        requireOwned(uid, id);
        goalMapper.deleteById(id);
    }

    /** 首页取进度最快的一个进行中目标 */
    public Goal topInProgress(Long uid) {
        return goalMapper.selectOne(Wrappers.<Goal>lambdaQuery()
                .eq(Goal::getUserId, uid)
                .eq(Goal::getStatus, 1)
                .orderByDesc(Goal::getId)
                .last("limit 1"));
    }

    private Goal requireOwned(Long uid, Long id) {
        Goal g = goalMapper.selectById(id);
        if (g == null || !uid.equals(g.getUserId())) {
            throw new BizException("目标不存在");
        }
        return g;
    }
}
