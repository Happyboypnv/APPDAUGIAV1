package com.mycompany.models;


public class NguoiBan implements HanhDongNguoiBan {
    private NguoiDung nguoiDung;
    private PhienDauGia phienDauGia;

    public NguoiBan(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    public void setPhienDauGia(PhienDauGia phienDauGia) {
        this.phienDauGia = phienDauGia;
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }

    @Override
    public void batDauPhienDauGia() {
        if (phienDauGia.getTrangThai() == TrangThaiPhien.DANG_MO) {
            phienDauGia.setTrangThai(TrangThaiPhien.DANG_DIEN_RA);
            return;
        } else if (phienDauGia.getTrangThai() == TrangThaiPhien.DANG_DIEN_RA) {
            System.out.println("Phòng đang diễn ra rồi!");
        }
        System.out.println("Phòng chưa được duyệt hoặc đã đóng!"); // sau thay hết bằng catch exception
    }

    @Override
    public void huyPhienDauGia() {
        if (phienDauGia.getTrangThai() == TrangThaiPhien.DANG_MO || phienDauGia.getTrangThai() == TrangThaiPhien.DANG_CHO_DUYET) {
            phienDauGia.setTrangThai(TrangThaiPhien.DA_HUY);
            return;
        }
        System.out.println("Phòng không thể huỷ khi đang diễn ra hoặc đã kết thúc!"); // sau thay bang exception
    }
}