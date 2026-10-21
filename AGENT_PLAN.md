# AGENT_PLAN.md: Correção de CORS, Rotas e Inicialização (Payment Gateway Engine)

## Contexto

Projeto: Spring Boot (microsserviços) + API Gateway + Angular (`http://localhost:3001`) + PostgreSQL + Flyway + Docker Compose.

**Objetivo:** `POST http://localhost:8080/api/v1/auth/register` deve funcionar a partir do frontend, sem erros de CORS, 403 ou 500, e o ambiente deve ser reproduzível do zero.

## Regras de conduta do agente

1. Não apagar volumes Docker sem avisar o usuário antes.
2. Mostrar o diff antes de aplicar mudanças.
3. Após cada fase, rodar a verificação e reportar o resultado.
4. Não avançar para a próxima fase se a verificação falhar.
5. Nunca editar uma migração Flyway já aplicada (`V1__...`). Mudanças viram `V2__...`.
6. `docker compose down -v` é aceitável **apenas em desenvolvimento local**. Em homologação ou produção, usar `flyway repair`, baseline ou migrações corretivas.

## Histórico das falhas (resumo)

| Fase | Sintoma | Causa provável |
|------|---------|----------------|
| CORS | `Access-Control-Allow-Origin` com valores duplicados | Gateway e `auth-service` emitindo CORS ao mesmo tempo |
| 403 | `POST /api/v1/auth/reqister` | Typo no endpoint do frontend (`reqister`) |
| 500 | `FlywayValidateException: Detected resolved migration not applied to database: 1` | Estado do banco divergente das migrações; `auth-service` em restart loop |

> **Atenção:** limpar cache do navegador não corrige typo. A correção está no código-fonte. Além disso, o Spring Cloud Gateway normalmente devolve 502/503 quando o serviço de destino está fora do ar; se o erro foi 500, confirmar nos logs do gateway a origem real.

---

## Fase 0: Diagnóstico (somente leitura)

```bash
docker compose ps
docker compose logs --tail=200 api-gateway auth-service postgres
grep -rn "auth/" frontend/src
grep -rniE "reqister|regsiter|registar" .
grep -rniE "cors|allowedOrigins|globalcors|CrossOrigin" --include=*.java --include=*.yml .
```

```sql
SELECT installed_rank, version, description, success FROM flyway_schema_history;
```

**Entregável:** relatório curto com a causa confirmada de cada falha.

---

## Fase 1: CORS em um único ponto (Gateway)

**Regra:** somente o `api-gateway` emite cabeçalhos CORS.

`api-gateway/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_FIRST
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:3001"
            allowedMethods: [GET, POST, PUT, PATCH, DELETE, OPTIONS]
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
```

`auth-service/.../security/SecurityConfig.java`:

```java
http.cors(AbstractHttpConfigurer::disable)
    .csrf(AbstractHttpConfigurer::disable)
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
        .anyRequest().authenticated());
```

Remover também qualquer `CorsConfigurationSource`, `@CrossOrigin` ou `WebMvcConfigurer.addCorsMappings` nos demais serviços.

**Verificação (preflight):**

```bash
curl -i -X OPTIONS http://localhost:8080/api/v1/auth/register \
  -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: POST"
```

Esperado: status 200/204 e **exatamente um** `Access-Control-Allow-Origin: http://localhost:3001`.

---

## Fase 2: Rotas e 403

- Corrigir o endpoint no frontend (`auth.ts`) e centralizar as URLs:

  ```ts
  export const AUTH_API = {
    register: '/api/v1/auth/register',
    login: '/api/v1/auth/login',
  } as const;
  ```

- Confirmar que as rotas do gateway (`Path=/api/v1/auth/**`) casam com os controllers.
- Garantir que as rotas públicas estão em `permitAll()` no `auth-service` e no gateway (se houver filtro JWT).
- Reconstruir o frontend sem cache (`rm -rf .angular/cache && ng build`).

**Verificação:**

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Teste","email":"teste@exemplo.com","password":"Senha@123"}'
```

Esperado: 200/201. Um 404 indica problema de rota; um 403 indica problema de segurança.

---

## Fase 3: Flyway e inicialização

1. Identificar a causa com a consulta ao `flyway_schema_history` (Fase 0). Causas comuns: volume antigo com schema criado por `ddl-auto`, baseline em versão maior, migração editada após aplicada.
2. **Somente em desenvolvimento** (avisar o usuário antes):

   ```bash
   docker compose down -v --remove-orphans
   docker compose up -d --build
   ```

3. Evitar recorrência:

   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate   # nunca create/update junto com Flyway
     flyway:
       enabled: true
       locations: classpath:db/migration
       baseline-on-migrate: false
   ```

4. Ordem de subida no `docker-compose.yml`, para evitar restart loop:

   ```yaml
   postgres:
     healthcheck:
       test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER}"]
       interval: 5s
       retries: 10
   auth-service:
     depends_on:
       postgres:
         condition: service_healthy
   ```

**Verificação:** `docker compose ps` com todos os serviços `running`/`healthy` e `flyway_schema_history` com `success = true`.

---

## Fase 4: Teste ponta a ponta

1. Subir tudo do zero.
2. Em `http://localhost:3001`, cadastrar um usuário e conferir na aba Network: preflight OK, POST com 2xx e um único cabeçalho CORS.
3. Conferir o registro no banco.
4. Registrar o mesmo e-mail novamente e verificar que retorna **409/400** (nunca 500).

---

## Fase 5: Prevenção

- Criar `scripts/smoke-test.sh` com os `curl` das fases 1 e 2.
- Criar teste automatizado (Testcontainers + MockMvc) para o registro.
- Documentar no README o POP: quando usar `down -v` e quando usar `flyway repair`.
- Adicionar `@ControllerAdvice` para devolver erros de negócio com status corretos, em vez de 500 genérico.

---

## Critérios de aceite

- [ ] Preflight retorna um único `Access-Control-Allow-Origin`.
- [ ] `/api/v1/auth/register` responde 2xx a partir do frontend.
- [ ] Nenhum typo de rota no repositório.
- [ ] `auth-service` sobe estável (sem restart loop) com banco limpo.
- [ ] Smoke test passa do zero.
- [ ] E-mail duplicado retorna 409/400, não 500.

## POP rápido

1. Alterou migrações Flyway em dev? `docker compose down -v` e suba de novo.
2. Erro 500 na API? `docker compose logs -f <serviço>` e leia o stack trace antes de qualquer mudança.
3. Erro de CORS? Verifique se **só o gateway** emite os cabeçalhos.
