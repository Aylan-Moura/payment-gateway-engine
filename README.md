# Payment Gateway Engine

Plataforma de pagamentos com microsservicos Spring Boot, API Gateway, Angular, PostgreSQL, RabbitMQ, Redis, S3 e observabilidade (Prometheus + Grafana).

## Arquitetura

| Componente | Porta (local) | Descricao |
|---|---|---|
| `frontend-dashboard` | 3001 | Angular servido por nginx (proxy `/api/` para o gateway) |
| `api-gateway` | 8080 | Ponto de entrada, roteamento, CORS e rate limit |
| `auth-service` | 8081 | Registro/login de lojistas, JWT |
| `payment-core-service` | 8082 | Criacao de transacoes, publicacao no RabbitMQ |
| `processor-worker` | 8083 | Consumo, status, comprovante em PDF no S3, webhook |
| PostgreSQL | 5432 | Banco unico com Flyway por servico |
| RabbitMQ | 5672 / 15672 | Mensageria |
| Redis | 6379 | Rate limit do gateway |
| LocalStack S3 | 4566 | S3 local para desenvolvimento |
| Prometheus / Grafana | 9090 / 3000 | Metricas e dashboards |

## Rodar localmente

Pre-requisitos: Docker + Docker Compose v2.

```bash
cp .env.example .env   # opcional no dev; o docker-compose.yml tem valores locais
docker compose up -d --build
docker compose ps
```

As imagens Java usam build multi-stage (Maven dentro do Docker), entao **nao e necessario** rodar `mvn package` antes.

Acessos:

- Dashboard: http://localhost:3001
- API Gateway: http://localhost:8080
- RabbitMQ: http://localhost:15672 (`admin` / `adminpassword`)
- Grafana: http://localhost:3000 (`admin` / `admin`)
- Prometheus: http://localhost:9090

Smoke test:

```bash
bash scripts/smoke-test.sh
```

## Testes

```bash
mvn -B clean test
```

Os testes E2E usam Testcontainers (PostgreSQL, RabbitMQ e LocalStack) e exigem Docker.

## Deploy em producao

### 1. Preparar o servidor (EC2/VPS)

- Docker Engine + Docker Compose v2 instalados.
- Diretorio `/opt/payment-gateway` com permissao de escrita.
- Portas liberadas no security group/firewall: `80` (dashboard), `8080` (API), `3000`/`9090` (observabilidade, opcional).
- As portas `8081`, `8082` e `8083` **nao** sao publicadas em producao de proposito.

### 2. Configurar variaveis

```bash
cd /opt/payment-gateway
cp .env.example .env
# edite .env com senhas fortes e JWT_SECRET base64 de 32 bytes
openssl rand -base64 32
```

### 3. Subir a stack

As imagens sao construidas e publicadas pelo workflow de CD. Para um deploy manual, rode o workflow ou faca build/push das 5 imagens antes.

```bash
docker compose -f docker-compose.prod.yml config -q
docker compose -f docker-compose.prod.yml up -d
```

O frontend e servido pelo nginx e faz proxy de `/api/` para o gateway, portanto o browser usa a mesma origem (sem hardcode de `localhost`). O CORS do gateway e configuravel por `CORS_ALLOWED_ORIGINS`.

### 4. S3

- Crie o bucket definido em `AWS_S3_BUCKET`.
- Prefira IAM Role/Instance Profile (nao descomente as chaves no `.env`).
- Para chaves estaticas, descomente `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` no `.env`; o worker carrega esse arquivo via `env_file`.
- Permissoes minimas: `s3:PutObject`, `s3:GetObject` em `arn:aws:s3:::SEU_BUCKET/receipts/*`.
- Para MinIO/S3 compativel, defina `AWS_S3_ENDPOINT_URL` e `AWS_S3_PATH_STYLE=true`.

### 5. CI/CD (GitHub Actions)

Secrets necessarios no repositorio:

| Secret | Uso |
|---|---|
| `DOCKER_HUB_USERNAME` / `DOCKER_HUB_TOKEN` | push das imagens (o username tambem e o namespace das imagens) |
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | deploy via SSH |

O workflow `.github/workflows/deploy.yml`:

1. roda os testes;
2. builda as 5 imagens (4 servicos Java + frontend) com a tag do commit;
3. publica no Docker Hub;
4. copia `docker-compose.prod.yml`, `prometheus.yml` e `grafana/` para `/opt/payment-gateway`;
5. atualiza a tag no compose e roda `docker compose up -d`.

O arquivo `/opt/payment-gateway/.env` precisa existir no servidor.

### 6. TLS / dominio

O compose nao inclui terminacao TLS. Recomenda-se um proxy reverso (Caddy, Traefik ou nginx) na frente do dashboard/API, com certificado Let's Encrypt, apontando para `:80` e `:8080`.

## Observabilidade

- Prometheus raspa `api-gateway`, `auth-service`, `payment-core-service` e `processor-worker` pelo nome dos servicos na rede Docker.
- Grafana ja provisiona o datasource do Prometheus.
- Healthchecks usam `/actuator/health`; metricas em `/actuator/prometheus`.

## Seguranca

- Nunca commite `.env`; rotacione as senhas de exemplo antes de producao.
- O `JWT_SECRET` de desenvolvimento nao deve ser usado em producao.
- Servicos internos nao expoem portas em producao.
- O webhook do lojista ainda usa uma URL fixa de demonstracao (`merchant-mock-url`); para producao real, cadastre a URL por lojista.
