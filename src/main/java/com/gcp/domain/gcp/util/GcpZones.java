package com.gcp.domain.gcp.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GcpZones {

    public static final Map<String, List<String>> REGIONS = new LinkedHashMap<>() {{
        put("us-west1", List.of("us-west1-a", "us-west1-b", "us-west1-c"));
        put("us-west4", List.of("us-west4-a", "us-west4-b", "us-west4-c"));
        put("us-central1", List.of("us-central1-a", "us-central1-b", "us-central1-c", "us-central1-f"));
        put("us-east1", List.of("us-east1-b", "us-east1-c", "us-east1-d"));
        put("us-east4", List.of("us-east4-a", "us-east4-b", "us-east4-c"));
        put("us-south1", List.of("us-south1-a", "us-south1-b", "us-south1-c"));

        put("europe-west1", List.of("europe-west1-b","europe-west1-c","europe-west1-d"));
        put("europe-west2", List.of("europe-west2-a","europe-west2-b","europe-west2-c"));

        put("europe-central2", List.of("europe-central2-a","europe-central2-b","europe-central2-c"));
        put("europe-north1", List.of("europe-north1-a","europe-north1-b","europe-north1-c"));
        put("europe-southwest1", List.of("europe-southwest1-a","europe-southwest1-b","europe-southwest1-c"));

        put("me-west1", List.of("me-west1-a","me-west1-b","me-west1-c"));
        put("me-central1", List.of("me-central1-a","me-central1-b","me-central1-c"));
        put("me-central2", List.of("me-central2-a","me-central2-b","me-central2-c"));

        put("asia-south1", List.of("asia-south1-a","asia-south1-b","asia-south1-c"));
        put("asia-south2", List.of("asia-south2-a","asia-south2-b","asia-south2-c"));
        put("asia-southeast1", List.of("asia-southeast1-a","asia-southeast1-b","asia-southeast1-c"));
        put("asia-southeast2", List.of("asia-southeast2-a","asia-southeast2-b","asia-southeast2-c"));

        put("asia-east1", List.of("asia-east1-a","asia-east1-b","asia-east1-c"));
        put("asia-east2", List.of("asia-east2-a","asia-east2-b","asia-east2-c"));

        put("asia-northeast1", List.of("asia-northeast1-a","asia-northeast1-b","asia-northeast1-c"));
        put("asia-northeast2", List.of("asia-northeast2-a","asia-northeast2-b","asia-northeast2-c"));

        put("australia-southeast1", List.of("australia-southeast1-a","australia-southeast1-b","australia-southeast1-c"));
        put("australia-southeast2", List.of("australia-southeast2-a","australia-southeast2-b","australia-southeast2-c"));

        put("africa-south1", List.of("africa-south1-a","africa-south1-b","africa-south1-c"));
    }};
}