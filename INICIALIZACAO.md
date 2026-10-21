# Guia de Inicialização - Payment Gateway Engine

Este documento fornece as instruções passo a passo para inicializar o projeto e observar seu funcionamento completo.

## 1. Pré-requisitos

Certifique-se de ter as seguintes ferramentas instaladas em sua máquina:
- **Java 17** ou superior
- **Maven 3.8+**
- **Docker** e **Docker Compose**

## 2. Passo a Passo para Inicialização

### Passo 1: (Opcional) Compilar os Microsserviços
As imagens Java usam build multi-stage: o Maven roda dentro do Docker. Portanto, **nao e necessario** compilar antes.

Se quiser rodar os testes/artefatos fora do Docker:

```bash
mvn clean package
```

### Passo 2: Subir a Infraestrutura e Serviços
Utilize o Docker Compose para subir o banco de dados, mensageria, cache e todos os microsserviços:

```bash
docker compose up -d --build
```
Este comando irá:
- Baixar as imagens necessárias (Postgres, Redis, RabbitMQ, Grafana, Prometheus).
- Construir as imagens locais para cada microsserviço.
- Iniciar todos os containers em segundo plano.

### Passo 3: Verificar o Status
Aguarde alguns instantes para que os serviços inicializem e verifique se todos estão rodando:

```bash
docker-compose ps
```

---

## 3. Acesso aos Componentes do Sistema

Após a inicialização, você poderá acessar as seguintes interfaces:

| Componente | URL | Credenciais (se aplicável) |
| :--- | :--- | :--- |
| **Frontend Dashboard** | [http://localhost:3001](http://localhost:3001) | - |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | - |
| **RabbitMQ Management** | [http://localhost:15672](http://localhost:15672) | `admin` / `adminpassword` |
| **Grafana (Dashboards)** | [http://localhost:3000](http://localhost:3000) | `admin` / `admin` |
| **Prometheus (Métricas)** | [http://localhost:9090](http://localhost:9090) | - |

---

## 4. Testando o Funcionamento (Fluxo Completo)

Para ver o sistema em ação, siga este fluxo:

1.  **Acesse o Dashboard**: Vá para `http://localhost:3001`.
2.  **Crie uma Conta de Merchant**: Registre-se no frontend. Isso salvará seus dados no `auth-service`.
3.  **Realize um Pagamento**: Utilize a interface de checkout para simular uma transação.
4.  **Acompanhe o Processamento**:
    - O `payment-core-service` receberá a requisição e salvará a transação como `PENDING`.
    - Uma mensagem será enviada para o **RabbitMQ**.
    - O `processor-worker` consumirá a mensagem, processará o pagamento (mudando para `APPROVED`) e gerará um recibo.
5.  **Verifique os Logs**:
    ```bash
    docker-compose logs -f processor-worker
    ```
6.  **Monitore no Grafana**: Acesse o Grafana para ver os gráficos de processamento de transações em tempo real.

---

## 5. Comandos Úteis de Manutenção

- **Parar tudo**: `docker-compose down`
- **Ver logs de um serviço específico**: `docker-compose logs -f <nome-do-serviço>`
- **Reiniciar um serviço**: `docker-compose restart <nome-do-serviço>`
- **Limpar volumes (resetar banco de dados)**: `docker-compose down -v`
