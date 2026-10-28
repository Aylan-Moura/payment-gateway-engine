package com.paymentgateway.core.domain;

import java.util.UUID;

/**
 * Dados do merchant necessarios para autorizar um pagamento no payment-core.
 *
 * @param id         identificador do merchant
 * @param apiKey     chave de API usada no header X-API-Key
 * @param webhookUrl URL de webhook configurada (pode ser nula)
 */
public record MerchantAccount(UUID id, String apiKey, String webhookUrl) {
}
