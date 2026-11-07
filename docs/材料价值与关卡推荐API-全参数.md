# 材料价值与关卡推荐 API（全参数）

传入完整定价配置时的接口说明。材料价值接口与关卡推荐接口共用同一套定价参数 `ItemValueConfigDTO`。仅需默认行为、不传任何参数时请见 [材料价值与关卡推荐API-无参数.md](./材料价值与关卡推荐API-无参数.md)。

## 通用说明

- 请求方式：`POST`
- 请求类型：`Content-Type: application/json`
- 请求体中的字段均可省略，服务端会为未填写的字段填充默认值
- 无需登录认证
- 统一返回结构：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| code | Integer | 业务状态码，成功为 `200` |
| msg | String | 业务提示信息，成功为 `操作成功` |
| data | Object | 业务数据 |

---

## 一、材料价值 API

### 路径

```
POST /item/v7/value
```

### 请求参数

请求体为 `ItemValueConfigDTO`。

| 字段 | 类型 | 默认值 | 说明 |
| :-- | :-- | :-- | :-- |
| source | String | `penguin` | 数据来源标识，当前不参与计算 |
| version | String | `v1.0` | 配置版本标识，当前不参与计算 |
| useActivityAverageStage | Boolean | `false` | 是否使用活动平均掉率虚拟关卡（`YTL_VIRTUAL`）参与定价与推荐 |
| useActivityAverageStageAndUnlimitedItem | Boolean | `false` | 活动平均掉率虚拟关卡是否追加无限池龙门币掉落；仅在 `useActivityAverageStage=true` 时生效 |
| sampleSize | Integer | `300` | 掉落样本量，必须大于 `50` |
| stageBlacklist | Set\<String\> | `[]` | 关卡黑名单，命中的 `stageId` 不参与计算 |
| stageWhitelist | Set\<String\> | `[]` | 关卡白名单，当前版本未生效 |
| orundumPricingStrategy | String | `ORUNDUM_PRICING_ORIGINITE_PRIME` | 合成玉定价策略 |
| orundumValue | Double | `0.75` | 合成玉自定义价值（理智），当 `orundumPricingStrategy` 不在已列出的策略中时使用 |
| originitePrimePricingStrategy | String | `ORIGINITE_PRIME_PRICING_ORUNDUM` | 源石定价策略 |
| originitePrimeCoefficient | Double | `180.0` | 源石定价系数 |
| kernelHeadhuntingPermitPricingStrategy | String | `KERNEL_HEADHUNTING_PERMIT_PRICING_DISTINCTION_CERTIFICATE` | 寻访凭证定价策略 |
| kernelHeadhuntingPermitCoefficient | Double | `216/258` | 寻访凭证定价系数 |
| lmdPricingStrategy | String | `LMD_PRICING_CE-6` | 龙门币定价策略 |
| lmdCoefficient | Double | `1.0` | 龙门币定价系数 |
| expPricingStrategy | String | `EXP_PRICING_BASE_LVL_3_TRADING_POST` | 经验定价策略 |
| expCoefficient | Double | `145/229` | 经验定价系数 |
| modUnlockTokenPricingStrategy | String | `MOD_UNLOCK_TOKEN_PRICING_PURCHASE_CERTIFICATE` | 模组解锁凭证定价策略 |
| modUnlockTokenValue | Double | `120*30/21` | 模组解锁凭证自定义价值，仅 `MOD_UNLOCK_TOKEN_PRICING_CUSTOM` 策略使用 |
| recruitmentPermitPricingStrategy | String | `RECRUITMENT_PERMIT_PRICING_3_4` | 招聘许可定价策略，纯占位，当前价值固定为 0 |
| recruitmentPermitValue | Double | 无 | 招聘许可自定义价值，当前未参与计算 |
| furniturePartPricingStrategy | String | `FURNITURE_PART_PRICING_ZERO` | 家具零件定价策略 |
| furniturePartValue | Double | `0.0` | 家具零件自定义价值 |
| customItem | List\<CustomItemDTO\> | 见下文 | 自定义材料价值 |
| workshopStrategy | WorkshopStrategyDTO | 见下文 | 加工站策略 |
| chipPreference | ChipPreferenceDTO | 全部 `BALANCED` | 芯片偏好 |

