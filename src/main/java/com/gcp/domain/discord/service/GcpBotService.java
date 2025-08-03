package com.gcp.domain.discord.service;

import com.gcp.domain.gcp.service.GcpService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GcpBotService extends ListenerAdapter {
    private final GcpService gcpService;
    private final DiscordUserService discordUserService;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("gcp")) return;

        User author = event.getUser();
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply("❌ 길드 정보를 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        String userName = author.getGlobalName();
        String userId = author.getId();
        String guildId = guild.getId();
        String guildName = guild.getName();

        switch (event.getSubcommandName()) {
            case "init" -> {
                boolean inserted = discordUserService.insertDiscordUser(userId, userName, guildId, guildName);
                String responseMsg = inserted
                        ? userName + "님 환영합니다."
                        : userName + "님은 " + guildName + "에 이미 등록되어 있습니다.";
                event.reply(responseMsg).queue();
            }
            case "register" -> {
                String userProfile = Optional.ofNullable(author.getAvatarUrl())
                        .orElse(author.getDefaultAvatarUrl());

                String infoRaw = userName + "|" + guildName + "|" + userProfile;

                String encodedInfo = Base64.getUrlEncoder()
                        .encodeToString(infoRaw.getBytes(StandardCharsets.UTF_8));


                String rawRedirectUrl = "https://gcpassist.com?info=" + encodedInfo;
                String encodedRedirectUrl = URLEncoder.encode(rawRedirectUrl, StandardCharsets.UTF_8);


                String redirectUri = UriComponentsBuilder
                        .fromHttpUrl("https://gcpassist.com/oauth2/authorization/google")
                        .queryParam("access_type", "offline")
                        .queryParam("mode", "login")
                        .queryParam("redirect_uri", encodedRedirectUrl)
                        .queryParam("userId", userId)
                        .queryParam("guildId", guildId)
                        .build()
                        .toUriString();

                event.reply("👇 아래 링크를 클릭해서 Google 계정을 연결해주세요:\n" + redirectUri).queue();
            }
            case "start" -> {
                String vmName = getRequiredOption(event, "vm_name");
                event.reply(gcpService.startVM(userId, guildId, vmName)).queue();
            }
            case "stop" -> {
                String vmName = getRequiredOption(event, "vm_name");
                event.reply(gcpService.stopVM(userId, guildId, vmName)).queue();
            }
            case "logs" -> {
                String vmName = getRequiredOption(event, "vm_name");
                event.deferReply().queue();

                List<String> logs = gcpService.getVmLogs(userId, guildId, vmName);

                if (logs.isEmpty()) {
                    event.getHook().sendMessage("📭 로그가 없습니다.").queue();
                    return;
                }

                for (String log : logs) {
                    event.getHook().sendMessage("```bash\n" + log + "\n```").queue();
                }
            }
            case "cost" -> event.reply(gcpService.getEstimatedCost()).queue();
            case "notify" -> {
                gcpService.enableVmNotifications();
                event.reply("✅ GCP VM 상태 변경 시 알림을 받을 수 있습니다!").queue();
            }
            case "list" -> event.reply(gcpService.getVmList(userId, guildId).toString()).queue();
            case "create" -> {
                try {
                    String vmName = getRequiredOption(event, "vm_name");
                    String machineType = getRequiredOption(event, "machine_type");
                    String osImage = getRequiredOption(event, "os_image");
                    int bootDiskGb = Integer.parseInt(getRequiredOption(event, "boot_disk_gb"));
                    boolean allowHttp = Boolean.parseBoolean(getRequiredOption(event, "allow_http"));
                    boolean allowHttps = Boolean.parseBoolean(getRequiredOption(event, "allow_https"));

                    if (bootDiskGb <= 9) {
                        event.reply("❌ 디스크 크기는 10 이상이 되어야 합니다.").queue();
                        return;
                    }

                    String result = gcpService.createVM(userId, guildId, vmName, machineType, osImage, bootDiskGb, allowHttp, allowHttps);
                    event.reply(result).queue();
                } catch (Exception e) {
                    event.reply("❌ VM 생성 중 오류 발생: " + e.getMessage()).queue();
                }
            }
            default -> event.reply("❌ 지원하지 않는 명령어입니다.").queue();
        }
    }

    private String getRequiredOption(SlashCommandInteractionEvent event, String name) {
        OptionMapping option = event.getOption(name);
        if (option == null) {
            throw new IllegalArgumentException("Missing required option: " + name);
        }
        return option.getAsString();
    }
}
