package com.gcp.domain.oauth2.service;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.oauth2.exception.OAuth2AuthenticationProcessingException;
import com.gcp.domain.oauth2.user.OAuth2UserInfo;
import com.gcp.domain.oauth2.user.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Service
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private OAuth2UserRequest oAuth2UserRequest;
    private final DiscordUserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationProcessingException {
        log.info("🔍 OAuth2UserService: 사용자 정보 요청 시작");
        this.oAuth2UserRequest = oAuth2UserRequest;

        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        log.info("🔍 OAuth2UserService: 사용자 정보 로드 완료 -> {}", oAuth2User.getAttributes());


        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {

        String registrationId = userRequest.getClientRegistration()
                .getRegistrationId();
        String accessToken = userRequest.getAccessToken().getTokenValue();


        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId,
                accessToken,
                oAuth2User.getAttributes());

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        return new OAuth2UserPrincipal(oAuth2UserInfo);
    }
}