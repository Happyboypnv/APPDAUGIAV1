package com.mycompany.utils;

import java.sql.*;


/// Moi luong mot Connection RIENG -> khong ai tranh chap ai
/// Connection la mot interface nhu duong dan giua code java va du lieu SQLite
/// -> khong can bat ky mot LOCK nao o tang Java
/// Concurrency do SQLite WAL xu ly o tang file
/// Khi bat WAL thi se xuat hien 3 file
/// 1. hipiti.db (du lieu goc)
/// 2. hipiti.db-wal (nhat ky ghi nhap)
/// 3. hipiti.db-shm (File chi muc chia se - Shared Memory)
/// + Khi doc (nhieu Connection cung doc) -> cac Connection doc lay du lieu goc tu file .db
/// -> nhin vao file .db-shm de xem co du lieu nao moi nam trong file .db-wal khong. Neu co se update du lieu moi
/// + Khi Ghi (mot Connection ghi) -> Cap nhat vao file .db-wal -> cap nhat chi muc vao .shm bao cho Connection khac
/// -> Khi 2 Connection ghi -> neu co 1 Connection ghi thi Connection khac se khong the ghi du lieu
/// Nhieu Connection doc cung luc
/// 1 Connection ghi va nhieu Connection doc
/// 2 Connection cung ghi -> 1 cai phai cho
public class KetNoiCSDL {
    /// URL Ket noi JDBC voi SQLite
    /// jdbc:sqlite:hipiti.db -> tao ra file hipiti.db trong thu muc chay app
    private static final String URL = "jdbc:sqlite:hipiti.db";
    /// ThreadLocal Connection
    /// ThreadLocal<Connection> -> moi luong mot Connection rieng biet
    /// Cach ThreadLocal hoat dong ben trong JVM:
    /// Moi Thread co 1 Map noi bo (ThreadLocalMap):
    /// + key : doi tuong ThreadLocal nay
    /// + value : Connection cua luong do
    /// Thread A goi get() -> JVM tim trong map cua A -> tra ve connA
    /// Thread B goi get() -> JVM tim trong map cua B -> tra ve connB
    /// -> cung goi get() tren mot ThreadLocal nhung nhan 2 gia tri khac nhau

    /// withInitial(supplier)  : con thuc khoi tao gia tri mac dinh
    ///  lan dau luong goi get() -> JVM goi supplier de tao Connection
    ///  tu lan 2 : tim thay trong Map -> tra ve luong khong goi supplier nua
    /// withInitial() nhan vao tham so la mot Supplier<T>
    /// + co the Lazy Initialization : khoi lenh ben trong withInitial() khong chay ngay lap tuc ma cho luong A lan dau goi
    /// threadLocalConn.get() sau lan goi dau tien no se tra ra kqua luon va withInitial() khong bi goi lai nua doi voi luong A
    /// * Supplier<T> la mot interface co dung 1 phuong thuc la T get() // nha cung cap
    /// * cai dac biet cua Supplier la su tri hoan da nhac toi o tren
    /// Boc RuntimeException
    /// + khi goi DriverManager.getConnection(URL) co kha nang gay ra loi -> throw SQLException
    /// + SQLException thuoc  nhom CheckedException
    /// -> 1 la phai try - cacth tai cho
    /// -> 2 la ham chua no phai throws SQLException
    /// tuy nhien khoi lenh Lamda() -> {} thu chat dang trien khai implement (ham get()) ma interface Supplier<T> khong THROWS
    /// -> throw ra RuntimeException ve RuntimeException thuoc nhom Unchecked Exception -> xuyen qua duoc gioi han cua Supplier de bay ra ngoai
    private static final ThreadLocal<Connection> CONNECTION =
            ThreadLocal.withInitial( ()-> {
                try{
                    Connection conn = DriverManager.getConnection(URL);
                    caiDatPragma(conn);
                    return conn;

                } catch (SQLException e) {
                    /// boc thanh RuntimeException
                    throw new RuntimeException(e);
                }
            }
    );

    /// ket noi
    /// tra ve Connection cua luong hien tai -> khong phai Lock vi no lay connection cua chinh no
    /// xuly RuntimeException tu initialValue()
    /// khi threadLocalConn.get() -> co the nem ra RuntimeException nhu da boc o tren(Cause = SQLException)
    /// -> throws SQLException
    /// 1. bat RuntimeException tu get()
    /// 2. kiem tra cause co phai la vi SQLException khong
    /// -> neu co -> upwrap va nem lai dung SQLException
    /// -> neu khong -> nem lai RuntimeException goc
    public static Connection layKetNoi() throws SQLException {
        try{
            Connection conn = CONNECTION.get();
            /// tao lai neu Connection da dong
            if(conn == null || conn.isClosed()){
                CONNECTION.remove();
                conn = CONNECTION.get();
            }
            return conn;
        } catch (RuntimeException e) {
            if(e.getCause() instanceof SQLException){
                throw (SQLException) e.getCause();
            }
            else throw e;
        }
    }
    /// cai dat PRAGMA cho Connection vua tao
    /// goi 1 lan duy nhat trong InitialValue() cua ThreadLocal
    /// -> Connection vua tao chua co PRAGMA -> cai dat PRAGMA
    /// -> Connection vua tao da co PRAGMA -> do nothing
    private static void caiDatPragma(Connection conn) throws SQLException {
        /// conn la con duong noi tu java den SQL
        /// Statement la chiec xe cho hang
        try(Statement stmt = conn.createStatement()) {
            /// WAL = Write - Ahead Logging
            /// che do mac dinh (DELETE journal):
            /// Writer lock toan bo .db -> read phai cho
            /// che do WAL :
            /// Write ghi vao file .db-wal rieng
            /// Read doc tu file .db va nhin vao file .db-shm de xem co du lieu nao moi nam trong file .db-wal khong. Neu co se update du lieu moi
            stmt.execute("PRAGMA journal_mode=WAL");
            /// busy_timeout = 5000ms
            /// WAL chi cho duy nhat mot luong ghi tai 1 thoi diem(file-level write lock)
            /// -> khi 2 Connection cung muon ghi
            /// + neu khong co timeout -> Connection thu 2 that bai ngay lap tuc
            /// + new co timeout -> thu lai lien tuc trong 5s truoc khi bao loi
            /// trong thuc te write cho mat vai ms
            /// -> Connection thuong thanh con trong lan thu dau
            stmt.execute("PRAGMA busy_timeout=5000;");
            /// foreign_keys  = ON
            /// SQLite mac dinh OFF enforcement cua foreign key
            /// neu tat thi se khong the tao duoc cac moi quan he giua cac bang -> mat tinh lien ket, dong bo, an toan cho du lieu
            stmt.execute("PRAGMA foreign_keys=ON;");
            /// synchronous = NORMAL
            /// lenh ep ghi du lieu ngay lap tuc : fsync
            /// FULL : fsync sau moi lan ghi -> an toan nhat nhung cham nhat
            /// NORMAL : fsync goi o cac checkpoints -> can bang : khuyen nghi dung voi WAL
            /// OFF : -> khong fsync -> nhanh nhat nhung co the mat du lieu nhung giao pho het cho he dieu hanh tu quyet.
            stmt.execute("PRAGMA synchronous=NORMAL;");
        }
    }

