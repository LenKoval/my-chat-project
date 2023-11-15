package org.client;

import javax.swing.*;
import java.awt.*;

public class ClientView extends JFrame implements Runnable {
    protected JTextArea outTextArea;
    protected JPanel southPanel;
    protected JTextField inTextField;
    protected JButton inTextSendButton;
    private SocketConnector socketConnector;

    public ClientView(String title, SocketConnector socketConnector) throws HeadlessException {
        super(title);
        southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(2, 1, 10, 10));
        southPanel.add(inTextField = new JTextField());
        inTextField.setEditable(true);
        southPanel.add(inTextSendButton = new JButton("Send message"));
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(BorderLayout.CENTER, outTextArea = new JTextArea());
        outTextArea.setLineWrap(true);
        outTextArea.setWrapStyleWord(true);
        outTextArea.setEditable(false);
        cp.add(BorderLayout.SOUTH, southPanel);

        this.socketConnector = socketConnector;

        inTextSendButton.addActionListener(event -> {
            String text = inTextField.getText();
            socketConnector.sendMessage(text);
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 500);
        setVisible(true);
        inTextField.requestFocus();
        (new Thread(this)).start();
        this.socketConnector.setCallback(new Callback() {
            @Override
            public void call(Object... args) {
                outTextArea.append((String) args[0]);
            }
        });
    }

    @Override
    public void run() {
    }
}
