package com.gcp.domain.gcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.domain.discord.entity.DiscordUser;
import com.gcp.domain.discord.repository.DiscordUserRepository;
import com.gcp.domain.discord.service.DiscordUserService;
import com.gcp.domain.gcp.aop.RequiredValidToken;
import com.gcp.domain.gcp.dto.ProjectZoneDto;
import com.gcp.domain.gcp.repository.GcpProjectRepository;

import com.gcp.domain.gcp.util.GcpImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@RequiredValidToken
public class GcpService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final DiscordUserRepository discordUserRepository;
    private final GcpProjectRepository gcpProjectRepository;
    private final GcpImageUtil gcpImageUtil;

    private final DiscordUserService discordUserService;


    public String startVM(String userId, String guildId, String vmName, String projectId, String zone) {
        try {
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/start",
                    projectId, zone, vmName
            );

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(null, headers); // payload 없이 헤더만
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return "🚀 `" + vmName + "` VM을 실행했습니다!";
        } catch (Exception e) {
            log.error("❌ VM 시작 오류", e);
            throw new RuntimeException("Compute API (start) 호출 도중 에러 발생: ", e);
        }
    }

    public String stopVM(String userId, String guildId, String vmName, String projectId, String zone) {
        try {
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/stop",
                    projectId, zone, vmName
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
            throw new RuntimeException("Compute API (stop) 호출 도중 에러 발생: ", e);
        }
    }

    public String getInstanceId(String userId, String guildId, String vmName, String projectId, String zone) {
        try {

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            String url = String.format(
                    "https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                    projectId, zone, vmName
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
            throw new RuntimeException("Compute API (인스턴스 ID 조회) 호출 도중 에러 발생: ", e);
        }
    }


    public List<String> getVmLogs(String userId, String guildId, String vmName, String projectId, String zone) {
        try {
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String vmId = getInstanceId(userId, guildId, vmName, projectId, zone);

            if (vmId == null){
                throw new RuntimeException("현재 보유 중인 VM이 없습니다.");
            }

            Instant now = Instant.now();
            Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

            String filter = String.format(
                    "resource.type=\"gce_instance\" " +
                            "AND resource.labels.instance_id=\"%s\" " +
                            "AND timestamp >= \"%s\"",
                    vmId,
                    weekAgo.toString()
            );

            Map<String, Object> body = Map.of(
                    "resourceNames", List.of("projects/" + projectId),
                    "pageSize", 50,
                    "orderBy", "timestamp desc",
                    "filter", filter
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            String url = "https://logging.googleapis.com/v2/entries:list";

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            System.out.println(root.toString());

            List<String> sbList = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (JsonNode entry : root.path("entries")) {
                String time = entry.path("timestamp").asText();
                String level = entry.path("severity").asText();

                // 여러 타입 중 있는 것 가져오기
                String message = "";
                if (entry.has("jsonPayload") && entry.get("jsonPayload").has("message")) {
                    message = entry.get("jsonPayload").get("message").asText();
                } else if (entry.has("textPayload")) {
                    message = entry.get("textPayload").asText();
                } else if (entry.has("protoPayload")) {
                    message = entry.get("protoPayload").path("methodName").asText(); // 예: v1.compute.instances.setMetadata
                }

                String combinedMessage = String.format("[%s] [%s] %s%n", time, level, message);

                if (sb.length() + combinedMessage.length() > 2000) {
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
            throw new RuntimeException("Logging API 호출 도중 에러 발생: ", e);
        }
    }


    @SneakyThrows
    public List<Map<String, String>> getVmList(String userId, String guildId, String projectId, String zone) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances",
                    projectId, zone);

            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(null, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            return parseVmResponse(response.getBody());
        } catch (Exception e){
            log.error("❌ VM 목록 조회 실패", e);
            throw new RuntimeException("Compute API (인스턴스 목록 조회) 호출 도중 에러 발생: ", e);
        }
    }

    public List<String> getProjectIds(String userId, String guildId) {
        try {
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
        } catch (Exception e) {
            log.error("❌ 프로젝트 ID 조회 중 에러 발생", e);
            throw new RuntimeException("CloudResourceManager API 호출 도중 에러 발생: ", e);
        }
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
                log.warn("❌ 프로젝트 Zone 조회 실패 {}", projectId, e);
                throw new RuntimeException("Compute API (VM Zone 조회) 호출 도중 에러 발생: ", e);
            }
        }

        return activeZones;
    }


    public List<String> getProjectInstanceZones(String userId, String guildId, String projectId) {
        String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
        DiscordUser discordUser = discordUserRepository.findByUserIdAndGuildId(userId, guildId).orElseThrow();


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

                return zoneNames;

            } catch (Exception e) {
                log.warn("❌ 프로젝트 Zone 조회 실패 {}", projectId, e);
                throw new RuntimeException("Compute API (VM Zone 조회) 호출 도중 에러 발생: ", e);
            }

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

    public void createVM(String userId, String guildId, String vmName, String machineType, String projectId, String zone,
                           String osFamilyKeyOrLink, int bootDiskGb, boolean allowHttp, boolean allowHttps) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances", projectId, zone);
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId).orElseThrow();
            String sourceImage = gcpImageUtil.resolveLatestSelfLink(accessToken, osFamilyKeyOrLink, "debian-12");


            List<String> tags = new ArrayList<>();
            if (allowHttp) tags.add("http-server");
            if (allowHttps) tags.add("https-server");

            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("name", vmName);
            bodyMap.put("machineType", String.format("zones/%s/machineTypes/%s", zone, machineType));
            if (!tags.isEmpty()) {
                bodyMap.put("tags", Map.of("items", tags));
            }

            bodyMap.put("disks", List.of(
                    Map.of(
                            "boot", true,
                            "autoDelete", true,
                            "initializeParams", Map.of(
                                    "diskSizeGb", bootDiskGb,
                                    "sourceImage", sourceImage
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
        } catch (Exception e) {
            log.error("❌ VM 생성 오류", e);
            throw new RuntimeException("Compute API (인스턴스 생성) 호출 도중 에러 발생: ", e);
        }
    }

    public List<Map<String, Object>> getFirewallRules(String userId, String guildId, String projectId) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/global/firewalls", projectId);
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId)
                    .orElseThrow();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());

            List<Map<String, Object>> ruleList = new ArrayList<>();
            if (root.has("items")) {
                for (JsonNode rule : root.get("items")) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", rule.path("name").asText());
                    map.put("direction", rule.path("direction").asText());
                    map.put("sourceRanges", rule.path("sourceRanges"));
                    map.put("targetTags", rule.path("targetTags"));

                    List<String> tcpPorts = new ArrayList<>();
                    for (JsonNode allow : rule.path("allowed")) {
                        if ("tcp".equals(allow.path("IPProtocol").asText())) {
                            for (JsonNode port : allow.path("ports")) {
                                tcpPorts.add(port.asText());
                            }
                        }
                    }
                    map.put("tcpPorts", tcpPorts);
                    ruleList.add(map);
                }
            }

            return ruleList;

        } catch (Exception e) {
            log.error("❌ 방화벽 규칙 조회 오류", e);
            throw new RuntimeException("Compute API (방화벽 규칙 조회) 호출 도중 에러 발생: ", e);
        }
    }
    public String createFirewallRule(String userId, String guildId, String projectId, int port, List<String> sourceRanges) {
        try {
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId)
                    .orElseThrow();

            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/global/firewalls", projectId);

            String ruleName = "allow-custom-" + port;

            Map<String, Object> body = Map.of(
                    "name", ruleName,
                    "direction", "INGRESS",
                    "allowed", List.of(
                            Map.of(
                                    "IPProtocol", "tcp",
                                    "ports", List.of(String.valueOf(port))
                            )
                    ),
                    "sourceRanges", sourceRanges,
                    "targetTags", List.of("custom-" + port)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(body);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            restTemplate.postForEntity(url, request, String.class);

            return "✅ 포트 " + port + " 에 대한 방화벽 규칙이 생성되었습니다.";

        } catch (HttpClientErrorException.Conflict e) {
            return "⚠️ 이미 포트 " + port + " 에 대한 방화벽 규칙이 존재합니다.";
        } catch (Exception e) {
            log.error("❌ 방화벽 규칙 생성 실패", e);
            throw new RuntimeException("Compute API (방화벽 규칙 생성) 호출 도중 에러 발생: ", e);
        }
    }

    public String deleteFirewallRule(String userId, String guildId, String projectId, int port) {
        try {
            String accessToken = discordUserRepository.findAccessTokenByUserIdAndGuildId(userId, guildId)
                    .orElseThrow();

            String ruleName = "allow-custom-" + port;

            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/global/firewalls/%s",
                    projectId, ruleName);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);

            return "🗑️ 포트 " + port + " 에 대한 방화벽 규칙이 삭제되었습니다.";

        } catch (HttpClientErrorException.NotFound e) {
            return "⚠️ 포트 " + port + " 에 대한 방화벽 규칙이 존재하지 않습니다.";
        } catch (Exception e) {
            log.error("❌ 방화벽 규칙 삭제 실패", e);
            throw new RuntimeException("Compute API (방화벽 규칙 삭제) 호출 도중 에러 발생: ", e);
        }
    }
}