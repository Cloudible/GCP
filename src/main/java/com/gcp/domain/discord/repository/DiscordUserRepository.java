package com.gcp.domain.discord.repository;

import com.gcp.domain.discord.entity.DiscordUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {

    @Query("SELECT u FROM DiscordUser u WHERE u.userId = :userId AND u.guildId = :guildId")
    Optional<DiscordUser> findByUserIdAndGuildId(@Param("userId") String userId,
                                                   @Param("guildId") String guildId);

    @Query("SELECT u FROM DiscordUser u WHERE u.googleAccessToken = :googleAccessToken")
    Optional<DiscordUser> findByGoogleAccessToken(@Param("googleAccessToken") String googleAccessToken);

    @Query("SELECT u.googleAccessToken FROM DiscordUser u WHERE u.userId = :userId AND u.guildId = :guildId")
    Optional<String> findAccessTokenByUserIdAndGuildId(@Param("userId") String userId,
                                                       @Param("guildId") String guildId);


}
