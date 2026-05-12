package com.mycompany.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lớp quản lý kết nối CSDL SQLite sử dụng ThreadLocal để tối ưu đa luồng.
 * Tích hợp cơ chế WAL và Shutdown Hook để bảo vệ dữ liệu.
 */
public class KetNoiCSDL {
    private static final Logger logger = LoggerFactory.getLogger(KetNoiCSDL.class);
    private static final String URL = "jdbc:sqlite:hipiti.db";

    // ThreadLocal giúp mỗi luồng sở hữu 1 Connection riêng, tránh tranh chấp.
    private static final ThreadLocal<Connection> CONNECTION =
            ThreadLocal.withInitial(() -> {
                try {
                    Connection conn = DriverManager.getConnection(URL);
                    caiDatPragma(conn);
                    return conn;
                } catch (SQLException e) {
                    throw new RuntimeException("Không thể khởi tạo Connection cho luồng: " + Thread.currentThread().getName(), e);
                }
            });

    // Tập hợp lưu trữ tất cả Connection để đóng khi tắt ứng dụng
    private static final Set<Connection> tatCaConnection =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Flag để đảm bảo Shutdown Hook chỉ đăng ký 1 lần
    private static boolean isHookRegistered = false;

    /**
     * Lấy kết nối của luồng hiện tại.
     */
    public static Connection layKetNoi() throws SQLException {
        try {
            Connection conn = CONNECTION.get();

            // Kiểm tra nếu connection bị đóng bất ngờ thì tạo mới
            if (conn == null || conn.isClosed()) {
                CONNECTION.remove();
                conn = CONNECTION.get();
            }

            // Đăng ký vào Set để theo dõi toàn cục
            tatCaConnection.add(conn);
            return conn;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Cấu hình SQLite để chạy tối ưu (WAL mode, Busy Timeout, Foreign Keys).
     */
    private static void caiDatPragma(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // WAL mode: Cho phép nhiều luồng đọc trong khi 1 luồng đang ghi
            stmt.execute("PRAGMA journal_mode=WAL;");
            // Đợi tối đa 5s nếu file DB đang bị khóa ghi
            stmt.execute("PRAGMA busy_timeout=5000;");
            // Bật ràng buộc khóa ngoại
            stmt.execute("PRAGMA foreign_keys=ON;");
            // Normal giúp cân bằng giữa tốc độ và an toàn dữ liệu khi dùng WAL
            stmt.execute("PRAGMA synchronous=NORMAL;");
        }

        // Tắt AutoCommit để kiểm soát Transaction thủ công
        conn.setAutoCommit(false);
        logger.debug("✅ Đã cài đặt PRAGMA và tắt AutoCommit cho Connection");
    }

    /**
     * Khởi tạo các bảng dữ liệu và thực hiện migration nếu cần.
     * Cần được gọi 1 lần khi khởi động App.
     */
    public static void khoiTao() {
        // Đăng ký Shutdown Hook để đóng kết nối sạch sẽ khi đóng App (JVM tắt)
        dangKyShutdownHook();

        String sqlNguoiDung = "CREATE TABLE IF NOT EXISTS nguoi_dung (" +
                "ma_nguoi_dung TEXT PRIMARY KEY, " +
                "ho_ten TEXT NOT NULL, " +
                "thu_dien_tu TEXT UNIQUE NOT NULL, " +
                "mat_khau TEXT NOT NULL, " +
                "salt TEXT NOT NULL, " + // Cột salt cho bảo mật
                "ngay_sinh TEXT, " +
                "dia_chi TEXT, " +
                "so_dien_thoai TEXT, " +
                "so_du_kha_dung REAL DEFAULT 0);";

        String sqlSanPham = "CREATE TABLE IF NOT EXISTS san_pham (" +
                "ma_san_pham TEXT PRIMARY KEY, " +
                "ten_san_pham TEXT NOT NULL);";

        String sqlPhien = "CREATE TABLE IF NOT EXISTS phien_dau_gia (" +
                "ma_phien TEXT PRIMARY KEY, " +
                "ten_phien TEXT NOT NULL, " +
                "gia_hien_tai REAL NOT NULL, " +
                "buoc_gia REAL DEFAULT 0, " +
                "thoi_gian_bat_dau TEXT, " +
                "thoi_gian_ket_thuc TEXT, " +
                "trang_thai TEXT NOT NULL, " +
                "ma_nguoi_ban TEXT, " +
                "ma_nguoi_thang_cuoc TEXT, " +
                "ma_san_pham TEXT, " +
                "is_closed INTEGER DEFAULT 0, " +
                "FOREIGN KEY (ma_nguoi_ban) REFERENCES nguoi_dung(ma_nguoi_dung), " +
                "FOREIGN KEY (ma_nguoi_thang_cuoc) REFERENCES nguoi_dung(ma_nguoi_dung), " +
                "FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham));";

        String sqlGiaoDich = "CREATE TABLE IF NOT EXISTS giao_dich (" +
                "ma_giao_dich TEXT PRIMARY KEY, " +
                "ma_phien TEXT NOT NULL, " +
                "trang_thai TEXT NOT NULL, " +
                "thoi_gian_tao TEXT NOT NULL, " +
                "FOREIGN KEY (ma_phien) REFERENCES phien_dau_gia(ma_phien));";

        String sqlNguoiTraGia = "CREATE TABLE IF NOT EXISTS nguoi_tra_gia (" +
                "ma_phien TEXT, " +
                "ma_nguoi_dung TEXT, " +
                "gia_tra REAL, " +
                "thoi_gian TEXT, " +
                "PRIMARY KEY (ma_phien, ma_nguoi_dung, thoi_gian), " +
                "FOREIGN KEY (ma_phien) REFERENCES phien_dau_gia(ma_phien), " +
                "FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung));";

        try (Connection conn = layKetNoi();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlNguoiDung);

            // Logic Migration: Thêm cột salt nếu database cũ chưa có
            try {
                stmt.execute("ALTER TABLE nguoi_dung ADD COLUMN salt TEXT;");
                logger.info("✅ Migration: Đã thêm cột salt vào bảng nguoi_dung");
            } catch (SQLException e) {
                // Lỗi này thường là do cột đã tồn tại, có thể bỏ qua
                logger.debug("ℹ️ Cột salt đã tồn tại.");
            }

            stmt.execute(sqlSanPham);
            stmt.execute(sqlPhien);
            stmt.execute(sqlGiaoDich);
            stmt.execute(sqlNguoiTraGia);

            conn.commit(); // Quan trọng: Phải commit vì auto-commit đã tắt
            logger.info("✅ Khởi tạo Database thành công.");
        } catch (SQLException e) {
            logger.error("❌ Lỗi khởi tạo DB: " + e.getMessage());
        }
    }

    /**
     * Đóng tất cả kết nối đã được mở bởi ứng dụng.
     */
    public static void dongTatCaKetNoi() {
        logger.info("Đang đóng tất cả database connections...");
        for (Connection conn : tatCaConnection) {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error("Lỗi khi đóng connection: " + e.getMessage());
            }
        }
        tatCaConnection.clear();
        logger.info("✅ Đã giải phóng toàn bộ tài nguyên Database.");
    }

    /**
     * Đóng kết nối của luồng hiện tại và giải phóng khỏi ThreadLocal.
     */
    public static void dongKetNoiHienTai() {
        try {
            Connection conn = CONNECTION.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
                tatCaConnection.remove(conn);
            }
        } catch (SQLException e) {
            logger.error("Lỗi đóng kết nối luồng hiện tại: " + e.getMessage());
        } finally {
            CONNECTION.remove();
        }
    }

    private static synchronized void dangKyShutdownHook() {
        if (!isHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                dongTatCaKetNoi();
            }));
            isHookRegistered = true;
            logger.debug("✅ Đã đăng ký Shutdown Hook.");
        }
    }
}