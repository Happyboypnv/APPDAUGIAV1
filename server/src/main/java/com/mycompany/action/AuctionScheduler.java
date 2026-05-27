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
    private final Map<String, ScheduledFuture<?>> deferredPollFutures = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> paymentFutures = new ConcurrentHashMap<>();
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionScheduler.class);

    private AuctionSessionService auctionSessionService() {
        return AuctionSessionService.getInstance();
    }

    /**
     * Lên lịch timeout cho PAYMENT_PENDING.
     * Nếu timeout tới mà người thắng chưa trả lời → gọi finalizePayment(phien, false)
     */
    public void setPaymentTimeout(AuctionSession phien, long seconds) {
        String maPhien = phien.getSessionId();
        // cancel nếu đã có lịch trước đó
        cancelPaymentTimeout(phien);
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                auctionSessionService().finalizePayment(phien, false);
            } catch (Exception ex) {
                logger.error("Lỗi finalizePayment tự động cho {}: {}", maPhien, ex.getMessage());
            } finally {
                paymentFutures.remove(maPhien);
            }
        }, seconds, TimeUnit.SECONDS);
        paymentFutures.put(maPhien, future);
    }

    public void cancelPaymentTimeout(AuctionSession phien) {
        ScheduledFuture<?> f = paymentFutures.remove(phien.getSessionId());
        if (f != null && !f.isDone()) {
            f.cancel(false);
        }
    }

    /**
     * Lên lịch tự động chuyển trạng thái AuctionSession từ WAITING → IN_PROGRESS
     * khi đến đúng thời gian startTime của phiên.
     *
     * Nếu startTime đã qua (delay <= 0), mở phiên ngay lập tức.
     *
     * @param phien phiên đấu giá cần lên lịch mở
     */
    public void setASAuction(AuctionSession phien) {
        String maPhien = phien.getSessionId();
        if (phien.getStartTime() == null) return;

        // Hủy lịch cũ nếu đã tồn tại
        cancelAS(phien);

        long delay = java.time.Duration.between(LocalDateTime.now(), phien.getStartTime()).toSeconds();

        switch (phien.isAccepted()) {
            case -1:
                if (delay <= 0) {
                    logger.info("Phiên {} chưa được duyệt nhưng start time đã qua, chờ admin duyệt", maPhien);
                    auctionSessionService().pollDeferredAuction(phien, 0);
                } else {
                    ScheduledFuture<?> future = executor.schedule(() -> {
                        asFutures.remove(maPhien);
                        auctionSessionService().pollDeferredAuction(phien, 0);
                    }, delay, TimeUnit.SECONDS);
                    asFutures.put(maPhien, future);
                    logger.info("Phiên {} chưa được duyệt, sẽ kiểm tra lại khi đến startTime", maPhien);
                }
                return;
            case 0:
                ScheduledFuture<?> cancelFuture = executor.schedule(() -> {
                    auctionSessionService().closeAuction(phien, SessionStatus.CANCELLED);
                    asFutures.remove(maPhien);
                }, 0, TimeUnit.SECONDS);
                asFutures.put(maPhien, cancelFuture);
                logger.info("[AuctionScheduler] Phiên đã bị huỷ bởi admin, đang đóng phiên...");
                return;
            case 1:
                break;
            default:
                logger.warn("Phiên {} cÃ³ tráº¡ng thÃ¡i duyá»‡t khÃ´ng há»£p lá»‡: {}", maPhien, phien.isAccepted());
                return;
        }

        if (delay <= 0) {
            // Thời gian mở đã qua hoặc đúng lúc → mở ngay lập tức (delay = 0)
            ScheduledFuture<?> future = executor.schedule(() -> {
                auctionSessionService().startAuction(phien);
                logger.info("Đã mở phiên đấu giá {} ngay lập tức vì startTime đã qua", maPhien);
                asFutures.remove(maPhien);
            }, 0, TimeUnit.SECONDS);
            asFutures.put(maPhien, future);
            logger.info("Đã thêm phiên đấu giá {} để nó bắt đầu ngay lập tức", maPhien);
        } else {
            ScheduledFuture<?> future = executor.schedule(() -> {
                auctionSessionService().startAuction(phien);
                logger.info("Đã mở phiên đấu giá {}", maPhien);
                asFutures.remove(maPhien);
            }, delay, TimeUnit.SECONDS);
            asFutures.put(maPhien, future);
            logger.info("Đã thêm phiên đấu giá {} vào đồng hồ", maPhien);
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
        cancelDeferredAuctionPoll(phien);
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
                auctionSessionService().closeAuction(phien, phien.getHasBid() ? SessionStatus.PAID : SessionStatus.CANCELLED);
            }, 0, TimeUnit.SECONDS);
            acFutures.put(maPhien, future);
        } else {
            ScheduledFuture<?> future = executor.schedule(() -> {
                auctionSessionService().closeAuction(phien, phien.getHasBid() ? SessionStatus.PAID : SessionStatus.CANCELLED);
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
        cancelDeferredAuctionPoll(phien);
    }

    public void scheduleDeferredAuctionPoll(AuctionSession phien, int retryCount, long delaySeconds) {
        String maPhien = phien.getSessionId();
        cancelDeferredAuctionPoll(phien);

        ScheduledFuture<?> future = executor.schedule(() -> {
            deferredPollFutures.remove(maPhien);
            auctionSessionService().pollDeferredAuction(phien, retryCount);
        }, Math.max(0, delaySeconds), TimeUnit.SECONDS);
        deferredPollFutures.put(maPhien, future);

        logger.info("[AuctionScheduler] Bắt đầu lên lịch phiên...");
    }

    public void cancelDeferredAuctionPoll(AuctionSession phien) {
        ScheduledFuture<?> future = deferredPollFutures.remove(phien.getSessionId());
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        logger.info("[AuctionScheduler] Phiên đã bắt đầu, huỷ đếm ngược...");
    }

    public void shutdown() {
        executor.shutdown();
    }
}
