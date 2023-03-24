# API

# 基础传输

请求一个 API 时, 包含了: API 终结点, 以及 API 所需参数.

### 请求说明

使用 HTTP GET:

|     名称      |                   说明                    |
| :-----------: | :---------------------------------------: |
| 请求 URL 格式 | /终结点?参数名=参数值&参数名=参数值...... |

使用 HTTP POST:

|     名称      |       说明        |
| :-----------: | :---------------: |
| 请求 URL 格式 |      /终结点      |
|    请求体     | 请求体一般为 JSON |

HTTP POST JSON 格式:

```
{
    "参数名": "参数值",
    "参数名2": "参数值"
}
```

### 响应说明

以下是响应的内容 <br>
其中 code（返回值），message（说明）字段：

| 返回值 |       说明       |
| :----: | :--------------: |
| 50002  |     数据错误     |
| 50001  |    数据未找到    |
|  404   |    API 不存在    |
|  200   | 一般代表调用成功 |

data 字段：
API 的响应数据，一般是一个 JSON，部分情况为 String

# 参数及响应数据

下面是请求所需 params 和响应包含的 data 格式

### 获取蓝材料最优图(JsonArray)

终结点：`/api/find/stage/t3`
<br>
请求类型：Get

#### 参数

|     字段名     | 数据类型 | 默认值 |                                      说明                                       |
| :------------: | :------: | :----: | :-----------------------------------------------------------------------------: |
| expCoefficient |  Double  |   无   | 经验书系数 一般默认经验书价值为龙门币价值的 0.625 倍（也可选择 0.72 和 0.0 倍） |

#### 响应数据

|      字段名      | 数据类型 |                                                           说明                                                            |
| :--------------: | :------: | :-----------------------------------------------------------------------------------------------------------------------: |
| stageEfficiency  |  Double  | 与所有常驻关卡中，无活动加成时综合效率最高者相比，该关卡的效率为 103.6%。该效率是统计了所有产物的综合效率，长期最优的结果 |
|    stageCode     |  String  |                                                      关卡的显示名称                                                       |
|     itemType     |  String  |                                                  该关卡属于某一材料体系                                                   |
|   secondaryId    |  String  |                                               副产物的物品 ID，1 为无副产物                                               |
| sampleConfidence |  Double  |                              样本量的置信度（误差不超过 3%的概率）为 99.9%，置信度过低的关卡                              |
|    stageState    |  String  |                               关卡状态，0:无事发生 1:SideStory 2:故事集 3:理智小样+物资补给                               |
|   activityName   |  String  |                                                      活动或章节名称                                                       |
|   knockRating    |  Double  |                                      主产物的掉率，短期急需该系材料的话参考意义较大                                       |
|    updateTime    |  String  |                                                      数据统计的时间                                                       |
|    sampleSize    |   Int    |                                                         样本数量                                                          |
|    secondary     |  String  |                                              副产物的物品名称，1 为无副产物                                               |
|     apExpect     |  Double  |                                      主产物的期望，短期急需该系材料的话参考意义较大                                       |
|      itemId      |  String  |                                                      主产物的物品 ID                                                      |
|       spm        |  String  |                                        SanityPerMinute，每分钟理论上可以消耗的理智                                        |
|    stageColor    |   Int    |        关卡标注颜色 橙色(双最优):4，紫色(综合效率最优):3，蓝色(普通关卡):2，绿色(主产物期望最优):1，红色(活动):-1         |

### 获取绿材料最优图(JsonArray)

终结点：`/api/find/stage/t2`
<br>
请求类型：Get

#### 参数

|     字段名     | 数据类型 | 默认值 |                                      说明                                       |
| :------------: | :------: | :----: | :-----------------------------------------------------------------------------: |
| expCoefficient |  Double  |   无   | 经验书系数 一般默认经验书价值为龙门币价值的 0.625 倍（也可选择 0.72 和 0.0 倍） |

#### 响应数据

