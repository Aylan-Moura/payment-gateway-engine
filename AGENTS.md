# AGENTS.md

## Projeto
Payment Gateway Engine: Spring Boot (microsserviços) + API Gateway (`:8080`) + Angular (`:3001`) + PostgreSQL + Flyway + Docker Compose.

## Tarefa atual
Seguir o plano em `AGENT_PLAN.md`, fase por fase (0 a 5), rodando a verificação de cada fase antes de avançar. Leia esse arquivo no início da sessão.

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
