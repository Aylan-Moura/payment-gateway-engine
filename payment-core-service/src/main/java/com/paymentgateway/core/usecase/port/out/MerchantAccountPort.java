package com.paymentgateway.core.usecase.port.out;

import com.paymentgateway.core.domain.MerchantAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * Acesso somente leitura aos dados do merchant (tabela compartilhada pelos servicos).
 */
public interface MerchantAccountPort {

    Optional<MerchantAccount> findById(UUID merchantId);

    Optional<MerchantAccount> findByEmail(String email);
}
