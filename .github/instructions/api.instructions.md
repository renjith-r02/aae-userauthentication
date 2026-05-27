# API Design Instructions

## API Standards
- Use OpenAPI 3.x
- Version all APIs
- Use JSON payloads
- Apply consistent error responses

## Security
- JWT authentication required
- RBAC enforcement mandatory
- Validate all inputs
- Apply rate limiting

## Error Response Format
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed"
}
```

## Observability
- Correlation IDs required
- Request tracing required
- Audit logging for sensitive APIs
