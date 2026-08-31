package com.paymentgateway.auth.security;

import com.paymentgateway.auth.domain.MerchantEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

public class MerchantUserDetails implements UserDetails {

    private final MerchantEntity merchant;

    public MerchantUserDetails(MerchantEntity merchant) {
        this.merchant = merchant;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return merchant.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return merchant.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
    public MerchantEntity getMerchant() {
        return merchant;
    }
}
