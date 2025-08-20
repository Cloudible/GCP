package com.gcp.domain.discord.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.gcp.dto.ProjectZoneDto;
import com.gcp.domain.gcp.repository.GcpProjectRepository;
import com.gcp.domain.gcp.service.GcpProjectCommandService;
import com.gcp.domain.gcp.service.GcpService;
import com.gcp.domain.gcp.util.GcpImageUtil;
import com.gcp.domain.gcp.util.GcpZones;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GcpBotService extends ListenerAdapter {
    private final GcpProjectRepository gcpProjectRepository;
    private final DiscordUserRepository discordUserRepository;
    private final GcpService gcpService;
    private final DiscordUserService discordUserService;
    private final GcpProjectCommandService gcpProjectCommandService;
    private final GcpImageUtil gcpImageUtil;

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

            case "project-list" -> {
                List<String> userProjectIds = gcpService.getProjectIds(userId, guildId);
                if (userProjectIds.isEmpty()) {
                    event.reply("📭 참여 중인 프로젝트가 없습니다.").queue();
                } else {
                    String message = "📦 **참여 중인 프로젝트 목록**\n" +
                            userProjectIds.stream()
                            .map(id -> "• " + id)
                            .collect(Collectors.joining("\n"));
                    event.reply(message).queue();
                }
            }

            case "login" -> {
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

            case "project-register" -> {
                try{
                    String projectId = getRequiredOption(event, "project_id");
                    gcpProjectCommandService.insertNewGcpProject(userId, guildId, projectId);
                    event.reply("프로젝트가 등록되었습니다.").queue();
                } catch (RuntimeException e){
                    event.reply(e.getMessage()).queue();
                }
            }
            case "zone-list" -> {
                    try {
                        List<ProjectZoneDto> result = gcpService.getActiveInstanceZones(userId, guildId);

                        StringBuilder message = new StringBuilder("📦 **프로젝트별 인스턴스 활성 ZONE 목록**\n\n");
                        for (ProjectZoneDto dto : result) {
                            message.append("🔹 **")
                                    .append(dto.projectId())
                                    .append("**\n");

                            for (String zone : dto.zoneList()) {
                                message.append("- ").append(zone).append("\n");
                            }

                            message.append("\n");
                            event.reply(message.toString()).queue();
                        }

                    } catch (Exception e) {
                        event.reply("❌ 오류 발생: " + e.getMessage()).queue();
                    }
            }

            case "start" -> {
                try{
                    String projectId = getRequiredOption(event, "project_id");
                    String zone = getRequiredOption(event, "zone");
                    String vmName = getRequiredOption(event, "vm_name");
                    event.reply(gcpService.startVM(userId, guildId, vmName, projectId, zone)).queue();
                } catch (RuntimeException e){
                    event.reply("❌ " + e.getMessage()).queue();;
                }
            }
            case "stop" -> {
                try {
                    String vmName = getRequiredOption(event, "vm_name");
                    String projectId = getRequiredOption(event, "project_id");
                    String zone = getRequiredOption(event, "zone");
                    event.reply(gcpService.stopVM(userId, guildId, vmName, projectId, zone)).queue();
                } catch (RuntimeException e){
                    event.reply("❌ " + e.getMessage()).queue();
                }
            }
            case "logs" -> {
                event.deferReply().queue(hook -> {
                    try {
                        String vmName = getRequiredOption(event, "vm_name");
                        String projectId = getRequiredOption(event, "project_id");
                        String zone = getRequiredOption(event, "zone");

                        List<String> logs = gcpService.getVmLogs(userId, guildId, vmName, projectId, zone);

                        if (logs.isEmpty()) {
                            hook.editOriginal("📭 로그가 없습니다.").queue();
                            return;
                        }

                        hook.editOriginal("📝 로그 출력 시작...").queue();

                        for (String log : logs) {
                            hook.sendMessage("```bash\n" + log + "\n```").queue();
                        }
                    } catch (Exception e) {
                        hook.editOriginal("❌ " + e.getMessage()).queue();
                    }
                });
            }
            case "list" -> {
                try {
                    String projectId = getRequiredOption(event, "project_id");
                    String zone = getRequiredOption(event, "zone");
                    event.reply(gcpService.getVmList(userId, guildId, projectId, zone).toString()).queue();
                } catch (Exception e){
                    event.reply("보유 중인 인스턴스가 없습니다.").queue();
                }
            }
            case "create" -> {
                event.deferReply().queue(hook -> {
                    try {
                        String vmName = getRequiredOption(event, "vm_name");
                        String machineType = getRequiredOption(event, "machine_type");
                        String osFamily = getRequiredOption(event, "os_image");
                        String projectId = getRequiredOption(event, "project_id");
                        String zone = getRequiredOption(event, "zone");
                        int bootDiskGb = Integer.parseInt(getRequiredOption(event, "boot_disk_gb"));
                        boolean allowHttp = Boolean.parseBoolean(getRequiredOption(event, "allow_http"));
                        boolean allowHttps = Boolean.parseBoolean(getRequiredOption(event, "allow_https"));

                        if (bootDiskGb <= 9) {
                            hook.editOriginal("❌ 디스크 크기는 10 이상이 되어야 합니다.").queue();
                            return;
                        }
                        gcpService.createVM(userId, guildId, vmName, machineType, projectId, zone,
                                osFamily, bootDiskGb, allowHttp, allowHttps);

                        hook.editOriginal("⚙️ `" + vmName + "` 인스턴스 생성 완료!").queue();
                    } catch (Exception e) {
                        hook.editOriginal("❌ " + e.getMessage()).queue();
                    }
                });
            }
            case "firewall-list" -> {
                try {
                    event.deferReply().queue();

                    String projectId = getRequiredOption(event, "project_id");
                    List<Map<String, Object>> rules = gcpService.getFirewallRules(userId, guildId, projectId);

                    if (rules.isEmpty()) {
                        event.getHook().sendMessage("📭 조회된 방화벽 규칙이 없습니다.").queue();
                        return;
                    }

                    StringBuilder sb = new StringBuilder("📌 현재 방화벽 규칙 목록 (TCP 기준):\n");

                    for (Map<String, Object> rule : rules) {
                        String name = (String) rule.get("name");
                        List<String> ports = (List<String>) rule.get("tcpPorts");
                        JsonNode sourceRanges = (JsonNode) rule.get("sourceRanges");

                        sb.append("• `").append(name).append("` - 포트: ")
                                .append(ports.isEmpty() ? "없음" : String.join(", ", ports))
                                .append(", IP 범위: ").append(sourceRanges.toString()).append("\n");
                    }

                    event.getHook().sendMessage(sb.toString()).queue();
                } catch (RuntimeException e){
                    event.reply("❌ " + e.getMessage()).queue();
                }
            }
            case "firewall-create" -> {
                try{
                    int port = Optional.ofNullable(event.getOption("port"))
                            .map(OptionMapping::getAsInt)
                            .orElseThrow(() -> new IllegalArgumentException("포트가 필요합니다."));

                    if (port < 1 || port > 65535) {
                        event.reply("❌ 유효하지 않은 포트 번호입니다. 1 ~ 65535 사이여야 합니다.").setEphemeral(true).queue();
                        return;
                    }

                    String ipRangeRaw = Optional.ofNullable(event.getOption("source_ranges"))
                            .map(OptionMapping::getAsString)
                            .orElse("0.0.0.0/0");

                    List<String> sourceRanges = List.of(ipRangeRaw.split("\\s*,\\s*"));

                    String projectId = getRequiredOption(event, "project_id");

                    String result = gcpService.createFirewallRule(userId, guildId, projectId, port, sourceRanges);
                    event.reply(result).queue();
                } catch (RuntimeException e){
                    event.reply("❌ " + e.getMessage()).queue();
                }
            }
            case "firewall-delete" -> {
                try{
                    int port = Optional.ofNullable(event.getOption("port"))
                            .map(OptionMapping::getAsInt)
                            .orElseThrow(() -> new IllegalArgumentException("포트가 필요합니다."));

                    if (port < 1 || port > 65535) {
                        event.reply("❌ 유효하지 않은 포트 번호입니다. 1 ~ 65535 사이여야 합니다.").setEphemeral(true).queue();
                        return;
                    }

                    String projectId = getRequiredOption(event, "project_id");

                    String result = gcpService.deleteFirewallRule(userId, guildId, projectId, port);
                    event.reply(result).queue();
                } catch (RuntimeException e){
                    event.reply("❌ " + e.getMessage()).queue();
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

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("gcp")) return;

        String userId = event.getUser().getId();
        String guildId = event.getGuild().getId();

        String eventType = event.getSubcommandName();
        String focused = event.getFocusedOption().getName();

        if("project_id".equals(focused)){
            DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId).orElseThrow();
            List<String> projectIds = gcpProjectRepository.findAllProjectIdsByDiscordUser(discordUser).orElseThrow();

            String userInput = event.getFocusedOption().getValue();

            List<Command.Choice> choices = projectIds.stream()
                    .filter(pid -> pid.startsWith(userInput))
                    .limit(25)
                    .map(pid -> new Command.Choice(pid, pid))
                    .toList();

            event.replyChoices(choices).queue();
        }

        if ("region".equals(focused)) {
            String typed = event.getFocusedOption().getValue();
            String lower = typed.toLowerCase();

            List<Command.Choice> suggestions = GcpZones.REGIONS.keySet().stream()
                    .filter(region -> region.toLowerCase().contains(lower))
                    .sorted()
                    .limit(25)
                    .map(region -> new Command.Choice(region, region))
                    .toList();

            event.replyChoices(suggestions).queue();
        }

        if ("zone".equals(focused)) {
            String typed = event.getFocusedOption().getValue();
            String lower = typed.toLowerCase();
            if ("create".equals(eventType)) {

                // 이미 입력된 region 값 가져오기
                String selectedRegion = Optional.ofNullable(event.getOption("region"))
                        .map(OptionMapping::getAsString)
                        .orElse("");

                // region이 있으면 그 안의 zone만 필터링
                List<String> candidateZones = selectedRegion.isEmpty()
                        ? GcpZones.REGIONS.values().stream().flatMap(List::stream).toList()
                        : GcpZones.REGIONS.getOrDefault(selectedRegion, List.of());

                List<Command.Choice> suggestions = candidateZones.stream()
                        .filter(zone -> zone.toLowerCase().contains(lower))
                        .sorted()
                        .limit(25)
                        .map(zone -> new Command.Choice(zone, zone))
                        .toList();

                event.replyChoices(suggestions).queue();
            } else{
                String inputProject = Optional.ofNullable(event.getOption("project_id"))
                        .map(OptionMapping::getAsString)
                        .orElse("");

                List<String> projectZones = gcpService.getProjectInstanceZones(userId, guildId, inputProject);

                List<Command.Choice> suggestions = projectZones.stream()
                        .filter(zone -> zone.toLowerCase().contains(lower))
                        .sorted()
                        .limit(25)
                        .map(zone -> new Command.Choice(zone, zone))
                        .toList();

                event.replyChoices(suggestions).queue();
            }
        }

        if("create".equals(eventType) && "os_image".contains(focused)){
            String typed = event.getFocusedOption().getValue(); // 사용자가 입력 중인 값 (프리픽스)

            // family 키 목록 불러와 필터링 (대소문자 무시 contains)
            List<String> all = gcpImageUtil.listFamilyKeys();
            String lower = typed.toLowerCase();

            List<Command.Choice> suggestions = all.stream()
                    .filter(k -> k.toLowerCase().contains(lower))
                    .sorted()
                    .limit(25) // Discord 제한
                    .map(k -> new Command.Choice(k, k))
                    .toList();

            event.replyChoices(suggestions).queue();
        }
    }

}
