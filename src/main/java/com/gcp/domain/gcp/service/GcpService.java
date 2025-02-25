package com.gcp.domain.gcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.domain.gcp.util.GcpAuthUtil;
import com.google.cloud.compute.v1.stub.HttpJsonInstancesStub;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesSettings;
import com.google.cloud.compute.v1.ListInstancesRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class GcpService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final GcpAuthUtil gcpAuthUtil;
    private static final String ZONE = "us-central1-c";
    private static final String PROJECT_ID = "semiotic-sylph-450506-u5";

    public String startVM(String vmName) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/start",
                    PROJECT_ID, ZONE, vmName);

            String accessToken = gcpAuthUtil.getAccessToken();
            log.info("액세스 토큰: {}", accessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            return "🚀 `" + vmName + "` VM을 실행했습니다!";
        } catch (Exception e) {
            log.error("VM 실행 오류", e);
            return "❌ `" + vmName + "` VM 실행 실패!";
        }
    }


    public String stopVM(String vmName) {
        try {
            String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s/stop", PROJECT_ID, ZONE, vmName);
            restTemplate.postForEntity(url, null, String.class);
            return "🛑 `" + vmName + "` VM을 중지했습니다!";
        } catch (Exception e) {
            log.error("❌ VM 중지 오류", e);
            return "❌ `" + vmName + "` VM 중지 실패!";
        }
    }

    public String getVmLogs() {
        try {
            String url = String.format("https://logging.googleapis.com/v2/entries:list?resourceNames=projects/%s", PROJECT_ID);
            String response = restTemplate.getForObject(url, String.class);
            return "📜 최근 GCP 로그:\n" + response;
        } catch (Exception e) {
            log.error("❌ 로그 조회 오류", e);
            return "❌ 로그 조회 실패!";
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
    public List<Map<String, String>> getVmList(){
        String url = String.format("https://compute.googleapis.com/compute/v1/projects/%s/zones/%s/instances",
                PROJECT_ID, ZONE);
        String accessToken = null;
        try {
            accessToken = gcpAuthUtil.getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);

        return parseVmResponse(response.getBody());
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

                // 머신 타입 가져오기 (URL의 마지막 부분만 추출)
                String machineType = instance.get("machineType").asText();
                vmInfo.put("machineType", machineType.substring(machineType.lastIndexOf("/") + 1));

                // 외부 IP 확인 (없을 수도 있음)
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
}