# 📄 Plano de Execução — Fase 6

## 1. Resumo Técnico (Fase 5 Concluída)

A Fase 5 consolidou a infraestrutura de entrada e a interface do usuário da plataforma.

### 🚀 Implementações Realizadas:
- **API Gateway (`api-gateway`):**
    - Implementado com **Spring Cloud Gateway**.
    - **Rate Limit Reativo:** Configurado com Redis, limitando a 10 req/s por `X-API-Key`.
    - **Roteamento Centralizado:** `/api/v1/auth/**` (8081) e `/api/v1/payments/**` (8082).
    - **CORS:** Configurado para permitir integração com o frontend.
    - **TDD:** Testes unitários para `KeyResolver` e integração com Redis via Testcontainers.
- **Documentação (OpenAPI/Swagger):**
    - Integrado `springdoc-openapi` nos serviços `auth` e `payment-core`.
    - Configurado **SecurityScheme Bearer JWT** para testes diretos via Swagger UI.
    - Anotações completas nos Controllers e DTOs.
- **Frontend Dashboard:**
    - Single Page Application (SPA) minimalista usando **HTML5, Vanilla JS e Bootstrap 5 (CDN)**.
    - Fluxos de: Cadastro de Merchant, Login (JWT), Criação de Pagamento e Listagem de Transações.
    - Integração com o Gateway usando headers de segurança (`Authorization` e `X-API-Key`).
- **Refatoração e Limpeza:**
    - Removido o acoplamento de segurança e Redis do `payment-core-service`, movendo a responsabilidade de Rate Limit para o Gateway e mantendo a Clean Architecture pura no core.

---

## 2. Plano para a Fase 6 — E2E, Observabilidade e CI/CD

O objetivo final é garantir a confiabilidade do fluxo completo e preparar o ambiente de produção.

### 🧪 Prompt 13 — Testes E2E (Testcontainers)
- Criar módulo de teste `e2e-integration-tests` ou adicionar testes no Gateway.
- **Cenário:** Simular um fluxo completo usando Testcontainers para subir:
    - PostgreSQL, RabbitMQ, Redis, LocalStack (S3).
    - Microserviços rodando em containers.
- **Fluxo:** Registro de Merchant → Login → Obtenção de API Key → POST de Pagamento via Gateway → Verificação de persistência no DB → Verificação de evento no RabbitMQ → Verificação de recibo no S3.

### 📈 Prompt 14 — Observabilidade (Actuator + Prometheus + Grafana)
- Adicionar `spring-boot-starter-actuator` e `micrometer-registry-prometheus` em todos os serviços.
- Configurar `/actuator/health` e `/actuator/metrics`.
- Atualizar `docker-compose.yml` para incluir:
    - **Prometheus:** Coletando métricas dos serviços.
    - **Grafana:** Dashboard pré-configurado para visualizar:
        - Taxa de sucesso/erro de pagamentos.
        - Latência do Gateway.
        - Mensagens na DLQ.

### 🚢 Prompt 15 — CI/CD e Deploy (GitHub Actions)
- Criar `.github/workflows/deploy.yml`.
- **Etapas:**
    - Build Maven e Testes.
    - Build de imagens Docker e push para registro (Docker Hub/GHCR).
    - Deploy automatizado via SSH em servidor (ex: EC2) usando `docker-compose.prod.yml`.
- Configuração de `healthchecks` no Docker Compose para garantir zero downtime durante o restart.

---

## 3. Próximos Passos Imediatos
1. Configurar o ambiente de testes E2E com Testcontainers.
2. Garantir que o `docker-compose.yml` atualizado reflita todos os serviços (Gateway, Auth, Core, Worker, Redis, Postgres, RabbitMQ).
