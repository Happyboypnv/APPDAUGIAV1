package com.mycompany.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.models.NguoiDung;
import java.io.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class KhoLuuTruNguoiDungJson implements IKhoLuuTruNguoiDung{
    private static final String TEN_FILE = "dulieunguoidung.json";
    private final Gson boChuyenDoiGson;

    private static class DuLieuNguoiDung{
        int maCuoiCung = 0;
        Map<String, NguoiDung> danhSach =  new HashMap<String, NguoiDung>();
    }
    public  KhoLuuTruNguoiDungJson() {
        boChuyenDoiGson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new BoChuyenDoiNgay())
                .setPrettyPrinting()
                .create();
    }
    public synchronized void luu(NguoiDung nguoiDung){
        DuLieuNguoiDung duLieu = docTuFile();
        duLieu.maCuoiCung++;
        String maMoi = String.format("PPTT%06d", duLieu.maCuoiCung);
        nguoiDung.setMaNguoiDung(maMoi);
        duLieu.danhSach.put(nguoiDung.layThuDienTu(), nguoiDung);
        ghiVaoFile(duLieu);
    }
    public Map<String, NguoiDung> layTatCa(){
        return docTuFile().danhSach;
    }
    public DuLieuNguoiDung docTuFile(){
        File file = new File(TEN_FILE);
        if(!file.exists() || file.length() == 0) return new DuLieuNguoiDung();
        try(Reader nguoiDoc = new FileReader(file)){
            DuLieuNguoiDung kq = boChuyenDoiGson.fromJson(nguoiDoc, DuLieuNguoiDung.class);
            if(kq ==  null) return new DuLieuNguoiDung();
            return kq;
        }
        catch(Exception e){
            return new DuLieuNguoiDung();
        }
    }
    private void ghiVaoFile(DuLieuNguoiDung duLieu){
        try(Writer nguoiGhi = new FileWriter(TEN_FILE)){
            boChuyenDoiGson.toJson(duLieu, nguoiGhi);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean kiemTraNguoiDung(String email, String password) {
        Map<String,NguoiDung> danhSachNguoiDung =  layTatCa();
        if (danhSachNguoiDung == null) {
            return false;
        }
        NguoiDung check = danhSachNguoiDung.get(email);
        if (check == null){
            return false;
        } else {
            return check.layMatKhau().equals(password);
        }
    }
    public boolean kiemTraEmail(String email) {
        Map<String, NguoiDung> danhSach = layTatCa();
        if(danhSach == null) return true;
        if(danhSach.containsKey(email)){
            return false;
        }
        return true;
    }
}
