# 📄 HANDOFF — Payment Gateway Engine (Fase 5)

> Documento de contexto para continuidade. A **SDD é a fonte de verdade** e a sequência de prompts deve ser seguida.

## 1. Objetivo do projeto

Plataforma distribuída de orquestração e processamento assíncrono de pagamentos (PIX e Cartão), em **Java 17 / Spring Boot 3.2.3**, monorepo Maven, com **TDD obrigatório (Red → Green → Refactor)**, Clean Architecture, microsserviços, Testcontainers, RabbitMQ (DLQ), Redis, PostgreSQL/Flyway, AWS S3, Docker, GitHub Actions, OpenAPI e dashboard.

## 2. Status por fase

| Fase | Escopo | Status |
|---|---|---|
| 1 | Monorepo Maven + Flyway | ✅ Completo |
| 2 | `auth-service` JWT/Refresh | ✅ Completo |
| 3 | `payment-core-service` Clean Architecture | ✅ Completo |
| 4 | `processor-worker` (DLQ, S3, Webhooks + Retry) | ✅ Completo |
| 5 | `api-gateway` + OpenAPI + frontend | ✅ Completo |
| 6 | E2E Testcontainers, Observabilidade, CI/CD deploy | ⏳ Pendente |

### Módulos e portas

- `auth-service` — porta **8081**; `POST /api/v1/auth/register|login|refresh`; JWT + refresh; Flyway `V1__create_merchants_table.sql`; testes com H2.
- `payment-core-service` — `POST /api/v1/payments`; domínio puro `Transaction`, `CreateTransactionUseCase`, portas `SaveTransactionPort` / `PublishPaymentEventPort`; publica na fila `payment.created`.
- `processor-worker` — `@RabbitListener` em `payment.created`, DLQ `payment.dlx`, `S3ReceiptService`, `WebhookService` com `@Retryable` + tabela `webhook_logs`; `@EnableRetry`.
- `docker-compose.yml` — postgres(5432), redis(6379), rabbitmq(5672/15672), prometheus(9090), grafana(3000).
- `.github/workflows/ci.yml` — `mvn clean compile -DskipTests` + `mvn test`.
- Não existe `mvnw`; usar `mvn`.

## 3. Decisões confirmadas (alinhadas à SDD)

1. **Reverter todo o código de segurança/rate-limit dentro do `payment-core-service`.**
2. **Rate Limit vai no `api-gateway`** — Spring Cloud Gateway + Redis, **10 req/s por API Key (`X-API-Key`)** (Prompt 10).
3. **Frontend = HTML5/JS + Bootstrap via CDN** (sem npm/build).
4. **Escopo atual = Fase 5 completa** (Prompts 10, 11 e 12).

## 4. Alterações não commitadas que devem ser revertidas

> O `git status` mostra muitos `target/` alterados — **ignore-os** (o repositório versionou artefatos de build).

Reverter / remover:

- `pom.xml` (raiz): **manter** `<module>api-gateway</module>` (a causa do build quebrado era a ausência de `api-gateway/pom.xml`, não o módulo).
- `payment-core-service/pom.xml`: remover `spring-boot-starter-security`, `spring-boot-starter-data-redis`, `jjwt-*`, `spring-security-test`.
- Remover `payment-core-service/src/main/java/com/paymentgateway/core/adapters/in/security/`.
- Remover `payment-core-service/src/test/java/com/paymentgateway/core/adapters/in/security/`.
- Remover `payment-core-service/src/test/java/.../adapters/in/web/PaymentControllerSecurityTest.java`.
- Restaurar `PaymentControllerTest.java` à versão original.
- Remover `payment-core-service/src/main/resources/application.yml` e `src/test/resources/application.yml`.

## 5. Plano da Fase 5 (TDD)

### Prompt 10 — `api-gateway` (Spring Cloud Gateway + Redis)

- Criar `api-gateway/pom.xml`: `spring-cloud-starter-gateway` + `spring-boot-starter-data-redis-reactive` (BOM Spring Cloud 2023.0.0 já importado no pai). Porta **8080**.
- `application.yml` com rotas:
  - `/api/v1/auth/**` → `auth-service` (8081)
  - `/api/v1/payments/**` → `payment-core-service` (8082)
- `RequestRateLimiter` com `RedisRateLimiter`: `replenishRate=10`, `burstCapacity=10`; `KeyResolver` lendo o header **`X-API-Key`**.
- **TDD**: teste unitário do `KeyResolver`; integração do rate limit com Redis via **Testcontainers** (`@Testcontainers(disabledWithoutDocker = true)` — Docker indisponível local, disponível no GH Actions).

### Prompt 11 — OpenAPI/Swagger

- Adicionar `springdoc-openapi-starter-webmvc-ui` em `auth-service` e `payment-core-service`.
- Anotações `@Operation`/`@ApiResponse`/`@Schema` + `SecurityScheme` Bearer JWT; expor `/swagger-ui.html`.

### Prompt 12 — `frontend-dashboard` (HTML5/JS + Bootstrap CDN)

- Tela de login/cadastro consumindo `/api/v1/auth/*` (JWT no `localStorage`).
- Formulário de checkout (PIX/Cartão) enviando ao `api-gateway`.
- Tabela de transações com status (`PENDING`/`APPROVED`/`REJECTED`) e botão de download do comprovante (S3).

## 6. Observações importantes

- **Docker indisponível** neste ambiente → testes Testcontainers precisam de `disabledWithoutDocker = true` localmente; no CI rodam normalmente.
- Fase 6 (Prompts 13–15) pendente: E2E Testcontainers, Actuator/Micrometer/Prometheus/Grafana e pipeline `deploy.yml` com build Docker + SSH na EC2.
