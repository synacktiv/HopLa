package com.hopla;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.hopla.Utils.alert;

public class AutoCompleteMenu {
    private static final int MAX_VISIBLE_ROWS = 10;
    private final JWindow frame;
    private final JList<String> suggestionList;
    private final MontoyaApi api;
    private final PayloadManager payloadManager;
    private final HopLa hopla;
    private JTextComponent source;
    private int caretStart = 0;
    private int caretPos = 0;

    public AutoCompleteMenu(HopLa hopla, MontoyaApi api, PayloadManager payloadManager) {
        this.api = api;
        this.hopla = hopla;
        this.payloadManager = payloadManager;

        frame = new JWindow();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(false);
        frame.setAlwaysOnTop(true);

        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setLayoutOrientation(JList.VERTICAL);
        suggestionList.setFocusable(false);
        suggestionList.setVisibleRowCount(2);
        suggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Double click
                if (e.getClickCount() == 2) {
                    insertSelectedSuggestion();
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(suggestionList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(400, 50));
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public void suggest(JTextComponent source, String input, int caretStart, int caretPos, String lastLine) {
        this.source = source;
        this.caretStart = caretStart;
        this.caretPos = caretPos;
        List<String> suggestions = this.payloadManager.getSuggestions(input);

        if (hopla.aiAutocompletionEnabled && input.length() > 4) {
            SwingUtilities.invokeLater(() -> {
                new AICompletion(frame, suggestionList, suggestions, lastLine).execute();
            });
        }

        if (suggestions.isEmpty()) {
            hide();
            return;
        }

        suggestionList.setListData(suggestions.toArray(new String[0]));
        suggestionList.setSelectedIndex(0);

        int rowHeight = suggestionList.getCellBounds(0, 0).height;
        int height = (Math.min(suggestions.size(), MAX_VISIBLE_ROWS) + 1) * rowHeight;
        frame.setPreferredSize(new Dimension(400, height));


        frame.pack();

        try {
            Point np = new Point();
            np.x = source.modelToView2D(source.getCaretPosition()).getBounds().x + source.getLocationOnScreen().x;
            np.y = source.modelToView2D(source.getCaretPosition()).getBounds().y + source.getLocationOnScreen().y + 20;
            frame.setLocation(np);
        } catch (BadLocationException e) {
            HopLa.montoyaApi.logging().logToOutput("Suggest suggestion error: " + e.getMessage());
            return;
        }

        frame.setVisible(true);


        if (Constants.DEBUG) {
            api.logging().logToOutput("suggestion: " + suggestions);
        }

    }

    public void handleKey(int keyCode) {
        int i = suggestionList.getSelectedIndex();
        switch (keyCode) {
            case KeyEvent.VK_UP:
                if (i > 0) {
                    suggestionList.setSelectedIndex(i - 1);
                    suggestionList.ensureIndexIsVisible(i - 1);
                }
                break;
            case KeyEvent.VK_DOWN:
                if (i < suggestionList.getModel().getSize() - 1) {
                    suggestionList.setSelectedIndex(i + 1);
                    suggestionList.ensureIndexIsVisible(i + 1);
                }
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_TAB:
                insertSelectedSuggestion();
                break;
            case KeyEvent.VK_ESCAPE:
                hide();
                break;
        }

    }

    private void insertSelectedSuggestion() {
        String val = suggestionList.getSelectedValue();
        if (val.contains("[CUSTOM]-> ")) {
            val = val.split("\\[CUSTOM]-> ")[1];
        }
        if (val == null || source == null) return;
        try {
            Document doc = source.getDocument();
            doc.remove(caretStart, caretPos - caretStart);
            doc.insertString(caretStart, val, null);
            source.setCaretPosition(caretStart + val.length());
        } catch (Exception ex) {
            api.logging().logToOutput("Insert suggestion error: " + ex.getMessage());
        }
        hide();
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }

    public void hide() {
        frame.setVisible(false);
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    class AICompletion extends SwingWorker<List<String>, Void> {
        private final JList<String> suggestionList;
        private final List<String> suggestions;
        private final JWindow frame;
        String input;

        public AICompletion(JWindow frame, JList<String> suggestionList, List<String> suggestions, String input) {
            this.suggestionList = suggestionList;
            this.suggestions = suggestions;
            this.input = input;
            this.frame = frame;
        }

        @Override
        protected List<String> doInBackground() throws Exception {
            return HopLa.aiConfiguration.aiProvider.autoCompletion(this.input);
        }

        @Override
        protected void done() {
            try {
                suggestions.addAll(get());
                suggestionList.setListData(suggestions.toArray(new String[0]));
                int rowHeight = suggestionList.getCellBounds(0, 0).height;
                int height = (Math.min(suggestions.size(), MAX_VISIBLE_ROWS) + 1) * rowHeight;
                frame.setPreferredSize(new Dimension(400, height));
                frame.pack();
                frame.setVisible(true);
            } catch (InterruptedException | ExecutionException e) {
                alert("AI Completion error: " + e.getMessage());
                api.logging().logToOutput("AI Completion: " + e.getMessage());
            }
        }
    }
}
