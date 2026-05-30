import {CopyOutlined, ReloadOutlined, SendOutlined, SettingOutlined} from "@ant-design/icons";
import {Line} from "@ant-design/plots";
import type {ColumnsType} from "antd/es/table";
import {
    Alert,
    Button,
    Card,
    Col,
    Descriptions,
    Drawer,
    Empty,
    Flex,
    Form,
    Grid,
    Input,
    InputNumber,
    Row,
    Select,
    Space,
    Statistic,
    Table,
    Tag,
    Tooltip,
    Typography,
    message,
    theme,
} from "antd";
import axios from "axios";
import {FunctionComponent, useMemo, useState} from "react";
import {PageData, StandardResponse, WebhookConfig, WebhookInfoResponse, WebhookLogRow, WebhookMetric} from "../index";

type WebhookIndexProps = {
    data: WebhookInfoResponse;
}

type FilterValues = {
    keyword?: string;
    status?: string;
    direction?: string;
}

const retentionOptions = [
    {label: "30 天", value: 30},
    {label: "90 天", value: 90},
    {label: "180 天", value: 180},
];

const statusOptions = [
    {label: "全部状态", value: ""},
    {label: "成功", value: "success"},
    {label: "失败", value: "failed"},
];

const directionOptions = [
    {label: "全部方向", value: ""},
    {label: "通知推送", value: "outbound"},
    {label: "入站请求", value: "inbound"},
];

const targetTypeOptions = [
    {label: "飞书机器人", value: "feishu"},
    {label: "通用 JSON", value: "generic_json"},
];

const request = async <T, >(url: string, params?: Record<string, string>) => {
    const {data} = await axios.post<StandardResponse<T>>(url, new URLSearchParams(params), {
        headers: {"Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"},
    });
    if (!data.success) {
        throw new Error(data.message || "操作失败");
    }
    return data.data;
};

const fetchLogs = async (params: Record<string, string>) => {
    const {data} = await axios.get<StandardResponse<PageData<WebhookLogRow>>>("list", {params});
    if (!data.success) {
        throw new Error(data.message || "加载失败");
    }
    return data.data;
};

const statusColor = (status: WebhookMetric["status"]) => {
    if (status === "processing") {
        return "processing";
    }
    if (status === "warning") {
        return "warning";
    }
    return "default";
};

const directionLabel = (value: string) => {
    if (value === "outbound") {
        return "通知推送";
    }
    if (value === "inbound") {
        return "入站请求";
    }
    return value || "-";
};

const channelLabel = (value: string) => {
    if (value === "webhook") {
        return "Webhook";
    }
    if (value === "incoming") {
        return "公开入口";
    }
    return value || "-";
};

