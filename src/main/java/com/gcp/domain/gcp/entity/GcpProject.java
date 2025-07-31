package com.gcp.domain.gcp.entity;

import com.gcp.domain.discord.entity.DiscordUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "gcp_project")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GcpProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String projectId;
    private String zone;

    @Column(columnDefinition = "TEXT")
    private String credentialsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private DiscordUser discordUser;
}
