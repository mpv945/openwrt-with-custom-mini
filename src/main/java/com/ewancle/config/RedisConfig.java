package com.ewancle.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.redis")
public interface RedisConfig {

    String hosts();

    String password();

    String publishChannelName();

    String subscribeChannelName();
}
