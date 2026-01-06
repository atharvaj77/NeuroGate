package com.neurogate.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TRACE_TOPIC = "neurogate-traces";
    public static final String ANNOTATION_TOPIC = "neurogate-annotations";

    @Bean
    public NewTopic traceTopic() {
        return TopicBuilder.name(TRACE_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic annotationTopic() {
        return TopicBuilder.name(ANNOTATION_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
