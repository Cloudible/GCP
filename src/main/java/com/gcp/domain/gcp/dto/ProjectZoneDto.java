package com.gcp.domain.gcp.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ProjectZoneDto(
        String projectId, List<String> zoneList
) {
}
