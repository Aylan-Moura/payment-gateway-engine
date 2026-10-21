# Relatório Técnico: Diagnóstico de Erros de CORS, Roteamento e Inicialização (Payment Gateway Engine)

## 1. Visão Geral do Problema
Durante os testes de acesso e cadastro de novos usuários na plataforma (Frontend Dashboard em `http://localhost:3001`), o sistema apresentou uma sequência de falhas em camadas distintas da arquitetura de microsserviços:
1. **Bloqueio por política de CORS (Cross-Origin Resource Sharing).**
2. **Erro HTTP 403 (Forbidden) devido a inconsistências de roteamento/URL.**
3. **Erro HTTP 500 (Internal Server Error) causado por falhas críticas na inicialização do microsserviço de autenticação (`auth-service`) decorrentes do versionamento de banco de dados (Flyway).**

---

## 2. Análise Detalhada das Camadas e Falhas

### FASE 1: Conflito de CORS (Access-Control-Allow-Origin)
* **Sintoma no Console do Navegador:** 
  `Access to fetch at 'http://localhost:8080/api/v1/auth/register' from origin 'http://localhost:3001' has been blocked by CORS policy: The 'Access-Control-Allow-Origin' header contains multiple values 'http://localhost:3001, http://localhost:3001', but only one is allowed.`
* **Causa Raiz:** 
  Ocorre quando há **duplicidade de configuração CORS**. Tanto o **API Gateway** (`api-gateway`) quanto o **Serviço de Autenticação** (`auth-service`) injetavam simultaneamente os cabeçalhos de controle de acesso nas respostas HTTP, resultando em múltiplos valores duplicados rejeitados pelo navegador.
* **Solução Aplicada:**
  * Desativação do bean de CORS e do filtro padrão de CORS no `SecurityConfig.java` do `auth-service`.
  * Centralização e refinamento da política global de CORS no arquivo `api-gateway/src/main/resources/application.yml`, mapeando explicitamente as origens permitidas (`http://localhost:3001`, `http://localhost:3000`) com credenciais habilitadas (`allow-credentials: true`).

---

### FASE 2: Erro HTTP 403 (Forbidden) / Requisição Bloqueada
* **Sintoma no Console do Navegador:** 
  `POST http://localhost:8080/api/v1/auth/reqister 403 (Forbidden)`
* **Causa Raiz:** 
  Após a resolução do CORS, a requisição passou pelo Gateway, mas retornou proibida (`403`). Analisando o console, identificou-se um **erro de digitação no endpoint chamado pelo frontend (`/reqister` com "q")**, enquanto o backend e o Spring Security mapeavam apenas a rota correta (`/register` com "g"). Rotas não mapeadas ou não autenticadas corretamente caem nas regras de segurança padrão do Spring Security gerando o status `403`.
* **Solução Aplicada:**
  * Validação das rotas no código do frontend Angular (`auth.ts`) e limpeza de cache do navegador (`Hard Reload`) para garantir que os bundles de compilação estáticos estivessem atualizados e apontando para `/api/v1/auth/register`.

---

### FASE 3: Erro HTTP 500 (Internal Server Error) & Falha no Flyway
* **Sintoma nos Logs do Backend (`auth-service`):**
  ```text
  Caused by: org.flywaydb.core.api.exception.FlywayValidateException: Validate failed: Migrations have failed validation
  Detected resolved migration not applied to database: 1.
  ```
* **Causa Raiz:** 
  O framework de migração de banco de dados **Flyway** detectou divergência entre o estado atual das tabelas do banco PostgreSQL e a versão dos scripts de migração (`V1__create_merchants_table.sql`). Como consequência, a aplicação inteira do `auth-service` **falhou ao inicializar (BeanCreationException)**, entrando em um loop infinito de reinicialização (`exited with code 1`). Quando o frontend tentava registrar um usuário, o `auth-service` estava indisponível ou fora do ar, gerando a resposta `500 Internal Server Error` no API Gateway.
* **Solução Aplicada:**
  * Limpeza profunda e recriação do ambiente Docker com remoção de volumes órfãos para garantir um estado limpo do banco de dados relacional:
    ```bash
    sudo docker compose down -v --remove-orphans
    sudo docker compose up -d --build
    ```

---

## 3. Resumo dos Arquivos Modificados no Código
1. **`auth-service/src/main/java/com/paymentgateway/auth/security/SecurityConfig.java`**:
   * Removido/comentado o filtro redundante de CORS para evitar conflitos com o API Gateway.
2. **`api-gateway/src/main/resources/application.yml`**:
   * Ajustada a política `globalcors` para aceitar explicitamente as origens do frontend com suporte a credenciais e métodos HTTP completos.

## 4. Recomendações e Procedimento Operacional Padrão (POP)
Para manter o ambiente estável após alterações de infraestrutura ou banco de dados:
1. Sempre limpe os volumes do Docker caso haja modificações estruturais nas migrações do Flyway (`docker compose down -v`).
2. Utilize o comando `docker compose logs -f <serviço>` para inspecionar rastros de exceção em tempo real sempre que um erro `500` for retornado pela API.
