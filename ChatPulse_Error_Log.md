# ChatPulse - Error Log

## Build Errors

### 1. Scala 3 Indentation Syntax with for/yield
- **Error**: `unindent expected, but 'yield' found` in Routes.scala
- **Cause**: Scala 3's indentation-based syntax with `for/yield` inside `HttpRoutes.of[IO]` pattern match cases
- **Fix**: Switched from indentation-based `for/yield` to brace-based `flatMap` chains

### 2. Missing circe-parser Import
- **Error**: `value parser is not a member of io.circe`
- **Cause**: Unused `import io.circe.parser.*` — circe-parser is a separate module not in dependencies
- **Fix**: Removed the unused import

### 3. IO Tuple Destructuring in for-comprehension
- **Error**: `value withFilter is not a member of cats.effect.IO[...]`
- **Cause**: Scala 3 `for` comprehension with tuple pattern matching `(app, _, _) <- mkApp` requires `withFilter` which `IO` doesn't implement
- **Fix**: Replaced tuple with `case class TestEnv(app, rs, ms)` for destructuring

### 4. Missing Encoder Instances for Request Types
- **Error**: `No given instance of type io.circe.Encoder[CreateRoomRequest]`
- **Cause**: Request case classes only had Decoder derivations, not Encoders
- **Fix**: Added `given Encoder[...] = deriveEncoder` to all request type companion objects

### 5. Ambiguous EntityEncoder
- **Error**: `Ambiguous given instances: jsonEncoder and streamJsonArrayEncoder`
- **Cause**: http4s-circe provides multiple EntityEncoder instances for Json
- **Fix**: Imported `org.http4s.circe.jsonEncoder` specifically, used helper method `jsonReq` with explicit encoder

### 6. CI Missing sbt
- **Error**: `sbt: command not found` in GitHub Actions
- **Cause**: CI workflow only had `actions/setup-java@v4` but no sbt setup step
- **Fix**: Added `sbt/setup-sbt@v1` step after Java setup

---

## Security Audit (10-Point Checklist)

| # | Category | Result | Notes |
|---|----------|--------|-------|
| 1 | Hardcoded Secrets | PASS | PORT from env var, no secrets in code |
| 2 | SQL Injection | N/A | In-memory stores only (Ref[IO, Map]) |
| 3 | Input Validation | PASS | Blank checks on name, userId, sender, content |
| 4 | Dependency Vulnerabilities | PASS | http4s 0.23.30, circe 0.14.10, cats-effect 3 |
| 5 | Auth / Access Control | N/A | Chat API demo — no sensitive data |
| 6 | Security Headers | PASS | CSP, HSTS, X-Frame-Options, X-Content-Type-Options, Permissions-Policy, Referrer-Policy |
| 7 | Sensitive Data Exposure | PASS | Structured JSON errors, no stack traces |
| 8 | Docker Security | PASS | Multi-stage build, Alpine JRE, non-root user (appuser) |
| 9 | CI Security | PASS | Pinned action versions, Java 21 target |
| 10 | Rate Limiting / DoS | NOTE | Thread-safe with cats-effect Ref. Production would need rate limiting |

### Security Features Implemented
1. Security headers middleware on all responses (7 headers)
2. Input validation on all endpoints
3. Thread-safe state management (cats-effect Ref)
4. Immutable data models (Scala case classes)
5. Non-root Docker container
6. Multi-stage Docker build
7. Functional error handling (Either, Option)
8. No hardcoded secrets or credentials
