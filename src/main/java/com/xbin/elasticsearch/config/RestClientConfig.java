package com.xbin.elasticsearch.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class RestClientConfig extends AbstractElasticsearchConfiguration {

    @Value(value = "${es.dsn}")
    private String dsn;

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

         ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(dsn)
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
}
