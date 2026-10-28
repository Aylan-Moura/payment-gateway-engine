package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.domain.MerchantAccount;
import com.paymentgateway.core.usecase.port.out.MerchantAccountPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Le os dados do merchant diretamente da tabela {@code merchants}
 * (banco compartilhado pelos servicos). A leitura e somente leitura e
 * tolerante a falhas: se a consulta falhar, o resultado e vazio e a
 * requisicao falha fechada (401/403) em vez de autorizar por engano.
 */
@Component
public class MerchantAccountAdapter implements MerchantAccountPort {

    private static final Logger log = LoggerFactory.getLogger(MerchantAccountAdapter.class);
    private static final String SELECT_BY_ID = "SELECT id, api_key, webhook_url FROM merchants WHERE id = ?";
    private static final String SELECT_BY_EMAIL = "SELECT id, api_key, webhook_url FROM merchants WHERE email = ?";

    private static final RowMapper<MerchantAccount> MAPPER = (rs, rowNum) -> new MerchantAccount(
            rs.getObject("id", UUID.class),
            rs.getString("api_key"),
            rs.getString("webhook_url")
    );

    private final JdbcTemplate jdbcTemplate;

    public MerchantAccountAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<MerchantAccount> findById(UUID merchantId) {
        if (merchantId == null) {
            return Optional.empty();
        }
        return query(SELECT_BY_ID, merchantId);
    }

    @Override
    public Optional<MerchantAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return query(SELECT_BY_EMAIL, email);
    }

    private Optional<MerchantAccount> query(String sql, Object parameter) {
        try {
            return jdbcTemplate.query(sql, MAPPER, parameter).stream().findFirst();
        } catch (DataAccessException e) {
            log.warn("Could not read merchant data: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
