package org.client;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        try(SocketConnector socketConnector = new SocketConnector()) {
            socketConnector.connect(8080);
            new ClientView("chat", socketConnector);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String msg = scanner.nextLine();
                socketConnector.sendMessage(msg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
