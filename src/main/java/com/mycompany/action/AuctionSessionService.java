package com.mycompany.action;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSessionService {
    private static volatile AuctionSessionService instance;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionSessionService.class);
    // Lock theo từng mã phiên để đảm bảo thread-safe
    private static final Map<String, Object> locks = new ConcurrentHashMap<>();

    private static final AuctionSessionRegistry auctionSessionRegistry = AuctionSessionRegistry.getInstance();
    private static final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();

    private AuctionSessionService() {}

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

    public void start(AuctionSession auction) {
        synchronized (getLock(auction.getSessionId())) {
            if (auction.getStatus() != SessionStatus.WAITING) return;

            LocalDateTime now = LocalDateTime.now();
            auction.setStartTime(now);
            auction.setEndTime(now.plusSeconds(auction.getDuration()));
            auction.setStatus(SessionStatus.IN_PROGRESS);

            auctionScheduler.setTimeCancelAuction(auction);
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
                auctionScheduler.cancel(auction);
            }

            auctionSessionRegistry.delete(auction);
            // Lưu ý: Không xóa lock khỏi Map locks để tránh lỗi đồng bộ
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
            // Nếu là người đầu tiên: giaToiThieu = gia khoi diem
            // Nếu đã có người đặt: giaToiThieu = gia hien tai + buoc gia
            double giaToiThieu = auction.isHasBid()
                    ? auction.getCurrentPrice() + auction.getPriceStep()
                    : auction.getCurrentPrice();

            if (gia < giaToiThieu) {
                return false;
            }

            // 4. Kiểm tra số dư người mua (giả định có method getSoDu)
            if (bidder.getAvailableBalance() < gia) {
                return false;
            }

            // 5. Logic gia hạn thời gian (Anti-sniping)
            LocalDateTime now = LocalDateTime.now();
            long thoiGianConLai = Duration.between(now, auction.getEndTime()).getSeconds();

            if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
                auction.setEndTime(auction.getEndTime().plusSeconds(30));
                auctionScheduler.setTimeCancelAuction(auction); // Cập nhật lại lịch đóng phiên
            }

            // 6. Cập nhật thông tin đấu giá
            if (!auction.isHasBid()) {
                auction.setHasBid(true);
            }

            // Hoàn lại tiền cho người trả giá cao nhất trước đó (nếu có)
            // FIX BUG #4: Use snapshot of bidders list BEFORE adding new bidder
            // This correctly identifies the previous leader for refund
            List<User> biddersBeforeAdd = auction.getBidderList();
            double oldPrice = auction.getCurrentPrice(); // lưu giá cũ TRƯỚC khi thêm bidder

            // Thêm bidder vào danh sách
            auction.addBidder(bidder);

            if (biddersBeforeAdd.size() >= 1) { // size TRƯỚC khi add → người trước đó
                User previousLeader = biddersBeforeAdd.get(biddersBeforeAdd.size() - 1);

                if (!previousLeader.getUserId().equals(bidder.getUserId())) {
                    // Người khác đang dẫn đầu → hoàn tiền cho họ
                    previousLeader.setAvailableBalance(
                            previousLeader.getAvailableBalance() + oldPrice
                    );
                    logger.info("💰 Hoàn " + oldPrice + " cho " + previousLeader.getFullName());
                } else {
                    // Cùng người đặt lại: chỉ hoàn phần chênh lệch
                    // Họ đã bị trừ oldPrice trước đó, giờ đặt gia mới hơn
                    // Không cần hoàn vì họ đang nâng giá của chính mình
                    logger.info("ℹ️ Cùng user đặt lại, không hoàn tiền");
                }
            }

            // Trừ tiền người đặt giá hiện tại
            bidder.setAvailableBalance(bidder.getAvailableBalance() - gia);

            // Cập nhật giá và bước giá
            auction.setCurrentPrice(gia);
            auction.setPriceStep(gia * auction.getMinPriceDiffRatio());
            return true;
        }
    }
}