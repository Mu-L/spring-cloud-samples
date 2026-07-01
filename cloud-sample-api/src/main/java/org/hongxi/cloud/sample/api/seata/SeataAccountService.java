package org.hongxi.cloud.sample.api.seata;

/**
 * Seata demo - Account Dubbo service interface
 */
public interface SeataAccountService {

    String deduct(String userId, int money);
}
