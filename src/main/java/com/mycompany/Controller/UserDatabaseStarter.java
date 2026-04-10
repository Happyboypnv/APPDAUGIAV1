package com.mycompany.Controller;

// Toi uu hoa tim kiem ten email = cach chia cho tung database nho de k phai tim tu dau
public class UserDatabaseStarter {
    public static String createFile(String startingLetter) { // dung cho SignUp nen moi la create
        switch (startingLetter) {
            case "a":
                return "A_User.txt";
            case "b":
                return "B_User.txt";
            case "c":
                return "C_User.txt";
            case "d":
                return "D_User.txt";
            case "e":
                return "E_User.txt";
            case "f":
                return "F_User.txt";
            case "g":
                return "G_User.txt";
            case "h":
                return "H_User.txt";
            case "i":
                return "I_User.txt";
            case "j":
                return "J_User.txt";
            case "k":
                return "K_User.txt";
            case "l":
                return "L_User.txt";
            case "m":
                return "M_User.txt";
            case "n":
                return "N_User.txt";
            case "o":
                return "O_User.txt";
            case "p":
                return "P_User.txt";
            case "q":
                return "Q_User.txt";
            case "r":
                return "R_User.txt";
            case "s":
                return "S_User.txt";
            case "t":
                return "T_User.txt";
            case "u":
                return "U_User.txt";
            case "v":
                return "V_User.txt";
            case "w":
                return "W_User.txt";
            case "x":
                return "X_User.txt";
            case "y":
                return "Y_User.txt";
            case "z":
                return "Z_User.txt";
        }
        return "";
    }

    public static String getFile(String startingLetter) { // Dung cho signin nen la get
        return createFile(startingLetter);
    }
}