package com.platform.workflow.config;

import com.platform.workflow.deployment.messaging.BundleDeployEvent;
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

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.platform.workflow.*,com.platform.common.*");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /** Used by ProcessStartConsumer — max.poll.records=20 for burst absorption backpressure */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> processStartListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setConsumerRebalanceListener(null);

        Map<String, Object> extraProps = new HashMap<>();
        extraProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 20);
        factory.getContainerProperties().setKafkaConsumerProperties(toProperties(extraProps));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    /**
     * Dedicated factory for the cross-service studio.deploy.events topic. platform-studio-backend
     * publishes its own BundleDeployEvent class, which isn't on this module's classpath, so type
     * headers are ignored and the payload is deserialized structurally into this module's own
     * BundleDeployEvent — the two services only agree on the JSON wire shape, not a shared class.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BundleDeployEvent> bundleDeployListenerContainerFactory() {
        JsonDeserializer<BundleDeployEvent> deserializer = new JsonDeserializer<>(BundleDeployEvent.class, false);
        deserializer.ignoreTypeHeaders();
        deserializer.trustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ConsumerFactory<String, BundleDeployEvent> cf =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);

        var factory = new ConcurrentKafkaListenerContainerFactory<String, BundleDeployEvent>();
        factory.setConsumerFactory(cf);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    private java.util.Properties toProperties(Map<String, Object> map) {
        java.util.Properties props = new java.util.Properties();
        props.putAll(map);
        return props;
    }
}
