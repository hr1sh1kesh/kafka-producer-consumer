package com.hrishi.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class SpringKafkaReceiverTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringKafkaReceiverTest.class);

  private static String RECEIVER_TOPIC = "receiver.t";

  @Autowired
  private Receiver receiver;

  private KafkaTemplate<String, String> template;

  @Autowired
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @ClassRule
  public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, RECEIVER_TOPIC);

  @Before
  public void setUp() throws Exception {
    Map<String, Object> senderProperties =
        KafkaTestUtils.senderProps(embeddedKafka.getBrokersAsString());

    ProducerFactory<String, String> producerFactory =
        new DefaultKafkaProducerFactory<String, String>(senderProperties);

    template = new KafkaTemplate<>(producerFactory);
    template.setDefaultTopic(RECEIVER_TOPIC);

    for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
        .getListenerContainers()) {
      ContainerTestUtils.waitForAssignment(messageListenerContainer,
          embeddedKafka.getPartitionsPerTopic());
    }
  }

  @Test
  public void testReceive() throws Exception {
    String greeting = "Hello Spring Kafka Receiver!";
    template.sendDefault(greeting);
    LOGGER.debug("test-sender sent message='{}'", greeting);

    receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
    assertThat(receiver.getLatch().getCount()).isEqualTo(0);
  }
}
