package com.gcp.domain.discord.entity;

import com.gcp.domain.gcp.entity.GcpProject;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discord_user")
@Getter
@Setter
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
public class DiscordUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String userName;
    private String guildId;
    private String guildName;

    @Convert(converter = AttributeConverter.class)
    private String googleRefreshToken;

    @Convert(converter = AttributeConverter.class)
    private String googleAccessToken;

    @OneToMany(mappedBy = "discordUser",cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GcpProject> gcpProjects = new ArrayList<>();

    public DiscordUser(String userId, String userName, String guildId, String guildName) {
        this.userId = userId;
        this.userName = userName;
        this.guildId = guildId;
        this.guildName = guildName;
    }

    public void updateAccessToken(String googleAccessToken){
        this.googleAccessToken = googleAccessToken;
    }


    public void updateRefreshToken(String googleRefreshToken){
        this.googleRefreshToken = googleRefreshToken;
    }

    public void addProject(GcpProject project) {
        gcpProjects.add(project);
        project.setDiscordUser(this);
    }

}
