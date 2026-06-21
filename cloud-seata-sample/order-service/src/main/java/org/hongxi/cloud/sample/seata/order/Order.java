package org.hongxi.cloud.sample.seata.order;

import lombok.Data;

import java.io.Serializable;

@Data
public class Order implements Serializable {

    private Long id;

    private String userId;

    private String commodityCode;

    private int count;

    private int money;
}