#### CustomItemDTO

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| itemId | String | 材料 ID |
| itemValue | Double | 材料价值（理智） |

默认值：

```json
[
  { "itemId": "30073", "itemValue": 1.8 },
  { "itemId": "30083", "itemValue": 2.16 },
  { "itemId": "30093", "itemValue": 2.52 },
  { "itemId": "30103", "itemValue": 2.88 }
]
```

#### WorkshopStrategyDTO

每个字段均为 `WorkshopItemDTO`：

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| strategy | String | 加工站策略，与 `byproductRateIncrease` 至少填写一个 |
| byproductRateIncrease | Double | 加工副产物提升比例，与 `strategy` 至少填写一个，且必须大于 `0` |

`WorkshopStrategyDTO` 字段：

| 字段 | 对应加工项 | 默认值 |
| :-- | :-- | :-- |
| eliteMaterialT1toT2 | 精英材料 T1 → T2 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":1.0}` |
| eliteMaterialT2toT3 | 精英材料 T2 → T3 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":1.0}` |
| eliteMaterialT3toT4 | 精英材料 T3 → T4 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":1.0}` |
| eliteMaterialT4toT5 | 精英材料 T4 → T5 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":1.0}` |
| skillSummary1to2 | 技巧概要 1 → 2 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":0.8}` |
| skillSummary2to3 | 技巧概要 2 → 3 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":0.8}` |
| baseMaterial | 基建材料 | `{"strategy":"WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN"}` |
| chip | 芯片 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":0.8}` |
| chipPack | 芯片组 | `{"strategy":"WORKSHOP_STRATEGY_COMMON","byproductRateIncrease":0.8}` |

#### ChipPreferenceDTO

按职业组填写芯片偏好，每组可选 `BALANCED` 或对应的两个职业：

| 字段 | 可选值 |
| :-- | :-- |
| TANK_MEDIC | `TANK` / `MEDIC` / `BALANCED` |
| SNIPER_CASTER | `SNIPER` / `CASTER` / `BALANCED` |
| PIONEER_SUPPORT | `PIONEER` / `SUPPORT` / `BALANCED` |
| WARRIOR_SPECIAL | `WARRIOR` / `SPECIAL` / `BALANCED` |

### 策略枚举值

| 参数 | 可选策略 |
| :-- | :-- |
| orundumPricingStrategy | `ORUNDUM_PRICING_ORIGINITE_PRIME`（按源石定价）<br>`ORUNDUM_PRICING_ORININUM_FARMING_ORIROCK_CUBE`（按固源岩搓玉定价）<br>`ORUNDUM_PRICING_ORININUM_FARMING_DEVICE`（按装置搓玉定价） |
| originitePrimePricingStrategy | `ORIGINITE_PRIME_PRICING_ORUNDUM` |
| kernelHeadhuntingPermitPricingStrategy | `KERNEL_HEADHUNTING_PERMIT_PRICING_DISTINCTION_CERTIFICATE` |
| lmdPricingStrategy | `LMD_PRICING_CE-6` |
| expPricingStrategy | `EXP_PRICING_BASE_LVL_3_TRADING_POST` |
| modUnlockTokenPricingStrategy | `MOD_UNLOCK_TOKEN_PRICING_PURCHASE_CERTIFICATE`<br>`MOD_UNLOCK_TOKEN_PRICING_DISTINCTION_CERTIFICATE`<br>`MOD_UNLOCK_TOKEN_PRICING_CUSTOM` |
| recruitmentPermitPricingStrategy | `RECRUITMENT_PERMIT_PRICING_3_4` |
| furniturePartPricingStrategy | `FURNITURE_PART_PRICING_ZERO` |

### 请求示例

