package org.hongxi.cloud.sample.api.seata;

/**
 * Seata demo - Storage Dubbo service interface
 */
public interface SeataStorageService {

    String deduct(String commodityCode, int count);
}
