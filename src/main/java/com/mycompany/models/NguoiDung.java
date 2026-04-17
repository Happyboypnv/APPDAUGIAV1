// NguoiDung.java — sửa SRP + bug List
package com.mycompany.models;
import com.mycompany.Action.*;
import java.util.ArrayList;
import java.util.List;

public class NguoiDung extends ConNguoi implements IHanhDongMua, IHanhDongBan, IHanhDongRoiPhong {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;
    private List<GiaoDich> cacGiaoDich; // Fix: khai báo field

    public NguoiDung(String hoTen) {
        super(hoTen);
    }

    public NguoiDung(String hoTen, String thuDienTu, String matKhau, String ngaySinh) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
    }

    public NguoiDung(String maNguoiDung, String hoTen, String thuDienTu,
                     String matKhau, String ngaySinh, String diaChi, String soDienThoai) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        this.cacGiaoDich = new ArrayList<>(); // Fix: gán vào field thực
    }

    @Override
    public void mua(SanPham p) {
        // Không in log ở đây — để Controller/View xử lý
    }

    @Override
    public void ban(SanPham p) {
        // Không in log ở đây
    }

    @Override
    public void roiKhoiPhong() {
        // Không in log ở đây
    }
}
