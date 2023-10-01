package com.lhs.entity.dto.stage;


import lombok.Data;

//用于转换企鹅物流的api的实体类
@Data
public class PenguinDataDto {

//    企鹅物流数据
    private String stageId;  //关卡id
    private String itemId; //物品id
    private Integer quantity;  //物品掉落次数
    private Integer times;  //关卡刷取次数
    private Long start; //开始时间
    private Long end;  //结束时间

}
