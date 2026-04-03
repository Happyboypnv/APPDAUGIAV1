package com.mycompany.models;

public class User extends Person implements IPersonActions{
    private String address;
    private String phoneNumber;
    private double balance;
    public User(String name){
        super(name);
    }
    public User(String id, String fullName, String email, String birth, String address, String phoneNumber){
        super(id, fullName, email, birth);
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.balance = 0;
        //List<Transactions> transactions = new ArrayList<>();
        /*
        Transactions
        -> ID
           Product -> name, ...;
           Time
           Seller
           Buyer
         */
    }

    @Override
    protected String findPerson(Person self) {
        //self o day la ban than user nay, se duoc truyen vao ngay trong controller
        return self.toString();
    }

    @Override
    public void buy(Product p) {
        // Lấy tên user từ thuộc tính hoặc hàm toString để in ra cho trực quan
        System.out.println("[MUA] Người dùng " + this.toString().split("\n")[1] + " đang đặt giá mua: " + p.getName());
    }

    @Override
    public void sell(Product p) {
        System.out.println("[BÁN] Người dùng " + this.toString().split("\n")[1] + " đang đăng bán: " + p.getName());
    }

    @Override
    public void quitBiddingRoom(){
        System.out.println("Người dùng đã rời phòng đấu giá.");
    }
    public String getName(){return this.getFullName();}
}
