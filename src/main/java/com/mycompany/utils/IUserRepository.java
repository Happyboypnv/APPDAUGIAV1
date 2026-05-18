package com.mycompany.utils;

import com.mycompany.models.User;

import java.util.Map;

public interface IUserRepository {

    void save(User nguoiDung);
    void update(User nguoiDung);
    Map<String, User> findAll();
    boolean verifyCredentials(String email, String password);
    boolean isEmailAvailable(String email);
    void delete(User nguoiDung);
    User findByEmail(String email);
}