package com.hopla;

import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import com.hopla.ai.*;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hopla.Constants.DEBUG_AI;
import static com.hopla.Utils.*;

public class AIChatPanel {
    private final static String REQUEST_PLACEHOLDER = "@request@";
    private final static String RESPONSE_PLACEHOLDER = "@response@";
    private final static String BUTTON_TEXT_SEND = "Ask";
    private final static String BUTTON_CANCEL_SEND = "Cancel";

    private final JLabel statusLabel = new JLabel(" ");
    private final AIConfiguration aiConfiguration;
    private final AIChats chats;
    private final HTMLEditorKit kit = new HTMLEditorKit();
    private final StyleSheet styleSheet = new StyleSheet();
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private JTextArea inputField;
    private JFrame frame;
    private JTextArea source;
    private JList<String> chatsList;
    private JTextPane editorPane;
    private AIProviderType currentProvider;
    private AIProvider aiProvider;
    private JScrollPane scrollPane;

    public AIChatPanel(AIConfiguration aiConfiguration, AIChats chats) {
        this.aiConfiguration = aiConfiguration;
        this.chats = chats;
        if (aiConfiguration.isAIConfigured) {
            this.currentProvider = aiConfiguration.defaultChatProvider.type;
        }

        loadCss();
    }

    public void show(MessageEditorHttpRequestResponse messageEditor, InputEvent event, String input) {
        if (frame != null) {
            frame.dispose();
        }
        if (!aiConfiguration.isAIConfigured) {
            alert("AI is not configured");
            return;
        }

        if (currentProvider == null) {
            this.currentProvider = aiConfiguration.defaultChatProvider.type;
        }

        this.source = (JTextArea) event.getSource();
        frame = generateJFrame();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                aiConfiguration.defaultChatProvider.cancelCurrentQuickActionRequest();
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        editorPane = new JTextPane();
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setContentType("text/html");
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        editorPane.setEditorKit(kit);
        Document doc = kit.createDefaultDocument();
        editorPane.setDocument(doc);
        editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        contextMenu.add(new JMenuItem(new AbstractAction("Insert selection in editor") {
            public void actionPerformed(ActionEvent e) {
                if (editorPane.getSelectedText() != null) {
                    source.insert(editorPane.getSelectedText(), source.getCaretPosition());
                }

            }
        }));

        editorPane.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            private void show(MouseEvent e) {
                if (editorPane.getSelectedText() != null && !editorPane.getSelectedText().isEmpty()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = panel.getWidth() - 50;
                editorPane.setMaximumSize(new Dimension(width, Short.MAX_VALUE));
                panel.repaint();
                panel.invalidate();
            }
        });

        scrollPane = new JScrollPane(editorPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(4);

        panel.add(scrollPane, BorderLayout.CENTER);


        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(statusLabel, BorderLayout.NORTH);

        inputField = new JTextArea(5, 20);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);

        JScrollPane inputScrollPane = new JScrollPane(inputField);

