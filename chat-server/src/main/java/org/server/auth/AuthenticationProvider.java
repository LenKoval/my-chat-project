package org.server.auth;

import java.util.List;

public interface AuthenticationProvider {
    String getUsernameByLoginAndPassword(String login, String password);
    boolean register(String login, String password, String username);
    boolean checkAccess(String str);
    String[] changeUsername(String str);
    void changeBanUser(String str);
    List<String> getBannedUsers();
}
