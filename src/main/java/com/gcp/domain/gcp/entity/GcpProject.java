package com.gcp.domain.gcp.entity;

import com.gcp.domain.discord.entity.DiscordUser;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "gcp_project")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GcpProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private DiscordUser discordUser;

    public static GcpProject create(String projectId, DiscordUser discordUser) {
        return GcpProject.builder()
                .projectId(projectId)
                .discordUser(discordUser)
                .build();
    }
}
