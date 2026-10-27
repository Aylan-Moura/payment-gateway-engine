package com.paymentgateway.auth.exception;

/**
 * Lancada quando o merchant informa uma URL de webhook que nao e uma URL
 * absoluta http(s) valida. Mapeada para HTTP 400 pelo GlobalExceptionHandler.
 */
public class InvalidWebhookUrlException extends RuntimeException {

    public InvalidWebhookUrlException(String message) {
        super(message);
    }
}
