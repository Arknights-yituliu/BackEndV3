package com.lhs.vo.maa;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;


@Data
public class MaaOperBoxVo {
     private String uuid;
     private JSONArray operBox;
     private String server;
     private String source;
     private String version;

     public MaaOperBoxVo() {
     }

     public MaaOperBoxVo(String uuid, JSONArray operBox, String server, String source, String version) {
          this.uuid = uuid;
          this.operBox = operBox;
          this.server = server;
          this.source = source;
          this.version = version;
     }
}
