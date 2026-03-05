package com.ewancle;

import com.ewancle.config.RedisConfig;
import com.ewancle.service.MqttService;
import com.ewancle.service.RedisService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

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

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        redisService.publish(redisConfig.publishChannelName(),"测试redis消息发布");
        return "Hello from Quarkus REST";
    }

    // 1️⃣ 模拟发送 MQTT 数据: mosquitto_pub -h localhost -t sensor/temperature -m '{"deviceId":"dev1","value":60}'
    // 2️⃣ 调用 REST 发送 MQTT 消息: curl -X POST http://localhost:8080/mqtt/alert/dev2/30
    @POST
    @Path("/alert/{device}/{value}")
    public String send(@PathParam("device") String device,
                       @PathParam("value") double value) {

        mqttService.sendAlert(device, value);
        return "Alert sent!";
    }
}
