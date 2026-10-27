package com.daka.common;

import com.daka.mapper.MetaMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据库当前时间。
 * <p>
 * 库里 create_time / update_time 这类列的默认值由 MySQL 的 CURRENT_TIMESTAMP 写入，
 * 用的是【数据库时区】；而 LocalDateTime.now() 用的是【JVM 时区】。
 * 两者在服务器上经常不一致（MySQL 容器多是 UTC，JVM 多是 Asia/Shanghai），
 * 于是「刚写入的数据」会被算成好几个小时之前——邀请码就是这样立刻过期的。
 * <p>
 * 约定：凡是写入或比较这类时间列，一律用 {@link #now()}，不要用 LocalDateTime.now()。
 */
@Component
public class DbClock {

    private final MetaMapper metaMapper;

    public DbClock(MetaMapper metaMapper) {
        this.metaMapper = metaMapper;
    }

    /** 数据库当前时间；取不到时退回 JVM 时间（不影响主流程） */
    public LocalDateTime now() {
        LocalDateTime n = metaMapper.now();
        return n != null ? n : LocalDateTime.now();
    }
}
