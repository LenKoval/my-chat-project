package org.server.auth;

public class User {
    private String login;
    private String password;
    private String username;
    private String role;
    private Boolean isBanned;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Boolean getBanned() {
        return isBanned;
    }

    public void setBanned(Boolean banned) {
        isBanned = banned;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User(String login, String password, String username, String  role, boolean isBanned) {
        this.login = login;
        this.password = password;
        this.username = username;
        this.role = role;
        this.isBanned = isBanned;
    }
}
