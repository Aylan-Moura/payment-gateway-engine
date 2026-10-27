# Payment Gateway Engine

Plataforma de pagamentos com microsservicos Spring Boot, API Gateway, Angular, PostgreSQL, RabbitMQ, Redis, S3 e observabilidade (Prometheus + Grafana).

## Arquitetura

| Componente | Porta (local) | Descricao |
|---|---|---|
| `frontend-dashboard` | 3001 | Angular servido por nginx (proxy `/api/` para o gateway) |
| `api-gateway` | 8080 | Ponto de entrada, roteamento, CORS e rate limit |
| `auth-service` | 8081 | Registro/login de lojistas, JWT e webhook por lojista |
| `payment-core-service` | 8082 | Criacao de transacoes, publicacao no RabbitMQ |
| `processor-worker` | 8083 | Consumo, status, comprovante em PDF no S3, webhook |
| PostgreSQL | 5432 | Banco unico com Flyway por servico |
| RabbitMQ | 5672 / 15672 | Mensageria (imagem 4.1) |
| Redis | 6379 | Rate limit do gateway |
| LocalStack S3 | 4566 | S3 local para desenvolvimento |
| Prometheus / Grafana | 9090 / 3000 | Metricas e dashboards |
| Caddy (producao) | 80 / 443 | TLS/HTTPS com Let's Encrypt na frente do dashboard e da API |

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

## Webhooks por lojista

Cada merchant pode cadastrar a propria URL de webhook no registro:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "companyName": "Loja Exemplo",
    "email": "loja@exemplo.com",
    "password": "Senha@123",
    "webhookUrl": "https://loja.exemplo.com/webhooks/payments"
  }'
```

Fluxo:

1. `auth-service` valida a URL (apenas `http`/`https` absoluta, ate 500 caracteres) e grava em `merchants.webhook_url`. URL invalida retorna **400**.
2. Ao criar o pagamento, o `payment-core-service` copia a URL para `transactions.webhook_url` e a inclui no evento do RabbitMQ.
3. O `processor-worker` envia `POST <webhookUrl>` com o corpo `APPROVED` e registra a tentativa em `webhook_log`.
4. Sem URL no merchant/transacao, o worker usa `WEBHOOK_DEFAULT_URL`. Se ela tambem estiver vazia, o webhook e ignorado (com log de aviso) e o pagamento continua `APPROVED`.

Para testar localmente, suba um receptor na sua maquina e cadastre `http://host.docker.internal:9999/webhook` (o compose de dev ja inclui o mapeamento `host.docker.internal`).

## Deploy em producao

### 1. Preparar o servidor (EC2/VPS)

- Docker Engine + Docker Compose v2 instalados.
- Diretorio `/opt/payment-gateway` com permissao de escrita.
- DNS: registro `A` de `DOMAIN` apontando para o IP publico.
- Portas liberadas no security group/firewall: `80` e `443` (Caddy) e `22` (SSH).
- `8080`, `8081`, `8082` e `8083` **nao** sao publicadas em producao. Prometheus (`9090`) e Grafana (`3000`) ficam restritos a `127.0.0.1`.

### 2. Configurar variaveis

```bash
cd /opt/payment-gateway
cp .env.example .env
# edite .env:
#   POSTGRES_PASSWORD, RABBITMQ_PASSWORD, GRAFANA_PASSWORD
#   JWT_SECRET (openssl rand -base64 32)
#   DOMAIN, ACME_EMAIL
#   CORS_ALLOWED_ORIGINS=https://<DOMAIN>
```

### 3. Subir a stack

As imagens sao construidas e publicadas pelo workflow de CD. Para um deploy manual, rode o workflow ou faca build/push das 5 imagens antes.

```bash
docker compose -f docker-compose.prod.yml config -q
docker compose -f docker-compose.prod.yml up -d
```

### 4. TLS / dominio (Caddy)

O `docker-compose.prod.yml` inclui um servico `caddy` que:

- obtem e renova certificados Let's Encrypt automaticamente para `DOMAIN`;
- publica apenas `80` e `443`;
- encaminha `/api/*` e `/actuator/*` para o `api-gateway` e o restante para o dashboard;
- adiciona HSTS, `X-Content-Type-Options`, `X-Frame-Options` e `Referrer-Policy`.

