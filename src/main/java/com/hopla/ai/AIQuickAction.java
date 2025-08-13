package com.hopla.ai;

import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import com.hopla.HopLa;
import com.hopla.Utils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

import static com.hopla.Constants.DEBUG_AI;
import static com.hopla.Utils.*;

public class AIQuickAction {
    private final static String REQUEST_PLACEHOLDER = "@request@";
    private final static String BUTTON_TEXT_SEND = "Apply";
    private final static String BUTTON_CANCEL_SEND = "Cancel";
    private final HTMLEditorKit kit = new HTMLEditorKit();
    private final StyleSheet styleSheet = new StyleSheet();
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();
    JFrame frame;
    AIConfiguration aiConfiguration;
    private JTextArea source;
    private JComboBox<LLMConfig.QuickAction> comboBox;
    private String outputData;
    private JTextPane output;
    private JScrollPane outputScrollPane;
    private JToggleButton markdownButton;

    public AIQuickAction(AIConfiguration aiConfiguration) {
        this.aiConfiguration = aiConfiguration;
        loadCss();
    }

    private void buildFrame(MessageEditorHttpRequestResponse messageEditor, InputEvent event, String input) {
        if (frame != null) {
            frame.dispose();
        }
        frame = generateJFrame();
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(700, 600));

        JLabel statusLabel = new JLabel("");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton insertButton = new JButton("Insert");
        JButton cancelButton = new JButton("Cancel");
        markdownButton = new JToggleButton("Markdown", true);
        buttonPanel.add(statusLabel);
        buttonPanel.add(markdownButton);
        buttonPanel.add(insertButton);
        buttonPanel.add(cancelButton);


