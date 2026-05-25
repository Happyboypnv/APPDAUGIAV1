package com.mycompany.action;

import com.mycompany.models.*;
import com.mycompany.utils.ITransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TransactionService {
  private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
  private final ITransactionRepository transactionRepository;

  public TransactionService(ITransactionRepository kho) {
    this.transactionRepository = kho;
  }

  // =========================================================
  // CHỨC NĂNG 1: TẠO GIAO DỊCH MỚI
  // =========================================================
  public Transaction creatTransaction(AuctionSession auction) {
    if (auction.getStatus() != SessionStatus.PAID) {
      logger.info("[creatTransaction] Phiên chưa kết thúc hoặc bị hủy. Trạng thái: {}", auction.getStatus());
      return null;
    }

    User winner = auction.getWinner();
    if (winner == null) {
      logger.info("[creatTransaction] Không thể tạo: không có người đặt giá.");
      return null;
    }

    Transaction newTransaction = new Transaction("TEMP", auction);
    newTransaction.setStatus(TransactionStatus.PAID);
    winner.addTransaction(newTransaction);
    auction.getSeller().addTransaction(newTransaction);
    transactionRepository.save(newTransaction);

    logger.info("========================================");
    logger.info("[creatTransaction] Giao dịch tạo thành công!");
    logger.info("  Sản phẩm  : {}", auction.getProduct().getProductName());
    logger.info("  Người bán : {}", auction.getSeller().getFullName());
    logger.info("  Người mua : {}", winner.getFullName());
    // FIX: Sử dụng String.format để định dạng số tiền
    logger.info("  Giá chốt  : {} VNĐ", String.format("%,.0f", auction.getCurrentPrice()));
    logger.info("  Trạng thái: ĐÃ THANH TOÁN");
    logger.info("========================================");

    return newTransaction;
  }

  // =========================================================
  // CHỨC NĂNG 2: XÁC NHẬN THANH TOÁN
  // =========================================================
  public boolean confirmPayment(Transaction Transaction) {
    if (Transaction == null) {
      logger.info("[confirmPayment] Giao dịch không tồn tại.");
      return false;
    }

    User winner = Transaction.getAuctionSession().getWinner();
    User seller   = Transaction.getAuctionSession().getSeller();
    double endPrice       = Transaction.getAuctionSession().getCurrentPrice();

    if (winner.getAvailableBalance() < endPrice) {
      // FIX: Sử dụng String.format
      logger.info("[confirmPayment] Số dư không đủ. Cần: {} | Có: {} VNĐ",
          String.format("%,.0f", endPrice),
          String.format("%,.0f", winner.getAvailableBalance()));
      return false;
    }

    winner.setActualBalance(winner.getActualBalance() - endPrice);
    winner.setFrozenBalance(Math.max(0, winner.getFrozenBalance() - endPrice));
    seller.setActualBalance(seller.getActualBalance() + endPrice);
    Transaction.setStatus(TransactionStatus.PAID);
    transactionRepository.save(Transaction);

    logger.info("========================================");
    logger.info("[confirmPayment] Thanh toán thành công!");
    logger.info("  Giao dịch : {}", Transaction.getId());
    // FIX: Định dạng số tiền
    logger.info("  Số tiền   : {} VNĐ", String.format("%,.0f", endPrice));
    logger.info("  Người mua : {} | Số dư còn: {} VNĐ",
        winner.getFullName(),
        String.format("%,.0f", winner.getAvailableBalance()));
    logger.info("  [Thông báo -> {}]: Bạn vừa nhận thanh toán cho giao dịch {}",
        seller.getFullName(), Transaction.getId());
    logger.info("  Số dư người bán: {} VNĐ", String.format("%,.0f", seller.getAvailableBalance()));
    logger.info("========================================");

    return true;
  }

  // =========================================================
  // CHỨC NĂNG 3: HOÀN TIỀN
  // =========================================================
  public boolean refund(Transaction Transaction, String lyDo) {
    if (Transaction == null) {
      logger.info("[refun] Giao dịch không tồn tại.");
      return false;
    }

    if (Transaction.getStatus() == TransactionStatus.REFUNDED) {
      logger.info("[refun] Giao dịch đã được hoàn tiền trước đó.");
      return false;
    }

    User winner = Transaction.getAuctionSession().getWinner();
    User seller   = Transaction.getAuctionSession().getSeller();
    double refundAmount    = Transaction.getAuctionSession().getCurrentPrice();

    if (winner == null) {
      logger.info("[refun] Không tìm thấy người cần hoàn tiền.");
      return false;
    }

    winner.setActualBalance(winner.getActualBalance() + refundAmount);
    if (seller != null) {
      seller.setActualBalance(seller.getActualBalance() - refundAmount);
    }
    Transaction.setStatus(TransactionStatus.REFUNDED);
    transactionRepository.update(Transaction);

    logger.info("========================================");
    logger.info("[refun] Hoàn tiền thành công!");
    logger.info("  Giao dịch       : {}", Transaction.getId());
    logger.info("  Người nhận hoàn : {}", winner.getFullName());
    // FIX: Định dạng số tiền
    logger.info("  Số tiền hoàn    : {} VNĐ", String.format("%,.0f", refundAmount));
    logger.info("  Số dư sau hoàn  : {} VNĐ", String.format("%,.0f", winner.getAvailableBalance()));
    logger.info("  Lý do           : {}", lyDo);
    logger.info("========================================");

    return true;
  }

  // =========================================================
  // CHỨC NĂNG 4: XEM LỊCH SỬ GIAO DỊCH
  // =========================================================
  public List<Transaction> viewTransactionHistory(String UserID, int page, int quantity) {
    List<Transaction> allTransaction = transactionRepository.findByUserId(UserID);

    if (allTransaction.isEmpty()) {
      logger.info("[xemLichSu] Người dùng [{}] chưa có giao dịch nào.", UserID);
      return allTransaction;
    }

    int totalPage = (int) Math.ceil((double) allTransaction.size() / quantity);
    int start    = (page - 1) * quantity;

    if (start >= allTransaction.size()) {
      logger.info("[xemLichSu] Không có dữ liệu ở page {} (tổng {} page).", page, totalPage);
      return List.of();
    }

    int end = Math.min(start + quantity, allTransaction.size());
    List<Transaction> currentPage = allTransaction.subList(start, end);

    logger.info("==========================================");
    logger.info("         LỊCH SỬ GIAO DỊCH               ");
    logger.info("==========================================");
    logger.info("  Người dùng : {}", UserID);
    // FIX: Định dạng dòng tiêu đề page
    logger.info("  page {} / {}  |  Tổng: {} giao dịch", page, totalPage, allTransaction.size());
    logger.info("------------------------------------------");

    for (int i = 0; i < currentPage.size(); i++) {
      Transaction gd        = currentPage.get(i);
      AuctionSession auction  = gd.getAuctionSession();
      User bidder = auction.getWinner();

      // FIX: Sử dụng placeholder {} thay vì %d, %s và định dạng tiền
      logger.info("  [{}] Mã GD     : {}", start + i + 1, gd.getId());
      logger.info("      Ngày tạo  : {}", gd.getCreatedAt());
      logger.info("      Sản phẩm  : {}", auction.getProduct().getProductName());
      logger.info("      Người bán : {}", auction.getSeller().getFullName());
      logger.info("      Người mua : {}", (bidder != null ? bidder.getFullName() : "Không có"));
      logger.info("      Số tiền   : {} VNĐ", String.format("%,.0f", auction.getCurrentPrice()));
      logger.info("      Trạng thái: {}", gd.getStatus().name());
      logger.info("  ------------------------------------------");
    }

    return currentPage;
  }
}
