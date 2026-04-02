package com.mycompany.models;
import  java.io.*;
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    public String name; // username nen la duy nhat
    public transient String password;
    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + ","  + password;
    }

    //Ko nên để Bidder và Seller là interface vì khi implement vào user vẫn có thể ép kiểu ngược lại bidder thành seller và seller thành bidder vì bản chất nó vẫn là seller nên chỉ cần instance of user là ép từ cái này thành cái kia được rồi
    // Bidder và seller nên đóng vai trò là lớp wrapper, bọc lấy user (bidder / seller has a user). User đóng vai trò thuần lưu thông tin của ng dùng, còn 2 lớp wrapper kia định nghĩa các phương thức chỉ riêng mỗi vai trò có => lớp này k dùng được phương thức riêng của lớp kia và k ép lớp này thành lớp kia được
}
