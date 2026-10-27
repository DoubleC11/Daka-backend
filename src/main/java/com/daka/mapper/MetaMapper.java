package com.daka.mapper;

import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 数据库元信息查询。
 * 只放与具体业务表无关的语句。
 */
public interface MetaMapper {

    @Select("SELECT NOW()")
    LocalDateTime now();
}
