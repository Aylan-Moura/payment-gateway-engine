package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.usecase.port.out.MerchantWebhookPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Le a URL de webhook do merchant diretamente da tabela {@code merchants}
 * (banco compartilhado pelos servicos). A leitura e somente leitura e
 * tolerante a falhas: se a tabela ainda nao existir ou a consulta falhar,
 * a transacao segue sem webhook especifico e o worker usa a URL padrao.
 */
@Component
public class MerchantWebhookAdapter implements MerchantWebhookPort {

    private static final Logger log = LoggerFactory.getLogger(MerchantWebhookAdapter.class);
    private static final String FIND_WEBHOOK_SQL =
            "SELECT webhook_url FROM merchants WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;

    public MerchantWebhookAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String findWebhookUrl(UUID merchantId) {
        if (merchantId == null) {
            return null;
        }

        try {
            List<String> urls = jdbcTemplate.queryForList(FIND_WEBHOOK_SQL, String.class, merchantId);
            return urls.stream()
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (DataAccessException e) {
            log.warn("Could not read webhook_url for merchant {}: {}", merchantId, e.getMessage());
            return null;
        }
    }
}
