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

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5, runnable -> {
        Thread task = new Thread(runnable);
        task.setDaemon(false);
        return task;
    });

    private final Map<String, ScheduledFuture<?>> acFutures = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> asFutures = new ConcurrentHashMap<>();
    private final AuctionSessionService auctionSessionService = AuctionSessionService.getInstance();
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionScheduler.class);

    /**
     * Lên lịch tự động chuyển trạng thái AuctionSession từ WAITING → IN_PROGRESS
     * khi đến đúng thời gian startTime của phiên.
     *
     * ✅ REVISED ALGORITHM:
     * - isAccepted = -1 (PENDING): If startTime passed, poll every 30s for admin decision (max 6 mins)
     *                              Otherwise wait for admin OR startTime, whichever comes first
     * - isAccepted = 0  (DENIED):  Cancel immediately
     * - isAccepted = 1  (APPROVED): Schedule auto-start at startTime
     *
     * @param phien phiên đấu giá cần lên lịch mở
     */
    public void setASAuction(AuctionSession phien) {
        String maPhien = phien.getSessionId();
        if (phien.getStartTime() == null) return;

        // Hủy lịch cũ nếu đã tồn tại
        cancelAS(phien);

        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getStartTime()).toSeconds();

        // ✅ NEW: Handle all 3 authorization states
        switch (phien.isAccepted()) {

            case -1: // PENDING - Admin hasn't decided yet
                if (delay <= 0) {
                    // StartTime has passed, but admin hasn't decided
                    // Poll every 30 seconds to check if admin accepts
                    logger.info("⏸️  Phiên {} chưa được duyệt nhưng startTime đã qua, kiểm tra lại sau 30s", maPhien);
                    auctionSessionService.pollDeferredAuction(phien, 0);
                } else {
                    logger.info("⏭️  Phiên {} chưa được duyệt, chờ admin duyệt trước startTime", maPhien);
                    // Wait - either admin approves or startTime arrives
                    // Schedule a check that monitors for decision
                }
                break;

            case 0: // DENIED - Admin explicitly rejected this auction
                logger.info("❌ Phiên {} bị admin từ chối, hủy ngay", maPhien);
                ScheduledFuture<?> cancelFuture = executor.schedule(() -> {
                    auctionSessionService.closeAuction(phien, SessionStatus.CANCELLED);
                    logger.info("❌ Phiên {} đã đóng vì bị admin từ chối", maPhien);
                    asFutures.remove(maPhien);
                }, 0, TimeUnit.SECONDS);
                asFutures.put(maPhien, cancelFuture);
                break;

            case 1: // APPROVED - Admin approved, proceed with auto-start
                if (delay <= 0) {
                    // Start immediately
                    logger.info("✅ Mở phiên {} ngay lập tức (startTime đã qua)", maPhien);
                    ScheduledFuture<?> future = executor.schedule(() -> {
                        auctionSessionService.startAuction(phien);
                        logger.info("✅ Phiên {} đã mở ngay lập tức", maPhien);
                        asFutures.remove(maPhien);
                    }, 0, TimeUnit.SECONDS);
                    asFutures.put(maPhien, future);
                } else {
                    // Schedule for future start
                    logger.info("✅ Lên lịch mở phiên {} lúc startTime trong {}s", maPhien, delay);
                    ScheduledFuture<?> future = executor.schedule(() -> {
                        auctionSessionService.startAuction(phien);
                        logger.info("✅ Phiên {} đã mở theo lịch", maPhien);
                        asFutures.remove(maPhien);
                    }, delay, TimeUnit.SECONDS);
                    asFutures.put(maPhien, future);
                }
                break;

            default:
                logger.warn("⚠️  Phiên {} có isAccepted không xác định: {}", maPhien, phien.isAccepted());
        }
    }

    /**
     * Hủy lịch tự động mở phiên (nếu cần hủy phiên trước khi bắt đầu).
     *
     * @param phien phiên đấu giá cần hủy lịch mở
     */
    public void cancelAS(AuctionSession phien) {
        String maPhien = phien.getSessionId();

        ScheduledFuture<?> future = asFutures.remove(maPhien);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * Lên lịch tự động đóng phiên khi đến endTime.
     *
     * Nếu endTime đã qua (delay <= 0), đóng phiên ngay lập tức.
     *
     * @param phien phiên đấu giá cần lên lịch đóng
     */
    public void setACAuction(AuctionSession phien) {
        String maPhien = phien.getSessionId();
        if (phien.getEndTime() == null) return;

        cancelAC(phien);

        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getEndTime()).toSeconds();

        if (delay <= 0) {
            ScheduledFuture<?> future = executor.schedule(() -> {
                auctionSessionService.closeAuction(phien, phien.getHasBid() ? SessionStatus.PAID : SessionStatus.CANCELLED);
            }, 0, TimeUnit.SECONDS);
            acFutures.put(maPhien, future);
        } else {
            ScheduledFuture<?> future = executor.schedule(() -> {
                auctionSessionService.closeAuction(phien, phien.getHasBid() ? SessionStatus.PAID : SessionStatus.CANCELLED);
            }, delay, TimeUnit.SECONDS);
            acFutures.put(maPhien, future);
        }
    }

    public void cancelAC(AuctionSession phien) {
        String maPhien = phien.getSessionId();

        ScheduledFuture<?> future = acFutures.remove(maPhien);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * ✅ NEW: Schedule a delayed check (used for polling deferred auctions)
     * This helper method is used by AuctionSessionService to schedule periodic checks
     * for admin decisions on pending auctions.
     */
    public void scheduleDelayedCheck(Runnable task, long delaySeconds) {
        executor.schedule(task, delaySeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        executor.shutdown();
    }
}