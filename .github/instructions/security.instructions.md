# Enterprise Security Instructions

## Security Principles
- Zero Trust Architecture
- Least Privilege Access
- Defense in Depth
- Secure by Default

## OWASP Top 10 Controls

### Broken Access Control
- Enforce RBAC
- Deny by default

### Cryptographic Failures
- Encrypt sensitive data
- Use TLS 1.2+
- Use bcrypt or Argon2

### Injection
- Use parameterized queries
- Validate all inputs

### Security Misconfiguration
- Harden infrastructure
- Disable unnecessary services

### Vulnerable Components
- Scan dependencies continuously

### Authentication Failures
- Enforce MFA where applicable
- Implement token expiration and rotation

### Logging and Monitoring
- Centralize audit logs
- Monitor authentication events

## Container Security
- Use minimal base images
- Run containers as non-root
- Scan images continuously

## Kubernetes Security
- Enforce network policies
- Restrict privileged containers
- Use secure secrets management
