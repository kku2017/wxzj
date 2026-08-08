package com.wxzj.common;

import java.util.concurrent.atomic.AtomicLong;

public final class NoGenerator {

    private static final AtomicLong SEQ = new AtomicLong(0);

    private NoGenerator() {
    }

    /** 生成全局唯一业务单号，如 CJ1786158823590_000 */
    public static String gen(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("_%03d", SEQ.getAndIncrement() % 1000);
    }
}
