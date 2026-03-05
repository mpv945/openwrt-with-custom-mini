package com.ewancle.service;

import com.ewancle.config.RedisConfig;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.pubsub.ReactivePubSubCommands;
import io.quarkus.redis.datasource.stream.*;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class RedisService {

    private static final Logger LOG = Logger.getLogger(RedisService.class);

    @ConfigProperty(name = "quarkus.redis.hosts")
    //@ConfigProperty(name = "redis.url", defaultValue = "redis://localhost:6379")
    String redisUrl;

    @Inject
    RedisConfig redisConfig;

    /*@Inject
    RedisClient redisClient;
    // 如果需要 Reactive
    @Inject
    ReactiveRedisClient redis;*/


    // 多 Redis Client
    //quarkus.redis.orders.hosts=redis://localhost:6379
    //quarkus.redis.cache.hosts=redis://localhost:6380
    /*@Inject
    @RedisClientName("orders")
    ReactiveRedisClient ordersRedis;*/


    // Quarkus Redis Reactive 命令接口概览
    //| 接口                               | 操作类型         | 说明                                                        |
    //| -------------------------------- | ------------ | --------------------------------------------------------- |
    //| `ReactiveKeyCommands<K,V>`  | 通用 key-value | 支持基本的 KV 操作、键存在判断、过期设置、删除等                                |
    //| `ReactiveValueCommands<K,V>`     | String value | 专门针对 Redis 字符串（String）类型，支持原子操作如 incr、decr、append、setNX 等 |
    //| `ReactiveHashCommands<K,H,V>`    | Hash         | 操作 Redis hash                                             |
    //| `ReactiveListCommands<K,V>`      | List         | 操作 Redis list                                             |
    //| `ReactiveSetCommands<K,V>`       | Set          | 操作 Redis set                                              |
    //| `ReactiveSortedSetCommands<K,V>` | Sorted Set   | 操作 Redis zset                                             |

    // 二、区别
    //| 特性      | ReactiveKeyValueCommands                              | ReactiveValueCommands                                 |
    //| ------   | ----------------------------------------------------- | ----------------------------------------------------- |
    //| 数据类型   | 通用 KV                                                 | Redis String                                          |
    //| 典型方法   | `exists`, `del`, `expire`, `get`, `set`               | `get`, `set`, `incr`, `decr`, `append`, `setIfAbsent` |
    //| 原子操作   | 需 Lua 脚本                                              | Redis 原生支持，如 `INCR`、`SET NX PX`                       |
    //| TTL 支持  | `expire(key, Duration)` 或 `set(key, value, Duration)` | 同样可以使用 `set(key, value, Duration)`                    |
    //| 适用场景   | KV 类型存储、过期策略、通用操作                                     | 计数器、限流、原子锁、需要 Redis 原生操作的 String 类型                   |

    private ReactiveRedisDataSource ds;
    private ReactiveValueCommands<String, String> values;
    private ReactivePubSubCommands<String> pubsub;
    /*private ReactiveStreamCommands<String, String, String> stream;
    private StreamCommands<String, String, String> syncStream;*/
    private ReactiveKeyCommands<String> keys;


    @Inject
    //public RedisService(ReactiveRedisDataSource ds, RedisDataSource syncDs) {
    public RedisService(ReactiveRedisDataSource ds) {
        this.ds = ds;
        this.values = ds.value(String.class);
        this.keys= ds.key(String.class);
        this.pubsub = ds.pubsub(String.class);
        /*stream = ds.stream(String.class, String.class, String.class);
        syncStream = syncDs.stream(String.class, String.class, String.class);*/
    }

    // 操作key
    public void testKey(){
        // 判断 key 是否存在
        keys.exists("myKey")
                .subscribe().with(exists -> {
                    if (exists) System.out.println("Key 存在");
                    else System.out.println("Key 不存在");
                });

        // 删除 key
        keys.del("myKey")
                .subscribe().with(del -> System.out.println("删除数量: " + del));

        // 设置过期时间
        keys.expire("myKey", Duration.ofSeconds(60))
                .subscribe().with(success -> System.out.println("expire 设置成功: " + success));
    }
    // 设值
    public void setValue(String key, String value) {
        values.set(key, value)
                .subscribe().with(
                        res -> System.out.println("OK"),
                        Throwable::printStackTrace
                );
    }

    /**
     * 设置过期的
     * @param key
     * @param value
     * @param expire Duration.ofSeconds(60) 秒 ； duration.toMillis() 毫秒数； duration.toMinutes() 分钟数
     */
    public void setValue(String key, String value, Duration expire) {
        values.setex("myKey", expire.getSeconds(), "value")
                .subscribe().with(resp -> {
                    System.out.println("设置成功: " + resp); // Redis 返回 "OK"
                });
    }
    public void getValue(String key) {
        values.get("key")
                .subscribe().with(System.out::println);
    }
    public void test(){
        // 原子计数
        values.incr("counterKey")
                .subscribe().with(count -> System.out.println("计数: " + count));
        values.decr("counter")
                .subscribe().with(count -> System.out.println("递减后: " + count));
        // 设置 key=value，并且 60 秒后过期
        values.setex("lockKey", 60, "locked")
                .subscribe().with(ok -> System.out.println("设置成功"));

        ds.execute("SET", "lockKey", "locked", "NX", "PX", "30000")
                .subscribe().with(resp -> {
                    if (resp != null && "OK".equals(resp.toString())) {
                        System.out.println("获得锁");
                    } else {
                        System.out.println("锁已存在");
                    }
                });
        //| 参数        | 类型     | 说明                                    |
        //| --------- | ------   | ------------------------------------- |
        //| `"SET"`   | 命令名称   | Redis SET 命令                          |
        //| `"myKey"` | String   | 要设置的 key                              |
        //| `"value"` | String   | key 对应的值                              |
        //| `"NX"`    | 选项     | 只在 key 不存在时设置（原子操作），如果 key 存在则返回 null |
        //| `"PX"`    | 选项     | 设置过期时间单位为毫秒                           |
        //| `"60000"` | 数字     | TTL 60000 毫秒 = 60 秒                   |
        ds.execute("SET", "myKey", "value", "NX", "PX", "60000")
                .subscribe().with(resp -> {
                    if (resp != null && "OK".equals(resp.toString())) {
                        System.out.println("成功设置 Key");
                    } else {
                        System.out.println("Key 已存在或未设置成功");
                    }
                });
        // 二、原子计数 + TTL（Lua 脚本）
        String lua = "local c = redis.call('INCR', KEYS[1]) " +
                "if c == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
                "return c";
        // KEYS[1] = counterKey, ARGV[1] = TTL 毫秒
        // | 参数             | 类型     | 说明                                      |
        //| -------------- | ------ | --------------------------------------- |
        //| `"EVAL"`       | 命令名称   | 执行 Lua 脚本                               |
        //| `lua`          | String | Lua 脚本内容                                |
        //| `"1"`          | String | `numkeys` → Lua 脚本中 KEYS 的数量，这里只有一个 key |
        //| `"counterKey"` | String | Lua 脚本 KEYS[1] 的值                       |
        //| `"60000"`      | String | Lua 脚本 ARGV[1] 的值，TTL 毫秒                |
        ds.execute("EVAL", lua, "1", "counterKey", "60000")
                .subscribe().with(resp -> {
                    long count = resp.toLong();
                    System.out.println("当前计数: " + count);
                });
    }

    // 发布消息：
    public void publish(String channelName, String message) {
        pubsub.publish(channelName, message)
                .subscribe().with(
                        subscribers -> {
                            System.out.println("消息发送成功 ");
                        },
                        Throwable::printStackTrace
                );
    }
    // 订阅：
    void onStart(@Observes StartupEvent ev) {
        LOG.infof("Redis 连接地址 = %s", redisUrl);
        Config config = ConfigProvider.getConfig();
        String redisPwd = config.getOptionalValue("quarkus.redis.password", String.class)
                .orElse("redis://localhost:6379");
        LOG.infof("Redis 连接密码 = %s", redisPwd);

        LOG.infof("Redis 连接地址 = %s, 连接密码 = %s", redisConfig.hosts(), redisConfig.password());
        pubsub.subscribe(redisConfig.subscribeChannelName())
                .subscribe().with(
                        message -> LOG.infof("收到消息: %s", message),
                        failure -> LOG.error("订阅出错", failure)
                );
    }

}
