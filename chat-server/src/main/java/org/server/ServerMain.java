package org.server;

import org.server.auth.InDBAuthenticationProvider;

import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        Server server = new Server(port, new InDBAuthenticationProvider());
        server.start();
    }
}
