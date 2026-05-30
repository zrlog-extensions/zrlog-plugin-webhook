import {FunctionComponent} from "react";
import WebhookIndex from "./components/WebhookIndex";
import {WebhookInfoResponse} from "./index";

export type AppBaseProps = {
    pluginInfo: WebhookInfoResponse;
}

const AppBase: FunctionComponent<AppBaseProps> = ({pluginInfo}) => {
    return <WebhookIndex data={pluginInfo}/>;
};

export default AppBase;
