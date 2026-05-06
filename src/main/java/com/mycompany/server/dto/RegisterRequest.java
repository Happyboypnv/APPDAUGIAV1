package com.mycompany.server.dto;

/**
 * DTO — chứa dữ liệu client gửi lên khi đăng ký tài khoản mới.
 *
 * Client gửi JSON body dạng:
 * {
 *   "hoTen": "Nguyen Van A",
 *   "email": "abc@gmail.com",
 *   "matKhau": "123456",
 *   "ngaySinh": "2000-01-15"
 * }
 */
public class RegisterRequest {

    private String hoTen;
    private String email;
    private String matKhau;
    private String ngaySinh;

    /** Constructor rỗng bắt buộc để Gson deserialize */
    public RegisterRequest() {}

    public String getHoTen()    { return hoTen; }
    public String getEmail()    { return email; }
    public String getMatKhau()  { return matKhau; }
    public String getNgaySinh() { return ngaySinh; }
}