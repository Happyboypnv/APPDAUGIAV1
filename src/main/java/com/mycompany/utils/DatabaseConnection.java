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
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String URL = "jdbc:sqlite:hipiti.db";

    // ThreadLocal giúp mỗi luồng sở hữu 1 Connection riêng, tránh tranh chấp.
    private static final ThreadLocal<Connection> CONNECTION =
        ThreadLocal.withInitial(() -> {
            try {
                Connection conn = DriverManager.getConnection(URL);
                setPragma(conn);
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
     * Force connection của thread hiện tại đọc lại data mới nhất từ DB.
     * Dùng khi cần đảm bảo thấy data vừa được commit bởi thread khác (server thread).
     * Cách hoạt động: rollback transaction rỗng → SQLite bắt đầu snapshot mới,
     * thấy được tất cả data đã commit trước đó.
     */
    public static void refreshConnection() {
        try {
            Connection conn = CONNECTION.get();
            if (conn != null && !conn.isClosed()) {
                conn.rollback(); // reset snapshot → thấy committed data mới nhất
            }
        } catch (SQLException e) {
            logger.warn("Lỗi refresh connection: " + e.getMessage());
        }
    }

    /**
     */
    public static Connection getConnection() throws SQLException {
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
    private static void setPragma(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // WAL mode: Cho phép nhiều luồng đọc trong khi 1 luồng đang ghi
            stmt.execute("PRAGMA journal_mode=WAL;");
            // Đợi tối đa 5s nếu file DB đang bị khóa ghi
            stmt.execute("PRAGMA busy_timeout=10000;");
            // Bật ràng buộc khóa ngoại
            stmt.execute("PRAGMA foreign_keys=ON;");
            // Normal giúp cân bằng giữa tốc độ và an toàn dữ liệu khi dùng WAL
            stmt.execute("PRAGMA synchronous=NORMAL;");
            //
            stmt.execute("PRAGMA wal_autocheckpoint=100;");
        }

        // Tắt AutoCommit để kiểm soát Transaction thủ công
        conn.setAutoCommit(false);
        logger.debug("✅ Đã cài đặt PRAGMA và tắt AutoCommit cho Connection");
    }

    /**
     * Khởi tạo các bảng dữ liệu và thực hiện migration nếu cần.
     * Cần được gọi 1 lần khi khởi động App.
     */
    public static void initialize() {
        // Đăng ký Shutdown Hook để đóng kết nối sạch sẽ khi đóng App (JVM tắt)
        registerShutdownHook();

        String sqlNguoiDung = "CREATE TABLE IF NOT EXISTS nguoi_dung (" +
            "ma_nguoi_dung TEXT PRIMARY KEY, " +
            "ho_ten TEXT NOT NULL, " +
            "thu_dien_tu TEXT UNIQUE NOT NULL, " +
            "mat_khau TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "ngay_sinh TEXT, " +
            "dia_chi TEXT, " +
            "so_dien_thoai TEXT, " +
            "so_du_kha_dung REAL DEFAULT 0, " +
            "so_tai_khoan_ngan_hang TEXT, " +
            "ten_ngan_hang TEXT, " +
            "duong_dan_avatar TEXT, " +
                "role INTEGER);";

        String sqlSanPham = "CREATE TABLE IF NOT EXISTS san_pham (" +
            "ma_san_pham TEXT PRIMARY KEY, " +
            "ten_san_pham TEXT NOT NULL, " +
                "mo_ta TEXT, " +
                "phan_loai);";

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
                "is_accepted INTEGER DEFAULT -1, " +
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
        String sqlBids = "CREATE TABLE IF NOT EXISTS bids (" +
                "bid_id TEXT PRIMARY KEY, " +
                "session_id TEXT NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "bid_time TEXT NOT NULL" +
                ");";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sqlNguoiDung);

            try {
                stmt.execute("ALTER TABLE nguoi_dung ADD COLUMN role INTEGER;");
                logger.info(" Thêm role thành công vào bảng người dùng!");
            } catch (SQLException e) {
                logger.debug("Cột role đã tồn tại");
            }

            try {
                stmt.execute("ALTER TABLE phien_dau_gia ADD COLUMN is_accepted INTEGER DEFAULT -1;");
                logger.info(" Thêm is_accepted thành công vào bảng phiên!");
            } catch (SQLException e) {
                logger.debug("Cột is_accepted đã tồn tại");
            }

            try {
                stmt.execute("ALTER TABLE san_pham ADD COLUMN mo_ta TEXT;");
                logger.info(" Thêm mo_ta thành công vào bảng san_pham!");
            } catch (SQLException e) {
                logger.debug("Cột mo_ta đã tồn tại");
            }

            try {
                stmt.execute("ALTER TABLE san_pham ADD COLUMN phan_loai TEXT;");
                logger.info(" Thêm phan_loai thành công vào bảng san pham!");
            } catch (SQLException e) {
                logger.debug("Cột phan_loai đã tồn tại");
            }

            stmt.execute(sqlSanPham);
            stmt.execute(sqlPhien);
            stmt.execute(sqlGiaoDich);
            stmt.execute(sqlNguoiTraGia);
            stmt.execute(sqlBids);

            conn.commit();
            logger.info("Khởi tạo Database thành công.");
        } catch (SQLException e) {
            logger.error("Lỗi khởi tạo DB: " + e.getMessage());
        }
    }

    /**
     * Đóng tất cả kết nối đã được mở bởi ứng dụng.
     */
    public static void closeAll() {
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
    public static void closeCurrent() {
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

    private static synchronized void registerShutdownHook() {
        if (!isHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                closeAll();
            }));
            isHookRegistered = true;
            logger.debug("✅ Đã đăng ký Shutdown Hook.");
        }
    }
}