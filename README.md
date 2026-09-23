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

## 原生制品发布

Linux amd64/arm64 制品在上传前会调用 `zrlog-artifact-service`，通过与 `plugin-core`
相同的固定版本 `process-artifact` Action 完成压缩和 SHA-256、文件大小校验。
处理成功后才会生成最终制品的 MD5 并上传；处理失败会停止该平台的发布。
服务接收的版本号使用 `bin/build-info.sh` 生成的实际插件版本。

发布前需要配置 Actions Secret `ARTIFACT_SERVICE_TOKEN`，可在仓库中单独设置，
或授权该仓库使用同名组织 Secret。服务地址为 `https://webdav.zrlog.com/artifact`。
