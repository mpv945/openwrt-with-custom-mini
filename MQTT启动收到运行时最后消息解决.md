你这个现象：

> ✅ 每次重启应用
> ✅ MQTT 都会消费到“最后一次消息”

这在 **MQTT 是正常行为**，通常由下面 3 个原因导致。

---

# 🎯 一句话结论

你大概率启用了：

* `clean-session=false`
* 或 topic 有 `retain=true`
* 或 QoS 1 / 2 未 ack

我们逐个分析。

---

# 一、最常见原因：Retain 消息

如果发布时使用：

```java
MqttMessage.of(topic, payload, 1, true);
```

最后一个参数 `true` = retain

Broker 会：

> 保存最后一条消息
> 任何新订阅者都会收到它

这就是你每次启动都会收到“最后一次消息”的原因。

---

## 如何确认？

查看你的发送代码是否：

```java
retain = true
```

或者配置：

```properties
mp.messaging.outgoing.xxx.retain=true
```

---

## 解决办法

如果不希望保留：

```java
MqttMessage.of(topic, payload, 1, false);
```

或者：

```properties
mp.messaging.outgoing.xxx.retain=false
```

---

# 二、第二种原因：clean-session=false

如果配置：

```properties
mp.messaging.incoming.temperature.clean-session=false
```

含义：

* Broker 会记住这个 clientId
* 离线期间的 QoS1/2 消息会缓存
* 重连时重新投递

---

## 生产建议

### 如果只是普通订阅

```properties
mp.messaging.incoming.temperature.clean-session=true
```

### 如果希望断线重连不丢消息

```properties
clean-session=false
```

但要保证：

* 唯一 client-id
* 正确 ACK

---

# 三、第三种原因：QoS 1 / 2 未正确 ack

如果：

* 使用 QoS 1 或 2
* 你没有正确 ack

Broker 会认为：

> 消息未确认
> 重连时再次投递

---

## 正确写法（手动 ack）

```java
@Incoming("temperature")
public Uni<Void> consume(Message<String> msg) {
    return process(msg.getPayload())
            .onItem().transformToUni(v -> msg.ack());
}
```

---

# 四、生产环境最佳组合

根据不同场景选择：

---

## 场景1：普通实时消费（不关心历史）

```properties
clean-session=true
retain=false
qos=0
```

---

## 场景2：可靠消费（断线不丢）

```properties
clean-session=false
qos=1
retain=false
```

并保证：

* 唯一 client-id
* 正确 ack

---

## 场景3：配置广播型（希望新客户端收到最后状态）

```properties
retain=true
```

典型场景：

* 设备状态
* 心跳
* 在线状态

---

# 五、快速排查步骤

请检查：

1️⃣ 发送端是否 `retain=true`
2️⃣ incoming 是否 `clean-session=false`
3️⃣ QoS 是否为 1 或 2
4️⃣ 是否使用固定 client-id

---

# 六、如果你想完全避免“启动时收到旧消息”

最安全配置：

```properties
mp.messaging.incoming.temperature.clean-session=true
mp.messaging.incoming.temperature.qos=0
```

发送端：

```java
retain=false
```

---

# 七、结合你当前场景（MQTT + Scheduler）

你之前在做：

* 定时发送 MQTT
* ACK 控制
* 生产级可靠发送

如果你做的是“心跳任务”，那：

* retain=true 是正常的
* 因为新节点上线需要知道当前状态

但如果是普通数据流：

* retain 一定要关掉

---

# 🎯 最终总结

| 现象          | 原因                  |
| ----------- | ------------------- |
| 每次启动收到最后消息  | retain=true         |
| 重启后收到离线期间消息 | clean-session=false |
| 重连重复消息      | 未正确 ack             |

---

如果你愿意，把你的：

* incoming 配置
* outgoing 配置
* QoS
* 是否设置 client-id

贴给我，我可以帮你做一次完整生产级审计，告诉你当前行为属于哪种模式。
