package com.mycompany.action;

import com.mycompany.models.*;
import com.mycompany.utils.ITransactionRepository;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionService transactionService;
    private ITransactionRepository mockRepo;
    private AuctionSession auction;
    private User seller;
    private User winner;

    @BeforeEach
    void setUp() {
        mockRepo = mock(ITransactionRepository.class);
        transactionService = new TransactionService(mockRepo);

        seller = new User("Nguoi Ban", "seller@test.com", "pass", "1990-01-01");
        seller.setUserId("SELLER001");
        seller.setAvailableBalance(0);

        winner = new User("Nguoi Thang", "winner@test.com", "pass", "1995-01-01");
        winner.setUserId("WINNER001");
        winner.setAvailableBalance(10_000_000);

        Product product = new Product("Laptop Dell", "SP001");
        auction = new AuctionSession("PH001", "Auction Test", product, 5_000_000, seller, 300);
        auction.setStatus(SessionStatus.PAID);
        auction.setWinner(winner);
    }

    // ===== creatTransaction() =====

    @Test
    @DisplayName("creatTransaction() phiên PAID có winner phải tạo transaction thành công")
    void creatTransaction_paidWithWinner_shouldSucceed() {
        Transaction tx = transactionService.creatTransaction(auction);
        assertNotNull(tx);
        assertEquals(TransactionStatus.PAID, tx.getStatus());
    }

    @Test
    @DisplayName("creatTransaction() phải gọi repository.save()")
    void creatTransaction_shouldCallRepositorySave() {
        transactionService.creatTransaction(auction);
        verify(mockRepo).save(any(Transaction.class));
    }

    @Test
    @DisplayName("creatTransaction() phiên chưa kết thúc (IN_PROGRESS) phải trả null")
    void creatTransaction_notPaid_shouldReturnNull() {
        auction.setStatus(SessionStatus.IN_PROGRESS);
        Transaction tx = transactionService.creatTransaction(auction);
        assertNull(tx);
    }

    @Test
    @DisplayName("creatTransaction() không có winner phải trả null")
    void creatTransaction_noWinner_shouldReturnNull() {
        auction.setWinner(null);
        Transaction tx = transactionService.creatTransaction(auction);
        assertNull(tx);
    }

    @Test
    @DisplayName("creatTransaction() phiên CANCELLED phải trả null")
    void creatTransaction_cancelled_shouldReturnNull() {
        auction.setStatus(SessionStatus.CANCELLED);
        Transaction tx = transactionService.creatTransaction(auction);
        assertNull(tx);
    }

    // ===== confirmPayment() =====

    @Test
    @DisplayName("confirmPayment() thành công phải trừ tiền winner và cộng tiền seller")
    void confirmPayment_success_shouldTransferMoney() {
        Transaction tx = transactionService.creatTransaction(auction);
        double winnerBefore = winner.getAvailableBalance();
        double sellerBefore = seller.getAvailableBalance();
        double price = auction.getCurrentPrice();

        transactionService.confirmPayment(tx);

        assertEquals(winnerBefore - price, winner.getAvailableBalance(), 0.01);
        assertEquals(sellerBefore + price, seller.getAvailableBalance(), 0.01);
    }

    @Test
    @DisplayName("confirmPayment() thành công phải đổi status sang PAID")
    void confirmPayment_success_shouldSetStatusPaid() {
        Transaction tx = transactionService.creatTransaction(auction);
        transactionService.confirmPayment(tx);
        assertEquals(TransactionStatus.PAID, tx.getStatus());
    }

    @Test
    @DisplayName("confirmPayment() số dư không đủ phải trả false")
    void confirmPayment_insufficientBalance_shouldReturnFalse() {
        winner.setAvailableBalance(100); // Ít hơn giá đấu giá
        Transaction tx = transactionService.creatTransaction(auction);
        boolean result = transactionService.confirmPayment(tx);
        assertFalse(result);
    }

    @Test
    @DisplayName("confirmPayment() transaction null phải trả false")
    void confirmPayment_nullTransaction_shouldReturnFalse() {
        assertFalse(transactionService.confirmPayment(null));
    }

    // ===== refund() =====

    @Test
    @DisplayName("refund() sau khi PAID phải hoàn tiền cho winner và trừ tiền seller")
    void refund_afterPaid_shouldRefundWinner() {
        Transaction tx = transactionService.creatTransaction(auction);
        transactionService.confirmPayment(tx); // Thanh toán trước

        double winnerAfterPayment = winner.getAvailableBalance();
        double price = auction.getCurrentPrice();

        transactionService.refund(tx, "Hàng bị lỗi");

        assertEquals(winnerAfterPayment + price, winner.getAvailableBalance(), 0.01);
        assertEquals(TransactionStatus.REFUNDED, tx.getStatus());
    }

    @Test
    @DisplayName("refund() transaction đã REFUNDED không được hoàn lần 2")
    void refund_alreadyRefunded_shouldReturnFalse() {
        Transaction tx = transactionService.creatTransaction(auction);
        transactionService.refund(tx, "Lần 1");
        boolean result = transactionService.refund(tx, "Lần 2");
        assertFalse(result, "Không được hoàn tiền 2 lần");
    }

    @Test
    @DisplayName("refund() transaction null phải trả false")
    void refund_nullTransaction_shouldReturnFalse() {
        assertFalse(transactionService.refund(null, "reason"));
    }
}