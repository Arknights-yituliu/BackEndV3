# 第三方API - 获取干员练度数据

## 路径

```
GET /open-api/operator/info
```

## 认证

请求头使用 `Authorization: Bearer <PAT>`。PAT 需具备 `ak-data.read`，上级 `gama-data.read` 或历史 `all` 可覆盖；只写权限不能读取。OAuth access_token 不能用于此接口。

为兼容已有调用方，也接受 `Authorization: <PAT明文>`。已经由 TCL 迁移为 PAT 的旧 64 位令牌可继续使用原明文；迁移由用户中心负责，本接口不执行迁移。

## 请求参数

无请求参数。

新 PAT 由签发时的 `payload.game_uid` 确定账号，且该账号必须绑定当前用户并有森空岛数据；不能通过 `akUid` 参数切换账号。无 payload 的历史令牌沿用用户中心的「默认来源 → 最近变更」取数规则。当前 PAT 消费接口仅提供读取。

响应包含 `Cache-Control: private, no-cache`，调用方不应公开缓存用户数据。

## 返回格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "id": "char_102_texas",
      "level": 60,
      "evolvePhase": 2,
      "mainSkillLevel": 7,
      "skills": [
        {
          "id": "skcom_charge_cost[3]",
          "level": 0
        },
        {
          "id": "skchr_texas_2",
          "level": 2
        }
      ],
      "equips": [
        {
          "id": "uniequip_002_texas",
          "type": "Y",
          "level": 0
        }
      ],
      "potentialRank": 5
    }
  ]
}
```

## 返回字段说明

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| id | String | 干员ID，如 `char_102_texas` |
| level | Integer | 干员等级 |
| evolvePhase | Integer | 精英化阶段（0/1/2） |
| mainSkillLevel | Integer | 当前主技能等级 |
| skills | List\<SkillInfo\> | 技能列表，技能数量取决于该干员实际拥有的技能数 |
| equips | List\<EquipInfo\> | 模组列表，仅包含该干员实际拥有的模组分支 |
| potentialRank | Integer | 潜能等级（0~6） |

### SkillInfo

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| id | String | 技能ID，如 `skchr_texas_2` |
| level | Integer | 该技能专精等级（0~3） |

### EquipInfo

| 字段 | 类型 | 说明 |
| :-- | :-- | :-- |
| id | String | 模组ID，如 `uniequip_002_texas` |
| type | String | 模组分支类型（X/Y/D/A/B） |
| level | Integer | 模组等级（0~3） |

## 数据来源

- 练度数据来源于用户通过森空岛导入的干员培养信息
- 技能ID和模组ID来源于 `character_table_simple.v2.json`，仅输出该干员实际拥有的技能和模组分支
