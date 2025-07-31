package com.gcp.domain.discord.service;

import com.gcp.domain.gcp.service.GcpService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GcpBotService extends ListenerAdapter {
    private final GcpService gcpService;
    private final DiscordUserService discordUserService;


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] parts = message.split(" ");

        User author = event.getAuthor();
        Guild guild = event.getGuild();

        String userName = author.getGlobalName();
        String userId = author.getId(); // 고유 사용자 ID;
        String guildId = guild.getId();
        String guildName = guild.getName();

        if (message.startsWith("/gcp")) {
            if (parts.length < 1) {
                event.getChannel().sendMessage("❌ 사용법: /gcp [명령어] [옵션]").queue();
                return;
            }

            String command = parts[1];
            switch (command) {
                case "init": {
                    if(parts.length > 2){
                        event.getChannel().sendMessage("❌ 사용법: /gcp init").queue();
                        return;
                    }
                    discordUserService.insertDiscordUser(userId, userName, guildId, guildName);
                    event.getChannel().sendMessage(userName + "님 환영합니다.").queue();
                    break;
                }

                case "register": {

                }

                case "start":
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp start {vm_name}").queue();
                        return;
                    }
                    String startVm = parts[2];
                    event.getChannel().sendMessage(gcpService.startVM(userId, guildId, startVm)).queue();
                    break;

                case "stop":
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp stop {vm_name}").queue();
                        return;
                    }
                    String stopVm = parts[2];
                    event.getChannel().sendMessage(gcpService.stopVM(userId, guildId, stopVm)).queue();
                    break;

                case "logs":
                    if (parts.length < 3) {
                        event.getChannel().sendMessage("❌ 사용법: /gcp logs {vm_name}").queue();
                        return;
                    }
                    String logVm = parts[2];
                    List<String> receivedMessages = gcpService.getVmLogs(userId, guildId, logVm);
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
                    event.getChannel().sendMessage(gcpService.getVmList(userId, guildId).toString()).queue();
                    break;
                default:
                    event.getChannel().sendMessage("❌ 지원하지 않는 명령어입니다.").queue();
            }
        }
    }
}