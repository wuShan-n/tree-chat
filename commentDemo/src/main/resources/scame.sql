-- ===============================================
-- 评论模块：自包含数据库（仅本模块表）
-- ===============================================

-- 扩展（一次性）
CREATE EXTENSION IF NOT EXISTS ltree;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- gen_random_uuid()


-- ================== 枚举类型（仅模块内使用） ==================
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'comment_status') THEN
            CREATE TYPE comment_status AS ENUM ('published','pending','hidden','deleted','spam');
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'reaction_type') THEN
            CREATE TYPE reaction_type AS ENUM ('up','down','emoji');
        END IF;
    END$$;

-- ================== 主体注册表（模块对外最小契约） ==================
-- 外部系统将业务对象（文章、视频、Issue…）注册为 subject。
-- subject_key 采用可读键（如 'post:12345' / 'video:abc' / 'issue:42'），仅本模块使用，不外键到外部模块。
CREATE TABLE comment_subject (
                                             subject_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             subject_key        CITEXT NOT NULL UNIQUE,       -- 业务方自定义的稳定键
                                             is_locked          BOOLEAN NOT NULL DEFAULT FALSE,
                                             is_archived        BOOLEAN NOT NULL DEFAULT FALSE,
                                             policy             JSONB   NOT NULL DEFAULT '{}'::jsonb,   -- 本模块策略（预审/最大深度等）
                                             comment_count      INT     NOT NULL DEFAULT 0,   -- 总评论数（含软删）
                                             visible_count      INT     NOT NULL DEFAULT 0,   -- 可见评论数（published）
                                             last_commented_at  TIMESTAMPTZ,
                                             created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ON comment_subject (last_commented_at DESC);

-- ================== 评论表（ltree 物化路径 + 模块内自洽） ==================
CREATE TABLE comment (
                                     id              BIGSERIAL PRIMARY KEY,
                                     subject_id      UUID NOT NULL REFERENCES comment_subject(subject_id) ON DELETE CASCADE,

    -- 层级结构（仅模块内自引用）
                                     root_id         BIGINT,
                                     parent_id       BIGINT,
                                     path            LTREE NOT NULL,

    -- 作者标识（不外键到用户模块，交由 API 约束）
                                     author_id       BIGINT NOT NULL,                 -- 外部用户的数值ID
                                     author_urn      CITEXT,                          -- 可选：'user:123' / 'org:u42' 解决多源冲突

    -- 内容与状态
                                     body_md         TEXT   NOT NULL,                 -- markdown/纯文本
                                     body_html       TEXT,                            -- 经过服务端白名单清洗后的 HTML（可选）
                                     status          comment_status NOT NULL DEFAULT 'published',
                                     toxicity_score  NUMERIC(4,3),                    -- AI 评分（可选）

    -- 计数
                                     up_count        INT NOT NULL DEFAULT 0,
                                     down_count      INT NOT NULL DEFAULT 0,
                                     reply_count     INT NOT NULL DEFAULT 0,

    -- 时间与审计
                                     created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                     edited_at       TIMESTAMPTZ,
                                     deleted_at      TIMESTAMPTZ,

    -- 生成列：深度（顶层为 0）
                                     depth           INT GENERATED ALWAYS AS (GREATEST(nlevel(path) - 1, 0)) STORED,

    -- 约束（仅模块内部）
                                     CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comment(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_comment_root   FOREIGN KEY (root_id)   REFERENCES comment(id) ON DELETE CASCADE,
                                     CONSTRAINT chk_parent_not_self CHECK (parent_id IS NULL OR parent_id <> id),
                                     CONSTRAINT chk_root_when_has_parent CHECK (parent_id IS NULL OR root_id IS NOT NULL)
);

-- 读写与结构索引
CREATE INDEX idx_cmt_subject                    ON comment (subject_id);
CREATE INDEX idx_cmt_subject_created_desc       ON comment (subject_id, created_at DESC);
CREATE INDEX idx_cmt_status                     ON comment (status);
CREATE INDEX idx_cmt_parent                     ON comment (parent_id);
CREATE INDEX idx_cmt_path_gist                  ON comment USING GIST (path);
-- 面向业务常用列表：仅取可见
CREATE INDEX idx_cmt_subject_visible_desc ON comment (subject_id, created_at DESC)
    WHERE status='published' AND deleted_at IS NULL;

-- ================== 反应（点赞/点踩/表情），完全模块内 ==================
CREATE TABLE comment_reaction (
                                              id          BIGSERIAL PRIMARY KEY,
                                              comment_id  BIGINT NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
                                              actor_id    BIGINT NOT NULL,                           -- 外部用户ID（无外键）
                                              actor_urn   CITEXT,                                    -- 可选
                                              type        reaction_type NOT NULL,
                                              emoji_code  TEXT,                                      -- type='emoji' 时需要
                                              created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                              CONSTRAINT chk_emoji_requires_code CHECK ((type <> 'emoji') OR (emoji_code IS NOT NULL)),
                                              CONSTRAINT uq_actor_reaction UNIQUE (comment_id, actor_id, type, COALESCE(emoji_code,''))
);

-- ================== 举报与审核动作（模块内治理） ==================
CREATE TABLE comment_report (
                                            id          BIGSERIAL PRIMARY KEY,
                                            comment_id  BIGINT NOT NULL REFERENCES comment(id) ON DELETE CASCADE,
                                            reporter_id BIGINT NOT NULL,
                                            reporter_urn CITEXT,
                                            reason      TEXT,
                                            status      TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewed','rejected','accepted')),
                                            created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE moderation_action (
                                               id            BIGSERIAL PRIMARY KEY,
                                               comment_id    BIGINT REFERENCES comment(id) ON DELETE SET NULL,
                                               operator_id   BIGINT NOT NULL,
                                               operator_urn  CITEXT,
                                               action        TEXT NOT NULL CHECK (action IN ('hide','delete','approve','shadow_ban','restore','spam')),
                                               reason        TEXT,
                                               prev_status   comment_status,
                                               new_status    comment_status,
                                               created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================== 触发器：维护 path/root 与计数（模块内自洽） ==================

-- 顶层：path='c{id}' 且 root_id=id；子层：path=parent.path||'c{id}'，root_id继承
CREATE OR REPLACE FUNCTION fn_set_path_and_root() RETURNS TRIGGER AS $$
DECLARE
    p_path  ltree;
    p_root  BIGINT;
BEGIN
    IF NEW.parent_id IS NULL THEN
        UPDATE comment
        SET root_id = NEW.id,
            path    = to_ltree('c' || NEW.id::text)
        WHERE id = NEW.id;
    ELSE
        SELECT path, root_id INTO p_path, p_root FROM comment WHERE id = NEW.parent_id FOR SHARE;
        IF p_path IS NULL OR p_root IS NULL THEN
            RAISE EXCEPTION 'Parent comment(%) not found or invalid', NEW.parent_id;
        END IF;
        UPDATE comment
        SET root_id = p_root,
            path    = p_path || to_ltree('c' || NEW.id::text)
        WHERE id = NEW.id;
    END IF;

    -- 更新主体的 last_commented_at 与计数（总数+1；若可见则可见数+1）
    UPDATE comment_subject
    SET comment_count = comment_count + 1,
        visible_count = visible_count + CASE WHEN NEW.status='published' AND NEW.deleted_at IS NULL THEN 1 ELSE 0 END,
        last_commented_at = now()
    WHERE subject_id = NEW.subject_id;

    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cmt_set_path
    AFTER INSERT ON comment
    FOR EACH ROW EXECUTE FUNCTION fn_set_path_and_root();

-- 父楼层回复计数
CREATE OR REPLACE FUNCTION fn_reply_count_adjust() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.parent_id IS NOT NULL THEN
            UPDATE comment SET reply_count = reply_count + 1 WHERE id = NEW.parent_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.parent_id IS NOT NULL THEN
            UPDATE comment SET reply_count = reply_count - 1 WHERE id = OLD.parent_id;
        END IF;
        -- 删除时同步主体总数与可见数
        UPDATE comment_subject
        SET comment_count = GREATEST(comment_count - 1, 0),
            visible_count = GREATEST(visible_count - CASE WHEN OLD.status='published' AND OLD.deleted_at IS NULL THEN 1 ELSE 0 END, 0)
        WHERE subject_id = OLD.subject_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cmt_reply_count_inc
    AFTER INSERT ON comment
    FOR EACH ROW EXECUTE FUNCTION fn_reply_count_adjust();

CREATE TRIGGER trg_cmt_reply_count_dec
    AFTER DELETE ON comment
    FOR EACH ROW EXECUTE FUNCTION fn_reply_count_adjust();

-- 反应计数（仅 up/down 冗余到评论表）
CREATE OR REPLACE FUNCTION fn_reaction_counter() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.type = 'up'   THEN UPDATE comment SET up_count   = up_count   + 1 WHERE id = NEW.comment_id; END IF;
        IF NEW.type = 'down' THEN UPDATE comment SET down_count = down_count + 1 WHERE id = NEW.comment_id; END IF;
        RETURN NEW;

    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.type = 'up'   THEN UPDATE comment SET up_count   = up_count   - 1 WHERE id = OLD.comment_id; END IF;
        IF OLD.type = 'down' THEN UPDATE comment SET down_count = down_count - 1 WHERE id = OLD.comment_id; END IF;
        RETURN OLD;

    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.comment_id <> NEW.comment_id THEN
            RAISE EXCEPTION 'comment_id is immutable for reactions';
        END IF;
        IF OLD.type IS DISTINCT FROM NEW.type THEN
            IF OLD.type = 'up'   THEN UPDATE comment SET up_count   = up_count   - 1 WHERE id = NEW.comment_id; END IF;
            IF OLD.type = 'down' THEN UPDATE comment SET down_count = down_count - 1 WHERE id = NEW.comment_id; END IF;
            IF NEW.type = 'up'   THEN UPDATE comment SET up_count   = up_count   + 1 WHERE id = NEW.comment_id; END IF;
            IF NEW.type = 'down' THEN UPDATE comment SET down_count = down_count + 1 WHERE id = NEW.comment_id; END IF;
        END IF;
        RETURN NEW;
    END IF;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cmt_reaction_counter
    AFTER INSERT OR UPDATE OR DELETE ON comment_reaction
    FOR EACH ROW EXECUTE FUNCTION fn_reaction_counter();

-- ================== 排序函数与物化视图（模块内） ==================
CREATE OR REPLACE FUNCTION wilson_lower_bound(up INT, down INT, z FLOAT8 DEFAULT 1.96)
    RETURNS FLOAT8 AS $$
DECLARE
    n     INT    := GREATEST(up + down, 0);
    phat  FLOAT8;
BEGIN
    IF n = 0 THEN RETURN 0; END IF;
    phat := up::FLOAT8 / n;
    RETURN (phat + z*z/(2*n) - z*sqrt((phat*(1-phat) + z*z/(4*n))/n)) / (1 + z*z/n);
END
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION hn_hot_score(up INT, down INT, created TIMESTAMPTZ, gravity FLOAT8 DEFAULT 1.8)
    RETURNS FLOAT8 AS $$
DECLARE
    p      INT    := GREATEST(up - down, 0);
    hours  FLOAT8 := EXTRACT(EPOCH FROM (now() - created)) / 3600.0;
BEGIN
    RETURN (p - 1) / POWER(hours + 2, gravity);
END
$$ LANGUAGE plpgsql STABLE;

CREATE MATERIALIZED VIEW IF NOT EXISTS comment_rank_mv AS
SELECT
    c.id,
    c.subject_id,
    wilson_lower_bound(c.up_count, c.down_count, 1.96) AS best_score,
    hn_hot_score(c.up_count, c.down_count, c.created_at, 1.8) AS hot_score,
    c.created_at,
    c.status
FROM comment c;

CREATE INDEX IF NOT EXISTS idx_cmt_rank_best
    ON comment_rank_mv (subject_id, best_score DESC, created_at DESC)
    WHERE status='published';

CREATE INDEX IF NOT EXISTS idx_cmt_rank_hot
    ON comment_rank_mv (subject_id, hot_score DESC, created_at DESC)
    WHERE status='published';

-- ================== 常用查询（示例，部署时可删） ==================
-- 1) 某 subject 的顶层评论（最佳）：
--   SELECT c.* FROM comment c
--   JOIN comment_rank_mv r ON r.id = c.id
--   WHERE c.subject_id = $1 AND c.depth = 0 AND c.status='published' AND c.deleted_at IS NULL
--   ORDER BY r.best_score DESC, c.created_at DESC
--   LIMIT $limit OFFSET $offset;
--
-- 2) 展开某楼层子树（按结构顺序）：
--   SELECT * FROM comment
--   WHERE path <@ (SELECT path FROM comment WHERE id = $comment_id)
--   ORDER BY path;
