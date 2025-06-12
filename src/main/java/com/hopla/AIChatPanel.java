package com.hopla;

import com.hopla.IA.AIConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static com.hopla.Utils.alert;

public class AIChatPanel {

    private static Boolean systemPrompt = false;
    private final JLabel statusLabel = new JLabel(" ");
    private final AIConfiguration aiConfiguration;
    private JTextArea chatArea;
    private JTextArea inputField;
    private JFrame frame;

    public AIChatPanel(AIConfiguration aiConfiguration) {
        this.aiConfiguration = aiConfiguration;
    }

    public void show() {
        if (frame != null) {
            frame.dispose();
            systemPrompt = true;
        }
        frame = new JFrame();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(true);
        frame.setFocusableWindowState(true);
        frame.setAlwaysOnTop(true);


        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(statusLabel, BorderLayout.NORTH);

        inputField = new JTextArea(5, 20);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);

        JScrollPane inputScrollPane = new JScrollPane(inputField);

        frame.add(inputScrollPane, BorderLayout.CENTER);

        JButton sendButton = new JButton("Ask");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(800, 500));

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userInput = inputField.getText().trim();
                if (!userInput.isEmpty()) {
                    chatArea.append("YOU : " + userInput + "\n------\n");
                    inputField.setText("");
                    statusLabel.setText("Thinking...");

                    try {
                        aiConfiguration.aiProvider.chat(systemPrompt, userInput, response -> SwingUtilities.invokeLater(() ->
                        {

                            chatArea.append("AI: " + response + "\n-----\n");
                            statusLabel.setText("");
                        }));
                        systemPrompt = false;
                    } catch (IOException exc) {
                        alert("AI chat error: " + exc.getMessage());
                        HopLa.montoyaApi.logging().logToOutput("AI chat error: " + exc.getMessage());
                    }
                }
            }
        };

        sendButton.addActionListener(sendAction);

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }


    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }

}
