import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, Layout, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import axios from "axios";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";

const {darkAlgorithm, defaultAlgorithm} = theme;
const {Content} = Layout;

export interface Plugin {
    id: string;
    version: string;
    name: string;
    paths: string[];
    actions: string[];
    desc: string;
    author: string;
    shortName: string;
    indexPage: string;
    previewImageBase64: string;
    services: string[];
    dependentService: string[];
}

export interface WebhookConfig {
    webhookUrl: string;
    targetType: string;
    signingSecret: string;
    incomingToken: string;
    timeoutSeconds: number;
    retentionDays: number;
}

export interface WebhookMetric {
    label: string;
    value: number;
    status: "normal" | "processing" | "warning";
}

export interface WebhookTrendRow {
    date: string;
    value: number;
}

export interface WebhookLogRow {
    id: string;
    timestamp: number;
    time: string;
    direction: string;
    channel: string;
    title: string;
    content: string;
    source: string;
    success: boolean;
    status: number;
    error: string;
    requestId: string;
}

export interface PageData<T> {
    rows: T[];
    total: number;
    page: number;
    pageSize: number;
}

export interface WebhookInfoResponse {
    dark: boolean;
    colorPrimary: string;
    plugin: Plugin;
    config: WebhookConfig;
    summary: WebhookMetric[];
    trend: WebhookTrendRow[];
    logs: PageData<WebhookLogRow>;
    incomingPath: string;
}

export interface StandardResponse<T> {
    success: boolean;
    message?: string;
    data: T;
}

const loadFromDocument = () => {
    try {
        const node = document.getElementById("pluginInfo");
        if (node === null || node.innerText.length === 0) {
            return null;
        }
        return JSON.parse(node.innerText) as StandardResponse<WebhookInfoResponse>;
    } catch (e) {
        return null;
    }
};

const Index = () => {
    const [response, setResponse] = useState<StandardResponse<WebhookInfoResponse> | null>(loadFromDocument);

    useEffect(() => {
        if (response === null) {
            axios.get<StandardResponse<WebhookInfoResponse>>("json").then(({data}) => {
                setResponse(data);
            });
        }
    }, [response]);

    if (response === null || !response.success) {
        return <></>;
    }

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: response.data.dark ? darkAlgorithm : defaultAlgorithm,
                token: {
                    colorPrimary: response.data.colorPrimary || "#1677ff",
                },
            }}
        >
            <StyleProvider transformers={[legacyLogicalPropertiesTransformer]}>
                <Content>
                    <App>
                        <AppBase pluginInfo={response.data}/>
                    </App>
                </Content>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
