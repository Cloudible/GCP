package com.gcp.domain.gcp.util;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.gcp.repository.GcpProjectRepository;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GcpAuthUtil {
    private final GcpProjectRepository gcpProjectRepository;
    private final DiscordUserRepository discordUserRepository;

    public String getAccessToken(String userId, String channelId) throws IOException {

        DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, channelId).orElseThrow();

        String base64EncodedKey = gcpProjectRepository.findByDiscordUser(discordUser)
                .orElseThrow(() -> new RuntimeException("GCP 프로젝트가 등록되지 않았습니다."))
                .getCredentialsJson();

        byte[] decodedJsonBytes = Base64.getDecoder().decode(base64EncodedKey);
        ByteArrayInputStream jsonStream = new ByteArrayInputStream(decodedJsonBytes);

        List<String> defaultScopes = List.of(
                "https://www.googleapis.com/auth/cloud-platform",
                "https://www.googleapis.com/auth/compute",
                "https://www.googleapis.com/auth/compute.readonly",
                "https://www.googleapis.com/auth/logging.read"
        );

        GoogleCredentials credentials = GoogleCredentials.fromStream(jsonStream)
                .createScoped(defaultScopes);

        credentials.refreshIfExpired();


        return credentials.getAccessToken().getTokenValue();
    }
}