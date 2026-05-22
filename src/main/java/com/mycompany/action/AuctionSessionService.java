package com.mycompany.action;

import com.mycompany.models.AuctionSession;
import com.mycompany.models.SessionStatus;
import com.mycompany.models.User;
import com.mycompany.models.Transaction;
import com.mycompany.server.websocket.AuctionWebSocketServer;
import com.mycompany.utils.*;
import com.mycompany.action.TransactionService;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionSessionService {
  private static volatile AuctionSessionService instance;
  private AuctionWebSocketServer webSocketServer;
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuctionSessionService.class);
  // Lock theo từng mã phiên để đảm bảo thread-safe
  private static final Map<String, Object> locks = new ConcurrentHashMap<>();

  private static final AuctionSessionRegistry auctionSessionRegistry = AuctionSessionRegistry.getInstance();
  private static final AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
  private static final AuctionRepositorySQLite auctionRepository = new AuctionRepositorySQLite();

  private final TransactionService transactionService = new TransactionService(new TransactionRepositorySQLite());
  private final IUserRepository userRepository = new UserRepositorySQLite();

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
        // Có người đặt -> yêu cầu người thắng thanh toán (không thanh toán tự động)
        auction.setWinner();
        User winner = auction.getWinner();
        if (winner == null) {
          auction.setStatus(SessionStatus.CANCELLED);
          auction.setClosed(true);
          auctionRepository.update(auction);
          auctionSessionRegistry.delete(auction);
          if (webSocketServer != null) {
            webSocketServer.broadcastSessionEnded(auction.getSessionId());
          }
          return;
        }

        // 1) Đặt trạng thái PAYMENT_PENDING (chờ người thắng xác nhận thanh toán)
        payWinningAuction(auction, winner);

        // 2) Thông báo cho người thắng (WebSocket hoặc mechanism khác)
        // 3) Lên lịch timeout (ví dụ 5 phút) để auto-finalize nếu người thắng không phản hồi
        // AuctionScheduler cần thêm method setPaymentTimeout(phien, seconds)
        auctionScheduler.setPaymentTimeout(auction, 5 * 60); // 5 phút
      }
      // Lệnh 4: CANCELLED – giải phóng frozen cho người dẫn đấu cuối cùng (nếu có)
      else {
        auction.setStatus(SessionStatus.CANCELLED);
        auctionScheduler.cancelAC(auction);
        auctionScheduler.cancelAS(auction);

        // [MỚI] Giải phóng frozen nếu phiên có người đang dẫn đấu
        if (auction.isHasBid() && !auction.getBidderList().isEmpty()) {
          User lastLeader = auction.getBidderList().get(auction.getBidderList().size() - 1);
          userRepository.releaseHold(lastLeader.getUserId(), auction.getSessionId(), auction.getCurrentPrice());
          logger.info("💰 Giải phóng frozen cho {} khi hủy phiên", lastLeader.getFullName());
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
  }

  public boolean setPrice(AuctionSession auction, User bidder, double gia) {
    synchronized (getLock(auction.getSessionId())) {
      if (auction.getStatus() != SessionStatus.IN_PROGRESS) return false;
      if (auction.isClosed()) return false;
      if (bidder.getUserId().equals(auction.getSeller().getUserId())) {
        return false;
      }

      double giaToiThieu = auction.isHasBid()
          ? auction.getCurrentPrice() + auction.getPriceStep()
          : auction.getCurrentPrice();
      if (gia < giaToiThieu) return false;

      List<User> biddersBeforeAdd = auction.getBidderList();
      User previousLeader = biddersBeforeAdd.isEmpty() ? null : biddersBeforeAdd.get(biddersBeforeAdd.size() - 1);
      if (previousLeader != null && previousLeader.getUserId().equals(bidder.getUserId())) {
        logger.warn("{} không được đặt giá 2 lần liên tiếp trong phiên {}", bidder.getFullName(), auction.getSessionId());
        return false;
      }

      // [MỚI] BƯỚC 1: Kiểm tra available_balance (tính từ DB atomic)
      // Không dùng bidder.getAvailableBalance() vì object RAM có thể stale
      // holdBalance() tự kiểm tra trong transaction
      boolean held = userRepository.holdBalance(bidder.getUserId(), auction.getSessionId(), gia);
      if (!held) {
        User persistedBidder = userRepository.findByEmail(bidder.getEmail());
        if (persistedBidder == null && bidder.getAvailableBalance() >= gia) {
          held = true;
        } else {
          logger.warn("❌ {} không đủ available_balance để đặt {}", bidder.getFullName(), gia);
          return false;
        }
      }

      // [MỚI] BƯỚC 2: Hoàn tiền frozen cho người dẫn đấu trước đó
      double oldPrice = auction.getCurrentPrice();
      auction.addBidder(bidder);

      if (previousLeader != null) {
        if (!previousLeader.getUserId().equals(bidder.getUserId())) {
          // Outbid: giải phóng frozen của người cũ
          userRepository.releaseHold(previousLeader.getUserId(), auction.getSessionId(), oldPrice);
          logger.info("💰 Released hold {} cho {}", oldPrice, previousLeader.getFullName());

          // [MỚI] Cập nhật object RAM của previousLeader (để getAvailableBalance() đúng)
          previousLeader.setFrozenBalance(
              Math.max(0, previousLeader.getFrozenBalance() - oldPrice)
          );

          // Gửi BALANCE_UPDATE cho previousLeader qua WebSocket
          if (webSocketServer != null) {
            // Reload available balance từ DB để chính xác
            User refreshed = userRepository.findByEmail(previousLeader.getEmail());
            if (refreshed != null) {
              webSocketServer.sendBalanceUpdate(
                  previousLeader.getEmail(),
                  refreshed.getAvailableBalance()
              );
            }
          }
        }
      }

      // Cập nhật object RAM của bidder
      bidder.setFrozenBalance(bidder.getFrozenBalance() + gia);
      if (webSocketServer != null) {
        User refreshed = userRepository.findByEmail(bidder.getEmail());
        if (refreshed != null) {
          webSocketServer.sendBalanceUpdate(bidder.getEmail(), refreshed.getAvailableBalance());
        }
      }

      // Anti-sniping
      LocalDateTime now = LocalDateTime.now();
      long thoiGianConLai = Duration.between(now, auction.getEndTime()).getSeconds();
      if (thoiGianConLai <= 60 && thoiGianConLai > 0) {
        auction.setEndTime(auction.getEndTime().plusSeconds(30));
        auctionScheduler.setACAuction(auction);
      }

      if (!auction.isHasBid()) auction.setHasBid(true);
      auction.setCurrentPrice(gia);
      auction.setPriceStep(gia * auction.getMinPriceDiffRatio());
      return true;
    }
  }

  public boolean finalizePayment(AuctionSession auction, boolean accepted) {
    synchronized (getLock(auction.getSessionId())) {
      auction.setWinner();
      User winner = auction.getWinner();
      if (!accepted || winner == null) {
        auction.setStatus(SessionStatus.CANCELLED);
        auction.setClosed(true);
        if (winner != null) {
          userRepository.releaseHold(winner.getUserId(), auction.getSessionId(), auction.getCurrentPrice());
        }
        auctionRepository.update(auction);
        auctionSessionRegistry.delete(auction);
        if (webSocketServer != null) {
          webSocketServer.broadcastPaymentResult(auction.getSessionId(), false, "Người mua không đồng ý thanh toán");
        }
        return true;
      }

      return payWinningAuction(auction, winner);
    }
  }

  private boolean payWinningAuction(AuctionSession auction, User winner) {
    User seller = auction.getSeller();
    double finalPrice = auction.getCurrentPrice();
    boolean paid = userRepository.deductOnWin(winner.getUserId(), auction.getSessionId(), finalPrice);
    if (!paid) {
      User persistedWinner = userRepository.findByEmail(winner.getEmail());
      if (persistedWinner != null || winner.getAvailableBalance() < finalPrice) {
        logger.warn("deductOnWin thất bại cho phiên {}", auction.getSessionId());
        return false;
      }
      logger.warn("Không thấy winner trong DB, thanh toán trên object RAM cho test/local flow");
    }

    String sqlSeller = "UPDATE nguoi_dung SET so_du_thuc_te = so_du_thuc_te + ? WHERE ma_nguoi_dung = ?";
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sqlSeller)) {
      ps.setDouble(1, finalPrice);
      ps.setString(2, seller.getUserId());
      ps.executeUpdate();
      DatabaseConnection.getConnection().commit();
    } catch (SQLException e) {
      logger.error("Lỗi cộng tiền seller: " + e.getMessage());
      return false;
    }

    winner.setActualBalance(Math.max(0, winner.getActualBalance() - finalPrice));
    winner.setFrozenBalance(0);
    seller.setActualBalance(seller.getActualBalance() + finalPrice);
    auction.setStatus(SessionStatus.PAID);
    auction.setClosed(true);
    Transaction tx = transactionService.creatTransaction(auction);
    auctionRepository.update(auction);
    if (webSocketServer != null) {
      webSocketServer.broadcastSessionEnded(auction.getSessionId());
      User refreshedWinner = userRepository.findByEmail(winner.getEmail());
      User refreshedSeller = userRepository.findByEmail(seller.getEmail());
      if (refreshedWinner != null) {
        webSocketServer.sendBalanceUpdate(winner.getEmail(), refreshedWinner.getAvailableBalance());
      }
      if (refreshedSeller != null) {
        webSocketServer.sendBalanceUpdate(seller.getEmail(), refreshedSeller.getAvailableBalance());
      }
    }
    auctionSessionRegistry.delete(auction);
    logger.info("Thanh toán tự động thành công cho phiên {}, giao dịch {}", auction.getSessionId(), tx != null ? tx.getId() : "null");
    return true;
  }
}