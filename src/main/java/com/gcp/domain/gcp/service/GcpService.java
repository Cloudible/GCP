package com.gcp.domain.gcp.service;

import com.gcp.domain.gcp.util.GcpAuthUtil;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;



@Service
@RequiredArgsConstructor
@Slf4j
public class GcpService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final GcpAuthUtil gcpAuthUtil;
    private static final String ZONE = "us-central1-c";
    private static final String PROJECT_ID = "semiotic-sylph-450506-u5";

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("gcp-credentials.json"))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

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

    public void enableVmNotifications() {
        log.info("📢 GCP VM 상태 변경 감지 알림 활성화!");
    }
}