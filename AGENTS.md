# AGENTS.md

## Projeto
Payment Gateway Engine: Spring Boot (microsserviços) + API Gateway (`:8080`) + Angular (`:3001`) + PostgreSQL + Flyway + Docker Compose.

## Estado atual
Deploy readiness concluído: webhook por lojista, RabbitMQ 4.1, TLS com Caddy, backup do PostgreSQL e JWT efetivo. Consulte `README.md` (deploy, backup e segurança) e `SYSTEM_DESIGN.md`. Mudanças planejadas ficam em `openspec/changes`.

## Regras permanentes
- CORS é configurado **somente** no `api-gateway`. Nunca nos serviços de backend.
- Nunca editar migração Flyway já aplicada; criar `V2__...`.
- `ddl-auto` deve ser `validate` (nunca `create`/`update`).
- `docker compose down -v` apaga dados: usar só em dev local e avisar o usuário antes.
- Mostrar o diff antes de aplicar mudanças.
- Para erros 500, ler `docker compose logs <serviço>` antes de alterar código.
- Endpoints de auth do frontend ficam centralizados em uma constante (`AUTH_API`).
- Não avançar de fase se a verificação da fase atual falhar.

## Comandos úteis
```bash
docker compose ps
docker compose logs -f <serviço>
docker compose down -v --remove-orphans && docker compose up -d --build   # só em dev
bash scripts/smoke-test.sh
```
