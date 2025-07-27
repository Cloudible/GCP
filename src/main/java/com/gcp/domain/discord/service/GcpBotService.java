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
import java.util.List;

@Service
public class GcpBotService extends ListenerAdapter {
    private final GcpService gcpService;

    public GcpBotService(GcpService gcpService, @Value("${bot.token}") String token) throws LoginException {
        this.gcpService = gcpService;
        JDA jda = JDABuilder.createDefault(token,
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
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp logs {vm_name}").queue();
                        return;
                    }
                    String logVm = parts[2];
                    List<String> receivedMessages = gcpService.getVmLogs(logVm);
                    receivedMessages.forEach(
                            receiveMessage -> {
                                event.getChannel().sendMessage(receiveMessage).queue();
                            }
                    );

                    break;

                case "cost":
                    event.getChannel().sendMessage(gcpService.getEstimatedCost()).queue();
                    break;

                case "notify":
                    event.getChannel().sendMessage("✅ GCP VM 상태 변경 시 알림을 받을 수 있습니다!").queue();
                    gcpService.enableVmNotifications();
                    break;

                case "list":
                    event.getChannel().sendMessage(gcpService.getVmList().toString()).queue();
                    break;
                case "create":
                    if (parts.length < 7) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp create {vm_name} {machine_type} {os_image} {boot_disk_gb} {allowHttp} {allowHttps}").queue();
                        return;
                    }

                    String vmName = parts[2];
                    String machineType = parts[3];
                    String osImage = parts[4];
                    int bootDiskGb;
                    try {
                        bootDiskGb = Integer.parseInt(parts[5]);
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage("❌ 디스크 크기는 숫자(GB 단위)여야 합니다.").queue();
                        return;
                    }
                    boolean allowHttp = Boolean.parseBoolean(parts[6]);
                    boolean allowHttps = Boolean.parseBoolean(parts[7]);

                    String result = gcpService.createVM(vmName, machineType, osImage, bootDiskGb, allowHttp, allowHttps);
                    event.getChannel().sendMessage(result).queue();
                    break;

                default:
                    event.getChannel().sendMessage("❌ 지원하지 않는 명령어입니다.").queue();
            }
        }
    }
}