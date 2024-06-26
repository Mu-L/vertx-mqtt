/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vertx.mqtt.it;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.mqtt.MqttClient;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * MQTT client testing on unsubscribing topics
 */
public class MqttClientUnsubscribeIT extends MqttClientBaseIT {

  private static final Logger log = LoggerFactory.getLogger(MqttClientUnsubscribeIT.class);

  private static final String MQTT_TOPIC = "/my_topic";

  private static final List<String> MQTT_TOPIC_LIST = Arrays.asList("my_topic1", "my_topic2");

  private int messageId = 0;

  @Test
  public void unsubscribeQoS0(TestContext context) throws InterruptedException {
    this.unsubscribe(context, MqttQoS.AT_MOST_ONCE);
  }

  @Test
  public void unsubscribeQoS1(TestContext context) throws InterruptedException {
    this.unsubscribe(context, MqttQoS.AT_LEAST_ONCE);
  }

  @Test
  public void unsubscribeQoS2(TestContext context) throws InterruptedException {
    this.unsubscribe(context, MqttQoS.EXACTLY_ONCE);
  }

  private void unsubscribe(TestContext context, MqttQoS qos) {

    this.messageId = 0;

    Async async = context.async();
    MqttClient client = MqttClient.create(Vertx.vertx());

    client.unsubscribeCompletionHandler(unsubackid -> {
      assertTrue(unsubackid == messageId);
      log.info("unsubscribing complete for message id = " + unsubackid);
      client.disconnect();
      async.countDown();
    });

    client.subscribeCompletionHandler(suback -> {
      context.assertTrue(suback.messageId() == messageId);
      context.assertTrue(suback.grantedQoSLevels().contains(qos.value()));
      log.info("subscribing complete for message id = " + suback.messageId() + " with QoS " + suback.grantedQoSLevels());

      client.unsubscribe(MQTT_TOPIC).onComplete(context.asyncAssertSuccess(res2 -> {
        messageId = res2;
        log.info("unsubscribing on [" + MQTT_TOPIC + "] message id = " + messageId);
      }));

      client.unsubscribe(MQTT_TOPIC_LIST).onComplete(context.asyncAssertSuccess(res2 -> {
        messageId = res2;
        log.info("unsubscribing on [" + MQTT_TOPIC_LIST + "] message id = " + messageId);
      }));
    });

    client.connect(port, host).onComplete(context.asyncAssertSuccess(v -> {
      client.subscribe(MQTT_TOPIC, qos.value()).onComplete(context.asyncAssertSuccess(res2 -> {
        messageId = res2;
        log.info("subscribing on [" + MQTT_TOPIC + "] with QoS [" + qos.value() + "] message id = " + messageId);
      }));
    }));

    async.await();
  }
}
