package com.mycompany.helper;

import java.sql.*;

/**
 * Helper để tạo database SQLite in-memory cho test.
 * Mỗi test class dùng một instance riêng để tránh xung đột.
 */
public class TestDatabaseHelper {

    private static Connection inMemoryConnection;

    /**
     * Khởi tạo database in-memory với cùng schema của production.
     * Gọi trong @BeforeAll hoặc @BeforeEach.
     */
    public static void setupInMemoryDatabase() throws SQLException {
        // :memory: = SQLite in-memory, không ghi ra file
        inMemoryConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        inMemoryConnection.setAutoCommit(false);

        try (Statement stmt = inMemoryConnection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=ON;");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS nguoi_dung (
                    ma_nguoi_dung TEXT PRIMARY KEY,
                    ho_ten TEXT NOT NULL,
                    thu_dien_tu TEXT UNIQUE NOT NULL,
                    mat_khau TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    ngay_sinh TEXT,
                    dia_chi TEXT,
                    so_dien_thoai TEXT,
                    so_du_kha_dung REAL DEFAULT 0,
                    so_tai_khoan_ngan_hang TEXT,
                    ten_ngan_hang TEXT
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS san_pham (
                    ma_san_pham TEXT PRIMARY KEY,
                    ten_san_pham TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS phien_dau_gia (
                    ma_phien TEXT PRIMARY KEY,
                    ten_phien TEXT NOT NULL,
                    gia_hien_tai REAL NOT NULL,
                    buoc_gia REAL DEFAULT 0,
                    thoi_gian_bat_dau TEXT,
                    thoi_gian_ket_thuc TEXT,
                    trang_thai TEXT NOT NULL,
                    ma_nguoi_ban TEXT,
                    ma_nguoi_thang_cuoc TEXT,
                    ma_san_pham TEXT,
                    is_closed INTEGER DEFAULT 0,
                    FOREIGN KEY (ma_nguoi_ban) REFERENCES nguoi_dung(ma_nguoi_dung),
                    FOREIGN KEY (ma_san_pham) REFERENCES san_pham(ma_san_pham)
                )
            """);

            inMemoryConnection.commit();
        }
    }

    public static Connection getConnection() {
        return inMemoryConnection;
    }

    public static void tearDown() throws SQLException {
        if (inMemoryConnection != null && !inMemoryConnection.isClosed()) {
            inMemoryConnection.close();
        }
    }

    public static void clearAllTables() throws SQLException {
        try (Statement stmt = inMemoryConnection.createStatement()) {
            stmt.execute("DELETE FROM phien_dau_gia");
            stmt.execute("DELETE FROM san_pham");
            stmt.execute("DELETE FROM nguoi_dung");
            inMemoryConnection.commit();
        }
    }
}