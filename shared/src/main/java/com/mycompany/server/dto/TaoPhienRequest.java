package com.mycompany.server.dto;

public class TaoPhienRequest {
  String tenPhien;
  String tenSanPham;
  String maSanPham;
  String danhMuc;
  String moTa;
  String thoiGianBatDau;
  double giaKhoiDiem;
  int thoiGianGiay;
  String productImgPath;

  public TaoPhienRequest(String tenPhien, String tenSanPham, String maSanPham,
                         String danhMuc, String moTa, String thoiGianBatDau,
                         double giaKhoiDiem, int thoiGianGiay,String productImgPath) {
    this.tenPhien = tenPhien;
    this.tenSanPham = tenSanPham;
    this.maSanPham = maSanPham;
    this.danhMuc = danhMuc;
    this.moTa = moTa;
    this.thoiGianBatDau = thoiGianBatDau;
    this.giaKhoiDiem = giaKhoiDiem;
    this.thoiGianGiay = thoiGianGiay;
    this.productImgPath = productImgPath;
  }
}
