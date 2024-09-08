package org.ar.pay.runable;

import java.util.concurrent.ExecutorService;

public class BarrierRun implements Runnable {
    private final ExecutorService threadPool;

    public BarrierRun(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }
        @Override
        public void run() {
            System.out.println("所有接口已经完成");

            threadPool.shutdown();
        }

    }


