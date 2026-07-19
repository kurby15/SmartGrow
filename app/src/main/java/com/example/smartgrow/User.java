package com.example.smartgrow;

import java.io.Serializable;

public class User implements Serializable {
    private String fullName;
    private String username;
    private String email;
    private String password;
    private String choice1;
    private String choice2;
    private String choice3;
    private String choice4;
    private String profilePic;

    public User() {
        // Required for Firebase
    }

    public User(String fullName, String username, String email, String password, String choice1, String choice2, String choice3, String choice4) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.choice1 = choice1;
        this.choice2 = choice2;
        this.choice3 = choice3;
        this.choice4 = choice4;
    }

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getChoice1() { return choice1; }
    public void setChoice1(String choice1) { this.choice1 = choice1; }

    public String getChoice2() { return choice2; }
    public void setChoice2(String choice2) { this.choice2 = choice2; }

    public String getChoice3() { return choice3; }
    public void setChoice3(String choice3) { this.choice3 = choice3; }

    public String getChoice4() { return choice4; }
    public void setChoice4(String choice4) { this.choice4 = choice4; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }
}