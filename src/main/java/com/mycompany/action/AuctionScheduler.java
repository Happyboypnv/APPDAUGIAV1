package com.mycompany.action;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.*;

public class AuctionScheduler {
    private static volatile AuctionScheduler instance;

    private AuctionScheduler() {}

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            synchronized (AuctionScheduler.class) {
                if (instance == null) {
                    instance = new AuctionScheduler();
                }
            }
        }
        return instance;
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3, runnable -> {
        Thread task = new Thread(runnable);
        task.setDaemon(false);
        return task;
    });

    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();
    private final AuctionSessionService auctionSessionService = AuctionSessionService.getInstance();

    public void setTimeCancelAuction(AuctionSession phien) {
        String maPhien = phien.getAuctionSessionId();
        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getEndTime()).toSeconds();
        if (delay <= 0) return;

        cancel(phien);

        ScheduledFuture<?> future = executor.schedule(() -> closeAuction(phien), delay, TimeUnit.SECONDS);
        scheduledFutures.put(maPhien, future);
    }
    public void cancel(AuctionSession phien) {
        String maPhien = phien.getSessionId();

        ScheduledFuture<?> future = scheduledFutures.remove(maPhien);
        if(future != null && !future.isDone()) {
            future.cancel(false);
        }
    }
    public void closeAuction(AuctionSession phien) {
        String maPhien = phien.getAuctionSessionId();

        auctionSessionService.closeAuction(phien, SessionStatus.PAID);
        scheduledFutures.remove(maPhien);
    }
    public void shutdown() {
        executor.shutdown();
    }
}