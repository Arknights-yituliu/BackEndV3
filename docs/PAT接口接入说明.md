# PAT 接入说明

依据用户中心 `docs/pat/PAT整体架构.md` 与 `docs/pat/PAT接口文档.md`，前后端同步更新。

## 本站签发

`POST /auth/pat`，沿用本站登录会话认证。请求为 JSON：

```json
{
  "name": "干员数据只读 PAT",
  "expires_in": 315360000,
  "scope": "ak-data.read",
  "payload": {"game_uid": "12345678", "game_name": "明日方舟", "source": "web"}
}
```

本站要求选择游戏账号；前端只列出森空岛来源，账号绑定关系最终由 TCL 消费端校验。有效期为 1～318038400 秒，name 必填且 trim 后不超过 50 字符，scope 不超过 256 字符，payload 序列化后不超过 4096 UTF-8 字节。每个用户每 60 秒最多签发 3 次。

后端从当前用户的缓存读取 refresh_token，调用 TCL `POST /oauth2/pat`，将其放入 JSON 请求体。浏览器不传入、不接收 refresh_token；上游请求不携带 client_id 或 client_secret。该操作不消费、不轮转 refresh_token。

响应为本站统一的 `{code,msg,data}`，data 是 PAT 签发结果，含 `id/token/tokenPrefix/name/clientId/scope/expiresIn/expireTime/payload/createdAt`。响应禁止缓存，不再返回 OAuth 令牌对。

## 列表与撤销

前端继续用 OAuth access_token，通过 TCL 客户端直接调用：

| 用途 | 接口 | 请求 |
| --- | --- | --- |
| 列表 | GET `/oauth2/pat/list` | 无参数 |
| 撤销 | POST `/oauth2/pat/revoke` | JSON `{"token":"PAT明文"}` |

应用侧按 uid + clientId 限定归属，撤销按 token 明文定位，不能套用用户侧的 id 撤销协议。历史记录和过期记录仍可查看、撤销。前端仅生成绑定账号的只读 PAT，不再提供只写令牌入口。

## 第三方读取

本站继续提供 `GET /open-api/operator/info` 并保留原干员数据返回格式，后端只调用一次 TCL `GET /pat/ak-accounts/operators`，不再用长期令牌调用 OAuth 账号列表、干员接口。详情见 [第三方 API 文档](第三方API-获取干员练度数据.md)。

正常登录、access_token 刷新和站内游戏数据的 OAuth 调用继续使用原认证链路。PAT 不能放入 OAuth 令牌缓存，也不执行 OAuth 刷新。

## 替换与发布

| 原调用 | 新调用 |
| --- | --- |
| 本站 POST `/auth/uc-token/custom` | 本站 POST `/auth/pat`（JSON） |
| TCL POST `/oauth2/token/from-refresh` | TCL POST `/oauth2/pat`（JSON） |
| TCL GET `/oauth2/custom-tokens` | TCL GET `/oauth2/pat/list` |
| TCL POST `/oauth2/custom-tokens/revoke` | TCL POST `/oauth2/pat/revoke` |
| 第三方令牌调用 TCL OAuth 账号/干员读取 | TCL GET `/pat/ak-accounts/operators` |

前后端应配套发布；旧签发地址已移除。本仓库不修改用户中心表结构，也不自动调用 PAT 数据迁移端点。旧令牌能否继续使用取决于用户中心是否已完成迁移，以及原权限是否包含读取权限。
