# 生成随机 uid

## 接口

```
GET /report/uid/generate
```

生成一个 uid 并直接返回，不落库，无需任何参数。

## 返回格式

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

## 调用示例

```bash
curl 'https://backend.yituliu.cn/report/uid/generate'
```

## 说明

- 每次调用返回一个新值
- 接口无鉴权，除生成 uid 外不产生任何影响
