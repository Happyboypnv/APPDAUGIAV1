package com.mycompany.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

import java.time.LocalDate;

/**
 * BoChuyenDoiNgay - Custom Gson TypeAdapter cho LocalDate
 *
 * MỤC ĐÍCH:
 * - Xử lý chuyển đổi giữa LocalDate (Java) và JSON string
 * - Khi lưu LocalDate vào JSON, nó sẽ là chuỗi text (vd: "2000-01-15")
 * - Khi đọc từ JSON, sẽ chuyển đổi ngược lại thành LocalDate
 *
 * TẠI SAO CẦN KỲ NÀY:
 * - Gson mặc định không biết cách xử lý LocalDate
 * - LocalDate là class Java phức tạp, không phải kiểu primitive
 * - Cần adapter tùy chỉnh để Gson hiểu cách serialize/deserialize
 *
 * CÁCH DÙNG:
 * Đăng ký trong GsonBuilder:
 * .registerTypeAdapter(LocalDate.class, new BoChuyenDoiNgay())
 */
public class BoChuyenDoiNgay extends TypeAdapter<LocalDate> {

    /**
     * PHƯƠNG THỨC: write(JsonWriter out, LocalDate localDate)
     * MỤC ĐÍCH: Chuyển đổi LocalDate thành JSON string (serialization)
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Khi lưu đối tượng chứa LocalDate vào file JSON
     * - Gson sẽ gọi phương thức này để biết cách ghi LocalDate
     * - Kết quả: "2000-01-15" thay vì đối tượng phức tạp
     *
     * @param out JsonWriter - đối tượng ghi dữ liệu vào JSON
     * @param localDate LocalDate cần lưu
     * @throws IOException nếu có lỗi ghi file
     */
    @Override
    public void write(JsonWriter out, LocalDate localDate) throws IOException {
        // Kiểm tra null
        // Nếu localDate là null, không ghi gì (hoặc ghi "null")
        if(localDate == null) return;

        // localDate.toString() chuyển LocalDate thành string dạng "YYYY-MM-DD"
        // Ví dụ: LocalDate của "2000-01-15" → "2000-01-15"
        out.value(localDate.toString());
        // localDate -> String
    }

    /**
     * PHƯƠNG THỨC: read(JsonReader in)
     * MỤC ĐÍCH: Chuyển đổi JSON string thành LocalDate (deserialization)
     *
     * GIẢI THÍCH CHI TIẾT:
     * - Khi đọc JSON file chứa LocalDate string
     * - Gson sẽ gọi phương thức này để chuyển string về LocalDate
     * - Kết quả: "2000-01-15" được chuyển thành LocalDate object
     *
     * @param in JsonReader - đối tượng đọc dữ liệu từ JSON
     * @return LocalDate object được tạo từ string
     * @throws IOException nếu có lỗi đọc file
     */
    @Override
    public LocalDate read(JsonReader in) throws IOException {
        // 🔹 BƯỚC 1: Kiểm tra: "Giá trị tiếp theo có phải null không?"
        if (in.peek() == JsonToken.NULL) {
            //  🔹 BƯỚC 2: Hành động: "Ăn/Nhảy qua chữ null đó để vị trí tiếp thật làm trống được con trỏ"
            // in.nextNull() đánh dấu rằng ta đã xử lý token null
            in.nextNull();
            // 🔹 BƯỚC 3: Kết quả: "Trả về giá trị null cho biến LocalDate trong Java"
            return null;
        }

        // Nếu không phải null, đọc chuỗi text từ JSON
        // in.nextString() lấy string tiếp theo từ JSON (vd: "2000-01-15")
        // LocalDate.parse() chuyển string "2000-01-15" thành LocalDate object
        return LocalDate.parse(in.nextString());
        // "2000-01-15" -> LocalDate
    }
}
