# 🚀 Contexto do Projeto e Guia de Prompts: Payment Gateway Engine (com TDD, CI/CD Contínuo & Gestão de Contexto)

---

## 📌 1. Contexto Geral do Projeto

Este projeto consiste no desenvolvimento do **Payment Gateway Engine**, uma plataforma distribuída de orquestração e processamento assíncrono de pagamentos (PIX e Cartão de Crédito) focada no ecossistema **Java / Spring Boot**[cite: 1, 2].

### Regras de Ouro para a IA Executora:

1. **Metodologia TDD Obrigatória:** Todo o desenvolvimento de regras de negócio, serviços e endpoints **DEVE seguir o ciclo TDD**:
   - **RED:** Escrever primeiro o teste que falha.
   - **GREEN:** Escrever o código mínimo para o teste passar.
   - **REFACTOR:** Refatorar o código mantendo a suíte de testes verde.
2. **CI/CD Integrado a Cada Passo:** A cada prompt finalizado e validado, você DEVE orientar os comandos de `git add`, `git commit` e `git push` para acionar a pipeline do GitHub Actions e validar a integração na nuvem.
3. **Resumo Técnico & Contexto para Próxima Conversa:** Ao final de CADA resposta/fase, você DEVE incluir obrigatoriamente:
   - **Resumo Técnico:** Decisões de arquitetura, padrões aplicados e contratos criados.
   - **Bloco de Contexto de Transição:** Um texto pronto para ser copiado e colado no início de uma **nova conversa**, garantindo que o próximo chat não perca o contexto nem estoure o limite de tokens.

### Stack Tecnológica Alvo
* **Linguagem & Framework:** Java 17+, Spring Boot 3+ (Spring Cloud Gateway, Spring Security, Spring Data JPA)[cite: 1, 2]
* **Arquitetura:** Clean Architecture / Domain-Driven Design (DDD) básico e Microsserviços[cite: 1, 2]
* **Desenvolvimento Guiado por Testes (TDD):** JUnit 5, Mockito, Testcontainers[cite: 1, 2]
* **Banco de Dados & Cache:** PostgreSQL, Flyway Migrations, Redis[cite: 1, 2]
* **Mensageria:** RabbitMQ (com suporte a Dead Letter Queues - DLQ)[cite: 1, 2]
* **Nuvem & Cloud Services:** AWS (EC2, S3, RDS, IAM)[cite: 1, 2]
* **DevOps & CI/CD:** Docker, Docker Compose, GitHub Actions[cite: 1, 2]
* **Observabilidade & Front-end:** Spring Boot Actuator, Prometheus, Grafana, Swagger UI, Dashboard Web (React ou HTML5/JS)[cite: 1, 2]

---

## 🎯 2. Sequência de Prompts para Desenvolvimento (Prompt-Driven Development)

Execute os prompts abaixo **um por um**. Ao final de cada prompt, exija o Resumo Técnico e o Contexto para a próxima conversa.

---

### Fase 1: Setup Inicial do Projeto, CI/CD Base e Estrutura de Pastas

#### Prompt 1: Criação da Estrutura Base e Pipeline CI Inicial
> "Atue como um Arquiteto de Software Java Sênior. Quero criar a estrutura inicial de um projeto Spring Boot chamado `payment-gateway-engine` usando Maven. A estrutura deve ser um monorepo com suporte a microsserviços ou módulos usando Clean Architecture[cite: 1, 2]. 
> 1. Gere o arquivo `pom.xml` pai e a estrutura de pastas dos módulos: `api-gateway`, `auth-service`, `payment-core-service`, `processor-worker` e `frontend-dashboard`[cite: 1, 2].
> 2. Inclua o arquivo `.github/workflows/ci.yml` do GitHub Actions pré-configurado para rodar `./mvnw test` a cada `push`[cite: 1, 2].
> 3. Forneça os comandos de Git (`git add`, `git commit`, `git push`) para validar a primeira execução do CI.
> 4. **OBRIGATÓRIO AO FINAL:** Forneça o **Resumo Técnico** do que foi criado e a caixa de **Contexto de Transição para o Próximo Chat**."

#### Prompt 2: Configuração do Ambiente Docker Local
> "Preciso subir as dependências do meu projeto localmente via Docker Compose[cite: 1, 2]. Crie um arquivo `docker-compose.yml` contendo os seguintes serviços: PostgreSQL (porta 5432), Redis (porta 6379), RabbitMQ com painel de gerenciamento (portas 5672 e 15672), Prometheus (porta 9090) e Grafana (porta 3000)[cite: 1, 2]. Inclua os volumes de persistência e variáveis de ambiente.
> ao final, forneça os comandos Git para subir ao GitHub, valide no CI e gere o **Resumo Técnico + Contexto de Transição**."

