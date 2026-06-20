# zrlog-plugin-webhook

ZrLog Webhook 通知插件。可作为 `webhook` 通知通道接收系统通知，并按配置发送到外部 Webhook 地址。

发送目标通过插件配置选择。当前支持：

- `feishu`：按飞书机器人文本消息格式发送，支持飞书签名密钥。
- `generic_json`：将系统通知内容以 JSON 发送到目标地址。

插件同时提供 `/p/webhook/incoming` 入口，用于外部系统写入入站 Webhook 记录。认证方式为请求头：

```http
Authorization: Bearer <incomingToken>
```

站内信属于 `zrlog-admin-web` 的消息中心功能，本插件不在本地存储或模拟后台站内信。

## 功能

- 配置 Webhook 目标类型和地址
- 发送测试消息
- 接收并记录入站 Webhook 请求
- 记录出站通知和入站请求的状态、内容和错误信息

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```
