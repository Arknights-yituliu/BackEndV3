package com.lhs.common.util;

import com.lhs.common.exception.ServiceException;

public class IdGenerator {

    private Long workerId;
    private static final Long START_TIMESTAMP = 1556676000000L;
    private long lastTimestamp = -1L;

    private long sequence = 0L;

    public IdGenerator(Long workerId) {
        this.workerId = workerId;
    }

    public synchronized Long nextId(){

//        System.out.println("上次的时间戳是："+lastTimestamp);

        long timestamp = timeGen();
        if(timestamp < lastTimestamp){
            throw new ServiceException(ResultCode.SYSTEM_TIME_ERROR);
        }

        if(timestamp == lastTimestamp){
            sequence++;
            if(sequence>99){
                timestamp = tilNextMillis(lastTimestamp);
            }
        }else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp-START_TIMESTAMP)*100+workerId)*100+sequence;
    }



    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}