```json
{
  "sampleSize": 300,
  "useActivityAverageStage": false,
  "stageBlacklist": ["main_01-07"],
  "orundumPricingStrategy": "ORUNDUM_PRICING_ORIGINITE_PRIME",
  "orundumValue": 0.75,
  "customItem": [
    { "itemId": "30073", "itemValue": 1.8 },
    { "itemId": "30083", "itemValue": 2.16 }
  ],
  "workshopStrategy": {
    "eliteMaterialT1toT2": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 1.0 },
    "eliteMaterialT2toT3": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 1.0 },
    "eliteMaterialT3toT4": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 1.0 },
    "eliteMaterialT4toT5": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 1.0 },
    "skillSummary1to2": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 0.8 },
    "skillSummary2to3": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 0.8 },
    "baseMaterial": { "strategy": "WORKSHOP_STRATEGY_NINE_COLORED_DEER_OBTAIN" },
    "chip": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 0.8 },
    "chipPack": { "strategy": "WORKSHOP_STRATEGY_COMMON", "byproductRateIncrease": 0.8 }
  },
  "chipPreference": {
    "TANK_MEDIC": "BALANCED",
    "SNIPER_CASTER": "BALANCED",
    "PIONEER_SUPPORT": "BALANCED",
    "WARRIOR_SPECIAL": "BALANCED"
  }
}
```

### 返回示例

数值随掉率数据与定价配置变化，以下仅为结构示意：

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
| itemValue | Double | 本次配置计算出的物品价值，单位为理智 |
| itemValueAp | Double | `item_info.json` 中的参考价值，不随本次配置重算 |
| rarity | Integer | 稀有度 |
| weight | Double | 加工站副产物权重 |
| cardNum | Integer | 卡片展示用数值 |
| groupId | Integer | 物品分组 ID |
| type | String | 物品类型，如 `精英材料` |

### 错误码

| 错误码 | 说明 |
| :-- | :-- |
| 10002 | 参数为空（默认值填充后仍存在必填项缺失） |
| 30100 | 样本量小于 50 |
| 30102 | 价值或系数不能小于 0 |
| 30104 | 请至少填写一种策略或价值 |

---

## 二、关卡推荐 API

### 路径

```
POST /stage/v7/recommended
```

### 请求参数

与材料价值 API 完全相同，复用 `ItemValueConfigDTO`，字段说明与默认值见上文「材料价值 API - 请求参数」。

接口内部会先完成材料价值计算，再基于计算结果返回推荐关卡与搓玉推荐。

### 返回示例

数值随掉率数据与定价配置变化，以下仅为结构示意：

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
        "stageExpectedOutput": 6.0,
        "dropDetail": []
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
| recommendedStage | List\<RecommendedStageVO\> | 按材料系列聚合的开放关卡推荐 |
| orundumRecommendedStageVO | List\<StageEfficiencyVO\> | 搓玉推荐关卡，按每理智合成玉降序 |
| historyActStage | List\<HistoryActStageVO\> | 历史活动关卡（仅 T3 主产物），按活动聚合 |
| historyActOrundumStage | List\<HistoryActStageVO\> | 历史活动搓玉关卡，按活动聚合 |

#### RecommendedStageVO

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| itemSeriesId | String | 材料系列 ID |
| stageResultList | List\<StageEfficiencyVO\> | 该系列的推荐关卡，按 `stageEfficiency` 降序；不返回 `dropDetail` 字段 |

#### HistoryActStageVO

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| zoneName | String | 活动名称 |
| stageType | String | 关卡类型 |
| endTime | Long | 活动结束时间（毫秒时间戳） |
| actStageList | List\<StageEfficiencyVO\> | 该活动下的关卡，按对应口径降序 |

