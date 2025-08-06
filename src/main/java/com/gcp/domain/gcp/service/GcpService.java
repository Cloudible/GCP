package com.gcp.domain.gcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.gcp.dto.ProjectZoneDto;
import com.gcp.domain.gcp.repository.GcpProjectRepository;
import com.google.cloud.compute.v1.Project;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcpService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DiscordUserRepository discordUserRepository;
    private final GcpProjectRepository gcpProjectRepository;
    private static final String ZONE = "us-central1-f";
    private static final String PROJECT_ID = "sincere-elixir-464606-j1";


    public String startVM(String userId, String guildId, String vmName) {
        try {
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/start",
                    PROJECT_ID, ZONE, vmName
            );

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(null, headers); // payload 없이 헤더만
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return "🚀 `" + vmName + "` VM을 실행했습니다!";
        } catch (Exception e) {
            log.error("VM 실행 오류", e);
            return "❌ `" + vmName + "` VM 실행 실패!";
        }
    }

    public String stopVM(String userId, String guildId, String vmName) {
        try {
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/stop",
                    PROJECT_ID, ZONE, vmName
            );

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return "🛑 `" + vmName + "` VM을 중지했습니다!";
        } catch (Exception e) {
            log.error("❌ VM 중지 오류", e);
            return "❌ `" + vmName + "` VM 중지 실패!";
        }
    }

    public String getInstanceId(String userId, String guildId, String vmName, String zone) {
        try {

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                    PROJECT_ID, zone, vmName
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());
            return json.path("id").asText();

        } catch (Exception e) {
            log.error("❌ instance_id 조회 실패", e);
            return null;
        }
    }


    public List<String> getVmLogs(String userId, String guildId, String vmName) {
        try {
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String vmId = getInstanceId(userId, guildId, vmName, ZONE);
            if (vmId == null){
                return List.of("❌ VM 인스턴스를 찾을 수 없습니다!");
            }

            String filter = String.format(
                    "resource.type=\"gce_instance\" AND resource.labels.instance_id=\"%s\" AND severity>=ERROR",
                    vmId
            );
            
            Map<String, Object> body = Map.of(
                    "resourceNames", List.of("projects/sincere-elixir-464606-j1"),
                    "pageSize", 50,
                    "orderBy", "timestamp desc",
                    "filter", filter
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = "https://logging.googleapis.com/v2/entries:list";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            List<String> sbList = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (JsonNode entry : root.get("entries")) {
                String time = entry.path("timestamp").asText();
                String level = entry.path("severity").asText();
                String message = entry.path("jsonPayload").path("message").asText();

                String combinedMessage = String.format("[%s] [%s] %s%n", time, level, message);


                // 디스코드에서 한 번에 출력 가능한 문자 수가 2000이라 기존 문자열 길이와 먼저 더해보고 2000보다 크면 기존 문자열은 반환.
                // 새 메시지는 새로 할당된 sb에 추가.
                if (sb.length() + combinedMessage.length() > 2000){
                    sbList.add(sb.toString());
                    sb = new StringBuilder();
                }

                sb.append(combinedMessage);

            }

            // 반복 이후에 sb에 메시지가 남아있을 수도 있으니 해당 메시지도 추가.
            if (!sb.isEmpty()) {
                sbList.add(sb.toString());
            }
            return sbList;

        } catch (Exception e) {
            log.error("❌ 로그 조회 오류", e);
            List<String> errorMessage = new ArrayList<>();
            errorMessage.add("❌ 로그 조회 실패!");

            return errorMessage;
        }
    }

    public String getEstimatedCost() {
        try {
            String url = String.format("https://cloudbilling.googleapis.com/v1/projects/%s/billingInfo", PROJECT_ID);
            String response = restTemplate.getForObject(url, String.class);
            return "💰 예상 비용: " + response;
        } catch (Exception e) {
            log.error("❌ 비용 조회 오류", e);
            return "❌ 비용 조회 실패!";
        }
    }

    @SneakyThrows
    public List<Map<String, String>> getVmList(String userId, String guildId) {
        String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances",
                PROJECT_ID, ZONE);

        String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return parseVmResponse(response.getBody());
    }

    public List<String> getProjectIds(String userId, String guildId) {
        String url = "https://cloudresourcemanager.googleapis.com/v1/projects";
        String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        JSONObject json = new JSONObject(response.getBody());
        JSONArray projects = json.getJSONArray("projects");

        List<String> projectIds = new ArrayList<>();
        for (int i = 0; i < projects.length(); i++) {
            projectIds.add(projects.getJSONObject(i).getString("projectId"));
        }
        return projectIds;
    }

    public List<ProjectZoneDto> getActiveInstanceZones(String userId, String guildId) {
        String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
        DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId).orElseThrow();
        List<String> projectIds = gcpProjectRepository.findAllProjectIdsByDiscordUser(discordUser).orElseThrow();

        List<ProjectZoneDto> activeZones = new ArrayList<>();

        for (String projectId : projectIds) {
            try {
                String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/aggregated/instances", projectId);

                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                JSONObject json = new JSONObject(response.getBody());
                JSONObject items = json.getJSONObject("items");

                List<String> zoneNames = new ArrayList<>();

                for (String key : items.keySet()) {
                    JSONObject zoneInfo = items.getJSONObject(key);
                    if (zoneInfo.has("instances") && key.startsWith("zones/")) {
                        String zoneName = key.substring("zones/".length());
                        zoneNames.add(zoneName);
                    }
                }

                ProjectZoneDto dto = ProjectZoneDto.builder()
                        .projectId(projectId)
                        .zoneList(zoneNames)
                        .build();
                activeZones.add(dto);

            } catch (Exception e) {
                log.warn("프로젝트 Zone 조회 실패 {}: {}", projectId, e.getMessage());
            }
        }

        return activeZones;
    }


    private static List<Map<String, String>> parseVmResponse(String json) throws IOException {
        List<Map<String, String>> vmList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(json);

        if (root.has("items")) {
            for (JsonNode instance : root.get("items")) {
                Map<String, String> vmInfo = new HashMap<>();
                vmInfo.put("name", instance.get("name").asText());
                vmInfo.put("status", instance.get("status").asText());

                String machineType = instance.get("machineType").asText();
                vmInfo.put("machineType", machineType.substring(machineType.lastIndexOf("/") + 1));

                if (instance.has("networkInterfaces") &&
                        instance.get("networkInterfaces").size() > 0 &&
                        instance.get("networkInterfaces").get(0).has("accessConfigs") &&
                        instance.get("networkInterfaces").get(0).get("accessConfigs").size() > 0) {
                    vmInfo.put("externalIP", instance.get("networkInterfaces").get(0)
                            .get("accessConfigs").get(0).get("natIP").asText());
                } else {
                    vmInfo.put("externalIP", "None");
                }

                vmList.add(vmInfo);
            }
        }

        return vmList;
    }

    public void enableVmNotifications() {
        log.info("📢 GCP VM 상태 변경 감지 알림 활성화!");
    }

    public String createVM(String userId, String guildId, String vmName, String machineType, String osImage, int bootDiskGb, boolean allowHttp, boolean allowHttps) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances", PROJECT_ID, ZONE);
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();

            Map<String, String> imageMap = Map.of(
                    "debian-11", "projects/debian-cloud/global/images/family/debian-11",
                    "ubuntu-2204", "projects/ubuntu-os-cloud/global/images/family/ubuntu-2204-lts",
                    "centos-7", "projects/centos-cloud/global/images/family/centos-7"
            );
            String image = imageMap.getOrDefault(osImage, imageMap.get("debian-11"));

            List<String> tags = new ArrayList<>();
            if (allowHttp) tags.add("http-server");
            if (allowHttps) tags.add("https-server");

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("name", vmName);
            bodyMap.put("machineType", String.format("zones/%s/machineTypes/%s", ZONE, machineType));
            if (!tags.isEmpty()) {
                bodyMap.put("tags", Map.of("items", tags));
            }

            bodyMap.put("disks", List.of(
                    Map.of(
                            "boot", true,
                            "autoDelete", true,
                            "initializeParams", Map.of(
                                    "diskSizeGb", bootDiskGb,
                                    "sourceImage", image
                            )
                    )
            ));

            bodyMap.put("networkInterfaces", List.of(
                    Map.of(
                            "network", "global/networks/default",
                            "accessConfigs", List.of(
                                    Map.of(
                                            "name", "External NAT",
                                            "type", "ONE_TO_ONE_NAT"
                                    )
                            )
                    )
            ));

            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(bodyMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, String.class);

            return "🆕 `" + vmName + "` VM 인스턴스 생성 중 (OS: %s, 디스크: %dGB, HTTP: %s, HTTPS: %s)".formatted(
                    osImage, bootDiskGb, allowHttp, allowHttps
            );
        } catch (Exception e) {
            log.error("❌ VM 생성 오류", e);
            return "❌ `" + vmName + "` VM 생성 실패!";
        }
    }
}