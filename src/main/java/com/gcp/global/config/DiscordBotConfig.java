package com.gcp.global.config;


import com.gcp.domain.discord.service.GcpBotService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DiscordBotConfig {

    @Value("${bot.token}")
    private String token;
    private final GcpBotService gcpBotService;

    @Bean
    public JDA jda() throws Exception {
        return JDABuilder.create(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(gcpBotService)
                .build();
    }
}
