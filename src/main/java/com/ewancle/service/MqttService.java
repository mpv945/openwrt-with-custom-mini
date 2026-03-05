package com.ewancle.service;

import com.ewancle.dto.Temperature;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class MqttService {

    /*@Incoming("temperature")
    public void consume(Temperature temp) {
        System.out.println("收到温度数据: device="
                + temp.deviceId + ", value=" + temp.value);
        if (temp.value > 50) {
            System.out.println("⚠ 温度过高报警！");
        }
    }
    @Incoming("temperature")
    public void consume1(MqttMessage<String> msg) {
        System.out.println("Payload: " + msg.getPayload());
        System.out.println("QoS: " + msg.getQosLevel());
        System.out.println("Retain: " + msg.isRetain());
    }*/

    /**
     * 带ack确认的消费组
     * @param message
     * @return
     */
    @Incoming("temperature")
    public CompletionStage<Void> consume(Message<String> message) {
        return CompletableFuture
                .runAsync(() -> saveToDatabase(message))
                .thenCompose(v -> message.ack())
                .exceptionally(ex -> {
                    message.nack(ex);
                    return null;
                });
    }
    private void saveToDatabase(Message<String> message) {
        CompletableFuture.runAsync(() -> {
            System.out.println("Payload 接收到消息内容: " + message.getPayload());
            //System.out.println("QoS: " + message.getQosLevel());
            //System.out.println("Retain: " + message.isRetain());
        });
    }

    @Inject
    @Channel("alert")
    MutinyEmitter<MqttMessage<String>> emitterAsync;
    public Uni<Void> send(MqttMessage<String> message) {

        return emitterAsync.send(message)
                .onItem().invoke(
                        () -> System.out.println("发送成功，MQTT ACK 已收到")
                //发送失败自动重试（最多3次，每次间隔1秒）
                ).onFailure().retry().withBackOff(Duration.ofSeconds(1)).atMost(3)
                .onFailure().invoke(
                        ex -> System.err.println("发送失败: " + ex.getMessage())
                );
    }



    /*@Inject
    @Channel("alert")
    Emitter<Temperature> emitter;
    public void sendAlert(String deviceId, double value) {
        Temperature t = new Temperature();
        t.deviceId = deviceId;
        t.value = value;
        emitter.send(t);
    }

    *//**
     * 异步发送
     *//*
    @Inject
    @Channel("alert")
    MutinyEmitter<Temperature> emitterAsync;
    public Uni<Void> sendAsync(Temperature t) {
        return emitterAsync.send(t);
    }

    @Inject
    @Channel("alert")
    Emitter<Message<String>> emitterPlus;
    public void send(String payload) {
        MqttMessage<String> message =
                MqttMessage.of(
                        "sensor/alert",  // topic (可动态指定)
                        payload,
                        MqttQoS.EXACTLY_ONCE,  // qos
                        true             // retain
                );

        emitterPlus.send(message);
    }

    @Inject
    @Channel("alert")
    Emitter<Message<byte[]>> emitterPlus1;
    public void sendJson(String topic, String json, int qos, boolean retain) {
        MqttMessage<byte[]> msg =
                MqttMessage.of(
                        topic,
                        json.getBytes(),
                        MqttQoS.valueOf(qos),
                        retain
                );
        emitterPlus1.send(msg);
    }*/

    //@Channel("alert")
    //Emitter<Message<String>> emitterPlus2;
    public void sendSafe(String msg) {
    //public CompletionStage<Void> sendSafe(String msg) {
        MqttMessage<String> message =
                MqttMessage.of(
                        "sensor/alert",  // topic (可动态指定)
                        msg,
                        MqttQoS.EXACTLY_ONCE,  // qos
                        true             // retain
                );
        /*return emitterPlus2.send(message)
                *//*.thenAccept(v -> {
                    // Broker ACK 成功（QoS1/2）
                    System.out.println("MQTT发送成功");
                })*//*
                .thenRun(this::markSuccess)
                .exceptionally(ex -> {
                    // 连接断开 / Broker拒绝
                    System.err.println("MQTT发送失败: " + ex);
                    return null;
                });*/

        /*CompletionStage<Void> stage = emitterPlus2.send(message);
        stage.whenComplete((res, ex) -> {
            if (ex != null) {
                //log.error("MQTT发送失败", ex);
            } else {
                //log.info("MQTT发送成功");
            }
        });*/

        // 同步等待（阻塞）：.toCompletableFuture().join()和.toCompletableFuture().get() 【生产慎用】
        /*emitter.send(message)
                .toCompletableFuture()
                .join();*/
        // 生产级可靠发送推荐模式
        /*return emitterPlus2.send(buildMessage(data))
                .thenRun(() -> markAsSent(data))
                .exceptionally(ex -> {
                    markAsFailed(data);
                    return null;
                });*/
    }

    private void markSuccess() {

    }
}
