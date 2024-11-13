package com.divyagyan.courierapp;

public class User {

    public String userName;
    public String password;
    public String email;
    public String phone;
    public String address;


    public User(){

    }
    public  User(String userName,String email,String phone, String address){
        this.userName = userName;
        this.email = email;
        this.phone=phone;
        this.address=address;
    }

}
