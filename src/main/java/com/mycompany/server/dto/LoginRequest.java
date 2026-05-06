package com.mycompany.server.dto;

/**
 * DTO (Data Transfer Object) — chứa dữ liệu client gửi lên khi đăng nhập.
 *
 * Client gửi JSON body dạng:
 * {
 *   "email": "abc@gmail.com",
 *   "matKhau": "123456"
 * }
 *
 * Gson tự động ánh xạ JSON → LoginRequest object khi server nhận request.
 */
public class LoginRequest {

    /** Địa chỉ email người dùng nhập */
    private String email;

    /** Mật khẩu người dùng nhập */
    private String matKhau;

    /** Constructor rỗng bắt buộc để Gson có thể deserialize */
    public LoginRequest() {}

    public String getEmail()   { return email; }
    public String getMatKhau() { return matKhau; }
}