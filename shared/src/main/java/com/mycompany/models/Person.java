package com.mycompany.models;

import java.io.Serializable;

/**
 * Person - Abstract class representing a user in the system.
 *
 * IMPORTANT CHANGES (SQLite Migration):
 * - Added 'salt' field to support password hashing
 * - Constructor stores password as-is (already hashed by caller)
 * - Added getter/setter for salt
 *
 * PURPOSE:
 * - Store basic user information (name, email, date of birth, password)
 * - Base class for User and Admin
 * - Provide common getter/setter methods
 * - Support password hashing with salt
 */
public class Person implements Serializable {

    private String userId;
    private String fullName;
    private String email;
    private String dateOfBirth;
    private String password;

    /**
     * Salt field for password hashing.
     * Each user has their own salt to prevent rainbow table attacks.
     */
    private String salt;

    /**
     * Avatar path field to store user's profile picture.
     * Default value is the default_avatar.jpg if not set.
     */
    private String avatarPath;

    @Override
    public String toString() {
        return "userId: " + userId
                + "\nfullName: " + fullName
                + "\nemail: " + email
                + "\ndateOfBirth: " + dateOfBirth;
    }

    /**
     * Full constructor - Used when creating a new person.
     *
     * The caller is responsible for hashing the password before passing it in.
     * This constructor stores the password as-is without hashing again.
     *
     * @param fullName    Full name of the person
     * @param email       Email address
     * @param password    Password (already hashed by caller, stored as-is)
     * @param dateOfBirth Date of birth
     */
    protected Person(String fullName, String email, String password, String dateOfBirth) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Simple constructor - Used when only the name is available.
     *
     * @param fullName Full name of the person
     */
    protected Person(String fullName) {
        this.fullName = fullName;
    }

    // ===== GETTERS =====

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return this.email;
    }

    public String getDateOfBirth() {
        return this.dateOfBirth;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getPassword() {
        return password;
    }

    /** Returns the salt used for password hashing. */
    public String getSalt() {
        return salt;
    }

    /** Returns the avatar path for the user. */
    public String getAvatarPath() {
        return avatarPath != null ? avatarPath : "image/default_avatar.jpg";
    }

    // ===== SETTERS =====

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** Sets the salt for password hashing. Required when migrating legacy users. */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /** Sets the avatar path for the user. */
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
