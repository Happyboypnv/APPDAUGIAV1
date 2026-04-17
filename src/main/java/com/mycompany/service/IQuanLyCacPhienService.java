package com.mycompany.service;
// IQuanLyCacPhienService.java (interface mới)
import com.mycompany.models.PhienDauGia;

public interface IQuanLyCacPhienService {
        void them(PhienDauGia phien);
        void xoa(PhienDauGia phien);
        void xoa(String maPhien);
        PhienDauGia tim(String maPhien);
}

