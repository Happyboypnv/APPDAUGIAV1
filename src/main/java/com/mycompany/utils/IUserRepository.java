package com.mycompany.utils;

import com.mycompany.models.NguoiDung;
import java.util.Map;

public interface IUserRepository {

    void save(NguoiDung nguoiDung);
    void update(NguoiDung nguoiDung);
    Map<String, NguoiDung> findAll();
    boolean verifyCredentials(String email, String password);
    boolean isEmailAvailable(String email);
    void delete(NguoiDung nguoiDung);
    NguoiDung findByEmail(String email);
}