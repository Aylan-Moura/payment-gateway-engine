# 📊 Relatório de Status do Projeto: Payment Gateway Engine

## 🏗️ 1. O que já foi realizado (Concluído)

O projeto está estruturado em um **monorepo Maven** utilizando **Java 17** e **Spring Boot 3**, seguindo princípios de Clean Architecture, microsserviços, mensageria assíncrona e práticas de TDD.

### **Fase 1: Setup Inicial e Monorepo**
- **Estrutura de Módulos criada:**
  - `api-gateway`: Ponto de entrada único e roteamento.
  - `auth-service`: Serviço de autenticação e gestão de lojistas.
  - `payment-core-service`: Domínio e orquestração de pagamentos.
  - `processor-worker`: Processamento assíncrono em background.
  - `frontend-dashboard`: Interface web do lojista.
  - `e2e-tests`: Módulo dedicado a testes de integração ponta a ponta.
- **CI/CD Inicial:** Pipeline base configurada no GitHub Actions (`ci.yml`) para compilação e execução de testes automatizados a cada *push* ou *pull request*.

### **Fase 2: Serviço de Autenticação (`auth-service`)**
- **Persistência & Migrations:** Flyway configurado com a migration `V1__create_merchants_table.sql` para gerenciamento da tabela de lojistas (`merchants`).
- **Segurança & JWT:** Spring Security integrado com emissão de token JWT e suporte a *Refresh Token*.
- **Endpoints:** Rotas REST implementadas para `/api/v1/auth/register`, `/api/v1/auth/login` e `/api/v1/auth/refresh`.
- **Documentação:** Integração com OpenAPI / Swagger UI (`/swagger-ui.html`) com suporte a esquema de segurança `Bearer JWT`.

### **Fase 3: Core de Pagamentos (`payment-core-service`)**
- **Clean Architecture & Domínio:** Implementação da entidade de domínio puro `Transaction`, regras de negócio para métodos de pagamento (`PIX`, `CREDIT_CARD`), e casos de uso (`CreateTransactionUseCase`).
- **Portas e Adaptadores:** Definição de portas de entrada (`CreateTransactionCommand`) e saída (`SaveTransactionPort`, `PublishPaymentEventPort`), além do controller REST (`POST /api/v1/payments`).
- **Documentação:** Swagger UI integrado.

### **Fase 4: Processador Assíncrono (`processor-worker`)**
- **Mensageria RabbitMQ:** Listener configurado para consumir mensagens da fila principal (`payment.created`).
- **Resiliência & DLQ:** Configuração de Dead Letter Queue (`payment.dlq`) para tratamento de falhas após retentativas.
- **Integração AWS S3 & Webhooks:**
  - `S3ReceiptService` para geração e armazenamento de comprovantes.
  - `WebhookService` com suporte a retentativas automáticas (`Spring Retry`) e tabela de log de webhooks (`webhook_logs`).

### **Fase 5: API Gateway & Frontend Dashboard**
- **Spring Cloud Gateway:** Roteamento centralizado e dinâmico:
  - `/api/v1/auth/**` → `auth-service` (8081)
  - `/api/v1/payments/**` → `payment-core-service` (8082)
- **Rate Limiting Reativo:** Filtro baseado em Redis limitando requisições a **10 req/s por API Key** (`X-API-Key`).
- **CORS:** Configurado no Gateway para permitir comunicação irrestrita com o painel front-end.
- **Frontend Dashboard:** Aplicação SPA minimalista (HTML5, Vanilla JS, Bootstrap 5 via CDN) contendo:
  - Telas de Cadastro e Login de Lojistas (com persistência de token no `localStorage`).
  - Formulário de checkout para simulação de cobranças.
  - Tabela de transações com status em tempo real (`PENDING`, `APPROVED`, `REJECTED`) e opção de download de recibos.

### **Fase 6 (Parcial): Observabilidade, Docker e CI/CD**
- **Observabilidade:**
  - `Spring Boot Actuator` e `Micrometer Prometheus` integrados em todos os microsserviços.
  - Arquivo `prometheus.yml` configurado para coleta de métricas.
- **Docker & Docker Compose:**
  - `docker-compose.yml` para ambiente de desenvolvimento local (PostgreSQL, Redis, RabbitMQ, Prometheus, Grafana).
  - `docker-compose.prod.yml` para ambiente de produção com *healthchecks* robustos.
- **Pipeline de Deploy Contínuo (`deploy.yml`):**
  - Automação no GitHub Actions para build das imagens Docker, push para registry e deploy automatizado via SSH em servidor remoto (AWS EC2).

---

## ⏳ 2. O que falta / Próximos Passos (Pendências e Evoluções)

Embora a infraestrutura e a arquitetura macro estejam totalmente montadas e alinhadas, existem pontos de refinamento e implementações concretas que restam para atingir 100% de maturidade operacional:

1. **Implementação dos Adaptadores de Infraestrutura no `payment-core-service`:**
   - Atualmente, as portas de saída (`SaveTransactionPort` e `PublishPaymentEventPort`) possuem contratos e testes unitários com mocks, mas faltam as classes concretas de persistência (Spring Data JPA / Repositórios reais do PostgreSQL) e o produtor real do RabbitMQ conectadas no core.
2. **Conclusão da Lógica de Negócio do `processor-worker`:**
   - O esqueleto do listener e dos serviços auxiliares (S3 e Webhooks) está criado, mas a lógica interna de processamento efetivo (atualização do status da transação no banco, chamada real ao S3/LocalStack e disparo HTTP real para o endpoint do lojista) precisa ser totalmente encadeada.
3. **Expansão dos Testes E2E (Testcontainers):**
   - O módulo `e2e-tests` possui uma suíte estruturada com `PaymentEngineE2ETest.java`, mas o teste atual valida apenas o carregamento do contexto do Spring com os containers subindo. Falta implementar o fluxo funcional ponta a ponta no teste (Requisição HTTP via Gateway → Persistência → Fila → Processamento pelo Worker).
4. **Dashboards Prontos no Grafana:**
   - A coleta de métricas via Actuator/Prometheus está configurada, mas a criação de um dashboard visual pré-pronto no Grafana (com gráficos de taxa de sucesso/erro de pagamentos, latência do Gateway e volume na DLQ) pode ser consolidada através de arquivos de provisão de dashboards.
5. **Testes de Carga e Validação de Resiliência Prática:**
   - Realizar validações práticas do funcionamento do Rate Limiter sob carga (disparando mais de 10 req/s para verificar o retorno HTTP 429) e testar o comportamento do RabbitMQ ao injetar falhas propositais para testar a DLQ.
