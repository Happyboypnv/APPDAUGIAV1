package com.mycompany.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

import java.time.LocalDate;
import java.time.LocalDate;

public class BoChuyenDoiNgay extends TypeAdapter<LocalDate> {
    @Override
    public void write(JsonWriter out, LocalDate localDate) throws IOException {
        if(localDate == null) return;
        out.value(localDate.toString());
        // localDate -> String
    }
    @Override
    public LocalDate read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) { // 1. Kiểm tra: "Có phải null không?"
            in.nextNull();                 // 2. Hành động: "Ăn/Nhảy qua chữ null đó để làm trống con trỏ"
            return null;                   // 3. Kết quả: "Trả về giá trị null cho biến LocalDate trong Java"
        }
        return LocalDate.parse(in.nextString());
    }
}