        cancelButton.addActionListener(e -> {
            aiConfiguration.quickActionProvider.cancelCurrentQuickActionRequest();
        });

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                aiConfiguration.quickActionProvider.cancelCurrentQuickActionRequest();
            }
        });

        JTextArea instruction = new JTextArea();
        instruction.setLineWrap(true);
        instruction.setWrapStyleWord(true);
        instruction.setText(input);


        output = new JTextPane();
        output.setEditable(false);
        output.setOpaque(false);
        output.setContentType("text/html");
        output.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        output.setEditorKit(kit);
        Document doc = kit.createDefaultDocument();
        output.setDocument(doc);
        output.setAlignmentX(Component.LEFT_ALIGNMENT);

        markdownButton.addItemListener(e -> {
            if (markdownButton.isSelected()) {
                output.setContentType("text/html");
                output.setDocument(kit.createDefaultDocument());
            } else {
                output.setContentType("text/plain");
                output.setDocument(new DefaultStyledDocument());
            }
        });


        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.add(new JMenuItem(new DefaultEditorKit.CopyAction()));
        contextMenu.add(new JMenuItem(new AbstractAction("Insert selection in editor") {
            public void actionPerformed(ActionEvent e) {
                if (output.getSelectedText() != null) {
                    Utils.insertPayload(messageEditor, output.getSelectedText(), event);
                }

            }
        }));

        output.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) show(e);
            }

            private void show(MouseEvent e) {
                if (output.getSelectedText() != null && !output.getSelectedText().isEmpty()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        insertButton.addActionListener(e -> {
            Utils.insertPayload(messageEditor, output.getText(), event);
        });


        JComboBox<LLMConfig.QuickAction> comboBox = new JComboBox<>(this.aiConfiguration.config.quick_actions.toArray(new LLMConfig.QuickAction[0]));

        comboBox.addActionListener(e -> {
            LLMConfig.QuickAction item = (LLMConfig.QuickAction) comboBox.getSelectedItem();
            if (item != null) {
                instruction.insert(item.content + " ", 0);
            }
        });


        JPanel middleButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton applyButton = new JButton("Apply");
        JButton requestButton = new JButton(REQUEST_PLACEHOLDER);
        requestButton.addActionListener(e -> {
            int start = instruction.getCaretPosition();
            int end = start;

            if (instruction.getSelectedText() != null && !instruction.getSelectedText().isEmpty()) {
                start = instruction.getSelectionStart();
                end = instruction.getSelectionEnd();
            }
            Document doc_instruct = instruction.getDocument();
            try {
                doc_instruct.remove(start, end - start);
                doc_instruct.insertString(start, REQUEST_PLACEHOLDER, null);
            } catch (BadLocationException exc) {
                HopLa.montoyaApi.logging().logToError("AI chat insertion error: " + exc.getMessage());
            }
            instruction.setCaretPosition(start + REQUEST_PLACEHOLDER.length());
        });
        JButton clearButton = new JButton("Clear");

        clearButton.addActionListener(e -> instruction.setText(""));
        cancelButton.setEnabled(false);

        applyButton.addActionListener(e -> {
            if (applyButton.getText().equals(BUTTON_CANCEL_SEND) && aiConfiguration.quickActionProvider != null) {
                aiConfiguration.quickActionProvider.cancelCurrentQuickActionRequest();
                applyButton.setText(BUTTON_TEXT_SEND);
                statusLabel.setText("Cancelled");
                return;
            }

            String userInput = instruction.getText().trim();
            userInput = userInput.replace(REQUEST_PLACEHOLDER, getRequest(messageEditor));

            if (!userInput.isEmpty()) {
                statusLabel.setText("Thinking...");
                outputData = "";
                output.setText("");
                cancelButton.setEnabled(true);

                try {
                    aiConfiguration.quickActionProvider.instruct(userInput, new AIProvider.StreamingCallback() {
                        @Override
                        public void onData(String chunk) {
                            if (!chunk.isEmpty()) {
                                SwingUtilities.invokeLater(() -> {
                                    updateOutput(chunk);
                                });

                            }
                        }

                        @Override
                        public void onDone() {
                            statusLabel.setText("");
                            applyButton.setText(BUTTON_TEXT_SEND);
                            cancelButton.setEnabled(false);
                        }

                        @Override
                        public void onError(String error) {
                            statusLabel.setText(error);
                            applyButton.setText(BUTTON_TEXT_SEND);
                            cancelButton.setEnabled(false);
                        }
                    });
                } catch (Exception exc) {
                    alert("AI quick action error: " + exc.getMessage());
                    HopLa.montoyaApi.logging().logToError("AI quick action error: " + exc.getMessage());
                    cancelButton.setEnabled(false);
                }


            }
        });


        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = frame.getWidth() - 50;
                output.setMaximumSize(new Dimension(width, Short.MAX_VALUE));
                frame.repaint();
                frame.invalidate();
            }
        });

        middleButtonPanel.add(requestButton);
        middleButtonPanel.add(applyButton);
        middleButtonPanel.add(clearButton);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));


        JScrollPane scrollPane1 = new JScrollPane(instruction);
        outputScrollPane = new JScrollPane(output);
        outputScrollPane.getVerticalScrollBar().setUnitIncrement(4);

        scrollPane1.setPreferredSize(new Dimension(600, 150));
        middleButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        outputScrollPane.setPreferredSize(new Dimension(600, 150));

        centerPanel.add(scrollPane1);
        centerPanel.add(middleButtonPanel);
        centerPanel.add(outputScrollPane);

        frame.add(comboBox, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.pack();
    }

    private void updateOutput(String chunk) {
        outputData += chunk;

        if (markdownButton.isSelected()) {
            String data = renderMarkdownToHtml(outputData);
            output.setText(data);
        } else {
            output.setText(outputData);
        }

        JScrollBar verticalBar = outputScrollPane.getVerticalScrollBar();
        boolean atBottom = verticalBar.getValue() + verticalBar.getVisibleAmount() >= verticalBar.getMaximum() + 70;

        if (!atBottom) {
            SwingUtilities.invokeLater(() -> {
                verticalBar.setValue(verticalBar.getMaximum());
            });
            output.setCaretPosition(output.getDocument().getLength());
        }

    }

    private String renderMarkdownToHtml(String markdown) {
        Node document = parser.parse(markdown);
        String body = renderer.render(document);
        if (DEBUG_AI) {
            HopLa.montoyaApi.logging().logToOutput("AI quick action html: " + body);
        }
        body = body.replaceAll("<([a-zA-Z][a-zA-Z0-9-]*)(?:\\s+[^<>]*?)?(/?)>", "<$1$2>");
        return body;
    }

    private void loadCss() {
        URL cssUrl = getClass().getResource("/style.css");
        styleSheet.importStyleSheet(cssUrl);
        kit.setStyleSheet(styleSheet);
    }

    public void show(MessageEditorHttpRequestResponse messageEditor, InputEvent event, String input) {
        this.source = (JTextArea) event.getSource();
        buildFrame(messageEditor, event, input);
        frame.setVisible(true);
    }

    public void hide() {
        frame.setVisible(false);
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