O `Caddyfile` fica na raiz do repositorio e e copiado pelo workflow de deploy. Os certificados ficam nos volumes `caddydata`/`caddyconfig`.

O frontend usa `/api` relativo (mesma origem), portanto nao ha hardcode de `localhost`. O CORS do gateway e configuravel por `CORS_ALLOWED_ORIGINS`.

### 5. S3

- Crie o bucket definido em `AWS_S3_BUCKET`.
- Prefira IAM Role/Instance Profile (nao descomente as chaves no `.env`).
- Para chaves estaticas, descomente `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` no `.env`; o worker carrega esse arquivo via `env_file`.
- Permissoes minimas: `s3:PutObject`, `s3:GetObject` em `arn:aws:s3:::SEU_BUCKET/receipts/*`.
- Para MinIO/S3 compativel, defina `AWS_S3_ENDPOINT_URL` e `AWS_S3_PATH_STYLE=true`.

### 6. CI/CD (GitHub Actions)

Secrets necessarios no repositorio:

| Secret | Uso |
|---|---|
| `DOCKER_HUB_USERNAME` / `DOCKER_HUB_TOKEN` | push das imagens (o username tambem e o namespace das imagens) |
| `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` | deploy via SSH |

O workflow `.github/workflows/deploy.yml`:

1. roda os testes;
2. builda as 5 imagens (4 servicos Java + frontend) com a tag do commit;
3. publica no Docker Hub;
4. copia `docker-compose.prod.yml`, `Caddyfile`, `prometheus.yml`, `scripts/` e `grafana/` para `/opt/payment-gateway`;
5. atualiza a tag no compose e roda `docker compose up -d`.

O arquivo `/opt/payment-gateway/.env` precisa existir no servidor.

## Backup e restore do PostgreSQL

O script `scripts/backup.sh` gera um dump comprimido, valida o arquivo (`gzip -t`), grava o `.sha256` e aplica retencao:

```bash
cd /opt/payment-gateway
BACKUP_DIR=/var/backups/payment-gateway BACKUP_RETENTION_DAYS=7 bash scripts/backup.sh
```

Agendamento com cron (diario as 03:00):

```cron
0 3 * * * cd /opt/payment-gateway && BACKUP_DIR=/var/backups/payment-gateway BACKUP_RETENTION_DAYS=7 bash scripts/backup.sh >> /var/log/payment-gateway-backup.log 2>&1
```

Restore (⚠️ sobrescreve os dados atuais):

```bash
gunzip -c /var/backups/payment-gateway/payment_gateway_AAAAMMDDTHHMMSSZ.sql.gz \
  | docker compose -f docker-compose.prod.yml exec -T postgres \
      psql -U admin -d payment_gateway
```

Recomendacoes:

- copie os dumps para fora do servidor (S3, outro volume ou outra regiao);
- teste o restore periodicamente em um banco separado;
- mantenha uma copia antes de qualquer migracao de risco.

## Observabilidade

- Prometheus raspa `api-gateway`, `auth-service`, `payment-core-service` e `processor-worker` pelo nome dos servicos na rede Docker.
- Grafana ja provisiona o datasource do Prometheus.
- Healthchecks usam `/actuator/health`; metricas em `/actuator/prometheus`.

## Seguranca

- Nunca commite `.env`; rotacione as senhas de exemplo antes de producao.
- O `JWT_SECRET` de desenvolvimento nao deve ser usado em producao.
- Servicos internos nao expoem portas em producao; apenas Caddy (80/443) e SSH.
- Webhook por lojista validado na borda (`http`/`https` absoluta); o `WEBHOOK_DEFAULT_URL` deve apontar para um endpoint confiavel.
- Webhooks sao URLs escolhidas pelo lojista: restrinja a saida de rede do worker (egress/allowlist) para reduzir risco de SSRF contra a rede interna e o metadata endpoint da cloud.
- Limitacao conhecida: o `auth-service` ainda nao valida JWT em rotas protegidas (nao ha filtro JWT) e o `payment-core-service` nao exige autenticacao; o `X-API-Key` hoje e usado apenas para rate limit. Antes de expor a API publicamente, implemente o filtro JWT no auth-service e a validacao no gateway/payment-core.
