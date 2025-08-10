package com.gcp.global.config;


import com.gcp.domain.discord.service.GcpBotService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
        JDA jda = JDABuilder.create(
                        token,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .addEventListeners(gcpBotService)
                .build();

        jda.awaitReady(); // 명령어 등록 전에 봇이 준비될 때까지 대기
        OptionData osFamilyOption =
                new OptionData(OptionType.STRING, "os_image", "OS 이미지 선택", true)
                        .setAutoComplete(true);

        jda.updateCommands().addCommands(
                        Commands.slash("gcp", "GCP 관련 명령어")
                                .addSubcommands(
                                        new SubcommandData("init", "디스코드 유저 등록"),
                                        new SubcommandData("login", "Google 계정 연동"),
                                        new SubcommandData("project-list", "소속 프로젝트 ID 목록 조회"),
                                        new SubcommandData("project-register", "프로젝트 ID를 서버에 등록")
                                                .addOption(OptionType.STRING, "project_id", "등록하고자 하는 프로젝트 ID", true),
                                        new SubcommandData("zone-list", "프로젝트 내 VM Zone 목록 조회"),
                                        new SubcommandData("start", "VM 시작")
                                                .addOption(OptionType.STRING, "vm_name", "시작할 VM 이름", true),
                                        new SubcommandData("stop", "VM 정지")
                                                .addOption(OptionType.STRING, "vm_name", "정지할 VM 이름", true),
                                        new SubcommandData("logs", "VM 로그 조회")
                                                .addOption(OptionType.STRING, "vm_name", "로그를 볼 VM 이름", true),
                                        new SubcommandData("cost", "예상 비용 조회"),
                                        new SubcommandData("notify", "알림 활성화"),
                                        new SubcommandData("list", "VM 목록 조회"),
                                        new SubcommandData("create", "VM 생성")
                                                .addOption(OptionType.STRING, "vm_name", "VM 이름", true)
                                                .addOption(OptionType.STRING, "machine_type", "머신 타입", true)
                                                .addOptions(osFamilyOption)
                                                .addOption(OptionType.INTEGER, "boot_disk_gb", "부트 디스크 크기(GB)", true)
                                                .addOption(OptionType.BOOLEAN, "allow_http", "HTTP 허용 여부", true)
                                                .addOption(OptionType.BOOLEAN, "allow_https", "HTTPS 허용 여부", true),
                                        new SubcommandData("firewall-list","방화벽 리스트 조회"),
                                        new SubcommandData("firewall-create", "방화벽 규칙 생성")
                                                .addOption(OptionType.INTEGER, "port", "허용할 포트 번호", true)
                                                .addOption(OptionType.STRING, "source_ranges", "허용할 IP 범위 (쉼표로 구분)", false),
                                        new SubcommandData("firewall-delete", "특정 포트를 TCP 기준으로 방화벽에서 삭제")
                                                .addOption(OptionType.INTEGER, "port", "삭제할 TCP 포트", true)

                                )
                ).queue();


        return jda;
    }
}
