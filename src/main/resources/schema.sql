-- 树形聊天系统数据库初始化脚本
-- PostgreSQL 14+

-- ============================================
-- 1. 用户表 (users)
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- 2. 项目表 (projects)
-- ============================================
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_projects_user_id ON projects(user_id);

-- ============================================
-- 3. 节点表 (nodes) - 核心业务表
-- ============================================
CREATE TABLE IF NOT EXISTS nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES nodes(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,

    -- 结论相关字段
    conclusion_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (conclusion_state IN ('DRAFT', 'FINALIZED')),
    ai_conclusion_draft TEXT,
    ai_conclusion_confidence VARCHAR(10) CHECK (ai_conclusion_confidence IN ('high', 'medium', 'low')),
    final_conclusion TEXT,

    -- 排序字段
    sibling_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nodes_user_id ON nodes(user_id);
CREATE INDEX idx_nodes_project_id ON nodes(project_id);
CREATE INDEX idx_nodes_parent_id ON nodes(parent_id);
CREATE INDEX idx_nodes_sibling_order ON nodes(sibling_order);

-- 全文检索索引 (PostgreSQL)
CREATE INDEX idx_nodes_title_fts ON nodes USING GIN(to_tsvector('english', title));
CREATE INDEX idx_nodes_conclusion_fts ON nodes USING GIN(to_tsvector('english', COALESCE(final_conclusion, '')));

-- ============================================
-- 4. 对话消息表 (messages)
-- ============================================
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    role VARCHAR(10) NOT NULL CHECK (role IN ('user', 'ai')),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_node_id ON messages(node_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- ============================================
-- 5. 节点引用关系表 (node_references)
-- ============================================
CREATE TABLE IF NOT EXISTS node_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    target_node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 防止重复引用
    UNIQUE(source_node_id, target_node_id)
);

CREATE INDEX idx_node_references_source ON node_references(source_node_id);
CREATE INDEX idx_node_references_target ON node_references(target_node_id);
CREATE INDEX idx_node_references_user_id ON node_references(user_id);

-- ============================================
-- 6. 项目UI元数据表 (project_ui_metadata)
-- ============================================
CREATE TABLE IF NOT EXISTS project_ui_metadata (
    project_id UUID PRIMARY KEY REFERENCES projects(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    zoom NUMERIC(5,2) NOT NULL DEFAULT 1.0,
    pan_x NUMERIC(10,2) NOT NULL DEFAULT 0,
    pan_y NUMERIC(10,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 7. 节点UI元数据表 (node_ui_metadata)
-- ============================================
CREATE TABLE IF NOT EXISTS node_ui_metadata (
    node_id UUID PRIMARY KEY REFERENCES nodes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    position_x NUMERIC(10,2) NOT NULL DEFAULT 0,
    position_y NUMERIC(10,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_node_ui_metadata_user_id ON node_ui_metadata(user_id);

-- ============================================
-- 8. 用户设置表 (user_settings)
-- ============================================
CREATE TABLE IF NOT EXISTS user_settings (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    tracking_data JSONB,  -- 用于自适应引导的行为追踪数据
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, setting_key)
);

CREATE INDEX idx_user_settings_tracking ON user_settings USING GIN(tracking_data);

-- ============================================
-- 9. 用户点数表 (user_credits)
-- ============================================
CREATE TABLE IF NOT EXISTS user_credits (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    balance NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 10. 点数交易记录表 (credit_transactions)
-- ============================================
CREATE TABLE IF NOT EXISTS credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount NUMERIC(10,2) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('PURCHASE', 'CONSUMPTION', 'REFUND', 'REWARD')),
    related_entity_id UUID,  -- 关联的实体ID（如洞察任务ID）
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_credit_transactions_created_at ON credit_transactions(created_at);

-- ============================================
-- 11. 全局洞察任务表 (insight_tasks)
-- ============================================
CREATE TABLE IF NOT EXISTS insight_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    request_payload JSONB NOT NULL,  -- 存储请求参数（节点IDs、分析透镜等）
    conversation_history JSONB,  -- 存储与AI教练的对话历史
    result TEXT,  -- 洞察结果
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_insight_tasks_user_id ON insight_tasks(user_id);
CREATE INDEX idx_insight_tasks_status ON insight_tasks(status);
CREATE INDEX idx_insight_tasks_created_at ON insight_tasks(created_at);

-- ============================================
-- 触发器：自动更新 updated_at 字段
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为需要的表添加触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_nodes_updated_at BEFORE UPDATE ON nodes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_project_ui_metadata_updated_at BEFORE UPDATE ON project_ui_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_node_ui_metadata_updated_at BEFORE UPDATE ON node_ui_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_settings_updated_at BEFORE UPDATE ON user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_credits_updated_at BEFORE UPDATE ON user_credits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_insight_tasks_updated_at BEFORE UPDATE ON insight_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 初始化数据（可选）
-- ============================================
-- 创建默认测试用户
INSERT INTO users (id, username, email, password_hash)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'testuser',
    'test@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'  -- password: "password"
) ON CONFLICT (username) DO NOTHING;

-- 为测试用户初始化点数
INSERT INTO user_credits (user_id, balance)
VALUES ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 100.00)
ON CONFLICT (user_id) DO NOTHING;
