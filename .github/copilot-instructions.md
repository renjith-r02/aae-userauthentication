# Enterprise GitHub Copilot Instructions

## Purpose
This repository follows enterprise-grade architecture, security, governance, and software engineering standards.

## Core Principles
- Security by Design
- API-First Development
- Cloud-Native Architecture
- Zero Trust Security
- Observability by Default
- Automated Validation and Governance

## Mandatory Standards
- Follow OWASP Top 10 controls
- APIs must be versioned
- Secrets must never be hardcoded
- JWT validation and RBAC are mandatory
- Structured logging is required
- All services must support health checks

## Security Requirements
- Use bcrypt or Argon2 for passwords
- Enforce rate limiting
- Prevent replay attacks
- Encrypt data in transit and at rest

## Definition of Done
- Security validation passes
- Architecture documentation updated
- APIs documented
- Tests passing