---

### Fase 2: Módulo de Autenticação e Segurança (`auth-service`) via TDD

#### Prompt 3: Modelagem e Migração de Banco com Flyway
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `auth-service`, preciso configurar a persistência para os usuários/lojistas (*Merchants*)[cite: 1, 2]. Crie a migration do Flyway `V1__create_merchants_table.sql` em PostgreSQL para a tabela `merchants` com os campos: id (UUID), company_name, email, password_hash, api_key e created_at[cite: 1, 2]. Crie a entidade JPA e a interface `MerchantRepository`.
> Forneça os comandos Git para envio ao repositório, garantindo que o CI passe 🟢, e finalize com o **Resumo Técnico + Contexto de Transição**."

#### Prompt 4: Autenticação Spring Security + JWT usando TDD (Red -> Green -> Refactor)
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `auth-service`, implemente a autenticação via JWT + Refresh Token usando **TDD estrito**[cite: 1, 2]:
> 1. Escreva primeiro os testes que falham (RED) para registro, login incorreto, emissão de JWT e renovação via `/api/v1/auth/refresh`[cite: 1, 2].
> 2. Implemente o código no Spring Security para fazer os testes passarem (GREEN)[cite: 1, 2].
> 3. Refatore (REFACTOR), forneça os comandos Git para acionar o CI/CD no GitHub Actions e termine com o **Resumo Técnico + Contexto de Transição**."

---

### Fase 3: Core do Gateway e Clean Architecture (`payment-core-service`) via TDD

#### Prompt 5: Estruturação da Clean Architecture e Domínio via TDD
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `payment-core-service`, aplique Clean Architecture estrita e TDD[cite: 1, 2]:
> 1. Escreva primeiro os testes unitários para a entidade de domínio `Transaction` (regras: amount > 0, status inicial `PENDING`, métodos PIX ou CREDIT_CARD)[cite: 1, 2].
> 2. Escreva o código da classe de domínio pura `Transaction` até que os testes passem[cite: 1, 2].
> Crie a estrutura de pacotes: `domain`, `usecase`, `adapters/in/web`, `adapters/out/persistence` e `adapters/out/messaging`[cite: 1, 2].
> Forneça os comandos de commit/push para validar o CI no GitHub e gere o **Resumo Técnico + Contexto de Transição**."

#### Prompt 6: Lógica de Pagamento, UseCase e Enfileiramento via TDD
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Desenvolva o caso de uso `CreateTransactionUseCase` no `payment-core-service` usando TDD[cite: 1, 2]:
> 1. Escreva o teste unitário mockando as portas de saída `SaveTransactionPort` e `PublishPaymentEventPort` (verificando salvamento e envio para a fila `payment.created` do RabbitMQ)[cite: 1, 2].
> 2. Implemente as interfaces, o UseCase e o Controller REST `POST /api/v1/payments` testado via `@WebMvcTest`[cite: 1, 2].
> Forneça os comandos de commit/push para testar no CI e finalize com o **Resumo Técnico + Contexto de Transição**."

---

### Fase 4: Processamento Assíncrono e Workers (`processor-worker`) via TDD

#### Prompt 7: Consumo de Fila e Dead Letter Queue (DLQ) via TDD
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `processor-worker`, use TDD para criar o listener do RabbitMQ na fila `payment.created`[cite: 1, 2]:
> 1. Escreva o teste que simula o recebimento, alteração do status para `APPROVED`/`REJECTED` e o redirecionamento para a Dead Letter Queue (`payment.dlq`) após 3 falhas[cite: 1, 2].
> 2. Implemente a classe de serviço e a configuração do RabbitMQ até o teste passar[cite: 1, 2].
> Passe os comandos do Git para o CI rodar no GitHub e apresente o **Resumo Técnico + Contexto de Transição**."

#### Prompt 8: Upload de Recibos no AWS S3 via TDD
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No `processor-worker`, crie a integração com AWS S3 usando TDD[cite: 1, 2]:
> 1. Crie o teste unitário mockando o `S3Client` para validar se o comprovante (PDF/JSON) é gerado e enviado ao bucket correto[cite: 1, 2].
> 2. Implemente o serviço `S3ReceiptService` com Spring Cloud AWS S3 para os testes passarem[cite: 1, 2].
> Forneça os comandos de commit/push para acionar o CI e gere o **Resumo Técnico + Contexto de Transição**."

