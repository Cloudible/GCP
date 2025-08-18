package com.gcp.domain.gcp.aop;


import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.discord.service.DiscordUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TokenAspect {

    private final DiscordUserRepository discordUserRepository;
    private final DiscordUserService discordUserService;

    @Around("@within(com.gcp.domain.gcp.aop.RequiredValidToken) && args(userId, guildId, ..)")
    public Object validateAndRefreshToken(ProceedingJoinPoint joinPoint, String userId, String guildId) throws Throwable {

        LocalDateTime tokenExp = discordUserRepository.findAccessTokenExpByUserIdAndGuildId(userId, guildId)
                .orElseThrow();

        if (tokenExp.isBefore(LocalDateTime.now())) {
            DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId)
                    .orElseThrow();

            Map<String, Object> reissued = discordUserService.refreshAccessToken(discordUser.getGoogleRefreshToken());

            discordUser.updateAccessToken((String) reissued.get("access_token"));
            discordUser.updateAccessTokenExpiration(
                    LocalDateTime.now().plusSeconds((Integer) reissued.get("expires_in"))
            );
        }

        return joinPoint.proceed();
    }
}