    /// khoi tao bang
    /// tao mot bang can thiet neu chua ton tai
    /// goi 1 lan trong app.java truoc khi load FXML
    public static void khoiTao() {

        String sqlNguoiDung = "CREATE TABLE IF NOT EXISTS nguoi_dung (" +
            "ma_nguoi_dung TEXT PRIMARY KEY, " +
            "ho_ten TEXT NOT NULL, " +
            "thu_dien_tu TEXT UNIQUE NOT NULL, " +
            "mat_khau TEXT NOT NULL, " +
            "ngay_sinh TEXT, " +
            "dia_chi TEXT, " +
            "so_dien_thoai TEXT, " +
            "so_du_kha_dung REAL DEFAULT 0);";

        String sqlSanPham = "CREATE TABLE IF NOT EXISTS san_pham (" +
            "ma_san_pham TEXT PRIMARY KEY, " +
            "ten_san_pham TEXT NOT NULL);";

        /**
         * FOREIGN KEY (ma_nguoi_ban) REFERENCES nguoi_dung(ma_nguoi_dung)
         * Cần "PRAGMA foreign_keys=ON" (đã đặt trong caiDatPragma) mới có hiệu lực.
         *
         * trang_thai TEXT: lưu tên enum ("DANG_CHO", "DANG_DIEN_RA", "KET_THUC")
         * SQLite không có kiểu ENUM → dùng TEXT + kiểm soát ở tầng Java.
         */
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
            "id TEXT PRIMARY KEY, " +
            "ma_phien TEXT, " +
            "FOREIGN KEY (ma_phien) REFERENCES phien_dau_gia(ma_phien));";

        String sqlNguoiTraGia = "CREATE TABLE IF NOT EXISTS nguoi_tra_gia (" +
            "ma_phien TEXT, " +
            "ma_nguoi_dung TEXT, " +
            "gia_tra REAL, " +
            "thoi_gian TEXT, " +
            "PRIMARY KEY (ma_phien, ma_nguoi_dung, thoi_gian), " +
            "FOREIGN KEY (ma_phien) REFERENCES phien_dau_gia(ma_phien), " +
            "FOREIGN KEY (ma_nguoi_dung) REFERENCES nguoi_dung(ma_nguoi_dung));";

        try (Statement stmt = layKetNoi().createStatement()) {
            System.out.println("Đang tạo bảng nguoi_dung...");
            stmt.execute(sqlNguoiDung);
            System.out.println("Bảng nguoi_dung đã tạo");

            System.out.println("Đang tạo bảng san_pham...");
            stmt.execute(sqlSanPham);
            System.out.println("Bảng san_pham đã tạo");

            System.out.println("Đang tạo bảng phien_dau_gia...");
            stmt.execute(sqlPhien);
            System.out.println("Bảng phien_dau_gia đã tạo");

            System.out.println("Đang tạo bảng giao_dich...");
            stmt.execute(sqlGiaoDich);
            System.out.println("Bảng giao_dich đã tạo");

            System.out.println("Đang tạo bảng nguoi_tra_gia...");
            stmt.execute(sqlNguoiTraGia);
            System.out.println("Bảng nguoi_tra_gia đã tạo");

            System.out.println("Database khởi tạo thành công");
        } catch (SQLException e) {
            System.err.println("Lỗi khởi tạo DB: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /// Dong ket noi
    /// Chi dong neu Connection da ton tai trong ThreadLocal hien tai
    /// tranh tao moi Connection chi de dong ngay lap tuc
    public static void dongKetNoiHienTai(){
        Connection conn = null;
        try{
            // Dung get() co kha nang tao moi connection, nen kiem tra truoc
            // Tuy nhien, vi ThreadLocal.withInitial() luon tao moi,
            // chung ta phai accept rang co the tao moi connection khi dong
            conn = CONNECTION.get();
            if(!conn.isClosed()){
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng Connection: " + e.getMessage());
        } catch (RuntimeException e) {
            // Neu khong the tao connection (vi du: database ko co)
            // thi chi log error va tiep tuc cleanup
            System.err.println("Lỗi khi truy cap Connection: " + e.getMessage());
        }
        finally {
            CONNECTION.remove();
        }
    }

}