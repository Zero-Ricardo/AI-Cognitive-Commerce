package com.aishop.commerce.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class ElasticsearchHttpClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(800)).build();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String indexName;
    private final boolean enabled;

    public ElasticsearchHttpClient(ObjectMapper mapper,
            @Value("${app.search.elasticsearch-url:http://localhost:9200}") String baseUrl,
            @Value("${app.search.index-name:products_search_v2}") String indexName,
            @Value("${app.search.elasticsearch-enabled:false}") boolean enabled) {
        this.mapper = mapper;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.indexName = indexName;
        this.enabled = enabled;
    }

    public boolean enabled() { return enabled; }
    public String indexName() { return indexName; }

    public JsonNode get(String path) throws IOException, InterruptedException {
        return execute("GET", path, null);
    }

    public JsonNode post(String path, JsonNode body) throws IOException, InterruptedException {
        return execute("POST", path, body == null ? null : mapper.writeValueAsString(body));
    }

    public JsonNode put(String path, JsonNode body) throws IOException, InterruptedException {
        return execute("PUT", path, body == null ? null : mapper.writeValueAsString(body));
    }

    public JsonNode delete(String path) throws IOException, InterruptedException {
        return execute("DELETE", path, null);
    }

    private JsonNode execute(String method, String path, String body) throws IOException, InterruptedException {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json");
        if (body != null) builder.header("Content-Type", "application/json");
        switch (method) {
            case "GET" -> builder.GET();
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "DELETE" -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported method: " + method);
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Elasticsearch HTTP " + response.statusCode() + ": " + abbreviate(response.body()));
        }
        return response.body() == null || response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body());
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        return value.length() > 300 ? value.substring(0, 300) : value;
    }
}
