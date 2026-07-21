package com.aishop.commerce.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HotScoreIndexSync {
    private static final Logger log = LoggerFactory.getLogger(HotScoreIndexSync.class);
    private final ElasticsearchHttpClient elasticsearch;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public HotScoreIndexSync(ElasticsearchHttpClient elasticsearch, JdbcTemplate jdbc, ObjectMapper mapper) {
        this.elasticsearch = elasticsearch; this.jdbc = jdbc; this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${app.hot-products.refresh-ms:300000}", initialDelay = 15000)
    public void sync() {
        if (!elasticsearch.enabled()) return;
        try {
            var rows = jdbc.query("SELECT product_id, hot_score FROM product_hot_snapshot",
                    (rs, rowNum) -> Map.entry(rs.getLong(1), rs.getDouble(2)));
            for (var row : rows) {
                var body = mapper.createObjectNode();
                body.putObject("doc").put("hotScore", row.getValue());
                try { elasticsearch.post("/" + elasticsearch.indexName() + "/_update/" + row.getKey(), body); }
                catch (Exception ignored) { }
            }
        } catch (Exception ex) {
            log.warn("同步 ES 热销分失败: {}", ex.getMessage());
        }
    }
}
