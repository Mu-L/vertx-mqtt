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

package io.vertx.mqtt.test.server;

import io.netty.handler.codec.DecoderException;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServerOptions;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * MQTT server testing about the maximum message size
 */
@RunWith(VertxUnitRunner.class)
public class MqttServerMaxMessageSizeTest extends MqttServerBaseTest {

  private Async async;
  private boolean expectReceiveMsg;

  private static final String MQTT_TOPIC = "/my_topic";
  private static final int MQTT_MESSAGE_SIZE = 64;
  private static final int MQTT_MAX_MESSAGE_SIZE =
      + 2 // Topic length
      + MQTT_TOPIC.length() // Topic
      + MQTT_MESSAGE_SIZE // Message
    ;

  @Before
  public void before(TestContext context) {

    MqttServerOptions options = new MqttServerOptions();
    options.setMaxMessageSize(MQTT_MAX_MESSAGE_SIZE);

    this.setUp(context, options);
  }

  @Test
  public void publishMaxMessageSize(TestContext context) {
    publishBigMessage(context, MQTT_MESSAGE_SIZE, true);
  }

  @Test
  public void publishLargerThanMaxMessageSize(TestContext context) {
    publishBigMessage(context, MQTT_MESSAGE_SIZE + 1, false);
  }

  private void publishBigMessage(TestContext context, int messageSize, boolean expectReceiveMsg) {

    this.async = context.async();
    this.expectReceiveMsg = expectReceiveMsg;

    try {

      MemoryPersistence persistence = new MemoryPersistence();
      MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_SERVER_HOST, MQTT_SERVER_PORT), "12345", persistence);
      client.connect();

      byte[] message = new byte[messageSize];

      // The client seems to fail when sending IO and block forever (see MqttOutputStream)
      // that makes the test hang forever
      client.setTimeToWait(1000);
      client.publish(MQTT_TOPIC, message, 0, false);
    } catch (MqttException ignore) {
    }
  }

  @After
  public void after(TestContext context) {

    this.tearDown(context);
  }

  @Override
  protected void endpointHandler(MqttEndpoint endpoint, TestContext context) {

    endpoint.publishHandler(pub -> {
      if (expectReceiveMsg) {
        this.async.complete();
      } else {
        context.fail("Was not expecting msg");
      }
    });
    endpoint.exceptionHandler(t -> {
      if (expectReceiveMsg) {
        context.fail(t);
      } else {
        if (t instanceof DecoderException) {
          this.async.complete();
        } else {
          context.fail(t);
        }
      }
    });

    endpoint.accept(false);
  }
}
