#!/usr/bin/env bash
set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
ORIGIN="${ORIGIN:-http://localhost:3001}"
EMAIL="smoke-$(date +%s)@exemplo.com"
PASSWORD="Password@123"

echo "=== Smoke Test: Verificando Gateway e Auth Service ==="

echo "1. Testando preflight OPTIONS para CORS..."
CORS_HEADER=$(curl -s -I -X OPTIONS "$GATEWAY_URL/api/v1/auth/register" \
  -H "Origin: $ORIGIN" \
  -H "Access-Control-Request-Method: POST" | grep -i "Access-Control-Allow-Origin" || true)

echo "Cabeçalho retornado: $CORS_HEADER"

if [ -z "$CORS_HEADER" ]; then
  echo "Falha: preflight não retornou Access-Control-Allow-Origin."
  exit 1
fi

echo "2. Testando cadastro de lojista via Gateway (/api/v1/auth/register) com $EMAIL..."
REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"companyName\":\"SmokeTest Corp\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
REGISTER_STATUS=$(echo "$REGISTER_RESPONSE" | tail -n1)
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')

echo "Status HTTP do registro: $REGISTER_STATUS (esperado 200)"
echo "Corpo do registro: $REGISTER_BODY"

if [ "$REGISTER_STATUS" -ne 200 ] && [ "$REGISTER_STATUS" -ne 201 ]; then
  echo "Falha: registro retornou HTTP $REGISTER_STATUS."
  exit 1
fi

echo "2b. Testando registro duplicado (mesmo e-mail)..."
DUPLICATE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"companyName\":\"SmokeTest Corp\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
echo "Status HTTP do registro duplicado: $DUPLICATE_STATUS (esperado 409)"

if [ "$DUPLICATE_STATUS" -ne 409 ]; then
  echo "Falha: e-mail duplicado deveria retornar 409, retornou $DUPLICATE_STATUS."
  exit 1
fi

echo "3. Testando login via Gateway (/api/v1/auth/login)..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$GATEWAY_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

echo "Status HTTP do login: $LOGIN_STATUS (esperado 200)"

if [ "$LOGIN_STATUS" -ne 200 ]; then
  echo "Falha: login retornou $LOGIN_STATUS."
  exit 1
fi

TOKEN=$(echo "$LOGIN_BODY" | grep -oE '"token":"[^"]+"' | cut -d'"' -f4)
APIKEY=$(echo "$LOGIN_BODY" | grep -oE '"apiKey":"[^"]+"' | cut -d'"' -f4)
MERCHANT=$(echo "$LOGIN_BODY" | grep -oE '"merchantId":"[^"]+"' | cut -d'"' -f4)

echo "4. Testando criação de pagamento via Gateway (/api/v1/payments)..."
PAYMENT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-API-Key: $APIKEY" \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":99.90,\"paymentMethod\":\"PIX\"}")

echo "Status HTTP do pagamento: $PAYMENT_STATUS (esperado 201)"

if [ "$PAYMENT_STATUS" -ne 201 ]; then
  echo "Falha: criação de pagamento retornou $PAYMENT_STATUS."
  exit 1
fi

echo "4a. Testando pagamento sem token (esperado 401)..."
NOAUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $APIKEY" \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":10.00,\"paymentMethod\":\"PIX\"}")

echo "Status HTTP sem token: $NOAUTH_STATUS (esperado 401)"

if [ "$NOAUTH_STATUS" -ne 401 ]; then
  echo "Falha: pagamento sem token deveria retornar 401, retornou $NOAUTH_STATUS."
  exit 1
fi

echo "4b. Testando merchantId de outro lojista (esperado 403)..."
FOREIGN_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GATEWAY_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-API-Key: $APIKEY" \
  -d "{\"merchantId\":\"00000000-0000-0000-0000-000000000000\",\"amount\":10.00,\"paymentMethod\":\"PIX\"}")

echo "Status HTTP merchantId estrangeiro: $FOREIGN_STATUS (esperado 403)"

if [ "$FOREIGN_STATUS" -ne 403 ]; then
  echo "Falha: merchantId de outro lojista deveria retornar 403, retornou $FOREIGN_STATUS."
  exit 1
fi

echo "4c. Testando webhook inválido (esperado 400)..."
WEBHOOK_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$GATEWAY_URL/api/v1/merchants/me/webhook" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"webhookUrl\":\"ftp://loja.exemplo.com/webhook\"}")

echo "Status HTTP webhook inválido: $WEBHOOK_STATUS (esperado 400)"

if [ "$WEBHOOK_STATUS" -ne 400 ]; then
  echo "Falha: webhook inválido deveria retornar 400, retornou $WEBHOOK_STATUS."
  exit 1
fi

echo "=== Smoke Test concluído com sucesso ==="
