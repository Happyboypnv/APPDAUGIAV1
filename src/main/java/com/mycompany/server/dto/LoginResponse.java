package com.mycompany.server.dto;

/**
 * DTO — chứa dữ liệu server trả về sau khi đăng nhập thành công.
 *
 * Server trả về JSON dạng:
 * {
 *   "token": "USER_abc@gmail.com_1714123456789",
 *   "email": "abc@gmail.com",
 *   "hoTen": "Nguyen Van A",
 *   "thongBao": "Đăng nhập thành công"
 * }
 *
 * Nếu đăng nhập thất bại, server trả HTTP 401 với:
 * { "thongBao": "Sai email hoặc mật khẩu" }
 */
public class LoginResponse {

    /**
     * Token xác thực đơn giản — client dùng token này để gọi các API tiếp theo.
     * Định dạng: "USER_<email>_<timestamp>"
     * (Trong thực tế nên dùng JWT, nhưng ở đây dùng đơn giản cho phù hợp scope)
     */
    private String token;

    /** Email người dùng vừa đăng nhập */
    private String email;

    /** Họ tên đầy đủ của người dùng */
    private String hoTen;

    /** Thông báo kết quả: thành công hoặc lý do thất bại */
    private String thongBao;

    /** Constructor rỗng để Gson serialize */
    public LoginResponse() {}

    /**
     * Constructor đầy đủ — dùng khi đăng nhập thành công.
     *
     * @param token    token xác thực
     * @param email    email người dùng
     * @param hoTen    họ tên người dùng
     * @param thongBao thông báo kết quả
     */
    public LoginResponse(String token, String email, String hoTen, String thongBao) {
        this.token    = token;
        this.email    = email;
        this.hoTen    = hoTen;
        this.thongBao = thongBao;
    }

    /**
     * Constructor ngắn — dùng khi chỉ cần trả thông báo lỗi.
     *
     * @param thongBao lý do thất bại
     */
    public LoginResponse(String thongBao) {
        this.thongBao = thongBao;
    }

    // Getters (Gson dùng khi serialize object → JSON)
    public String getToken()    { return token; }
    public String getEmail()    { return email; }
    public String getHoTen()    { return hoTen; }
    public String getThongBao() { return thongBao; }
}