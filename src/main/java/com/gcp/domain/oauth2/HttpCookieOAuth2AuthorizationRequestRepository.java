package com.gcp.domain.oauth2;


import com.gcp.domain.oauth2.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;




@RequiredArgsConstructor
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";
    public static final String MODE_PARAM_COOKIE_NAME = "mode";
    public static final String ACCESS_TYPE_NAME = "access_type";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {

        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
            CookieUtils.deleteCookie(request, response, ACCESS_TYPE_NAME);
            return;
        }

        CookieUtils.addCookie(response,
                OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME,
                CookieUtils.serialize(authorizationRequest),
                COOKIE_EXPIRE_SECONDS);

        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtils.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
            System.out.println("redirect_uri 쿠키 저장됨: " + redirectUriAfterLogin);
        } else {
            System.out.println("redirect_uri 쿠키가 없음");
        }

        String mode = request.getParameter(MODE_PARAM_COOKIE_NAME);
        String accessType = request.getParameter(ACCESS_TYPE_NAME);
        if (StringUtils.hasText(mode)) {
            CookieUtils.addCookie(response, MODE_PARAM_COOKIE_NAME, mode, COOKIE_EXPIRE_SECONDS);
            System.out.println("mode 쿠키 저장됨: " + mode);
        } else {
            System.out.println("mode 쿠키가 없음");
        }
        if (StringUtils.hasText(accessType)){
            CookieUtils.addCookie(response, ACCESS_TYPE_NAME, accessType, COOKIE_EXPIRE_SECONDS);
            System.out.println("accessType 쿠키 저장됨: " + accessType);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, MODE_PARAM_COOKIE_NAME);
        CookieUtils.deleteCookie(request, response, ACCESS_TYPE_NAME);
    }
}