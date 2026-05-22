package com.mycompany.utils;

import com.mycompany.models.User;

import java.util.Map;

public interface IUserRepository {

    boolean holdBalance(String userId, String maPhien, double amount);
    void releaseHold(String userId, String maPhien, double amount);
    boolean deductOnWin(String userId, String maPhien, double amount);
    void save(User nguoiDung);
    void update(User nguoiDung);
    Map<String, User> findAll();
    boolean verifyCredentials(String email, String password);
    boolean isEmailAvailable(String email);
    void delete(User nguoiDung);
    User findByEmail(String email);
}