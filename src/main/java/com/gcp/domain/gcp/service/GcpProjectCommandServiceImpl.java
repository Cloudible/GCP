package com.gcp.domain.gcp.service;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.gcp.entity.GcpProject;
import com.gcp.domain.gcp.repository.GcpProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GcpProjectCommandServiceImpl implements GcpProjectCommandService{

    private final GcpProjectRepository gcpProjectRepository;
    private final DiscordUserRepository discordUserRepository;

    @Override
    public void insertNewGcpProject(String userId, String guildId, String projectId) {
        DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId).orElseThrow(
                () -> new RuntimeException("이미 등록된 프로젝트 입니다.")
        );

        GcpProject gcpProject = GcpProject.create(projectId, discordUser);
        gcpProjectRepository.save(gcpProject);
    }
}
