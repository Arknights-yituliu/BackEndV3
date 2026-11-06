# 材料价值与关卡推荐 API（无参数）

不传任何定价参数、全部使用服务端默认配置时的接口说明。需要自定义参数时请见 [材料价值与关卡推荐API-全参数.md](./材料价值与关卡推荐API-全参数.md)。

## 通用说明

- 请求方式：`POST`
- 请求类型：`Content-Type: application/json`
- 请求体传空 JSON 对象 `{}`（不能完全不传请求体）
- 无需登录认证
- 统一返回结构：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| code | Integer | 业务状态码，成功为 `200` |
| msg | String | 业务提示信息，成功为 `操作成功` |
| data | Object | 业务数据 |

两个接口共用默认配置，主要默认行为如下：

| 配置项 | 默认值 | 说明 |
| :-- | :-- | :-- |
| 数据来源 | 企鹅物流 | 使用企鹅物流掉落矩阵 |
| sampleSize | `300` | 掉落样本量 |
| useActivityAverageStage | `false` | 不使用活动平均掉率虚拟关卡 |
| stageBlacklist / stageWhitelist | 空 | 不限制关卡 |
| 合成玉定价 | `ORUNDUM_PRICING_ORIGINITE_PRIME` | 按源石定价 |
| 龙门币定价 | `LMD_PRICING_CE-6` | 按 CE-6 定价 |
| 经验定价 | `EXP_PRICING_BASE_LVL_3_TRADING_POST` | 按 3 级贸易站定价 |
| 加工站策略 | `WORKSHOP_STRATEGY_COMMON` | 精英材料副产物 1.0，技巧概要/芯片 0.8 |
| 芯片偏好 | 全部 `BALANCED` | 职业组芯片均衡定价 |

---

## 一、材料价值 API

### 路径

```
POST /item/v7/value
```

### 请求示例

```json
{}
```

### 返回示例

数值随掉率数据变化，以下仅为结构示意：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "itemId": "30033",
      "itemName": "聚酸酯组",
      "itemValue": 22.28875697265037,
      "itemValueAp": 22.28875697265037,
      "rarity": 3,
      "weight": 0.05991440798858773,
      "cardNum": 3,
      "groupId": 3,
      "type": "精英材料"
    }
  ]
}
```

### 返回字段说明

`data` 为 `List<ItemInfoDTO>`，每个元素对应一种物品：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| itemId | String | 物品 ID |
| itemName | String | 物品名称 |
| itemValue | Double | 本次计算出的物品价值，单位为理智 |
| itemValueAp | Double | `item_info.json` 中的参考价值，不随本次计算重算 |
| rarity | Integer | 稀有度 |
| weight | Double | 加工站副产物权重 |
| cardNum | Integer | 卡片展示用数值 |
| groupId | Integer | 物品分组 ID |
| type | String | 物品类型，如 `精英材料` |

---

## 二、关卡推荐 API

### 路径

```
POST /stage/v7/recommended
```

### 请求示例

```json
{}
```

接口内部会先按默认配置完成材料价值计算，再返回推荐关卡与搓玉推荐。

### 返回示例

数值随掉率数据变化，以下仅为结构示意：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "recommendedStage": [
      {
        "itemSeriesId": "30013",
        "stageResultList": [
          {
            "stageCode": "1-7",
            "stageId": "main_01-07",
            "stageType": "MAIN",
            "zoneName": "第一章",
            "itemName": "固源岩",
            "itemId": "30012",
            "itemSeriesId": "30013",
            "itemRarity": 2,
            "secondaryItemId": "",
            "apExpect": 6.0,
            "knockRating": 1.0,
            "sampleSize": 1200,
            "stageEfficiency": 1.0,
            "leT2Efficiency": 1.0,
            "leT3Efficiency": 0.0,
            "leT4Efficiency": 0.0,
            "orundumPerAp": 1.0898,
            "orundumPerApEfficiency": 1.0,
            "lmdCost": 0.0,
            "end": 4073691312000,
            "spm": 0.0,
            "apCost": 6,
            "stageExpectedOutput": 6.0
          }
        ]
      }
    ],
    "orundumRecommendedStageVO": [
      {
        "stageCode": "1-7",
        "stageType": "MAIN",
        "zoneName": "第一章",
        "itemName": "固源岩",
        "orundumPerAp": 1.0898,
        "orundumPerApEfficiency": 1.0,
        "apCost": 6,
        "end": 4073691312000
      }
    ],
    "historyActStage": [
      {
        "zoneName": "活动名称",
        "stageType": "ACT",
        "endTime": 1760000000000,
        "actStageList": []
      }
    ],
    "historyActOrundumStage": [
      {
        "zoneName": "活动名称",
        "stageType": "ACT",
        "endTime": 1760000000000,
        "actStageList": []
      }
    ]
  }
}
```

