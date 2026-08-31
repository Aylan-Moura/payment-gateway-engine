package com.paymentgateway.auth.security;

import com.paymentgateway.auth.domain.MerchantEntity;
import com.paymentgateway.auth.repository.MerchantRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MerchantRepository repository;

    public UserDetailsServiceImpl(MerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MerchantEntity merchant = repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Merchant not found"));
        return new MerchantUserDetails(merchant);
    }
}
