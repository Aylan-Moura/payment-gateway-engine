package com.paymentgateway.auth.dto;
import lombok.Data;
@Data
public class RegisterRequest {
    private String companyName;
    private String email;
    private String password;
}
