package com.gcp.domain.oauth2.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository repo, String baseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, baseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
        return customize(req, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
        return customize(req,request);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req, HttpServletRequest request) {
        if (req == null) return null;

        String userId = request.getParameter("userId");
        String guildId = request.getParameter("guildId");

        Map<String, Object> extraParams = new HashMap<>(req.getAdditionalParameters());
        extraParams.put("access_type", "offline");
        extraParams.put("prompt", "consent"); // 프로덕션에서 필요 없으면 빼도 됨

        // base state 유지
        String baseState = req.getState();
        StringBuilder combinedState = new StringBuilder(baseState != null ? baseState : "");

        if (userId != null && !userId.isBlank()) {
            combinedState.append(combinedState.length() > 0 ? "&" : "")
                    .append("userId=").append(URLEncoder.encode(userId, StandardCharsets.UTF_8));
        }
        if (guildId != null && !guildId.isBlank()) {
            combinedState.append(combinedState.length() > 0 ? "&" : "")
                    .append("guildId=").append(URLEncoder.encode(guildId, StandardCharsets.UTF_8));
        }

        return OAuth2AuthorizationRequest.from(req)
                .additionalParameters(extraParams)
                .state(combinedState.toString())
                .build();
    }
}
