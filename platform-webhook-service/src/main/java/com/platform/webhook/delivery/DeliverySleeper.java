package com.platform.webhook.delivery;

@FunctionalInterface
public interface DeliverySleeper {
    void sleep(long millis) throws InterruptedException;
}