#### Prompt 9: Disparo de Webhooks com Retentativas (Spring Retry) via TDD
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Crie o serviço de disparo de Webhooks no `processor-worker` usando TDD[cite: 1, 2]:
> 1. Escreva um teste com `MockRestServiceServer` simulando HTTP POST para o lojista com sucesso (200) e falha com 3 retentativas automáticas (Spring Retry)[cite: 1, 2].
> 2. Escreva o código do serviço e o log na tabela `webhook_logs` até o teste passar[cite: 1, 2].
> Envie os comandos Git para validação no CI e conclua com o **Resumo Técnico + Contexto de Transição**."

---

### Fase 5: API Gateway, Documentação OpenAPI e Dashboard Front-End

#### Prompt 10: Spring Cloud Gateway e Rate Limiting
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `api-gateway`, crie a configuração do Spring Cloud Gateway com Redis[cite: 1, 2]. Configure rotas dinâmicas para `/api/v1/auth/**` (`auth-service`) e `/api/v1/payments/**` (`payment-core-service`)[cite: 1, 2]. Adicione o filtro de Rate Limiting (máx 10 req/s por API Key)[cite: 1, 2].
> Faça o commit/push para o CI do GitHub e apresente o **Resumo Técnico + Contexto de Transição**."

#### Prompt 11: Documentação Interativa com OpenAPI / Swagger UI
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Adicione a dependência `springdoc-openapi-starter-webmvc-ui` nos módulos `auth-service` e `payment-core-service`[cite: 1, 2]. Configure anotações do Swagger nos Controllers e DTOs para expor a documentação em `/swagger-ui.html` com suporte a Bearer JWT[cite: 1, 2].
> Envie os comandos Git para o CI e finalize com o **Resumo Técnico + Contexto de Transição**."

#### Prompt 12: Front-End / Dashboard do Lojista (React ou HTML5/JS)
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> No módulo `frontend-dashboard`, crie a interface web funcional para o painel do lojista[cite: 1, 2]:
> 1. Tela de Login e Cadastro (armazenando JWT no `localStorage`)[cite: 1, 2].
> 2. Formulário para simular cobrança (PIX ou Cartão)[cite: 1, 2].
> 3. Tabela de transações com status em tempo real (`PENDING`, `APPROVED`, `REJECTED`) e download do comprovante do S3[cite: 1, 2].
> Faça o commit/push do front-end e apresente o **Resumo Técnico + Contexto de Transição**."

---

### Fase 6: Testes E2E, Observabilidade e CI/CD Final na Nuvem (AWS)

#### Prompt 13: Testes de Integração com Testcontainers (Ponta a Ponta)
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Crie a suíte de testes E2E no `payment-core-service` utilizando JUnit 5 e **Testcontainers**[cite: 1, 2]. O teste deve subir contêineres reais de PostgreSQL e RabbitMQ validando o fluxo: REST -> Banco -> Broker -> Worker[cite: 1, 2].
> Envie os comandos Git para validar essa suíte completa no CI do GitHub e gere o **Resumo Técnico + Contexto de Transição**."

#### Prompt 14: Observabilidade (Spring Actuator, Prometheus e Grafana)
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Configure `Spring Boot Actuator` e `Micrometer` no `payment-core-service` para exportar métricas Prometheus em `/actuator/prometheus`[cite: 1, 2]. Crie um contador para transações aprovadas vs. rejeitadas e adicione um dashboard básico do Grafana[cite: 1, 2].
> Envie para o GitHub para validar no CI e apresente o **Resumo Técnico + Contexto de Transição**."

#### Prompt 15: Pipeline Completa de Continuous Deployment (CD na AWS EC2)
> "*[COLE O CONTEXTO DE TRANSIÇÃO DO PROMPT ANTERIOR AQUI]*
> Atualize o arquivo `.github/workflows/deploy.yml` para incluir o estágio de CD (Continuous Deployment)[cite: 1, 2]:
> 1. **Test:** Executar `mvn test` (rodando Testcontainers)[cite: 1, 2].
> 2. **Build:** Gerar imagens Docker dos microsserviços[cite: 1, 2].
> 3. **Deploy:** Conectar via SSH em uma instância AWS EC2 e executar `docker-compose up -d` automaticamente a cada `push` na branch `main`[cite: 1, 2].
> Forneça o commit/push final e apresente o **Resumo Técnico Final** do projeto completo."