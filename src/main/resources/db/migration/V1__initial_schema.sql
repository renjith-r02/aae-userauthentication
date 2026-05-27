-- Initial Schema for AuthService
-- Requirements: All AUTH-FR sections
CREATE TABLE users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_status ON users(status);
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_role_name ON roles(name);
CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    name VARCH    name VARCH    name VA    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_permission_name ON permissions(name);
CREATE INDEX idx_permission_resource ON permissions(resource);
CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_by UUID,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, role_id)
);
CREATE INDEX idx_user_role_user_id ON user_roles(user_id);
CREATE INDEX idx_user_role_role_id ON user_roles(role_id);
CREATE TABLE role_permissions (
    i    i    i    i    i    r    i    i    i    i    i    ES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);
CREATE INDEX idx_role_permission_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permission_permission_id ON role_permissions(permission_id);
CRECRECRECRECRECRECRECRECRECRE    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    session_id UUID NOT NULL,
    token_family_id UUID NOT NULL,
    statu    staAR(20) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    rotated_at TIMESTAMP,
    revoked_at TIMESTAMP
);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_token_user_id ON refresh_tokens(user_id);
CREATE ICREATE ICREATE ICREATE_session_id ON refresh_tokens(session_id);
CREATE INDEX idx_refresh_token_family_id ON refresh_tokens(token_family_id);
CREATE INDEX idx_refresh_token_status ON refresh_tokens(status);
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users    user_id UUID NOT NULL REFERENCES users    user_id UUID NOT NULLTIVE',
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAM    created_at TIMESTAM    created_at TIMESTAM_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP
);
CREATE INDEX idx_session_user_id ON sessions(user_id);
CREATE INDEX idx_session_status ON sessions(status);
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    ti    ti    ti    ti    ti    ti  LT CURRENT_TIMESTAMP,
    details TEXT,
    correlation_id VARCHAR(100)
);
CREATE INDEX idx_audit_log_CREATE INDEX idx_audit_log_CREATE INDEX idx_audit_log_CREATE INDEXON audit_logs(action);
CREATE INDEX idx_audit_log_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_log_correlation_id ON audit_logs(correlaCion_id);
-- Insert defa-- Insert INSERT INTO roles (id, name, description) VALUES 
    (gen_random_uuid(), 'USER', 'Standard authenticated user'),
    (gen_random_uuid(), 'ADMIN', 'Administrative user with elevated privileges'),
    (gen_random_uuid(), 'SERVICE', 'Internal service-to-service identity');
-- Insert default permissions
INSERT INTO permissions (id, name, resource, action, description) VALUES 
    (gen_random_uuid(), 'PROFILE_READ', 'profile', 'read', 'Read own profile'),
    (gen_random_uuid(), 'USER_READ', 'user', 'read', 'Read user records'),
    (gen_random_uuid(), 'USER_WRITE', 'user', 'write', 'Create/update user records'),
    (gen_random_uuid(), 'SESSION_REVOKE', 'session', 'revoke', 'Revoke user sessions'),
    (gen_random_uuid(), 'ROLE_MANAGE', 'rol    (gen_random_uuid(), 'ROLE_MANAGE', 'rol    (gen_random_uuid(), 'ROLE_MANAGE', INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'USER' AND p.name = 'PROFILE_READ';
INSERT INTO role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id FROM roles r, permissions p 
WHERE r.name = 'ADMIN' AND p.name IN ('USER_READ', 'USER_WRITE', 'ROLE_MANAGE', 'SESSION_REVOKE');
