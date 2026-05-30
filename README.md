# zrlog-plugin-webhook

ZrLog Webhook 通知插件。当前版本通过 `notification.webhook.send` 暴露为 v4 插件运行时的标准 `webhook` 通知通道。

发送目标通过插件配置选择。当前内置：

- `feishu`：飞书机器人文本消息格式，支持飞书签名密钥。
- `generic_json`：直接把标准通知载荷以 JSON 发送给目标地址。

插件同时提供 `/p/webhook/incoming` 公开入口，用于外部系统写入 Webhook 接收日志。认证方式为标准请求头：

```http
Authorization: Bearer <incomingToken>
```

站内信属于 `zrlog-admin-web` 的消息中心能力，本插件不在本地存储或模拟后台站内信；后续如需接入站内信，应通过后台消息中心暴露的能力或 API 写入。

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```
