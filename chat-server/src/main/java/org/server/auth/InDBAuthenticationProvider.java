package org.server.auth;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InDBAuthenticationProvider implements AuthenticationProvider {
    private final List<User> users;
    private DBConnector dbConnector;
    private static final Logger logger = LogManager.getLogger(InDBAuthenticationProvider.class.getName());

    public InDBAuthenticationProvider() {
        this.users = new ArrayList<>();
        this.dbConnector = new DBConnector();
        users.addAll(dbConnector.selectUsers());
        logger.info("Данные пользователей из базы добавлены в список.");
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (Objects.equals(user.getPassword(), password)
                    && Objects.equals(user.getLogin(), login)
                    && Objects.equals(user.getBanned(), false)) {
                logger.info("Пользователь прошел аутентификацию.");
                return user.getUsername();
            }
        }
        return null;
    }

    @Override
    public synchronized boolean register(String login, String password, String username) {
        for (User user : users) {
            if (Objects.equals(user.getUsername(), username) && Objects.equals(user.getLogin(), login)) {
                return false;
            }
        }
        users.add(new User(login, password, username, "user", false));
        logger.info("Новый пользователь зарегестрирован.");
        dbConnector.insertUser(login, password, username);
        return true;
    }

    @Override
    public synchronized boolean checkAccess(String info) {
        String[] data = info.split(" ", 2);
        for (User user : users) {
            if (Objects.equals(user.getLogin(), data[0])
                    && Objects.equals(user.getPassword(), data[1])
                    && Objects.equals(user.getRole(), "admin")) {
                logger.info("Администратор прошел аутентификацию.");
                return true;
            }
        }
        return false;
    }

    public synchronized String[] changeUsername(String message) {
        String[] data = message.split(" ", 4);
        for (User user : users) {
            if (Objects.equals(user.getUsername(), data[3])) {
                return null;
            } else {
                if(Objects.equals(user.getUsername(), data[1])) {
                    user.setUsername(data[3]);
                    logger.info("Username изменен в списке.");
                    dbConnector.updateUser(data[2], data[3]);
                }
            }
        }
        return data;
    }
    @Override
    public synchronized void changeBanUser(String message) {
        String[] data = message.split(" ", 3);
        Boolean changeBan = Boolean.parseBoolean(data[2]);
        for (User user : users) {
            if (Objects.equals(user.getUsername(), data[1]) && !Objects.equals(user.getBanned(), changeBan)) {
                user.setBanned(changeBan);
                logger.info("Изменен статус ban в списке у пользователя " + user.getUsername());
                dbConnector.updateBan(data[1], changeBan);
            }
        }
    }
    @Override
    public synchronized List<String> getBannedUsers() {
        List<String> bannedUsers = new ArrayList<>();
        for (User user : users) {
            if (user.getBanned() == (true)) {
                bannedUsers.add(user.getUsername());
            }
        }
        return bannedUsers;
    }
}
