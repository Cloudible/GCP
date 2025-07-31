package com.gcp.domain.gcp.repository;

import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.gcp.entity.GcpProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GcpProjectRepository extends JpaRepository<GcpProject, Long> {

    @Query("SELECT p FROM GcpProject p WHERE p.discordUser = :discordUser")
    Optional<GcpProject> findByDiscordUser(DiscordUser discordUser);
}
