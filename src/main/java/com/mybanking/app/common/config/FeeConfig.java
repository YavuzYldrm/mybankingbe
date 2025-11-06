package com.mybanking.app.common.config;

import com.mybanking.app.common.util.FeePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeeConfig {
    @Bean
    public FeePolicy feePolicy() {
        return new FeePolicy.CreditCardOnePercent();
    }
}
