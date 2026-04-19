package com.mycompany.models;

import com.mycompany.action.HanhDongNguoiDung;

import java.util.List;
import java.util.ArrayList;

public class NguoiDung extends ConNguoi implements HanhDongNguoiDung {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;

    public NguoiDung(String hoTen) {
        super(hoTen);
    }

    public NguoiDung(String hoTen, String thuDienTu, String maKhau, String ngaySinh) {
        super(hoTen, thuDienTu, maKhau, ngaySinh);
    }

    public NguoiDung(String maNguoiDung, String hoTen, String thuDienTu, String matKhau, String ngaySinh, String diaChi, String soDienThoai) {
        super(hoTen, thuDienTu, matKhau, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        List<GiaoDich> cacGiaoDich = new ArrayList<>();
        /*
        Transactions
        -> maNguoiDung
           Product -> name, ...;
           Time
           Seller
           Buyer
         */
    }
        @Override
        protected String timKiemNguoiDung (ConNguoi self){
            //self o day la ban than NguoiDung nay, se duoc truyen vao ngay trong controller
            return self.toString();
        }

    @Override
    public void vaoPhong(PhienDauGia phienDauGia) {
        phienDauGia.themVaoPhong(this);
    }

    @Override
    public PhienDauGia taoPhienDauGia(String maPhien, String tenPhien, SanPham sanPhanDauGia, double giaKhoiDiem) {
        return new PhienDauGia(maPhien,tenPhien,sanPhanDauGia,giaKhoiDiem,this);
    }


//    @Override
//    public void mua(SanPham p) {
//        // Lấy tên NguoiDung từ thuộc tính hoặc hàm toString để in ra cho trực quan
//        System.out.println("[MUA] Người dùng " + this.toString().split("\n")[1] + " đang đặt giá mua: " + p.layTenSanPham());
//    }
//    @Override
//    public void ban(SanPham p) {
//        System.out.println("[BÁN] Người dùng " + this.toString().split("\n")[1] + " đang đăng bán: " + p.layTenSanPham());
//    }
//    @Override
//    public void roiKhoiPhong(){
//        System.out.println("Người dùng đã rời phòng đấu giá.");
//    }

}
