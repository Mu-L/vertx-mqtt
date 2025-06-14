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

package examples;

import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.messages.MqttAuthenticationExchangeMessage;
import io.vertx.mqtt.messages.codes.MqttAuthenticateReasonCode;

public class VertxMqttClientExamples {

  /**
   * Example for demonstration of how {@link MqttClient#connect(int, String)} and  {@link MqttClient#disconnect()} methods
   * should be used
   *
   * @param vertx
   */
  public void example1(Vertx vertx) {
    MqttClient client = MqttClient.create(vertx);

    client.connect(1883, "mqtt.eclipse.org").onComplete(s -> {
      client.disconnect();
    });
  }

  /**
   * Example for handling publish messages from server
   *
   * @param client
   */
  public void example2(MqttClient client) {
    client.publishHandler(s -> {
      System.out.println("There are new message in topic: " + s.topicName());
      System.out.println("Content(as string) of the message: " + s.payload().toString());
      System.out.println("QoS: " + s.qosLevel());
    })
      .subscribe("rpi2/temp", 2);
  }

  /**
   * Example for sending publish message
   *
   * @param client
   */
  public void example3(MqttClient client) {
    client.publish("temperature",
      Buffer.buffer("hello"),
      MqttQoS.AT_LEAST_ONCE,
      false,
      false);
  }

  /**
   * Example for disabling keepAlive feature
   *
   * @param options
   */
  public void example4(MqttClientOptions options) {
    options.setAutoKeepAlive(false);
  }

  /**
   * Example for publishCompletionHandler method demonstration
   *
   * @param client
   */
  public void example5(MqttClient client) {
    client.publishCompletionHandler(id -> {
      System.out.println("Id of just received PUBACK or PUBCOMP packet is " + id);
    });
      // The line of code below will trigger publishCompletionHandler (QoS 2)
    client.publish("hello", Buffer.buffer("hello"), MqttQoS.EXACTLY_ONCE, false, false);
      // The line of code below will trigger publishCompletionHandler (QoS is 1)
    client.publish("hello", Buffer.buffer("hello"), MqttQoS.AT_LEAST_ONCE, false, false);
      // The line of code below does not trigger because QoS value is 0
    client.publish("hello", Buffer.buffer("hello"), MqttQoS.AT_LEAST_ONCE, false, false);

  }

  /**
   * Example for subscribeCompletionHandler method demonstration
   *
   * @param client
   */
  public void example6(MqttClient client) {
    client.subscribeCompletionHandler(mqttSubAckMessage -> {
      System.out.println("Id of just received SUBACK packet is " + mqttSubAckMessage.messageId());
      for (int s : mqttSubAckMessage.grantedQoSLevels()) {
        if (s == 0x80) {
          System.out.println("Failure");
        } else {
          System.out.println("Success. Maximum QoS is " + s);
        }
      }
    });
    client.subscribe("temp", 1);
    client.subscribe("temp2", 2);
  }

  /**
   * Example for unsubscribeCompletionHandler method demonstration
   *
   * @param client
   */
  public void example7(MqttClient client) {
    client
      .unsubscribeCompletionHandler(id -> {
        System.out.println("Id of just received UNSUBACK packet is " + id);
      });
    client.subscribe("temp", 1);
    client.unsubscribe("temp");
  }

  /**
   * Example for unsubscribe method demonstration
   *
   * @param client
   */
  public void example8(MqttClient client) {
    client.subscribe("temp", 1);
    client.unsubscribe("temp").onSuccess(id -> {
        System.out.println("Id of just sent UNSUBSCRIBE packet is " + id);
      });
  }

  /**
   * Example for pingResponseHandler method demonstration
   *
   * @param client
   */
  public void example9(MqttClient client) {
    client.pingResponseHandler(s -> {
      //The handler will be called time to time by default
      System.out.println("We have just received PINGRESP packet");
    });
  }

  /**
   * Example for authenticationExchangeHandler and authenticationExchange method demonstration
   *
   * @param client
   */
  public void example10(MqttClient client) {
    client.authenticationExchange(MqttAuthenticationExchangeMessage.create(MqttAuthenticateReasonCode.SUCCESS, MqttProperties.NO_PROPERTIES));
    client.authenticationExchangeHandler(auth -> {
      //The handler will be called time to time by default
      System.out.println("We have just received AUTH packet: " + auth.reasonCode());
    });
  }

  public void tls(Vertx vertx, String algo) {
    MqttClientOptions options = new MqttClientOptions();
    options
      .setSsl(true)
      .setTrustOptions(new PemTrustOptions().addCertPath("/path/to/server.crt"))
      // Algo can be the empty string "" or "HTTPS" to verify the server hostname
      .setHostnameVerificationAlgorithm(algo);
    MqttClient client = MqttClient.create(vertx, options);
    client.connect(1883, "mqtt.eclipse.org").onComplete(s -> {
      client.disconnect();
    });
  }
}
