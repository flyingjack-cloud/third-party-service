# third-party-service 部署操作手册

## 概述

本服务通过 ArgoCD GitOps 部署，k8s 配置由 `k8s-gitops` 仓库管理。**Secrets 不进 GitOps 仓库**，需在目标集群手动提前创建；ArgoCD 只管理 Deployment / ConfigMap / Service 等无密态资源。

所有敏感配置通过 `envFrom: secretRef` 注入为 OS 环境变量，服务启动时由 Spring Boot 读取，不依赖 Spring Cloud Kubernetes API。

| 环境 | 访问方式 | TLS |
|---|---|---|
| beta | 公网，Istio Gateway，HTTPS，`api.flyingjack.top/third` | cert-manager 自动签发/续签 |
| prod | 公网，Istio Gateway，HTTPS，`api.flyingjack.top/third` | cert-manager 自动签发/续签 |

---

## 手动创建 Secrets（首次部署或凭据轮换时执行）

本服务需三组 Secret，通过 `envFrom: secretRef` 直接注入为 OS 环境变量，无需特殊标签。

### Beta 环境

```bash
# 切换到目标命名空间（如不存在先创建）
kubectl create namespace flyingjack-beta --dry-run=client -o yaml | kubectl apply -f -

# 阿里云短信凭据
kubectl create secret generic sms-access-secret \
  --from-literal=SMS_ACCESS_ID=<阿里云 Access Key ID> \
  --from-literal=SMS_ACCESS_SECRET=<阿里云 Access Key Secret> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -

# 网易邮件凭据
kubectl create secret generic email-access-secret \
  --from-literal=EMAIL_USERNAME=<163邮箱地址> \
  --from-literal=EMAIL_PASSWORD=<163 SMTP 授权码，非登录密码> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -

# Redis 凭据
kubectl create secret generic redis-access-secret \
  --from-literal=REDIS_HOST=<Redis 地址> \
  --from-literal=REDIS_PASSWORD=<Redis 密码，无密码则留空字符串> \
  -n flyingjack-beta \
  --dry-run=client -o yaml | kubectl apply -f -
```

### Prod 环境

```bash
kubectl create namespace flyingjack-prod --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic sms-access-secret \
  --from-literal=SMS_ACCESS_ID=<阿里云 Access Key ID> \
  --from-literal=SMS_ACCESS_SECRET=<阿里云 Access Key Secret> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic email-access-secret \
  --from-literal=EMAIL_USERNAME=<163邮箱地址> \
  --from-literal=EMAIL_PASSWORD=<163 SMTP 授权码> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic redis-access-secret \
  --from-literal=REDIS_HOST=<Redis 地址> \
  --from-literal=REDIS_PASSWORD=<Redis 密码> \
  -n flyingjack-prod \
  --dry-run=client -o yaml | kubectl apply -f -
```

### 镜像仓库拉取凭据（仅认证 registry 需要）

使用无认证的 registry:2 时跳过此步骤。如果 registry 需要认证（如 Harbor），需在每个命名空间创建拉取凭据，并在 `deployment-patch.yaml` 中补充 `imagePullSecrets`：

```bash
kubectl create secret docker-registry harbor-pull-secret \
  --docker-server=<registry地址> \
  --docker-username=<用户名> \
  --docker-password=<密码> \
  -n flyingjack-beta   # prod 环境替换命名空间重复执行
```

---

## 验证 Secrets 是否正确

### Beta 环境

```bash
# 检查 secret 存在
kubectl get secret sms-access-secret email-access-secret redis-access-secret -n flyingjack-beta

# 查看 secret 的 key（不显示值）
kubectl get secret sms-access-secret -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret email-access-secret -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret redis-access-secret -n flyingjack-beta -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
```

### Prod 环境

```bash
kubectl get secret sms-access-secret email-access-secret redis-access-secret -n flyingjack-prod

kubectl get secret sms-access-secret -n flyingjack-prod -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret email-access-secret -n flyingjack-prod -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
kubectl get secret redis-access-secret -n flyingjack-prod -o jsonpath='{.data}' | python3 -c "import sys,json; [print(k) for k in json.load(sys.stdin)]"
```

---

## 前置：确认 api.flyingjack.top HTTPS 已就绪

本服务通过共享 Gateway 暴露在 `https://api.flyingjack.top`，TLS 证书由 `shared-networking` 统一管理。**首次在新集群部署前，需确认 `shared-networking` 已部署且证书已签发**，步骤见 `k8s-gitops/shared-networking/DEPLOY.md`。

---

## ArgoCD Application 创建

> **前置条件**：Namespace 须提前手动创建，见 `k8s-gitops/shared/DEPLOY.md`。ArgoCD Application 不负责创建 Namespace，防止 auto-prune 误删命名空间。

在 ArgoCD 所在集群执行（或通过 ArgoCD UI 导入）：

```bash
kubectl apply -f - <<EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: third-party-service-beta
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/flyingjack-cloud/k8s-gitops
    targetRevision: main
    path: third-party-service/overlays/beta
  destination:
    server: https://kubernetes.default.svc
    namespace: flyingjack-beta
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
EOF
```

prod 环境将 `beta` 替换为 `prod` 重复执行。

---

## 部署验证（Smoke Test）

以下接口可用于验证整条链路是否正常。

### 图片验证码接口

```
GET https://api.flyingjack.top/third/captcha/generate/image
```

预期返回：
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "image": "data:image/png;base64,..."
  }
}
```

验证点：
- 能返回上述 JSON 说明 **Istio `/third/` 前缀路由**正常（前缀已被剥除，请求正确到达本服务）
- 若返回 502/503，检查 Pod 是否正常运行：`kubectl get pods -n flyingjack-beta`
- 若返回 404，检查 VirtualService 路由规则是否已被 ArgoCD 同步

---

## 注意事项

- Secret 变更后需手动 `kubectl rollout restart deployment/third-party-service-v1 -n <namespace>` 触发 Pod 重启以读取新值。
- prod 环境禁止直接 `kubectl apply`，所有变更须通过 ArgoCD 同步。
- `/captcha/verify` 为内部接口，仅供其他微服务通过 Feign 调用；VirtualService 只路由 `/third/` 前缀，外部无法直接访问该路径。