|      字段名      | 数据类型 |                                                           说明                                                           |
| :--------------: | :------: |:----------------------------------------------------------------------------------------------------------------------:|
| stageEfficiency  |  Double  |                                   与所有常驻关卡中，无活动加成时综合效率最高者相比。该效率是统计了所有产物的综合效率，长期最优的结果                                    |
|    stageCode     |  String  |                                                        关卡的显示名称                                                         |
|     itemType     |  String  |                                                      该关卡属于某一材料体系                                                       |
|   secondaryId    |  String  |                                                   副产物的物品 ID，1 为无副产物                                                    |
| sampleConfidence |  Double  |                                          样本量的置信度（误差不超过 3%的概率）为 99.9%，置信度过低的关卡                                          |
|    stageState    |  String  |                                       关卡状态，0:无事发生 1:SideStory 2:故事集 3:理智小样+物资补给                                        |
|   activityName   |  String  |                                                        活动或章节名称                                                         |
|   knockRating    |  Double  |                                                主产物的掉率，短期急需该系材料的话参考意义较大                                                 |
|    updateTime    |  String  |                                                        数据统计的时间                                                         |
|    sampleSize    |   Int    |                                                          样本数量                                                          |
|    secondary     |  String  |                                                    副产物的物品名称，1 为无副产物                                                    |
|     apExpect     |  Double  |                                                主产物的期望，短期急需该系材料的话参考意义较大                                                 |
|      itemId      |  String  |                                                       主产物的物品 ID                                                        |
|       spm        |  String  |                                             SanityPerMinute，每分钟理论上可以消耗的理智                                              |
|    stageColor    |   Int    |                            关卡标注颜色 橙色(双最优):4，紫色(综合效率最优):3，蓝色(普通关卡):2，绿色(主产物期望最优):1，红色(活动):-1                            |

### 获取常驻商店性价比(JsonObject)

终结点：`/api/find/store/perm`
<br>
请求类型：Get

#### 参数

> 该 API 无需参数

#### 响应数据
|    数据类型    |    字段名    |  说明   |
|:----------:|:---------:|:-----:|
| JSONObject | StoreType | 商店的类型 |


|  数据类型  |    字段名    |                      说明                      |
|:------:|:---------:|:--------------------------------------------:|
| String |  itemId   |                    物品 id                     |
| String | itemName  |                     物品名称                     |
| Double | itemValue |                     物品价值                     |
| Double |   cost    | 单位售价 （比如 5000 龙门币卖 7 代币 单位售价是 7/5000=0.0014) |
| String |  rawCost  |                    商店原始售价                    |
| Double |  costPer  |                     性价比                      |
| String | storeType |                     商店类型                     |

### 获取活动商店性价比(JsonArray)

终结点：`/api/find/store/act`
<br>
请求类型：Get

#### 参数

> 该 API 无需参数

#### 响应数据

|     字段名      |  数据类型  |    说明     |
|:------------:|:------:|:---------:|
| actStartData |  Long  |  活动开始时间   |
|  actEndData  |  Long  |  活动结束时间   |
|   actName    | String |   活动名称    |
|  actTagArea  | String |           |
|  actPPRBase  | Double |  商店原始售价   |
| actPPRStair  | String |    性价比    |
|   actStore   | Object | 商店性价比具体内容 |

其中`actStore`字段包含六个字段

|     字段名      |  数据类型  |          说明          |
|:------------:|:------:|:--------------------:|
|   itemArea   |  Int   | 区域索引，用于判断是无限池区还是有限池区 |
|   itemName   | String |         材料名称         |
|   itemPPR    | Double |        材料性价比         |
|  itemPrice   |  Int   |         商店售价         |
| itemQuantity |  Int   |       商店每次售卖个数       |
|  itemStock   |  Int   |         商店库存         |

### 获取所有物品价值(JsonArray)

终结点：`/api/find/item/value/`
<br>
请求类型：Get

#### 参数

|     字段名     | 数据类型 | 默认值 |                                      说明                                       |
| :------------: | :------: | :----: | :-----------------------------------------------------------------------------: |
| expCoefficient |  Double  |   无   | 经验书系数 一般默认经验书价值为龙门币价值的 0.625 倍（也可选择 0.72 和 0.0 倍） |

#### 响应数据

|  字段名   | 数据类型 |       说明       |
| :-------: | :------: | :--------------: |
|  itemId   |  String  |     物品 id      |
| itemName  |  String  |     物品名称     |
| itemValue |   Int    |     物品价值     |
|   type    |  String  |    物品稀有度    |
|  cardNum  |  String  | 前端排序的用索引 |

### 获取历史活动最优图(JsonArray)

终结点：`/api/find/stage/closedStage`
<br>
请求类型：Get

#### 参数

