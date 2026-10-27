package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.UserContext;
import com.daka.dto.CategoryDto;
import com.daka.entity.Bill;
import com.daka.entity.Category;
import com.daka.mapper.BillMapper;
import com.daka.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final BillMapper billMapper;

    public CategoryService(CategoryMapper categoryMapper, BillMapper billMapper) {
        this.categoryMapper = categoryMapper;
        this.billMapper = billMapper;
    }

    /** 系统预置（user_id 为 null）+ 当前用户自定义 */
    public List<Category> list(Integer type) {
        Long uid = UserContext.require();
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .and(w -> w.isNull(Category::getUserId).or().eq(Category::getUserId, uid))
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
    }

    public Category create(CategoryDto.SaveReq req) {
        Long uid = UserContext.require();
        Long dup = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getUserId, uid)
                .eq(Category::getName, req.name().trim())
                .eq(Category::getType, req.type()));
        if (dup != null && dup > 0) {
            throw new BizException("已存在同名分类");
        }

        Category c = new Category();
        c.setUserId(uid);
        c.setName(req.name().trim());
        c.setType(req.type());
        c.setColor(req.color() == null ? "violet" : req.color());
        c.setIconText(req.iconText().trim());
        c.setSortOrder(90);
        categoryMapper.insert(c);
        return c;
    }

    public void delete(Long id) {
        Long uid = UserContext.require();
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException("分类不存在");
        }
        if (c.getUserId() == null) {
            throw new BizException("系统预置分类不可删除");
        }
        if (!uid.equals(c.getUserId())) {
            throw new BizException("无权操作该分类");
        }
        Long used = billMapper.selectCount(Wrappers.<Bill>lambdaQuery().eq(Bill::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BizException("该分类下还有 " + used + " 条记账记录，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    /** 分类 id -> 分类，用于给流水平铺分类信息 */
    public Map<Long, Category> mapByIds(List<Long> ids) {
        Map<Long, Category> map = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return map;
        }
        List<Category> list = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().in(Category::getId, ids));
        for (Category c : list) {
            map.put(c.getId(), c);
        }
        return map;
    }
}
