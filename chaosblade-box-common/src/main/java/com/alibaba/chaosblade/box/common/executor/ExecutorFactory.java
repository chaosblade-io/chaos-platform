package com.alibaba.chaosblade.box.common.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author yefei
 */
public interface ExecutorFactory {

    /**
     *
     * @param
     * @return
     */
    ExecutorService createExecutorService(ThreadFactory threadFactory);
}
