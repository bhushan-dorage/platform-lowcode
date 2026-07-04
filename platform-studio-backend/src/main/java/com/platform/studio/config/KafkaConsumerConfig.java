package com.platform.studio.config;

import com.platform.studio.artifact.messaging.DeploymentStatusEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * platform-workflow-engine publishes its own DeploymentStatusEvent class, which isn't on this
     * module's classpath, so type headers are ignored and the payload is deserialized structurally
     * into this module's own DeploymentStatusEvent.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeploymentStatusEvent> deploymentStatusListenerContainerFactory() {
        JsonDeserializer<DeploymentStatusEvent> deserializer = new JsonDeserializer<>(DeploymentStatusEvent.class, false);
        deserializer.ignoreTypeHeaders();
        deserializer.trustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, DeploymentStatusEvent> cf =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, DeploymentStatusEvent>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
