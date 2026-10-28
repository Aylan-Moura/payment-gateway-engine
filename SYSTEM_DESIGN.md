# Documento de System Design: Payment Gateway Engine

## 1. Visão Geral
Este projeto implementa um **Gateway de Pagamento** baseado em uma arquitetura de microsserviços. O sistema foi projetado para oferecer alta disponibilidade, escalabilidade e resiliência, permitindo o processamento assíncrono de transações de pagamento.

## 2. Arquitetura de Alto Nível
O sistema utiliza uma abordagem de **Clean Architecture** em seus serviços de negócio e uma infraestrutura robusta de suporte.

### Stack Tecnológica:
- **Linguagem:** Java 17
- **Framework:** Spring Boot 3
- **Database:** PostgreSQL (gerenciado pelo Flyway)
- **Mensageria:** RabbitMQ
- **Caching/Rate Limiting:** Redis
- **Infraestrutura:** Docker & Docker Compose, AWS S3 (via LocalStack em dev)
- **Observabilidade:** Prometheus & Grafana
- **API Gateway:** Spring Cloud Gateway

### Diagrama da Arquitetura:
```mermaid
graph TD
    %% Estilos de nós
    classDef client fill:#3b82f6,stroke:#1d4ed8,color:#fff,stroke-width:2px;
    classDef gateway fill:#f59e0b,stroke:#d97706,color:#fff,stroke-width:2px;
    classDef service fill:#10b981,stroke:#047857,color:#fff,stroke-width:2px;
    classDef infra fill:#6b7280,stroke:#374151,color:#fff,stroke-width:2px;
    classDef storage fill:#8b5cf6,stroke:#6d28d9,color:#fff,stroke-width:2px;

    %% Clientes externos
    subgraph Clients [" Camada de Apresentação & Clientes "]
        FE["Frontend Dashboard\n(SPA / Bootstrap 5)"]:::client
        API_CLIENT["Sistemas de Lojistas\n(Webhooks & REST APIs)"]:::client
    end

    %% Entrada do Sistema
    subgraph Edge [" Edge Layer "]
        GW["API Gateway\n(Spring Cloud Gateway)"]:::gateway
        REDIS[("Redis\n(Rate Limiting 10 req/s)")]:::storage
    end

    %% Microsserviços Principais
    subgraph CoreServices [" Core Application Domain "]
        AUTH["Auth Service\n(Spring Security + JWT)"]:::service
        PAYMENT["Payment Core Service\n(Clean Architecture)"]:::service
        WORKER["Processor Worker\n(Spring Retry + Background Worker)"]:::service
    end

    %% Persistência e Mensageria
    subgraph Persistence [" Persistence & Messaging Layer "]
        DB_AUTH[("PostgreSQL\n(Tabela: merchants)")]:::storage
        DB_PAYMENT[("PostgreSQL\n(Tabela: transactions)")]:::storage
        
        subgraph RabbitMQ [" RabbitMQ Message Broker "]
            EXCHANGE["payment.exchange"]:::infra
            QUEUE["payment.created (Queue)"]:::infra
            DLQ["payment.dlq (Dead Letter Queue)"]:::infra
        end
        
        S3[("AWS S3 / LocalStack\n(Receipt Storage)")]:::storage
    end

    %% Monitoramento
    subgraph Observability [" Observabilidade & Monitoramento "]
        PROM["Prometheus\n(Métricas / Actuator)"]:::infra
        GRAF["Grafana\n(Dashboards de Latência, Erros & DLQ)"]:::infra
    end

    %% Conexões - Edge & Auth
    FE -->|HTTP/REST| GW
    API_CLIENT -->|HTTP/REST| GW
    GW <-->|Check Rate Limit| REDIS
    GW -->|/api/v1/auth| AUTH
    GW -->|/api/v1/payments| PAYMENT

    AUTH -->|CRUD Lojistas| DB_AUTH

    %% Conexões - Fluxo de Pagamento
    PAYMENT -->|1. Salva 'PENDING'| DB_PAYMENT
    PAYMENT -->|2. Publica evento| EXCHANGE
    EXCHANGE -->|Routing Key| QUEUE

    %% Conexões - Processamento Assíncrono
    QUEUE -->|3. Consome evento| WORKER
    WORKER -->|4. Atualiza para 'APPROVED'| DB_PAYMENT
    WORKER -->|5. Salva Recibo| S3
    WORKER -.->|6. Notificação Webhook| API_CLIENT
    WORKER -.->|Em caso de falha persistente| DLQ

    %% Conexões - Observabilidade
    AUTH -.->|Métricas| PROM
    PAYMENT -.->|Métricas| PROM
    WORKER -.->|Métricas| PROM
    GW -.->|Métricas| PROM
    PROM --> GRAF
```