#### StageEfficiencyVO

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| stageCode | String | 关卡代号，如 `1-7` |
| stageId | String | 关卡 ID，如 `main_01-07` |
| stageType | String | 关卡类型：`MAIN` / `ACT` / `ACT_REP` / `ACT_MINI` / `ACT_PERM` / `YTL_VIRTUAL` |
| zoneName | String | 所属章节或活动名称 |
| itemName | String | 主产物名称 |
| itemId | String | 主产物 ID |
| itemSeriesId | String | 主产物所属精英材料系列 ID |
| itemRarity | Integer | 主产物稀有度 |
| secondaryItemId | String | 副产物 ID（期望产出排名第二且理智转化率大于 0.1），无副产物为 `""` |
| apExpect | Double | 主产物期望理智 = 关卡理智 / 主产物掉率 |
| knockRating | Double | 主产物单次掉落率 |
| sampleSize | Long | 主产物样本数 |
| stageEfficiency | Double | 关卡理智转化率 = 掉落期望总价值 / 关卡理智 |
| leT2Efficiency | Double | 同系列 T1~T2 材料的理智转化率 |
| leT3Efficiency | Double | 同系列 T1~T3 材料的理智转化率 |
| leT4Efficiency | Double | 同系列 T1~T4 材料的理智转化率 |
| orundumPerAp | Double | 每理智可转化合成玉数量（仅统计 30011/30012/30061/30062） |
| orundumPerApEfficiency | Double | 相对搓玉效率，搓玉榜首为 `1` |
| lmdCost | Double | 每搓一抽（600 合成玉）所需龙门币，单位万；无搓玉产出时为 `null` |
| end | Long | 关卡结束时间（毫秒时间戳），长期开放为 2099 年 |
| spm | Double | 每分钟理智消耗 |
| apCost | Integer | 关卡理智消耗 |
| stageExpectedOutput | Double | 关卡期望产出总价值（理智） |
| dropDetail | List\<StageDropDetailVO\> | 掉落明细，按期望产出价值降序；仅 `orundumRecommendedStageVO`、`historyActStage`、`historyActOrundumStage` 返回，`recommendedStage` 不返回该字段 |

#### StageDropDetailVO

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| itemId | String | 物品 ID |
| itemName | String | 物品名称 |
| itemValue | Double | 物品单价（理智） |
| expectedOutput | Double | 期望产出价值 = 掉落率 × 物品价值 |
| knockRating | Double | 材料掉率 |
| isUnlimited | Boolean | 是否为活动商店无限池兑换物的虚拟掉落 |
| rarity | Integer | 稀有度 |
| quantity | Long | 掉落数量 |
| times | Long | 样本数 |
| sampleSize | Long | 样本数（与 `times` 相同，保留前端字段） |
| end | Long | 关卡结束时间（毫秒时间戳） |

### 计算口径

1. 材料价值由基础物品（合成玉、源石、龙门币、经验、模组解锁凭证、采购凭证、家具零件、自定义材料等）按配置策略确定，再结合关卡掉率与加工站合成/副产物关系迭代收敛，最多 10 轮，单轮总变化小于 `1e-6` 时停止。
2. 关卡理智转化率 `stageEfficiency` = 该关所有掉落期望价值之和 / 关卡理智消耗。
3. 开放推荐关卡只保留 `stageEfficiency > 0.5` 且结束时间晚于当前时间的关卡，按材料系列聚合，系列内按 `stageEfficiency` 降序；材料系列推荐关卡不返回 `dropDetail`。
4. 搓玉推荐只保留 `orundumPerAp > 0.2` 的关卡，按 `orundumPerAp` 降序；`orundumPerApEfficiency` 为相对榜首的比值。
5. 主产物取掉落明细中期望产出价值最高的物品；若最高项为活动商店无限池虚拟掉落，则与第二名交换，保证主产物是真实掉落物。主产物不属于任何精英材料系列时，该关卡结果被丢弃。
6. 活动关卡（`ACT` / `ACT_REP` / `YTL_VIRTUAL`）会按活动商店无限池最优兑换物追加一条虚拟掉落（`isUnlimited=true`）。
7. 历史活动关卡只聚合 `ACT` / `ACT_REP` 且主产物稀有度为 3 的关卡，按 `zoneName` 聚合、按活动结束时间降序。
8. 历史活动搓玉关卡按 `zoneName + endTime` 聚合，按 `orundumPerAp` 降序、按活动结束时间降序。
9. `stageBlacklist` 中的关卡不参与计算；`stageWhitelist` 在当前版本中未生效。

### 错误码

与材料价值 API 一致：`10002`、`30100`、`30102`、`30104`。

---

## 三、调用示例

```powershell
# 材料价值
$body = '{"sampleSize":300}'
Invoke-RestMethod -Method Post -Uri 'http://localhost:10012/item/v7/value' `
  -ContentType 'application/json; charset=utf-8' -Body $body

# 关卡推荐
Invoke-RestMethod -Method Post -Uri 'http://localhost:10012/stage/v7/recommended' `
  -ContentType 'application/json; charset=utf-8' -Body $body
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
