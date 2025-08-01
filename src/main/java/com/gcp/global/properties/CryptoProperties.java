package com.gcp.global.properties;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "security.crypto")
public class CryptoProperties {
    private String aesKey;
    private String aesIv;

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public void setAesIv(String aesIv) {
        this.aesIv = aesIv;
    }
}