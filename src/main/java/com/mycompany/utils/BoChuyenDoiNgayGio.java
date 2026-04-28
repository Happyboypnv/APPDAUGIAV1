package com.mycompany.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * BoChuyenDoiNgayGio - Custom Gson TypeAdapter cho LocalDateTime
 *
 * MỤC ĐÍCH:
 * - Xử lý chuyển đổi giữa LocalDateTime (Java) và JSON string
 * - Khi lưu LocalDateTime vào JSON, nó sẽ là chuỗi text (vd: "2024-04-28T15:30:45")
 * - Khi đọc từ JSON, sẽ chuyển đổi ngược lại thành LocalDateTime
 *
 * KHÁC BIỆT VỚI BoChuyenDoiNgay:
 * - BoChuyenDoiNgay: chỉ xử lý ngày (LocalDate) - "2024-04-28"
 * - BoChuyenDoiNgayGio: xử lý ngày + giờ (LocalDateTime) - "2024-04-28T15:30:45"
 *
 * CÁCH DÙNG:
 * Đăng ký trong GsonBuilder:
 * .registerTypeAdapter(LocalDateTime.class, new BoChuyenDoiNgayGio())
 */
public class BoChuyenDoiNgayGio extends TypeAdapter<LocalDateTime> {

    /**
     * PHƯƠNG THỨC: write(JsonWriter out, LocalDateTime value)
     * MỤC ĐÍCH: Chuyển đổi LocalDateTime thành JSON string (serialization)
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Khi lưu đối tượng chứa LocalDateTime vào file JSON
     * - Gson sẽ gọi phương thức này để biết cách ghi LocalDateTime
     * - Kết quả: "2024-04-28T15:30:45" thay vì đối tượng phức tạp
     *
     * @param out JsonWriter - đối tượng ghi dữ liệu vào JSON
     * @param value LocalDateTime cần lưu
     * @throws IOException nếu có lỗi ghi file
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        // Kiểm tra null trước
        if (value == null) {
            // Nếu value là null, ghi "null" vào JSON
            out.nullValue();
            return;
        }
        // value.toString() chuyển LocalDateTime thành string dạng "YYYY-MM-DDTHH:mm:ss"
        // Ví dụ: LocalDateTime "2024-04-28 15:30:45" → "2024-04-28T15:30:45"
        out.value(value.toString());
    }

    /**
     * PHƯƠNG THỨC: read(JsonReader in)
     * MỤC ĐÍCH: Chuyển đổi JSON string thành LocalDateTime (deserialization)
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Khi đọc JSON file chứa LocalDateTime string
     * - Gson sẽ gọi phương thức này để chuyển string về LocalDateTime
     * - Kết quả: "2024-04-28T15:30:45" được chuyển thành LocalDateTime object
     *
     * @param in JsonReader - đối tượng đọc dữ liệu từ JSON
     * @return LocalDateTime object được tạo từ string
     * @throws IOException nếu có lỗi đọc file
     */
    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        // Kiểm tra: "Giá trị tiếp theo có phải null không?"
        if (in.peek() == JsonToken.NULL) {
            // Nếu là null, đánh dấu rằng ta xử lý nó
            in.nextNull();
            // Trả về null thay vì giá trị LocalDateTime
            return null;
        }

        // Nếu không phải null, đọc chuỗi text từ JSON
        // in.nextString() lấy string tiếp theo từ JSON (vd: "2024-04-28T15:30:45")
        // LocalDateTime.parse() chuyển string thành LocalDateTime object
        return LocalDateTime.parse(in.nextString());
        // "2024-04-28T15:30:45" -> LocalDateTime
    }
}