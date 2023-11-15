package org.server.client.connection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private Server server;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private static int userCount = 0;
    private static final int maxUsersCount = 10;
    private static final Logger logger = LogManager.getLogger(ClientHandler.class.getName());

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        if (userCount < maxUsersCount) {
            username = "User" + userCount++;
        }
        logger.info("Клиент " + username + " подключился.");

        try {
            socket.setSoTimeout(1200000);
        } catch (SocketException se) {
            sendMessage("Время ожидания истекло.");
        }

        new Thread(() -> {
            try {
                authenticateUser(server);
                communicateWithUser(server);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    protected void communicateWithUser(Server server) throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                } else if (message.startsWith("/w")) {
                    server.pointToPoint(this, message);
                } else if (message.equals("/list")) {
                    List<String> userList = server.getUsernameList();
                    String joinedUsers = String.join(", ", userList);
                    sendMessage(joinedUsers);
                } else if (message.startsWith("/kick")) {
                    sendMessage("Введите логин и пароль.");
                    String info = in.readUTF();
                    if (server.getAuthenticationProvider().checkAccess(info)) {
                        server.kickUser(message);
                    } else {
                        sendMessage("У вас нет прав для операции.");
                    }
                } else if (message.startsWith("/changeNick")) {
                    if (!server.changeNick(server.getAuthenticationProvider().changeUsername(message))) {
                        sendMessage("Вы ввели некорректные данные или такой ник уже занят. Попоробуйте еще раз.");
                    }
                } else if (message.startsWith("/banList")) {
                    sendMessage("Введите логин и пароль.");
                    String info = in.readUTF();
                    if (server.getAuthenticationProvider().checkAccess(info)) {
                        List<String> bannedList = server.getAuthenticationProvider().getBannedUsers();
                        String bannedUsers = String.join(", ", bannedList);
                        sendMessage(bannedUsers);
                    } else {
                        sendMessage("У вас нет прав для операции.");
                    }
                } else if (message.startsWith("/changeBan")) {
                    sendMessage("Введите логин и пароль.");
                    String info = in.readUTF();
                    if (server.getAuthenticationProvider().checkAccess(info)) {
                        server.banClient(message);
                    } else {
                        sendMessage("У вас нет прав для операции.");
                    }
                } else if (message.startsWith("/shutdown")) {
                    sendMessage("Введите логин и пароль");
                    String info = in.readUTF();
                    if (server.getAuthenticationProvider().checkAccess(info)) {
                        server.stop();
                    } else {
                        sendMessage("У вас нет прав для операции.");
                    }
                }
            } else {
                server.broadcastMessage(username + ": " + message);
            }
        }
    }

    protected void authenticateUser(Server server) throws IOException {
        boolean isAuthenticated = false;
        while (!isAuthenticated) {
            String message = in.readUTF();
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/org/server": {
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (username == null || username.isBlank()) {
                        sendMessage("Указан неверный логин/пароль.");
                    } else {
                        setUsername(username);
                        sendMessage(username + ", добро пожаловать в чат! Доступные операции:\n" +
                                "каждое слово вводится через пробел\n" +
                                "личное сообщение: /w + nick\n" +
                                "список активных клиентов: /list\n" +
                                "смена ника: /changeNick + nick + password + new nick\n" +
                                "выход из чата: /exit\n");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String nick = args[2];
                    String password = args[3];
                    boolean isRegister = server.getAuthenticationProvider().register(login, password, nick);
                    if (!isRegister) {
                        sendMessage("Указан неверный логин/пароль.");
                    } else {
                        setUsername(nick);
                        sendMessage(nick + ", добро пожаловать в чат! Доступные операции:\n" +
                                "каждое слово вводится через пробел\n" +
                                "личное сообщение: /w + nick\n" +
                                "список активных клиентов: /list\n" +
                                "смена ника: /changeNick + nick + password + new nick\n" +
                                "выход из чата: /exit\n");
                        logger.info(nick + " вошел в чат.");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                default: {
                    sendMessage("Введите /auth + login + password для входа в чат\n" +
                            "или зарегестрируйтесь /register + login + nick + password\n" +
                            "каждое слово вводите через пробел.");
                }
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try{
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        String dateTime = DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(LocalDateTime.now());
        try {
            out.writeUTF(dateTime + " " + message + "\r\n");
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }
}
