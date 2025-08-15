package com.gcp.domain.discord.service;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.CLIENT_SECRET;

@Service
@RequiredArgsConstructor
public class DiscordUserService {
    private final DiscordUserRepository discordUserRepository;
    private final RestTemplate restTemplate;


    public boolean insertDiscordUser(String userId, String userName, String guildId, String guildName){
        if (!discordUserRepository.existsByUserIdAndGuildId(userId, guildId)) {
            DiscordUser discordUser = new DiscordUser(userId, userName, guildId, guildName);
            discordUserRepository.save(discordUser);
            return true;
        }
        return false;
    }

    public Map<String, Object> refreshAccessToken(String refreshToken) {
        String url = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", CLIENT_SECRET);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> bodyMap = response.getBody();

            result.put("access_token", bodyMap.get("access_token"));
            result.put("expires_in", bodyMap.get("expires_in"));  // 보통 초 단위 (ex: 3599)
            result.put("scope", bodyMap.get("scope"));
            result.put("token_type", bodyMap.get("token_type"));

            return result;
        } else {
            throw new RuntimeException("리프레시 토큰으로 액세스 토큰 재발급 실패");
        }
    }
}
