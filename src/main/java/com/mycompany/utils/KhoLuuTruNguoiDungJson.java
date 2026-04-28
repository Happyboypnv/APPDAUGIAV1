package com.mycompany.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mycompany.models.NguoiDung;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class KhoLuuTruNguoiDungJson implements IKhoLuuTruNguoiDung {
    private static final String TEN_FILE = "dulieunguoidung.json";
    private static final Object LOCK = new Object();
    private final Gson boChuyenDoiGson;
    // Sử dụng Type để giúp Gson hiểu cấu trúc phức tạp bên trong Map
    private static final Type KIEU_DU_LIEU = new TypeToken<DuLieuNguoiDung>(){}.getType();

    private static class DuLieuNguoiDung {
        int maCuoiCung = 0;
        Map<String, NguoiDung> danhSach = new HashMap<>();
    }

   public KhoLuuTruNguoiDungJson() {
        boChuyenDoiGson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new BoChuyenDoiNgay())
                .registerTypeAdapter(LocalDateTime.class, new BoChuyenDoiNgayGio())
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void luu(NguoiDung nguoiDung) {
        synchronized (LOCK) {
            DuLieuNguoiDung duLieu = docTuFile();
            duLieu.maCuoiCung++;
            String maMoi = String.format("PPTT%06d", duLieu.maCuoiCung);
            nguoiDung.setMaNguoiDung(maMoi);
            duLieu.danhSach.put(nguoiDung.layThuDienTu(), nguoiDung);
            ghiVaoFile(duLieu);
        }
    }

    @Override
    public synchronized Map<String, NguoiDung> layTatCa() {
        return docTuFile().danhSach;
    }

    // Đổi private để dùng nội bộ, thêm synchronized để an toàn luồng
    private synchronized DuLieuNguoiDung docTuFile() {
        File file = new File(TEN_FILE);
        if (!file.exists() || file.length() == 0) return new DuLieuNguoiDung();

        try (Reader nguoiDoc = new FileReader(file)) {
            DuLieuNguoiDung kq = boChuyenDoiGson.fromJson(nguoiDoc, KIEU_DU_LIEU);
            return (kq == null) ? new DuLieuNguoiDung() : kq;
        } catch (IOException e) {
            System.err.println("Lỗi đọc file: " + e.getMessage());
            return new DuLieuNguoiDung();
        }
    }

    private synchronized void ghiVaoFile(DuLieuNguoiDung duLieu) {
        try (Writer nguoiGhi = new FileWriter(TEN_FILE)) {
            boChuyenDoiGson.toJson(duLieu, nguoiGhi);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean kiemTraNguoiDung(String email, String password) {
        synchronized (LOCK) {
            Map<String, NguoiDung> danhSach = layTatCa();
            if (danhSach == null) return false;
            NguoiDung check = danhSach.get(email);
            if (check == null) return false;
            return check.layMatKhau() != null && check.layMatKhau().equals(password);
        }
    }

    public boolean kiemTraEmail(String email) {
        synchronized (LOCK) {
            Map<String, NguoiDung> danhSach = layTatCa();
            if (danhSach == null) return true;
            return !danhSach.containsKey(email);
        }
    }
}