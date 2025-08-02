package com.gcp.domain.oauth2.handler;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.gcp.domain.oauth2.service.OAuth2UserPrincipal;
import com.gcp.domain.oauth2.user.OAuth2UserUnlinkManager;
import com.gcp.domain.oauth2.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gcp.domain.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.MODE_PARAM_COOKIE_NAME;
import static com.gcp.domain.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;


@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;
    private final OAuth2UserUnlinkManager oAuth2UserUnlinkManager;
    private final DiscordUserRepository discordUserRepository;

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {


        String targetUrl = determineTargetUrl(request, response, authentication);


        OAuth2UserPrincipal principal = getOAuth2UserPrincipal(authentication);

        if (principal != null) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());


            String state = request.getParameter("state");

            Map<String, String> parsed = Arrays.stream(state.split("&"))
                    .map(s -> s.split("="))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

            String userId = parsed.get("userId");
            String guildId = parsed.get("guildId");

            if(userId == null || guildId == null){
                log.error("state 파라미터에 필수 정보가 없습니다: {}", state);
                throw new IllegalArgumentException("Invalid state parameter");
            }

            DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId)
                           .orElseThrow(() -> new IllegalStateException(
                                    String.format("DiscordUser를 찾을 수 없습니다: userId=%s, guildId=%s", userId, guildId)));

            String googleAccessToken = client.getAccessToken().getTokenValue();
            String googleRefreshToken = client.getRefreshToken().getTokenValue();
            Instant accessTokenExpiresAt = client.getAccessToken().getExpiresAt();;


            if(googleAccessToken != null) {
                discordUser.updateAccessToken(googleAccessToken);
                discordUser.updateAccessTokenExpiration(LocalDateTime.ofInstant(Objects.requireNonNull(accessTokenExpiresAt), ZoneId.of("Asia/Seoul")));
            }

            if (googleRefreshToken != null){
                discordUser.updateRefreshToken(googleRefreshToken);
            }


            log.info("✅ OAuth 로그인 성공! 쿠키 설정 완료! 리디렉션 실행: {}", targetUrl);
        }

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
        log.info("redirectUrl: {}", redirectUri);
        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        log.info("targetUrl: {}", targetUrl);
        String mode = CookieUtils.getCookie(request, MODE_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse("");

        OAuth2UserPrincipal principal = getOAuth2UserPrincipal(authentication);
        if (principal == null) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "Login failed")
                    .build().toUriString();
        }

        log.info("🔹 로그인 성공: mode={}, email={}", mode, principal.getUserInfo().getEmail());

        if ("login".equalsIgnoreCase(mode)) {
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .build().toUriString();
        }

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", "Login failed")
                .build().toUriString();
    }

    private OAuth2UserPrincipal getOAuth2UserPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2UserPrincipal) {
            return (OAuth2UserPrincipal) principal;
        }
        return null;
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}