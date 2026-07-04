const http = require("http");
const https = require("https");

const port = process.env.PORT || 9095;

async function sendToDingTalk(webhook, secret, alert) {
  if (!webhook) return;
  
  const timestamp = Date.now();
  const sign = secret ? generateSign(timestamp, secret) : null;
  
  const body = {
    msgtype: "markdown",
    markdown: {
      title: `${alert.status.toUpperCase()}: ${alert.labels.alertname}`,
      text: `**告警名称**: ${alert.labels.alertname}\n` +
            `**级别**: ${alert.labels.severity || "unknown"}\n` +
            `**状态**: ${alert.status}\n` +
            `**开始时间**: ${new Date(alert.startsAt).toLocaleString("zh-CN")}\n` +
            `**描述**: ${alert.annotations.description || "无"}\n` +
            `**详情**: ${alert.annotations.summary || "无"}`
    }
  };
  
  return new Promise((resolve) => {
    const url = new URL(webhook);
    if (sign) {
      url.searchParams.set("timestamp", timestamp);
      url.searchParams.set("sign", sign);
    }
    
    const req = https.request({
      hostname: url.hostname,
      path: url.pathname + url.search,
      method: "POST",
      headers: { "Content-Type": "application/json" }
    }, (res) => {
      let data = "";
      res.on("data", d => data += d);
      res.on("end", () => resolve({ status: res.statusCode, body: data }));
    });
    
    req.on("error", () => resolve({ status: 500, error: "DingTalk send failed" }));
    req.write(JSON.stringify(body));
    req.end();
  });
}

async function sendToFeishu(webhook, alert) {
  if (!webhook) return;
  
  const body = {
    msg_type: "text",
    content: {
      text: `[${alert.status.toUpperCase()}] ${alert.labels.alertname}\n` +
            `级别: ${alert.labels.severity || "unknown"}\n` +
            `描述: ${alert.annotations.description || "无"}`
    }
  };
  
  return new Promise((resolve) => {
    const url = new URL(webhook);
    const req = https.request({
      hostname: url.hostname,
      path: url.pathname + url.search,
      method: "POST",
      headers: { "Content-Type": "application/json" }
    }, (res) => {
      let data = "";
      res.on("data", d => data += d);
      res.on("end", () => resolve({ status: res.statusCode, body: data }));
    });
    
    req.on("error", () => resolve({ status: 500, error: "Feishu send failed" }));
    req.write(JSON.stringify(body));
    req.end();
  });
}

async function sendToWeCom(webhook, alert) {
  if (!webhook) return;
  
  const body = {
    msgtype: "text",
    text: {
      content: `【${alert.status.toUpperCase()}】${alert.labels.alertname}\n` +
               `级别: ${alert.labels.severity || "unknown"}\n` +
               `描述: ${alert.annotations.description || "无"}`
    }
  };
  
  return new Promise((resolve) => {
    const url = new URL(webhook);
    const req = https.request({
      hostname: url.hostname,
      path: url.pathname + url.search,
      method: "POST",
      headers: { "Content-Type": "application/json" }
    }, (res) => {
      let data = "";
      res.on("data", d => data += d);
      res.on("end", () => resolve({ status: res.statusCode, body: data }));
    });
    
    req.on("error", () => resolve({ status: 500, error: "WeCom send failed" }));
    req.write(JSON.stringify(body));
    req.end();
  });
}

function generateSign(timestamp, secret) {
  const crypto = require("crypto");
  const stringToSign = `${timestamp}\n${secret}`;
  return crypto.createHmac("sha256", secret).update(stringToSign).digest("base64");
}

const server = http.createServer(async (req, res) => {
  if (req.method === "POST") {
    let body = "";
    req.on("data", d => body += d);
    req.on("end", async () => {
      try {
        const alerts = JSON.parse(body);
        console.log(`Received ${alerts.length} alerts`);
        
        const results = [];
        for (const alert of alerts) {
          console.log(`Processing alert: ${alert.labels.alertname} (${alert.status})`);
          
          const dingTalkResult = await sendToDingTalk(
            process.env.DINGTALK_WEBHOOK, 
            process.env.DINGTALK_SECRET, 
            alert
          );
          if (dingTalkResult) results.push({ type: "dingtalk", ...dingTalkResult });
          
          const feishuResult = await sendToFeishu(process.env.FEISHU_WEBHOOK, alert);
          if (feishuResult) results.push({ type: "feishu", ...feishuResult });
          
          const weComResult = await sendToWeCom(process.env.WECOM_WEBHOOK, alert);
          if (weComResult) results.push({ type: "wecom", ...weComResult });
        }
        
        console.log("Alert forwarding completed:", results);
        res.writeHead(200, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ success: true, results }));
      } catch (error) {
        console.error("Error processing alert:", error.message);
        res.writeHead(500, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ success: false, error: error.message }));
      }
    });
  } else {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ 
      status: "ready", 
      services: {
        dingtalk: !!process.env.DINGTALK_WEBHOOK,
        feishu: !!process.env.FEISHU_WEBHOOK,
        wecom: !!process.env.WECOM_WEBHOOK
      }
    }));
  }
});

server.listen(port, () => {
  console.log(`Alert webhook listening on :${port}`);
  console.log("Enabled services:", {
    dingtalk: !!process.env.DINGTALK_WEBHOOK,
    feishu: !!process.env.FEISHU_WEBHOOK,
    wecom: !!process.env.WECOM_WEBHOOK
  });
});