const WebhookIndex: FunctionComponent<WebhookIndexProps> = ({data}) => {
    const [config, setConfig] = useState<WebhookConfig>(data.config);
    const [metrics, setMetrics] = useState<WebhookMetric[]>(data.summary || []);
    const [trend, setTrend] = useState(data.trend || []);
    const [logs, setLogs] = useState<PageData<WebhookLogRow>>(data.logs);
    const [filters, setFilters] = useState<FilterValues>({});
    const [loading, setLoading] = useState(false);
    const [settingOpen, setSettingOpen] = useState(false);
    const [detail, setDetail] = useState<WebhookLogRow | null>(null);
    const [form] = Form.useForm<WebhookConfig>();
    const [messageApi, contextHolder] = message.useMessage();
    const {token} = theme.useToken();
    const screens = Grid.useBreakpoint();

    const loadLogs = async (page = logs.page, pageSize = logs.pageSize, nextFilters = filters) => {
        setLoading(true);
        try {
            setLogs(await fetchLogs({
                page: String(page),
                pageSize: String(pageSize),
                keyword: nextFilters.keyword || "",
                status: nextFilters.status || "",
                direction: nextFilters.direction || "",
            }));
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    };

    const refreshPage = async () => {
        setLoading(true);
        try {
            const {data: response} = await axios.get<StandardResponse<WebhookInfoResponse>>("json");
            if (!response.success) {
                throw new Error(response.message || "加载失败");
            }
            setConfig(response.data.config);
            setMetrics(response.data.summary || []);
            setTrend(response.data.trend || []);
            setLogs(response.data.logs);
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "加载失败");
        } finally {
            setLoading(false);
        }
    };

    const openSetting = () => {
        form.setFieldsValue(config);
        setSettingOpen(true);
    };

    const saveSetting = async () => {
        const values = await form.validateFields();
        try {
            const saved = await request<WebhookConfig>("update", {
                webhookUrl: values.webhookUrl || "",
                webhookTargetType: values.targetType || "feishu",
                webhookSigningSecret: values.signingSecret || "",
                incomingToken: values.incomingToken || "",
                webhookTimeoutSeconds: String(values.timeoutSeconds || 10),
                webhookLogRetentionDays: String(values.retentionDays || 30),
            });
            setConfig(saved);
            await refreshPage();
            messageApi.success("已保存");
            setSettingOpen(false);
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "保存失败");
        }
    };

    const sendTest = async () => {
        setLoading(true);
        try {
            const result = await request<{ status: number }>("testWebhook");
            if (result.status === 200) {
                messageApi.success("测试消息已发送");
            } else {
                messageApi.error("测试消息发送失败");
            }
            await refreshPage();
        } catch (e) {
            messageApi.error(e instanceof Error ? e.message : "测试消息发送失败");
            await refreshPage();
        } finally {
            setLoading(false);
        }
    };

    const copyToken = async () => {
        try {
            await navigator.clipboard.writeText(config.incomingToken || "");
            messageApi.success("Token 已复制");
        } catch (e) {
            messageApi.error("复制失败");
        }
    };

    const columns = useMemo<ColumnsType<WebhookLogRow>>(() => [
        {
            title: "时间",
            dataIndex: "time",
            width: 168,
        },
        {
            title: "方向",
            dataIndex: "direction",
            width: 104,
            render: (value: string) => (
                <Tag color={value === "outbound" ? "processing" : "cyan"}>{directionLabel(value)}</Tag>
            ),
        },
        {
            title: "标题",
            dataIndex: "title",
            render: (value: string) => <Typography.Text strong ellipsis>{value || "-"}</Typography.Text>,
        },
        {
            title: "来源",
            dataIndex: "source",
            width: 140,
            render: (value: string) => (
                <Tooltip title={value || "-"}>
                    <Typography.Text ellipsis style={{maxWidth: 120}}>{value || "-"}</Typography.Text>
                </Tooltip>
            ),
        },
        {
            title: "通道",
            dataIndex: "channel",
            width: 104,
            render: (value: string) => <Tag>{channelLabel(value)}</Tag>,
        },
        {
            title: "状态",
            dataIndex: "success",
            width: 96,
            render: (value: boolean) => <Tag color={value ? "success" : "error"}>{value ? "成功" : "失败"}</Tag>,
        },
        {
            title: "详情",
            key: "action",
            width: 84,
            render: (_, row) => <Button size="small" onClick={() => setDetail(row)}>查看</Button>,
        },
    ], []);

    const incomingCurl = `curl -X POST '${data.incomingPath}' -H 'Authorization: Bearer ${config.incomingToken || ""}' -H 'Content-Type: application/json' -d '{"title":"测试","content":"来自外部系统"}'`;
    const emptyDescription = "暂无 Webhook 记录。发送测试消息或外部系统调用入站入口后会显示记录。";
    const targetTypeLabel = targetTypeOptions.find(item => item.value === config.targetType)?.label || "飞书机器人";

    return (
        <div style={{
            width: "100%",
            maxWidth: 1180,
            padding: screens.xs ? 16 : 24,
            boxSizing: "border-box",
            margin: "0 auto",
        }}>
            {contextHolder}
            <Flex
                justify="space-between"
                align={screens.xs ? "stretch" : "flex-start"}
                vertical={screens.xs}
                gap={16}
                style={{ marginBottom: 20 }}
            >
                <div>
                    <Typography.Title level={3} style={{ margin: 0 }}>Webhook 通知</Typography.Title>
                    <Typography.Text type="secondary" style={{ marginTop: 4, display: "block" }}>
                        通用 Webhook 通知通道和最近 {config.retentionDays || 30} 天 Webhook 记录
                    </Typography.Text>
                </div>
                <Space wrap>
                    <Button icon={<ReloadOutlined/>} onClick={refreshPage} loading={loading}>刷新</Button>
                    <Button icon={<SendOutlined/>} onClick={sendTest} loading={loading}>测试推送</Button>
                    <Button icon={<SettingOutlined/>} onClick={openSetting}>设置</Button>
                </Space>
            </Flex>

            <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
                {metrics.map(metric => (
                    <Col key={metric.label} xs={24} sm={12} md={6}>
                        <Card size="small" style={{ position: "relative", minHeight: 92 }}>
                            <Statistic title={metric.label} value={metric.value}/>
                            <Tag
                                color={statusColor(metric.status)}
                                style={{ position: "absolute", top: 14, right: 12, margin: 0 }}
                            >
                                {metric.status === "warning" ? "需关注" : "记录中"}
                            </Tag>
                        </Card>
                    </Col>
                ))}
            </Row>

            <Alert
                style={{ marginBottom: 16 }}
                type={config.webhookUrl ? "success" : "info"}
                showIcon
                message={config.webhookUrl ? "Webhook 目标已配置" : "Webhook 目标未配置"}
                description={config.webhookUrl ? `标准通知会按 ${targetTypeLabel} 格式推送。` : "配置 Webhook 地址后，通知中心可以把消息推送到外部系统。"}
                action={<Button size="small" onClick={openSetting}>设置</Button>}
            />

            <Card style={{ marginBottom: 16 }}>
                <Typography.Text strong style={{ display: "block", fontSize: 15, marginBottom: 12 }}>
                    公开入站入口
                </Typography.Text>
                <Descriptions column={1} size="small" bordered>
                    <Descriptions.Item label="地址">
                        <Typography.Text copyable>{data.incomingPath}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="认证">
                        <Space>
                            <Typography.Text code>Authorization: Bearer {config.incomingToken || "-"}</Typography.Text>
                            <Tooltip title="复制 Token">
                                <Button size="small" icon={<CopyOutlined/>} onClick={copyToken}/>
                            </Tooltip>
                        </Space>
                    </Descriptions.Item>
                    <Descriptions.Item label="示例">
                        <Typography.Text copyable style={{wordBreak: "break-all"}}>{incomingCurl}</Typography.Text>
                    </Descriptions.Item>
                </Descriptions>
            </Card>

            <Card style={{ marginBottom: 16 }}>
                <Typography.Text strong style={{ display: "block", fontSize: 15, marginBottom: 12 }}>
                    记录趋势
                </Typography.Text>
                {trend.length === 0 ? (
                    <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无数据"/>
                ) : (
                    <Line
                        data={trend}
                        height={220}
                        autoFit
                        xField="date"
                        yField="value"
                        color={data.colorPrimary || token.colorPrimary}
                        point={{size: 3}}
                    />
                )}
            </Card>

            <Card>
                <Flex
                    justify="space-between"
                    align={screens.xs ? "stretch" : "center"}
                    vertical={screens.xs}
                    gap={12}
                    style={{ marginBottom: 12 }}
                >
                    <Space wrap>
                        <Input.Search
                            allowClear
                            placeholder="搜索标题、来源、内容、错误"
                            onSearch={value => {
                                const next = {...filters, keyword: value};
                                setFilters(next);
                                loadLogs(1, logs.pageSize, next);
                            }}
                            style={{width: 260}}
                        />
                        <Select
                            value={filters.status || ""}
                            options={statusOptions}
                            onChange={value => {
                                const next = {...filters, status: value};
                                setFilters(next);
                                loadLogs(1, logs.pageSize, next);
                            }}
                            style={{width: 120}}
                        />
                        <Select
                            value={filters.direction || ""}
                            options={directionOptions}
                            onChange={value => {
                                const next = {...filters, direction: value};
                                setFilters(next);
                                loadLogs(1, logs.pageSize, next);
                            }}
                            style={{width: 132}}
                        />
                    </Space>
                    <Typography.Text type="secondary">共 {logs.total} 条</Typography.Text>
                </Flex>
                <Table
                    rowKey="id"
                    size="middle"
                    loading={loading}
                    columns={columns}
                    dataSource={logs.rows}
                    scroll={{x: 980}}
                    locale={{emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyDescription}/>}}
                    pagination={{
                        current: logs.page,
                        pageSize: logs.pageSize,
                        total: logs.total,
                        showSizeChanger: true,
                    }}
                    onChange={pagination => loadLogs(pagination.current || 1, pagination.pageSize || 10)}
                />
            </Card>

            <Drawer
                title="Webhook 设置"
                open={settingOpen}
                width={520}
                onClose={() => setSettingOpen(false)}
                extra={<Button type="primary" onClick={saveSetting}>保存</Button>}
            >
                <Form form={form} layout="vertical">
                    <Form.Item label="目标类型" name="targetType" rules={[{required: true, message: "请选择目标类型"}]}>
                        <Select options={targetTypeOptions}/>
                    </Form.Item>
                    <Form.Item label="Webhook 地址" name="webhookUrl">
                        <Input placeholder="https://example.com/webhook"/>
                    </Form.Item>
                    <Form.Item label="签名密钥" name="signingSecret">
                        <Input.Password placeholder="当前用于飞书机器人签名校验，未开启时可留空"/>
                    </Form.Item>
                    <Form.Item label="入站 Token" name="incomingToken" rules={[{required: true, message: "请输入入站 Token"}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item label="请求超时" name="timeoutSeconds">
                        <InputNumber min={3} max={60} addonAfter="秒" style={{width: 160}}/>
                    </Form.Item>
                    <Form.Item label="记录保留" name="retentionDays">
                        <Select options={retentionOptions}/>
                    </Form.Item>
                </Form>
            </Drawer>

            <Drawer
                title="Webhook 详情"
                open={detail !== null}
                width={600}
                onClose={() => setDetail(null)}
            >
                {detail && (
                    <Descriptions column={1} size="small" bordered>
                        <Descriptions.Item label="时间">{detail.time}</Descriptions.Item>
                        <Descriptions.Item label="方向">{directionLabel(detail.direction)}</Descriptions.Item>
                        <Descriptions.Item label="通道">{channelLabel(detail.channel)}</Descriptions.Item>
                        <Descriptions.Item label="标题">{detail.title || "-"}</Descriptions.Item>
                        <Descriptions.Item label="来源">{detail.source || "-"}</Descriptions.Item>
                        <Descriptions.Item label="内容">{detail.content || "-"}</Descriptions.Item>
                        <Descriptions.Item label="请求 ID">{detail.requestId || "-"}</Descriptions.Item>
                        <Descriptions.Item label="状态">
                            <Tag color={detail.success ? "success" : "error"}>{detail.success ? "成功" : "失败"}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="状态码">{detail.status || "-"}</Descriptions.Item>
                        <Descriptions.Item label="错误">{detail.error || "-"}</Descriptions.Item>
                    </Descriptions>
                )}
            </Drawer>
        </div>
    );
};

export default WebhookIndex;
