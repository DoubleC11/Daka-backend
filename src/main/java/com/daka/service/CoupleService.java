package com.daka.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.daka.common.BizException;
import com.daka.common.DbClock;
import com.daka.common.UserContext;
import com.daka.entity.CoupleBinding;
import com.daka.entity.User;
import com.daka.mapper.CoupleBindingMapper;
import com.daka.mapper.UserMapper;
import com.daka.vo.CoupleVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 情侣空间：邀请码绑定 + 关系维护。
 * 绑定只影响「能否查看对方数据」，不改动任何业务数据的归属。
 */
@Service
public class CoupleService {

    public static final int PENDING = 1;
    public static final int ACTIVE = 2;
    public static final int RELEASED = 3;

    /** 邀请码有效期（分钟） */
    private static final long INVITE_TTL_MINUTES = 30;
    /** 去掉易混淆的 I/O/0/1 */
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CoupleBindingMapper coupleMapper;
    private final UserMapper userMapper;
    private final DbClock dbClock;

    public CoupleService(CoupleBindingMapper coupleMapper, UserMapper userMapper, DbClock dbClock) {
        this.coupleMapper = coupleMapper;
        this.userMapper = userMapper;
        this.dbClock = dbClock;
    }

    /**
     * 查看当前有效的邀请码，没有则返回 null（不生成新码）。
     * 用于页面刷新后仍能看到自己还没被使用的邀请码。
     */
    public CoupleVo.Invite currentInvite() {
        Long uid = UserContext.require();
        // 已绑定的人没有“待接受”的邀请，直接返回 null，不报错
        Long bound = coupleMapper.selectCount(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getStatus, ACTIVE)
                .and(w -> w.eq(CoupleBinding::getUserAId, uid).or().eq(CoupleBinding::getUserBId, uid)));
        if (bound != null && bound > 0) {
            return null;
        }
        LocalDateTime dbNow = dbNow();
        CoupleBinding b = latestPending(uid, dbNow);
        return b == null ? null : toInvite(b, dbNow);
    }

    /**
     * 生成邀请码。
     * refresh=false：已有有效邀请码时直接复用（首次点“生成”不会让旧码失效）；
     * refresh=true：作废旧的待接受邀请，生成全新的码（“重新生成”按钮走这里）。
     */
    public CoupleVo.Invite invite(boolean refresh) {
        Long uid = UserContext.require();
        requireNotBound(uid);
        LocalDateTime dbNow = dbNow();

        if (!refresh) {
            CoupleBinding exist = latestPending(uid, dbNow);
            if (exist != null) {
                return toInvite(exist, dbNow);
            }
        } else {
            // 作废旧码，避免“重新生成”后旧码仍可被使用
            coupleMapper.update(null, Wrappers.<CoupleBinding>lambdaUpdate()
                    .eq(CoupleBinding::getUserAId, uid)
                    .eq(CoupleBinding::getStatus, PENDING)
                    .set(CoupleBinding::getStatus, RELEASED));
        }

        // 唯一索引 uk_invite_code 是硬约束，极端并发下插入可能冲突，重试几次
        for (int attempt = 0; attempt < 5; attempt++) {
            CoupleBinding b = new CoupleBinding();
            b.setUserAId(uid);
            b.setStatus(PENDING);
            b.setInviteCode(newUniqueCode());
            // 显式写入数据库时间：否则实体里是 null，算剩余时间时只能用 JVM 时间兜底，
            // 一旦两端时区不一致，页面显示的剩余时间就是错的
            b.setCreateTime(dbNow);
            try {
                coupleMapper.insert(b);
                return toInvite(b, dbNow);
            } catch (DuplicateKeyException e) {
                // 换一个码再试
            }
        }
        throw new BizException("邀请码生成失败，请稍后重试");
    }

    /** 取自己最新的一条待接受邀请，顺手清掉过期的 */
    private CoupleBinding latestPending(Long uid, LocalDateTime dbNow) {
        coupleMapper.delete(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getUserAId, uid)
                .eq(CoupleBinding::getStatus, PENDING)
                .lt(CoupleBinding::getCreateTime, dbNow.minusMinutes(INVITE_TTL_MINUTES)));

        List<CoupleBinding> pending = coupleMapper.selectList(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getUserAId, uid)
                .eq(CoupleBinding::getStatus, PENDING)
                .orderByDesc(CoupleBinding::getId));
        return pending.isEmpty() ? null : pending.get(0);
    }

    /** 凭邀请码绑定 */
    public CoupleVo.Info bind(String code) {
        Long me = UserContext.require();
        requireNotBound(me);
        if (code == null || code.isBlank()) {
            throw new BizException("请输入邀请码");
        }
        String c = code.trim().toUpperCase();

        CoupleBinding b = coupleMapper.selectOne(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getInviteCode, c)
                .eq(CoupleBinding::getStatus, PENDING)
                .last("limit 1"));
        if (b == null) {
            throw new BizException("邀请码无效或已被使用");
        }
        // 与 create_time 一样用数据库时间做基准，避免 JVM / MySQL 时区不一致时误判过期
        LocalDateTime dbNow = dbNow();
        if (b.getCreateTime() == null
                || b.getCreateTime().plusMinutes(INVITE_TTL_MINUTES).isBefore(dbNow)) {
            throw new BizException("邀请码已过期，请让对方重新生成");
        }
        Long inviter = b.getUserAId();
        if (inviter.equals(me)) {
            throw new BizException("这是你自己的邀请码，请把它发给对方，由对方输入完成绑定");
        }
        // 发起方可能在此期间已被别人绑定
        requireNotBound(inviter);

        b.setUserBId(me);
        b.setStatus(ACTIVE);
        b.setBindTime(dbNow);
        coupleMapper.updateById(b);

        // 双方剩下的待接受邀请一并作废，避免误用旧码
        coupleMapper.update(null, Wrappers.<CoupleBinding>lambdaUpdate()
                .eq(CoupleBinding::getStatus, PENDING)
                .in(CoupleBinding::getUserAId, me, inviter)
                .set(CoupleBinding::getStatus, RELEASED));

        return buildInfo(b, me);
    }

    /** 当前绑定信息，未绑定返回 null */
    public CoupleVo.Info info() {
        Long uid = UserContext.require();
        CoupleBinding b = activeBinding(uid);
        return b == null ? null : buildInfo(b, uid);
    }

    /** 解除绑定，双方均可发起 */
    public void unbind() {
        Long uid = UserContext.require();
        CoupleBinding b = activeBinding(uid);
        if (b == null) {
            throw new BizException("当前没有绑定关系");
        }
        b.setStatus(RELEASED);
        coupleMapper.updateById(b);
    }

    /**
     * 当前生效绑定中的对方 id。未绑定时抛业务异常。
     * 共同日历等「双方共享」的功能用它确定另一端是谁。
     */
    public Long requirePartnerId(Long uid) {
        CoupleBinding b = activeBinding(uid);
        if (b == null) {
            throw new BizException("还没有绑定伙伴，绑定后才能一起记录");
        }
        Long partner = uid.equals(b.getUserAId()) ? b.getUserBId() : b.getUserAId();
        if (partner == null) {
            throw new BizException("还没有绑定伙伴，绑定后才能一起记录");
        }
        return partner;
    }

    /** 两人是否存在生效绑定（视图拦截器校验用） */
    public boolean isBound(Long a, Long b) {
        if (a == null || b == null) {
            return false;
        }
        Long c = coupleMapper.selectCount(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getStatus, ACTIVE)
                .and(w -> w.eq(CoupleBinding::getUserAId, a).eq(CoupleBinding::getUserBId, b)
                        .or().eq(CoupleBinding::getUserAId, b).eq(CoupleBinding::getUserBId, a)));
        return c != null && c > 0;
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------

    private CoupleBinding activeBinding(Long uid) {
        return coupleMapper.selectOne(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getStatus, ACTIVE)
                .and(w -> w.eq(CoupleBinding::getUserAId, uid).or().eq(CoupleBinding::getUserBId, uid))
                .last("limit 1"));
    }

    private void requireNotBound(Long uid) {
        Long c = coupleMapper.selectCount(Wrappers.<CoupleBinding>lambdaQuery()
                .eq(CoupleBinding::getStatus, ACTIVE)
                .and(w -> w.eq(CoupleBinding::getUserAId, uid).or().eq(CoupleBinding::getUserBId, uid)));
        if (c != null && c > 0) {
            throw new BizException("你已绑定伙伴，请先解除绑定");
        }
    }

    private CoupleVo.Info buildInfo(CoupleBinding b, Long me) {
        Long partnerId = me.equals(b.getUserAId()) ? b.getUserBId() : b.getUserAId();
        if (partnerId == null) {
            return null;
        }
        User u = userMapper.selectById(partnerId);
        if (u == null) {
            return null;
        }
        // bind_time 是 DB 时区写入的，天数必须同基准比较，否则服务器时区不一致时「在一起 N 天」会差一天
        LocalDateTime now = dbNow();
        LocalDateTime bindTime = b.getBindTime() == null ? now : b.getBindTime();
        int days = (int) ChronoUnit.DAYS.between(bindTime.toLocalDate(), now.toLocalDate()) + 1;
        return new CoupleVo.Info(
                b.getId(),
                new CoupleVo.Partner(u.getId(), u.getNickname(), u.getUsername(), u.getAvatarColor()),
                bindTime.withNano(0).toString(),
                Math.max(days, 1));
    }

    private CoupleVo.Invite toInvite(CoupleBinding b, LocalDateTime dbNow) {
        LocalDateTime base = b.getCreateTime() == null ? dbNow : b.getCreateTime();
        LocalDateTime deadline = base.plusMinutes(INVITE_TTL_MINUTES);
        long millis = ChronoUnit.MILLIS.between(dbNow, deadline);
        // 封顶，避免 DB 与 JVM 毫秒级时钟差导致首帧显示 30:01
        int sec = (int) Math.min(Math.max(Math.ceil(millis / 1000.0), 0), INVITE_TTL_MINUTES * 60);
        int minutes = (int) Math.max(Math.ceil(sec / 60.0), 1);
        // 失效时刻：以本机真实时间戳为基准 + 剩余秒数。
        // 不能直接拿 deadline 转 epoch——那会掺入 JVM 时区，服务器时区配错时前端会算出负数。
        long expireAt = System.currentTimeMillis() + sec * 1000L;
        return new CoupleVo.Invite(b.getInviteCode(), minutes, sec, expireAt);
    }

    /** 与 create_time 同一时区空间的“现在” */
    private LocalDateTime dbNow() {
        return dbClock.now();
    }

    /** 生成一个当前库里不存在的邀请码 */
    private String newUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String code = randomCode();
            Long used = coupleMapper.selectCount(Wrappers.<CoupleBinding>lambdaQuery()
                    .eq(CoupleBinding::getInviteCode, code));
            if (used == null || used == 0) {
                return code;
            }
        }
        throw new BizException("邀请码生成失败，请稍后重试");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
