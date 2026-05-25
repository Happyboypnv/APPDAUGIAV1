package com.mycompany.action;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.server.websocket.AuctionWebSocketServer;
import com.mycompany.utils.*;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Connection;
import java.sql.SQLException;
import com.mycompany.models.Bid;

public class AuctionSessionService {
    private static volatile AuctionSessionService instance;
    private AuctionWebSocketServer webSocketServer;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionSessionService.class);
    // Lock theo từng mã phiên để đảm bảo thread-safe
    private static final Map<String, Object> locks = new ConcurrentHashMap<>();

    private static final AuctionSessionRegistry auctionSessionRegistry = AuctionSessionRegistry.getInstance();
    private static final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
    private static final IAuctionRepository auctionRepository = new AuctionRepositorySQLite();
    private static final IUserRepository userRepository = new UserRepositorySQLite();
    private static final BidRepositorySQLite bidRepository = new BidRepositorySQLite();

    private AuctionSessionService() {}
    public void setWebSocketServer(AuctionWebSocketServer server) {
        this.webSocketServer = server;
    }
    public static AuctionSessionService getInstance() {
        if (instance == null) {
            synchronized (AuctionSessionService.class) {
                if (instance == null) {
                    instance = new AuctionSessionService();
                }
            }
        }
        return instance;
    }

    private Object getLock(String maauction) {
        return locks.computeIfAbsent(maauction, k -> new Object());
    }

    /**
     * Mở phiên đấu giá: chuyển WAITING → IN_PROGRESS.
     *
     * Nếu phiên đã có startTime được lên lịch sẵn (từ DB hoặc từ tạo phiên),
     * giữ nguyên startTime đó và tính endTime = startTime + duration.
     *
     * Nếu phiên chưa có startTime (bắt đầu thủ công ngay lập tức),
     * đặt startTime = now và endTime = now + duration.
     */
    public void startAuction(AuctionSession auction) {
        synchronized (getLock(auction.getSessionId())) {
            if (auction.getStatus() != SessionStatus.WAITING) return;

            LocalDateTime now = LocalDateTime.now();

            // Nếu phiên có startTime được lên lịch sẵn: giữ nguyên startTime,
            // chỉ tính lại endTime dựa trên duration (nếu endTime chưa có).
            // Nếu chưa có startTime (bắt đầu ngay): đặt startTime = now.
            if (auction.getStartTime() == null) {
                auction.setStartTime(now);
            }

            // Tính endTime nếu chưa có (dùng duration từ phiên)
            if (auction.getEndTime() == null) {
                auction.setEndTime(auction.getStartTime().plusSeconds(auction.getDuration()));
            }

            auction.setStatus(SessionStatus.IN_PROGRESS);
            auctionSessionRegistry.add(auction);
            auctionScheduler.setACAuction(auction);
            auctionRepository.update(auction);

            logger.info("Phiên {} đã mở lúc {}, sẽ đóng lúc {}",
                    auction.getSessionId(), auction.getStartTime(), auction.getEndTime());
        }
    }

    public void closeAuction(AuctionSession auction, SessionStatus lyDo) {
        synchronized (getLock(auction.getSessionId())) {
            if (auction.getStatus() == SessionStatus.PAID ||
                    auction.getStatus() == SessionStatus.CANCELLED) {
                return;
            }

            if (!auction.isHasBid()) {
                lyDo = SessionStatus.CANCELLED;
            }

            if (lyDo == SessionStatus.PAID) {
                auction.setStatus(SessionStatus.PAID);
                auction.setWinner();
                // TODO: Thực hiện trừ tiền người thắng và cộng tiền người bán ở đây
            } else {
                auction.setStatus(SessionStatus.CANCELLED);
                auctionScheduler.cancelAC(auction);
                // Hủy lịch tự động mở phiên nếu phiên bị đóng trước khi bắt đầu
                auctionScheduler.cancelAS(auction);
            }

            auctionRepository.update(auction);
            auctionSessionRegistry.delete(auction);
            // Lưu ý: Không xóa lock khỏi Map locks để tránh lỗi đồng bộ

            if (webSocketServer != null) {
                webSocketServer.broadcastSessionEnded(auction.getSessionId());
            }
        }
    }

    public boolean setPrice(AuctionSession auction, User bidder, double gia) {
        synchronized (getLock(auction.getSessionId())) {
            // 1. Kiểm tra trạng thái phiên
            if (auction.getStatus() != SessionStatus.IN_PROGRESS) {
                return false;
            }
            // 2. Chủ phòng không được tự đấu giá
            if (bidder.equals(auction.getSeller())) {
                return false;
            }
            // 3. Tính toán giá tối thiểu cần đặt
            double giaToiThieu = auction.isHasBid()
                    ? auction.getCurrentPrice() + auction.getPriceStep()
                    : auction.getCurrentPrice();
            if (gia < giaToiThieu) {
                return false;
            }
            // 4. Kiểm tra số dư người mua
            if (bidder.getAvailableBalance() < gia) {
                return false;
            }
            // 5. Logic gia hạn thời gian (Anti-sniping)
            LocalDateTime now = LocalDateTime.now();
            long thoiGianConLai = Duration.between(now, auction.getEndTime()).getSeconds();
            if (thoiGianConLai < 60 && thoiGianConLai > 0) {
                LocalDateTime newEnd = now.plusSeconds(60);
                if (newEnd.isAfter(auction.getEndTime())) {
                    auction.setEndTime(newEnd);
                    auctionScheduler.setACAuction(auction);
                    logger.info("⏱️  Gia hạn phiên {} đến {}", auction.getSessionId(), newEnd);
                }
            }
            // 6. Lưu lại thông tin hiện tại để rollback nếu cần
            boolean isCurBidderRepeating = false;
            User previousLeader = null;
            double oldPrice = auction.getCurrentPrice();
            if (auction.isHasBid() && !auction.getBidderList().isEmpty()) {
                previousLeader = auction.getBidderList().get(auction.getBidderList().size() - 1);
                isCurBidderRepeating = bidder.getUserId().equals(previousLeader.getUserId());
            }
            // 7. Cập nhật trạng thái phiên
            if (!auction.isHasBid()) {
                auction.setHasBid(true);
            }
            // 8. Chuẩn bị Bid entity (unique ID)
            Bid bid = new Bid(auction.getSessionId(), bidder.getUserId(), gia);
            Connection conn = null;
            boolean success = false;
            try {
                conn = com.mycompany.utils.DatabaseConnection.getConnection();
                conn.setAutoCommit(false);
                // 8.1. Idempotency: Nếu bid đã tồn tại, coi là thành công (không double-process)
                if (bidRepository.existsByBidId(bid.getBidId())) {
                    logger.info("Bid {} đã tồn tại, idempotent success", bid.getBidId());
                    return true;
                }
                // 8.2. Xử lý tiền tệ (in-memory)
                if (isCurBidderRepeating && previousLeader != null) {
                    bidder.setAvailableBalance(bidder.getAvailableBalance() + oldPrice);
                    logger.info("💰 Hoàn {} cho {} (nâng giá cũ)", oldPrice, bidder.getFullName());
                } else if (previousLeader != null && !isCurBidderRepeating) {
                    previousLeader.setAvailableBalance(previousLeader.getAvailableBalance() + oldPrice);
                    logger.info("💰 Hoàn {} cho {} (thua người khác)", oldPrice, previousLeader.getFullName());
                }
                bidder.setAvailableBalance(bidder.getAvailableBalance() - gia);
                logger.info("💸 Trừ {} từ {}", gia, bidder.getFullName());
                // 8.3. Thêm bidder vào danh sách nếu chưa có
                if (!isBidderInList(auction, bidder)) {
                    auction.addBidder(bidder);
                    logger.info("✅ Thêm bidder {} vào danh sách", bidder.getFullName());
                } else {
                    logger.info("ℹ️  Bidder {} đã có trong danh sách, không thêm lại", bidder.getFullName());
                }
                // 8.4. Cập nhật giá hiện tại và bước giá
                auction.setCurrentPrice(gia);
                auction.setPriceStep(gia * auction.getMinPriceDiffRatio());
                // 8.5. Lưu Bid vào DB
                bidRepository.insertBid(bid);
                // 8.6. Lưu auction và user vào DB
                auctionRepository.update(auction);
                userRepository.update(bidder);
                logger.info("✅ Cập nhật balance DB cho bidder: {}", bidder.getFullName());
                if (previousLeader != null && !isCurBidderRepeating) {
                    userRepository.update(previousLeader);
                    logger.info("✅ Cập nhật balance DB cho previousLeader: {}", previousLeader.getFullName());
                }
                conn.commit();
                logger.info("✅ Transaction commit thành công cho bid {}", bid.getBidId());
                success = true;
            } catch (Exception e) {
                logger.error("❌ Lỗi transaction đặt giá: " + e.getMessage(), e);
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { logger.error("Lỗi rollback: " + ex.getMessage()); }
                }
                // Rollback in-memory state
                if (isCurBidderRepeating && previousLeader != null) {
                    bidder.setAvailableBalance(bidder.getAvailableBalance() + oldPrice);
                } else if (previousLeader != null && !isCurBidderRepeating) {
                    previousLeader.setAvailableBalance(previousLeader.getAvailableBalance() - oldPrice);
                }
                bidder.setAvailableBalance(bidder.getAvailableBalance() + gia);
                success = false;
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(false); } catch (SQLException ignore) {}
                }
            }
            // 9. Broadcast WebSocket: responsibility moved to WebSocket server (handleBid)
            //    To avoid duplicate broadcasts and keep single source-of-truth for persistence,
            //    AuctionWebSocketServer.handleBid(...) will broadcast BID_RESULT to clients.
            return success;
        }
    }

    /**
     * ✅ Helper method: Check if bidder already in bidderList (avoid duplicates)
     */
    private boolean isBidderInList(AuctionSession auction, User bidder) {
        if (auction.getBidderList() == null || auction.getBidderList().isEmpty()) {
            return false;
        }
        Set<String> bidderIds = new HashSet<>();
        for (User u : auction.getBidderList()) {
            bidderIds.add(u.getUserId());
        }
        return bidderIds.contains(bidder.getUserId());
    }

    /**
     * ✅ NEW: Accept an auction request (admin only operation).
     * Only admin can call this to approve pending auctions.
     */
    public void acceptAuctionRequest(AuctionSession auction) {
        synchronized (getLock(auction.getSessionId())) {
            if (auction.getStatus() != SessionStatus.WAITING) {
                logger.warn("❌ Không thể duyệt phiên {} vì không ở trạng thái WAITING", auction.getSessionId());
                return;
            }
            auction.setAccepted(1);
            auctionRepository.update(auction);
            
            // Check if startTime has already passed - if so, start immediately
            LocalDateTime now = LocalDateTime.now();
            if (auction.getStartTime() != null && !auction.getStartTime().isAfter(now)) {
                logger.info("✅ Phiên {} được duyệt, startTime đã qua - mở phiên ngay!", auction.getSessionId());
                startAuction(auction);
            } else {
                logger.info("✅ Phiên {} được duyệt, sẽ mở lúc startTime", auction.getSessionId());
            }
        }
    }

    /**
     * ✅ NEW: Deny an auction request (admin only operation).
     * Only admin can call this to reject pending auctions.
     * Sets isAccepted = 0 and triggers immediate cancellation.
     */
    public void denyAuctionRequest(AuctionSession auction) {
        synchronized (getLock(auction.getSessionId())) {
            if (auction.getStatus() != SessionStatus.WAITING) {
                logger.warn("❌ Không thể từ chối phiên {} vì không ở trạng thái WAITING", auction.getSessionId());
                return;
            }
            // Set isAccepted = 0 to explicitly mark as DENIED
            auction.setAccepted(0);
            auctionRepository.update(auction);
            auctionScheduler.cancelAS(auction);
            logger.info("✅ Phiên {} đã bị admin từ chối (isAccepted = 0)", auction.getSessionId());
            
            // Close auction immediately
            closeAuction(auction, SessionStatus.CANCELLED);
        }
    }

    /**
     * ✅ NEW: Poll for admin decision when startTime has passed but auction is still PENDING.
     * Checks every 30 seconds (max 6 minutes = 12 retries) to see if admin approved/denied.
     * If admin approved, starts the auction.
     * If admin denied, cancels the auction.
     * If timeout, auto-cancels the auction.
     */
    public void pollDeferredAuction(AuctionSession auction, int retryCount) {
        final int MAX_RETRIES = 12;  // 12 × 30s = 6 minutes
        final long POLL_DELAY_SECONDS = 30;
        
        String maPhien = auction.getSessionId();
        
        // Schedule the polling check
        AuctionScheduler.getInstance().scheduleDelayedCheck(() -> {
            synchronized (getLock(maPhien)) {
                // Reload from DB to get latest isAccepted status
                try {
                    AuctionSession latestAuction = auctionRepository.findById(maPhien);
                    if (latestAuction == null) {
                        logger.warn("❌ Phiên {} không tìm thấy trong DB", maPhien);
                        return;
                    }
                    
                    logger.info("🔍 Poll phiên {} (lần {}): isAccepted = {}", 
                            maPhien, retryCount + 1, latestAuction.isAccepted());
                    
                    switch (latestAuction.isAccepted()) {
                        case 1: // APPROVED
                            logger.info("✅ Admin duyệt phiên {}, mở ngay!", maPhien);
                            startAuction(latestAuction);
                            break;
                            
                        case 0: // DENIED
                            logger.info("❌ Admin từ chối phiên {}, hủy ngay!", maPhien);
                            closeAuction(latestAuction, SessionStatus.CANCELLED);
                            break;
                            
                        case -1: // STILL PENDING
                            if (retryCount < MAX_RETRIES) {
                                logger.info("⏸️  Phiên {} vẫn chờ duyệt, kiểm tra lại sau {}s (lần {}/{})", 
                                        maPhien, POLL_DELAY_SECONDS, retryCount + 1, MAX_RETRIES);
                                pollDeferredAuction(latestAuction, retryCount + 1);
                            } else {
                                logger.warn("⏰ Timeout chờ admin duyệt phiên {}, tự động hủy", maPhien);
                                closeAuction(latestAuction, SessionStatus.CANCELLED);
                            }
                            break;
                    }
                } catch (Exception e) {
                    logger.error("❌ Lỗi polling phiên {}: {}", maPhien, e.getMessage(), e);
                }
            }
        }, POLL_DELAY_SECONDS);
    }
}