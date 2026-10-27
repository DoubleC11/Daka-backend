-- ============================================================
-- 打卡 + 生活记录网站 · 建库建表脚本
-- MySQL 8.0+ / utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS daka
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE daka;

-- 按外键依赖顺序删除
DROP TABLE IF EXISTS couple_calendar;
DROP TABLE IF EXISTS couple_binding;
DROP TABLE IF EXISTS bill;
DROP TABLE IF EXISTS todo;
DROP TABLE IF EXISTS goal;
DROP TABLE IF EXISTS diary;
DROP TABLE IF EXISTS habit_record;
DROP TABLE IF EXISTS habit;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS `user`;

-- ------------------------------------------------------------
-- 用户
-- ------------------------------------------------------------
CREATE TABLE `user` (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL COMMENT '登录名',
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  nickname      VARCHAR(50)  NOT NULL COMMENT '昵称',
  avatar_color  VARCHAR(20)  DEFAULT NULL COMMENT '头像渐变色标识',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ------------------------------------------------------------
-- 习惯
-- ------------------------------------------------------------
CREATE TABLE habit (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(50) NOT NULL COMMENT '习惯名称',
  color       VARCHAR(20) NOT NULL DEFAULT 'violet' COMMENT '主题色标识',
  icon_text   VARCHAR(4)  DEFAULT NULL COMMENT '图标文字',
  frequency   VARCHAR(20) NOT NULL DEFAULT 'daily' COMMENT 'daily/weekly',
  sort_order  INT         NOT NULL DEFAULT 0,
  archived    TINYINT     NOT NULL DEFAULT 0 COMMENT '0正常 1归档',
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_habit_user (user_id, archived),
  CONSTRAINT fk_habit_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='习惯';

-- ------------------------------------------------------------
-- 习惯打卡记录（一天一个习惯最多一条）
-- ------------------------------------------------------------
CREATE TABLE habit_record (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  habit_id    BIGINT UNSIGNED NOT NULL,
  record_date DATE         NOT NULL COMMENT '打卡日期',
  done        TINYINT      NOT NULL DEFAULT 1 COMMENT '1已打卡 0取消',
  note        VARCHAR(200) DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_habit_date (habit_id, record_date),
  KEY idx_hr_user_date (user_id, record_date),
  CONSTRAINT fk_hr_habit FOREIGN KEY (habit_id) REFERENCES habit (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='习惯打卡记录';

-- ------------------------------------------------------------
-- 生活日记（不做图片上传）
-- ------------------------------------------------------------
CREATE TABLE diary (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  diary_date  DATE         NOT NULL COMMENT '日记归属日期',
  mood        VARCHAR(20)  NOT NULL DEFAULT '平静',
  content     TEXT         NOT NULL,
  tags        VARCHAR(200) DEFAULT NULL COMMENT '逗号分隔标签',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_diary_user_date (user_id, diary_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生活日记';

-- ------------------------------------------------------------
-- 目标
-- ------------------------------------------------------------
CREATE TABLE goal (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id       BIGINT UNSIGNED NOT NULL,
  title         VARCHAR(100)  NOT NULL,
  target_value  DECIMAL(12,2) NOT NULL DEFAULT 100 COMMENT '目标值',
  current_value DECIMAL(12,2) NOT NULL DEFAULT 0   COMMENT '当前进度',
  unit          VARCHAR(20)   DEFAULT NULL COMMENT '单位：本/公斤/元/节',
  color         VARCHAR(20)   NOT NULL DEFAULT 'violet',
  deadline      DATE          DEFAULT NULL,
  status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1进行中 2已完成 3已放弃',
  create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_goal_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目标';

-- ------------------------------------------------------------
-- 待办
-- ------------------------------------------------------------
CREATE TABLE todo (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  goal_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '可关联目标',
  title       VARCHAR(200) NOT NULL,
  tag         VARCHAR(20)  DEFAULT NULL COMMENT '生活/工作/读书',
  due_date    DATE         DEFAULT NULL,
  done        TINYINT      NOT NULL DEFAULT 0,
  done_time   DATETIME     DEFAULT NULL,
  sort_order  INT          NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_todo_user_done (user_id, done),
  KEY idx_todo_user_due (user_id, due_date),
  CONSTRAINT fk_todo_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_todo_goal FOREIGN KEY (goal_id) REFERENCES goal (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待办';

-- ------------------------------------------------------------
-- 收支分类（user_id 为 NULL 表示系统预置，全员可用）
-- ------------------------------------------------------------
CREATE TABLE category (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED DEFAULT NULL COMMENT 'NULL=系统预置',
  name       VARCHAR(20) NOT NULL,
  type       TINYINT     NOT NULL COMMENT '1收入 2支出',
  color      VARCHAR(20) NOT NULL DEFAULT 'violet',
  icon_text  VARCHAR(4)  NOT NULL COMMENT '图标文字，如 餐/交/购',
  sort_order INT         NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_category_user (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类';

-- ------------------------------------------------------------
-- 记账流水
-- ------------------------------------------------------------
CREATE TABLE bill (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  type        TINYINT       NOT NULL COMMENT '1收入 2支出',
  amount      DECIMAL(12,2) NOT NULL COMMENT '金额，恒为正数',
  bill_date   DATE          NOT NULL,
  title       VARCHAR(100)  NOT NULL COMMENT '事项：今天做了什么/买了什么',
  note        VARCHAR(200)  DEFAULT NULL,
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_bill_user_date (user_id, bill_date),
  KEY idx_bill_user_type (user_id, type),
  CONSTRAINT fk_bill_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_bill_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='记账流水';

-- ------------------------------------------------------------
-- 预置分类
-- ------------------------------------------------------------
INSERT INTO category (user_id, name, type, color, icon_text, sort_order) VALUES
  (NULL, '餐饮', 2, 'violet', '餐', 1),
  (NULL, '交通', 2, 'sky',    '交', 2),
  (NULL, '购物', 2, 'orange', '购', 3),
  (NULL, '居住', 2, 'teal',   '住', 4),
  (NULL, '医疗', 2, 'rose',   '医', 5),
  (NULL, '娱乐', 2, 'pink',   '娱', 6),
  (NULL, '学习', 2, 'amber',  '学', 7),
  (NULL, '工资', 1, 'emerald','薪', 8),
  (NULL, '兼职', 1, 'cyan',   '兼', 9),
  (NULL, '理财', 1, 'indigo', '理', 10),
  (NULL, '红包', 1, 'rose',   '红', 11);

-- ------------------------------------------------------------
-- 情侣绑定（status：1待接受 2已绑定 3已解除）
-- ------------------------------------------------------------
CREATE TABLE couple_binding (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_a_id   BIGINT UNSIGNED NOT NULL COMMENT '发起方',
  user_b_id   BIGINT UNSIGNED DEFAULT NULL COMMENT '接受方，待接受时为 NULL',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1待接受 2已绑定 3已解除',
  invite_code VARCHAR(10)  NOT NULL COMMENT '邀请码',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  bind_time   DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invite_code (invite_code),
  KEY idx_cb_a (user_a_id, status),
  KEY idx_cb_b (user_b_id, status),
  CONSTRAINT fk_cb_user_a FOREIGN KEY (user_a_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_cb_user_b FOREIGN KEY (user_b_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣绑定';

-- ------------------------------------------------------------
-- 共同日历（情侣空间右侧栏）：一天一条，双方都能改
-- user_a_id / user_b_id 存「较小 id / 较大 id」，同一对用户只有一份日历，
-- 解绑后重新绑定同一人也能找回以前的内容
-- ------------------------------------------------------------
CREATE TABLE couple_calendar (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_a_id   BIGINT UNSIGNED NOT NULL COMMENT '配对中较小的用户 id',
  user_b_id   BIGINT UNSIGNED NOT NULL COMMENT '配对中较大的用户 id',
  event_date  DATE          NOT NULL COMMENT '哪天',
  content     VARCHAR(300)  NOT NULL COMMENT '这天干了什么',
  editor_id   BIGINT UNSIGNED NOT NULL COMMENT '最后编辑人',
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cc_pair_day (user_a_id, user_b_id, event_date),
  KEY idx_cc_editor (editor_id),
  CONSTRAINT fk_cc_user_a FOREIGN KEY (user_a_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_cc_user_b FOREIGN KEY (user_b_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_cc_editor FOREIGN KEY (editor_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣共同日历';
