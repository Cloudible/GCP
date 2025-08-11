package com.gcp.domain.gcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

@Component
public class GcpImageUtil {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String> FAMILY_BY_KEY = Map.ofEntries(
            entry("debian-12", "projects/debian-cloud/global/images/family/debian-12"),
            entry("debian-11", "projects/debian-cloud/global/images/family/debian-11"),

            entry("ubuntu-2504",     "projects/ubuntu-os-cloud/global/images/family/ubuntu-2504-amd64"),
            entry("ubuntu-2404-lts", "projects/ubuntu-os-cloud/global/images/family/ubuntu-2404-lts-amd64"),
            entry("ubuntu-2204-lts", "projects/ubuntu-os-cloud/global/images/family/ubuntu-2204-lts")

    );

    public List<String> listFamilyKeys() {
        return FAMILY_BY_KEY.keySet().stream().sorted().toList();
    }

    public String resolveLatestSelfLink(String accessToken, String familyKeyOrLink, String fallbackFamilyKey) {
        if (familyKeyOrLink == null || familyKeyOrLink.isBlank()) {
            familyKeyOrLink = fallbackFamilyKey;
        }

        if (familyKeyOrLink.startsWith("projects/") && !familyKeyOrLink.contains("/images/family/")) {
            return familyKeyOrLink;
        }

        String familyLink = familyKeyOrLink.startsWith("projects/")
                ? familyKeyOrLink
                : FAMILY_BY_KEY.getOrDefault(familyKeyOrLink, FAMILY_BY_KEY.get(fallbackFamilyKey));

        return describeFromFamily(accessToken, familyLink);
    }

    public String describeFromFamily(String accessToken, String familyLink) {
        String url = "https://compute.googleapis.com/compute/v1/" + familyLink;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode image = mapper.readTree(resp.getBody());
            String selfLink = image.path("selfLink").asText(null);
            if (selfLink == null || selfLink.isBlank()) {
                throw new IllegalStateException("selfLink 파싱 실패: " + image.toString());
            }
            return selfLink;
        } catch (Exception e) {
            throw new RuntimeException("family 최신 이미지 조회 실패: " + familyLink, e);
        }
    }
}