package com.gcp.domain.gcp.entity;

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
    private String userId; // 사용자 ID
    private String projectId; // GCP 프로젝트 ID

    @Column(columnDefinition = "TEXT")
    private String credentialsJson;
}