## 3. Detalhes dos Componentes

### 3.1. API Gateway (`api-gateway`)
Ponto de entrada centralizado.
- **Roteamento:** Direciona requisições para `auth-service` e `payment-core-service`.
- **Rate Limiting:** Filtro reativo baseado em Redis que limita cada API Key a 10 req/s.
- **CORS:** Configurado para suportar o frontend.

### 3.2. Auth Service (`auth-service`)
Gerenciamento de identidades.
- **Funcionalidades:** Registro, Login, Refresh Token (JWT) e configuracao de webhook do lojista.
- **Segurança:** Spring Security com filtro JWT; rotas `/api/v1/auth/*` sao publicas e as demais exigem Bearer token.
- **Armazenamento:** Tabela `merchants` (PostgreSQL), incluindo `webhook_url`.

### 3.3. Payment Core Service (`payment-core-service`)
Orquestração de pagamentos.
- **Domínio:** Implementa a lógica de negócio principal (Clean Architecture).
- **Endpoints:** `/api/v1/payments` (exige JWT e `X-API-Key` do mesmo merchant).
- **Persistência:** Tabela `transactions` (com `webhook_url` copiada do merchant).
- **Mensageria:** Publica eventos (`payment.created`) para RabbitMQ para processamento assíncrono.

### 3.4. Processor Worker (`processor-worker`)
Processamento em background.
- **Responsabilidades:**
    - Consumo da fila `payment.created`.
    - Atualização do status da transação para `APPROVED`.
    - Geração de recibos e armazenamento em S3 (`S3ReceiptService`).
    - Disparo de webhooks para a URL do lojista (ou `WEBHOOK_DEFAULT_URL`) com suporte a retentativas automáticas (`Spring Retry`).
- **Resiliência:** Configuração de Dead Letter Queue (`payment.dlq`) para mensagens que falham após retentativas.

### 3.5. Frontend Dashboard (`frontend-dashboard`)
SPA.
- **Tecnologias:** Angular, nginx e Tailwind.
- **Features:** Cadastro/Login, Checkout, Tabela de transações, Download de recibos.

## 4. Esquema de Banco de Dados
### 4.1. Tabela `merchants` (`auth-service`)
- `id` (UUID), `company_name` (VARCHAR), `email` (UNIQUE VARCHAR), `password_hash` (VARCHAR), `api_key` (UNIQUE VARCHAR), `webhook_url` (VARCHAR), `created_at` (TIMESTAMP).

### 4.2. Tabela `transactions` (`payment-core-service`)
- `id` (UUID), `merchant_id` (UUID), `amount` (NUMERIC), `status` (VARCHAR), `payment_method` (VARCHAR), `webhook_url` (VARCHAR), `created_at` (TIMESTAMP).

## 5. Fluxo de Processamento (Mensageria)
1. Requisição recebida no `payment-core-service` (autenticada por JWT + API Key).
2. Transação persistida como `PENDING` com a URL de webhook do merchant.
3. Evento publicado em `payment.exchange` com routing-key `payment.created.routing`.
4. `processor-worker` consome a fila `payment.created`.
5. Transação atualizada para `APPROVED`.
6. Recibo gerado no S3 e webhook enviado ao lojista (ou ignorado, com log, se nao houver URL).

## 6. Observabilidade
- **Actuator/Prometheus:** Métricas coletadas em todos os serviços.
- **Grafana:** Dashboards centralizados para monitoramento de latência, taxa de erro, volume de transações e DLQ.

## 7. Estratégia de Deploy
- **CI/CD:** GitHub Actions para automação de build, testes e deploy via SSH.
- **Containers:** Imagens Docker otimizadas para ambientes de desenvolvimento e produção.
