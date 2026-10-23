# 上报接口

## 生成随机 uid

```
GET /report/uid/generate
```

生成一个 uid 并直接返回，不落库，无需任何参数。

### 返回格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": 742193874020352100
}
```

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| code | Integer | 200 表示成功 |
| msg | String | 结果描述 |
| data | Long | 生成的 uid |

### 调用示例

```bash
curl 'https://backend.yituliu.cn/report/uid/generate'
```

### 说明

- 每次调用返回一个新值
- 接口无鉴权，除生成 uid 外不产生任何影响

## 回收站产出上报

```
POST /report/recycleDrop
```

产出数据上报接口，面向所有数据来源。

### 认证

接口无鉴权。请求头 `authorization` 仅用于携带玩家 uid，格式如下：

```
authorization: Bearer {uid}
```

服务端按空白字符切分该请求头，取第二个片段作为 uid（最长 50 字符）。

### 请求参数

#### Header

| 字段 | 是否必填 | 说明 |
| :-- | :-- | :-- |
| authorization | 是 | 格式 `Bearer {uid}`，取第二个空白分隔片段作为玩家 uid，不超过 50 字符 |

#### Body（JSON）

```json
{
  "drops": [
    {
      "itemId": "30012",
      "dropTime": 1759400000000
    },
    {
      "itemId": "30063",
      "dropTime": 1759400001234
    }
  ],
  "server": "CN",
  "source": "MAA",
  "version": "v5.8.0"
}
```

| 字段 | 类型 | 必填 | 说明 |
| :-- | :-- | :-- | :-- |
| drops | List\<Object\> | 是 | 产出明细列表，1–100 条 |
| drops[].itemId | String | 是 | 物品 id，非空白，不超过 100 字符 |
| drops[].dropTime | Long | 是 | 物品产出时间，毫秒级时间戳，必须为正数 |
| server | String | 是 | 服务器地区，非空白，不超过 20 字符 |
| source | String | 是 | 数据来源，非空白，不超过 100 字符 |
| version | String | 是 | 客户端版本号，非空白，不超过 50 字符 |

### 返回格式

成功时 HTTP 200，**无响应体**。

如果服务端处理异常，返回：

```json
{
  "code": 500,
  "msg": "错误信息",
  "data": null
}
```

### 校验规则

以下任一条件不满足时，本次上报会被**静默丢弃**：不返回错误，响应仍是 HTTP 200 空响应。

| # | 规则 |
| :-- | :-- |
| 1 | `drops`、`server`、`source`、`version` 均不能为空 |
| 2 | `server`、`source`、`version` 不能为空白字符串 |
| 3 | `drops` 不能为 null 或空列表，条数不超过 **100** |
| 4 | `drops` 中每一项不能为 null，`itemId` 非空白且不超过 **100** 字符，`dropTime` 不能为空且大于 0 |
| 5 | 请求头 `authorization` 存在，切分后至少两段，第二段非空白且不超过 **50** 字符 |

`itemId` 的具体内容格式不做校验。

### 防重复提交

- 同一 uid 在 **5 秒** 内多次上报，只有第一次会被接收。
- 同一条明细（`uid` + `itemId` + `dropTime` 完全相同）在 **24 小时** 内只入库一次。批次中重复的明细会被跳过，其余明细正常写入。

### 调用示例

```bash
curl -X POST 'https://backend.yituliu.cn/report/recycleDrop' \
  -H 'Content-Type: application/json' \
  -H 'authorization: Bearer 123456789' \
  -d '{
    "drops": [
      {
        "itemId": "30012",
        "dropTime": 1759400000000
      },
      {
        "itemId": "30063",
        "dropTime": 1759400001234
      }
    ],
    "server": "CN",
    "source": "MAA",
    "version": "v5.8.0"
  }'
```

预期响应：HTTP 200，无响应体。

### 注意事项

1. **无法从响应判断数据是否被接收**。校验失败与成功都返回 200 空响应。
2. **字段超长会导致上报失败**。`server` / `source` / `version` 超过上表长度时会返回 `code: 500`，请自行控制长度。
3. **uid 未做真实性校验**，请求方可以声称任意 uid，请勿用于不可信场景。
