package com.paymentgateway.core.usecase.port.out;

import java.util.UUID;

/**
 * Resolve a URL de webhook configurada pelo merchant no momento da criacao
 * da transacao. O valor e persistido junto da transacao para que o
 * processor-worker nao dependa do schema do auth-service.
 */
public interface MerchantWebhookPort {

    /**
     * @return a URL de webhook do merchant ou {@code null} quando nao configurada.
     */
    String findWebhookUrl(UUID merchantId);
}
