package com.sahastra.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch client configuration.
 */
@Configuration
@Slf4j
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String host;

    @Value("${opensearch.port:9200}")
    private int port;

    @Value("${opensearch.username:admin}")
    private String username;

    @Value("${opensearch.password:admin}")
    private String password;

    @Value("${opensearch.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public OpenSearchClient openSearchClient() {
        try {
            String scheme = sslEnabled ? "https" : "http";
            HttpHost httpHost = new HttpHost(scheme, host, port);

            ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder.builder(httpHost)
                    .setMapper(new JacksonJsonpMapper())
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        if (username != null && !username.isBlank() && password != null) {
                            org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider credentialsProvider =
                                    new org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider();
                            credentialsProvider.setCredentials(
                                    new org.apache.hc.client5.http.auth.AuthScope(httpHost),
                                    new org.apache.hc.client5.http.auth.UsernamePasswordCredentials(username, password.toCharArray())
                            );
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        }
                        return httpClientBuilder;
                    })
                    .build();

            OpenSearchClient client = new OpenSearchClient(transport);
            log.info("OpenSearch client initialized: {}://{}:{}", scheme, host, port);
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize OpenSearch client", e);
            throw new RuntimeException("OpenSearch initialization failed", e);
        }
    }
}
