package com.mycompany.models;

public class NguoiDung extends ConNguoi implements HanhDongNguoiDung {
    private String diaChi;
    private String soDienThoai;
    private double soDuKhaDung;
    public NguoiDung(String ten){
        super(ten);
    }
    public NguoiDung(String maNguoiDung, String hoTen, String thuDienTu, String ngaySinh, String diaChi, String soDienThoai){
        super(maNguoiDung, hoTen, thuDienTu, ngaySinh);
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.soDuKhaDung = 0;
        //List<Transactions> transactions = new ArrayList<>();
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
    protected String timKiemNguoiDung(ConNguoi self) {
        //self o day la ban than NguoiDung nay, se duoc truyen vao ngay trong controller
        return self.toString();
    }
    @Override
    public void mua(SanPham p) {
        // Lấy tên NguoiDung từ thuộc tính hoặc hàm toString để in ra cho trực quan
        System.out.println("[MUA] Người dùng " + this.toString().split("\n")[1] + " đang đặt giá mua: " + p.layTenSanPham());
    }
    @Override
    public void ban(SanPham p) {
        System.out.println("[BÁN] Người dùng " + this.toString().split("\n")[1] + " đang đăng bán: " + p.layTenSanPham());
    }
    @Override
    public void roiKhoiPhong(){
        System.out.println("Người dùng đã rời phòng đấu giá.");
    }
}
