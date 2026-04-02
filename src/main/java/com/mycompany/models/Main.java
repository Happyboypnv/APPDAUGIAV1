package com.mycompany.models;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        String filename = "user.ser";
        List<User> Users = new ArrayList<User>();
        Scanner input = new Scanner(System.in);
        int n = Integer.parseInt(input.nextLine());
        for(int i = 0; i < n; i++){
            String line = input.nextLine().trim();
            String[] tmp =  line.split("\\s+");
            String name = tmp[0];
            String password = tmp[1];
            User u = new User(name,password);
            Users.add(u);
        }
        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))){
            out.writeObject(Users);
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        List<User> users2 = new ArrayList<>();
        try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))){
            users2 = (List<User>) in.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        for(User u : users2){
            System.out.println(u.toString());
        }
    }
}
