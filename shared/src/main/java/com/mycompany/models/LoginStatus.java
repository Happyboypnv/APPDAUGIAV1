package com.mycompany.models;

public enum LoginStatus {
    SUCCESS,
    INVALID_CREDENTIALS, // Sai email hoặc mật khẩu
    BANNED               // Tài khoản bị khóa
}