|     字段名     | 数据类型 | 默认值 |                                      说明                                       |
| :------------: | :------: | :----: | :-----------------------------------------------------------------------------: |
| expCoefficient |  Double  |   无   | 经验书系数 一般默认经验书价值为龙门币价值的 0.625 倍（也可选择 0.72 和 0.0 倍） |

#### 响应数据

|      字段名      | 数据类型 |                                                           说明                                                            |
| :--------------: | :------: | :-----------------------------------------------------------------------------------------------------------------------: |
| stageEfficiency  |  Double  | 与所有常驻关卡中，无活动加成时综合效率最高者相比，该关卡的效率为 103.6%。该效率是统计了所有产物的综合效率，长期最优的结果 |
|    stageCode     |  String  |                                                      关卡的显示名称                                                       |
|     itemType     |  String  |                                                  该关卡属于某一材料体系                                                   |
|   secondaryId    |  String  |                                               副产物的物品 ID，1 为无副产物                                               |
| sampleConfidence |  Double  |                              样本量的置信度（误差不超过 3%的概率）为 99.9%，置信度过低的关卡                              |
|    stageState    |  String  |                               关卡状态，0:无事发生 1:SideStory 2:故事集 3:理智小样+物资补给                               |
|   activityName   |  String  |                                                      活动或章节名称                                                       |
|   knockRating    |  Double  |                                      主产物的掉率，短期急需该系材料的话参考意义较大                                       |
|    updateTime    |  String  |                                                      数据统计的时间                                                       |
|    sampleSize    |   Int    |                                                         样本数量                                                          |
|    secondary     |  String  |                                              副产物的物品名称，1 为无副产物                                               |
|     apExpect     |  Double  |                                      主产物的期望，短期急需该系材料的话参考意义较大                                       |
|      itemId      |  String  |                                                      主产物的物品 ID                                                      |
|       spm        |  String  |                                        SanityPerMinute，每分钟理论上可以消耗的理智                                        |
|    stageColor    |   Int    |        关卡标注颜色 橙色(双最优):4，紫色(综合效率最优):3，蓝色(普通关卡):2，绿色(主产物期望最优):1，红色(活动):-1         |

### 获取商店礼包性价比(JsonArray)

终结点：`/api/find/store/act`
<br>
请求类型：Get

#### 参数

> 该 API 无需参数

#### 响应数据

|       字段名        | 数据类型 |                             说明                             |
| :-----------------: | :------: | :----------------------------------------------------------: |
|    packShowName     |  String  |                    礼包展示在页面上的名称                    |
|      packType       |  String  |        礼包的种类，如一次性、月卡、常驻，用于筛选礼包        |
|      packDraw       |  Double  |                         礼包包含抽数                         |
|     packPPRDraw     |  Double  |              礼包仅计抽卡性价比，相对 648 源石               |
|  packPPROriginium   |  Double  |                 礼包综合价比，相对 648 源石                  |
|       packImg       |  String  |                礼包图片名称，用于查找图片地址                |
|      packPrice      |   Int    |                          礼包的价格                          |
|       packID        |   Int    |                   ID 用于配合前端筛选礼包                    |
| packRmbPerOriginium |  Double  |        礼包全部物品折算源石后，每 RMB 能买到的源石数         |
|     gachaPermit     |   Int    |             礼包内单抽数量，gachaCal 使用此字段              |
|      packState      |   Int    |           是否正在售卖，这个建议以后改成用时间判断           |
|    packOriginium    |  Double  |                 礼包全部物品折算为源石的数量                 |
|   packRmbPerDraw    |  Double  |                       礼包每一抽的价格                       |
|    gachaPermit10    |   Int    |             礼包内十连数量，gachaCal 使用此字段              |
|   gachaOriginium    |   Int    |             礼包内源石数量，gachaCal 使用此字段              |
|      packName       |  String  |                         礼包官方名称                         |
|    gachaOrundum     |   Int    |            礼包内合成玉数量，gachaCal 使用此字段             |
|     packContent     |  Object  |                   礼包内除抽卡资源外的内容                   |
|       packTag       |  String  | 需要显示的附加说明，如“含有不可计价材料，请点击图片查看详情” |

其中`packContent`字段包含俩个字段

|       字段名        | 数据类型 |   说明   |
| :-----------------: | :------: | :------: |
| packContentQuantity |  String  | 材料数量 |
|   packContentItem   |  String  | 材料名称 |
