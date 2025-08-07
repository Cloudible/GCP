package com.gcp.domain.discord.service;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiscordUserService {
    private final DiscordUserRepository discordUserRepository;


    public boolean insertDiscordUser(String userId, String userName, String guildId, String guildName){
        if (!discordUserRepository.existsByUserIdAndGuildId(userId, guildId)) {
            DiscordUser discordUser = new DiscordUser(userId, userName, guildId, guildName);
            discordUserRepository.save(discordUser);
            return true;
        }
        return false;
    }
}
