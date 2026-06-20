# third-party-service API 文档

**Base URL (dev):** `http://localhost:7100`

**Base URL (prod):** `https://api.flyingjack.top/third` （Istio 路由匹配 `/third/` 前缀并裁切后转发至本服务）

所有响应均为统一格式 `ApiRes<T>`，见[响应格式](#响应格式)。

---

## 响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... },
  "timestamp": 1714000000000
}
```

### 错误响应

```json
{
  "code": 429,
  "message": "Attempt to many",
  "path": "/captcha/generate/sms",
  "timestamp": 1714000000000
}
```

| HTTP 状态码 | 含义 |
|-------------|------|
| 200 | 成功 |
| 400 | 参数错误 / 缺少必填字段 |
| 429 | 请求过于频繁 |
| 500 | 服务内部错误 / 三方服务异常 |

---

## 验证码接口

### 1. 生成图片验证码

> **对外接口**，需要经过 Istio / Gateway 转发才能访问。

```
GET /captcha/generate/image
```

**请求参数：** 无

**响应 `data`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `uuid` | `string (UUID)` | 验证码唯一标识，后续 verify 时作为 `captchaId` 传入 |
| `base64Image` | `string` | Base64 编码的 PNG 图片（160×60 px） |

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "uuid": "a3f1c2d4-5678-4abc-9def-000000000001",
    "base64Image": "iVBORw0KGgoAAAANSUhEUgAA..."
  },
  "timestamp": 1714000000000
}
```

**说明：**
- 验证码为 6 位大小写字母+数字混合（排除 `0`/`O` 等易混字符）
- 有效期 **5 分钟**，过期后需重新获取
- 严格模式（默认开启）下，verify 时会校验请求 IP 必须与生成时一致

---

### 2. 发送短信验证码

> **会产生阿里云短信费用，禁止随意调用。**
> 同一 IP **60 秒内只能发送一次**，超出返回 429。

```
GET /captcha/generate/sms?phone={phone}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `phone` | `string` | 是 | 接收验证码的手机号（需为有效手机号格式） |

**响应 `data`：** `true`（发送成功）

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": true,
  "timestamp": 1714000000000
}
```

**错误情况：**

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 手机号格式无效 | 400 | `IllegalArgumentException: Not a valid phone` |
| 同 IP 60 秒内重复请求 | 429 | `TO_MANY_LOGIN_ATTEMPT` |
| 阿里云短信服务异常 | 500 | `ThirdPartySystemException` |

**说明：**
- 验证码为 **6 位纯数字**，有效期 **5 分钟**
- 后续 verify 时 `captchaId` 填手机号本身
- 严格模式下 verify 会校验 IP 一致性

---

### 3. 发送邮件验证码

> 同一 IP **30 秒内只能发送一次**，超出返回 429。

```
GET /captcha/generate/mail?email={email}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `email` | `string` | 是 | 收件人邮箱地址 |

**请求头（可选）：**

| Header | 说明 |
|--------|------|
| `Accept-Language` | 控制邮件语言，支持 `zh-CN`、`en-US`，默认跟随服务器 Locale |

**响应 `data`：** `true`（发送成功）

**响应示例：**

```json
{
  "code": 200,
  "message": "Success",
  "data": true,
  "timestamp": 1714000000000
}
```

**错误情况：**

| 场景 | HTTP 状态 | 说明 |
|------|-----------|------|
| 邮箱格式无效 | 400 | `IllegalArgumentException: Not a valid email` |
| 同 IP 30 秒内重复请求 | 429 | `TO_MANY_LOGIN_ATTEMPT` |
| 邮件服务异常（网易 SMTP）| 500 | `ThirdPartySystemException` |

**说明：**
- 验证码为 **6 位大小写字母+数字**，通过 Thymeleaf HTML 模板 `email_code.html` 渲染发送
- 后续 verify 时 `captchaId` 填邮箱地址本身
- 严格模式下 verify 会校验 IP 一致性

---

### 4. 验证验证码

> **内部服务调用接口**，不对外暴露，由其他微服务（如 auth-service）通过 OpenFeign 调用。

```
POST /captcha/verify
Content-Type: application/json
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `captchaId` | `string` | 是 | 图片验证码填 UUID；短信/邮件验证码填手机号/邮箱 |
| `captcha` | `string` | 是 | 用户输入的验证码 |

**请求示例：**

```json
{
  "captchaId": "a3f1c2d4-5678-4abc-9def-000000000001",
  "captcha": "aB3dE9"
}
```

**响应：** 直接返回 `boolean`（非包装格式）

| 值 | 含义 |
|----|------|
| `true` | 验证通过，缓存已自动清除（一次性验证） |
| `false` | 验证失败（验证码错误、已过期、IP 不一致） |

**说明：**
- 严格模式（`captcha.verify.strict=true`，默认）：请求 IP 必须与生成时 IP 一致，否则直接返回 `false`
- 验证通过后缓存立即删除，验证码不可复用

---

## 频率限制汇总

| 接口 | 限制维度 | 冷却时间 |
|------|----------|----------|
| `GET /captcha/generate/sms` | 客户端 IP | 60 秒 |
| `GET /captcha/generate/mail` | 客户端 IP | 30 秒 |
| `GET /captcha/generate/image` | 无限制 | — |
| `POST /captcha/verify` | 无限制 | — |

> 生产环境额外的限流由 **Istio** 层统一管控，不在此服务内实现。

---

## 验证码有效期汇总

| 类型 | 有效期 | `captchaId` 取值 |
|------|--------|-----------------|
| 图片验证码 | 5 分钟 | 生成返回的 UUID |
| 短信验证码 | 5 分钟 | 手机号 |
| 邮件验证码 | 5 分钟 | 邮箱地址 |

---

## 典型调用流程

### 图片验证码登录流

```
1. 客户端 GET /captcha/generate/image
   ← { uuid, base64Image }

2. 用户填写验证码后，客户端携带 uuid + 输入值
   发起登录请求至 auth-service

3. auth-service 内部调用 POST /captcha/verify
   { captchaId: uuid, captcha: "用户输入" }
   ← true / false
```

### 手机号/邮箱验证码流

```
1. 客户端 GET /captcha/generate/sms?phone=xxx
              或 GET /captcha/generate/mail?email=xxx

2. 用户收到验证码，填写后发起请求至对应业务服务

3. 业务服务内部调用 POST /captcha/verify
   { captchaId: "手机号或邮箱", captcha: "用户输入" }
   ← true / false
```
