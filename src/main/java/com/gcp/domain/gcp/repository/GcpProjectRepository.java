package com.gcp.domain.gcp.repository;

import com.gcp.domain.gcp.entity.GcpProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GcpProjectRepository extends JpaRepository<GcpProject, Long> {
    Optional<GcpProject> findByUserId(String userId);
}
