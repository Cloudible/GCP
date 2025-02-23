package com.gcp.domain.discord.service;

import com.gcp.domain.gcp.service.GcpService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.io.IOException;

@Service
public class GcpBotService extends ListenerAdapter {
    private final GcpService gcpService;
    @Value("${bot.token}")
    private String TOKEN;

    public GcpBotService(GcpService gcpService) throws LoginException {
        this.gcpService = gcpService;
        JDA jda = JDABuilder.createDefault(TOKEN,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(this)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] parts = message.split(" ");

        if (message.startsWith("/gcp")) {
            if (parts.length < 2) {
                event.getChannel().sendMessage("❌ 사용법: /gcp [명령어] [옵션]").queue();
                return;
            }

            String command = parts[1];
            switch (command) {
                case "start":
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp start {vm_name}").queue();
                        return;
                    }
                    String startVm = parts[2];
                    event.getChannel().sendMessage(gcpService.startVM(startVm)).queue();
                    break;

                case "stop":
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp stop {vm_name}").queue();
                        return;
                    }
                    String stopVm = parts[2];
                    event.getChannel().sendMessage(gcpService.stopVM(stopVm)).queue();
                    break;

                case "logs":
                    event.getChannel().sendMessage(gcpService.getVmLogs()).queue();
                    break;

                case "cost":
                    event.getChannel().sendMessage(gcpService.getEstimatedCost()).queue();
                    break;

                case "notify":
                    event.getChannel().sendMessage("✅ GCP VM 상태 변경 시 알림을 받을 수 있습니다!").queue();
                    gcpService.enableVmNotifications();
                    break;

                default:
                    event.getChannel().sendMessage("❌ 지원하지 않는 명령어입니다.").queue();
            }
        }
    }
}