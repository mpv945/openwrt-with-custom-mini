package com.ewancle;

import com.ewancle.config.RedisConfig;
import com.ewancle.dto.Extension;
import com.ewancle.service.MqttService;
import com.ewancle.service.OpenwrtRestClientService;
import com.ewancle.service.RedisService;
import com.ewancle.service.RestClientService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

@Path("/hello")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GreetingResource {

    @Inject
    RedisService redisService;

    @Inject
    MqttService mqttService;

    @Inject
    RedisConfig redisConfig;

    /*@RestClient
    RestClientService restClientService;*/

    @RestClient
    OpenwrtRestClientService openwrtRestClientService;

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        redisService.publish(redisConfig.publishChannelName(),"测试redis消息发布");

        Set<Extension> restClientExtensions = openwrtRestClientService.getExtensionsById("io.quarkus:quarkus-hibernate-validator");
        try {
            String json = objectMapper.writeValueAsString(restClientExtensions);
            System.out.println("http请求结果="+json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return "Hello from Quarkus REST";
    }

    //REDIS_PWD=ezzBTBzKUwGp0VkKFsdbV6DHwZrMuiGu;REDIS_URL=redis://redis-19019.c283.us-east-1-4.ec2.cloud.redislabs.com:19019;MQTT_HOST=154.21.85.97;MQTT_PORT=1883;MQTT_NAME=device001;MQTT_PWD=asdf1@34
    // 1️⃣ 模拟发送 MQTT 数据: mosquitto_pub -h localhost -t sensor/temperature -m '{"deviceId":"dev1","value":60}'
    // 2️⃣ 调用 REST 发送 MQTT 消息: curl -X POST http://localhost:8080/mqtt/alert/dev2/30
    //@POST
    @GET
    /*@Path("/alert/{device}/{value}")
    public String send(@PathParam("device") String device,
                       @PathParam("value") double value) {*/
    @Path("/mqtt/send")
    public Uni<Response> send(@QueryParam("payload") String payload) {

        //mqttService.sendAlert(device, value);
        MqttMessage<String> message =
                MqttMessage.of(
                        "sensor/alert",  // topic (可动态指定)
                        payload,
                        MqttQoS.EXACTLY_ONCE,  // qos
                        true             // retain
                );
        return mqttService
                .send(message).onItem().transform(v -> Response.ok("消息发送成功").build())
                .onFailure().recoverWithItem(ex ->
                        Response.status(500)
                                .entity("发送失败: " + ex.getMessage())
                                .build()
                );


    }


    // 调用http
    /*@GET
    @Path("/properties")
    public RestResponse<Set<Extension>> responseProperties(@RestQuery String id) {
        RestResponse<Set<Extension>> clientResponse = restClientService.getByIdResponseProperties(id);
        String contentType = clientResponse.getHeaderString("Content-Type");
        int status = clientResponse.getStatus();
        String setCookie = clientResponse.getHeaderString("Set-Cookie");
        Date lastModified = clientResponse.getLastModified();

        *//*Log.infof("content-Type: %s status: %s Last-Modified: %s Set-Cookie: %s", contentType, status, lastModified,
                setCookie);*//*

        return RestResponse.fromResponse(clientResponse);
    }*/

    /*@POST
    public Uni<Response> upload(
            @RestForm("file") InputStream file,
            @RestForm("username") String username
    ) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            java.nio.file.Path target = Paths.get("/tmp/uploads", username + ".bin");
            try {
                Files.copy(file, target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return Response.ok("OK").build();
        }));
    }*/
}