        JButton sendButton = new JButton(BUTTON_TEXT_SEND);

        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    inputField.insert("\n", inputField.getCaretPosition());
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (sendButton.getText().equals(BUTTON_TEXT_SEND)) {
                        sendButton.doClick();
                    }
                    e.consume();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    AIChats.Chat chat = getCurrentChat();
                    inputField.setText(chat.getLastUserMessage().getContent());
                    e.consume();
                }
            }
        });


        panel.add(inputPanel, BorderLayout.SOUTH);

        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new BoxLayout(historyPanel, BoxLayout.Y_AXIS));
        historyPanel.setPreferredSize(new Dimension(200, 500));
        historyPanel.setMinimumSize(new Dimension(100, 0));
        historyPanel.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));

        JButton buttonNewChat = new JButton("New chat");
        buttonNewChat.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonNewChat.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonNewChat.getPreferredSize().height));
        buttonNewChat.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chats.getChats().add(new AIChats.Chat(LocalDateTime.now().format(dateFormatter), new ArrayList<>()));
                loadChatList();
                sendButton.setText(BUTTON_TEXT_SEND);
                statusLabel.setText("");
            }
        });
        historyPanel.add(buttonNewChat);

        DefaultListModel<String> listModel = new DefaultListModel<>();


        chatsList = new JList<>(listModel);
        chatsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chatsList.setLayoutOrientation(JList.VERTICAL);
        chatsList.setFixedCellWidth(200);
        historyPanel.add(chatsList);
        loadChatList();


        final int[] clickedItem = new int[1];
        JPopupMenu chatContextMenu = new JPopupMenu();
        chatContextMenu.add(new JMenuItem(new AbstractAction("Delete chat") {
            public void actionPerformed(ActionEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        frame,
                        "Delete ?",
                        null,
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    chats.getChats().remove(chatsList.getModel().getSize() - 1 - clickedItem[0]);
                    loadChatList();
                }

            }
        }));

        chatsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = chatsList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        int chatIndex = chatsList.getModel().getSize() - 1 - index;
                        loadChat(chats.getChats().get(chatIndex));

                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            private void show(MouseEvent e) {
                clickedItem[0] = chatsList.getSelectedIndex();
                chatContextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });


        JScrollPane historyScroll = new JScrollPane(historyPanel);
        historyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


        panel.setPreferredSize(new Dimension(800, 500));
        frame.add(historyPanel, BorderLayout.WEST);
        frame.add(panel, BorderLayout.CENTER);
        inputField.setText(input);

        ActionListener sendAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (sendButton.getText().equals(BUTTON_CANCEL_SEND) && aiProvider != null) {
                    aiProvider.cancelCurrentChatRequest();
                    sendButton.setText(BUTTON_TEXT_SEND);
                    statusLabel.setText("Cancelled");
                    return;
                }

                String userInput = inputField.getText().trim();
                if (!userInput.isEmpty()) {

                    userInput = userInput.replace(REQUEST_PLACEHOLDER, getRequest(messageEditor));
                    userInput = userInput.replace(RESPONSE_PLACEHOLDER, getResponse(messageEditor));

                    inputField.setText("");
                    statusLabel.setText("Thinking...");

                    AIChats.Chat chat = getCurrentChat();
                    AIChats.Message message = new AIChats.Message(
                            AIChats.MessageRole.USER,
                            userInput
                    );
                    chat.addMessage(message);
                    loadChat(chat);
                    chats.save();
                    sendButton.setText(BUTTON_CANCEL_SEND);

                    try {
                        aiProvider = aiConfiguration.getChatProvider(currentProvider);
                        AIChats.Message answer = new AIChats.Message(
                                AIChats.MessageRole.ASSISTANT,
                                ""
                        );
                        chat.addMessage(answer);

                        aiProvider.chat(chat, new AIProvider.StreamingCallback() {
                            @Override
                            public void onData(String chunk) {
                                if (!chunk.isEmpty()) {
                                    chat.getLastMessage().appendContent(chunk);
                                    chats.save();
                                    SwingUtilities.invokeLater(() -> {
                                        loadChat(chat);
                                    });

                                }
                            }

                            @Override
                            public void onDone() {
                                chats.save();
                                loadChat(chat);
                                statusLabel.setText("");
                                sendButton.setText(BUTTON_TEXT_SEND);
                            }

                            @Override
                            public void onError(String error) {
                                statusLabel.setText(error);
                                sendButton.setText(BUTTON_TEXT_SEND);
                            }
                        });
                    } catch (Exception exc) {
                        alert("AI chat error: " + exc.getMessage());
                        HopLa.montoyaApi.logging().logToError("AI chat error: " + exc.getMessage());
                    }


                }
            }
        };

        sendButton.addActionListener(sendAction);


        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


        JButton buttonRequest = new JButton(REQUEST_PLACEHOLDER);
        buttonRequest.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTextTextArea(REQUEST_PLACEHOLDER);
            }
        });
        buttonPanel.add(buttonRequest);

        JButton buttonResponse = new JButton(RESPONSE_PLACEHOLDER);
        buttonResponse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTextTextArea(RESPONSE_PLACEHOLDER);

            }
        });
        buttonPanel.add(buttonResponse);

        JComboBox<LLMConfig.Prompt> selectBox = new JComboBox<>(aiConfiguration.config.prompts.toArray(new LLMConfig.Prompt[0]));
        selectBox.addActionListener(e -> {
            int idx = selectBox.getSelectedIndex();
            if (idx != -1) {
                insertTextTextArea(aiConfiguration.config.prompts.get(idx).content);

            }
        });

        buttonPanel.add(selectBox);

        Map<AIProviderType, LLMConfig.Provider> enabledProviders = aiConfiguration.config.providers.entrySet().stream()
                .filter(entry -> entry.getValue().enabled)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        JComboBox<AIProviderType> aiProviderSelectBox = new JComboBox<>(enabledProviders.keySet().toArray(new AIProviderType[0]));

        aiProviderSelectBox.addActionListener(e -> {
            AIProviderType selectedProvider = (AIProviderType) aiProviderSelectBox.getSelectedItem();
            if (selectedProvider != null) {
                currentProvider = selectedProvider;
                if (DEBUG_AI) {
                    HopLa.montoyaApi.logging().logToOutput("Provider selected:" + selectedProvider);
                }
            }
        });

        aiProviderSelectBox.setSelectedItem(aiConfiguration.defaultChatProvider.type);
        buttonPanel.add(aiProviderSelectBox);


        bottomBar.add(buttonPanel, BorderLayout.WEST);
        inputPanel.add(bottomBar, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
    }

    private AIChats.Chat getCurrentChat() {
        return chats.getChats().get(chatsList.getModel().getSize() - 1 - chatsList.getSelectedIndex());
    }

    private void loadChatList() {
        if (chats.getChats().isEmpty()) {
            chats.getChats().add(new AIChats.Chat(LocalDateTime.now().format(dateFormatter), new ArrayList<>()));
            chats.save();
        }
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (AIChats.Chat item : chats.getChats().reversed()) {
            listModel.addElement(item.timestamp);
        }


        chatsList.setModel(listModel);

        if (!listModel.isEmpty()) {
            chatsList.setSelectedIndex(0);
            loadChat(chats.getChats().getLast());
        }
    }

    private void loadChat(AIChats.Chat chat) {
        String m = "";
        for (AIChats.Message message : chat.getMessages()) {
            String html = renderMarkdownToHtml(message.getContent());
            m += "<div class=\"" + message.getRole().toString().toLowerCase() + "\">" +
                    "<span class=\"role\">" + message.getRole().toString() + "</span>" +
                    html +
                    "</div>";
        }
        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI chat html: " + m);

        }

        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        boolean atBottom = verticalBar.getValue() + verticalBar.getVisibleAmount() >= verticalBar.getMaximum() + 70;

        editorPane.setText(m);
        if (!atBottom) {
            editorPane.setCaretPosition(editorPane.getDocument().getLength());
        }

    }

    private void loadCss() {
        URL cssUrl = getClass().getResource("/style.css");
        styleSheet.importStyleSheet(cssUrl);
        kit.setStyleSheet(styleSheet);
    }

    private String renderMarkdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        String body = renderer.render(document);
        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToError("AI chat html: " + body);
        }
        return body;
    }

    private void insertTextTextArea(String text) {
        int start = inputField.getCaretPosition();
        int end = start;

        if (inputField.getSelectedText() != null && !inputField.getSelectedText().isEmpty()) {
            start = inputField.getSelectionStart();
            end = inputField.getSelectionEnd();
        }
        Document doc = inputField.getDocument();
        try {
            doc.remove(start, end - start);
            doc.insertString(start, text, null);
        } catch (BadLocationException exc) {
            HopLa.montoyaApi.logging().logToError("AI chat insertion error: " + exc.getMessage());
        }
        inputField.setCaretPosition(start + text.length());
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }

}