### 返回字段说明

`data`（`StageEfficiencyResultVO`）：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| recommendedStage | List\<RecommendedStageVO\> | 按材料系列聚合的开放关卡推荐，每个系列内按 `stageEfficiency` 降序，关卡不含 `dropDetail` |
| orundumRecommendedStageVO | List\<StageEfficiencyVO\> | 搓玉推荐关卡，按 `orundumPerAp` 降序 |
| historyActStage | List\<HistoryActStageVO\> | 历史活动关卡（仅 T3 主产物），按活动结束时间降序 |
| historyActOrundumStage | List\<HistoryActStageVO\> | 历史活动搓玉关卡，按活动结束时间降序 |

`RecommendedStageVO`：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| itemSeriesId | String | 材料系列 ID |
| stageResultList | List\<StageEfficiencyVO\> | 该系列的推荐关卡 |

`HistoryActStageVO`：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| zoneName | String | 活动名称 |
| stageType | String | 关卡类型 |
| endTime | Long | 活动结束时间（毫秒时间戳） |
| actStageList | List\<StageEfficiencyVO\> | 该活动下的关卡 |

`StageEfficiencyVO` 主要字段：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| stageCode | String | 关卡代号，如 `1-7` |
| stageId | String | 关卡 ID，如 `main_01-07` |
| stageType | String | `MAIN` / `ACT` / `ACT_REP` / `ACT_MINI` / `ACT_PERM` / `YTL_VIRTUAL` |
| zoneName | String | 所属章节或活动名称 |
| itemName | String | 主产物名称 |
| itemId | String | 主产物 ID |
| itemSeriesId | String | 主产物所属精英材料系列 ID |
| itemRarity | Integer | 主产物稀有度 |
| apExpect | Double | 主产物期望理智 |
| knockRating | Double | 主产物掉率 |
| stageEfficiency | Double | 关卡理智转化率 |
| orundumPerAp | Double | 每理智可转化合成玉数量 |
| orundumPerApEfficiency | Double | 相对搓玉效率，榜首为 `1` |
| lmdCost | Double | 每搓一抽所需龙门币，单位万；无搓玉产出时为 `null` |
| apCost | Integer | 关卡理智消耗 |
| end | Long | 关卡结束时间（毫秒时间戳） |
| dropDetail | List\<StageDropDetailVO\> | 掉落明细；仅搓玉推荐与历史活动关卡返回，材料系列推荐关卡不返回 |

完整字段说明（含 `leT2Efficiency`、`stageExpectedOutput`、`StageDropDetailVO` 等）见 [材料价值与关卡推荐API-全参数.md](./材料价值与关卡推荐API-全参数.md)。

### 推荐口径摘要

1. 开放推荐关卡只保留 `stageEfficiency > 0.5` 且尚未结束的关卡，按材料系列聚合；该列表不返回 `dropDetail`。
2. 搓玉推荐只保留 `orundumPerAp > 0.2` 的关卡，按每理智合成玉降序。
3. 历史活动关卡只聚合 `ACT` / `ACT_REP` 且主产物稀有度为 3 的关卡。

---

## 三、调用示例

```powershell
# 材料价值
Invoke-RestMethod -Method Post -Uri 'http://localhost:10012/item/v7/value' `
  -ContentType 'application/json; charset=utf-8' -Body '{}'

# 关卡推荐
Invoke-RestMethod -Method Post -Uri 'http://localhost:10012/stage/v7/recommended' `
  -ContentType 'application/json; charset=utf-8' -Body '{}'
```

```bash
# 材料价值
curl -X POST 'http://localhost:10012/item/v7/value' \
  -H 'Content-Type: application/json' \
  -d '{}'

# 关卡推荐
curl -X POST 'http://localhost:10012/stage/v7/recommended' \
  -H 'Content-Type: application/json' \
  -d '{}'
```

> 端口与服务前缀以实际部署为准。
