package com.hopla;

import burp.api.montoya.MontoyaApi;
import com.hopla.ai.AIConfiguration;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.hopla.Constants.DEBUG;
import static com.hopla.Utils.alert;
import static com.hopla.Utils.generateJWindow;

public class AutoCompleteMenu {
    public static final String CUSTOM_KEYWORD_SEPARATOR = " [CUSTOM]-> ";
    public static final String AI_KEYWORD_SEPARATOR = " [AI] ";
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int FRAME_TOP_MARGIN = 20;
    private static final int FRAME_WIDTH = 400;
    private static final int FRAME_HEIGHT = 50;
    private static final int SCROLL_STEP = 50;
    private final JWindow frame;
    private final JList<String> suggestionList;
    private final MontoyaApi api;
    private final PayloadManager payloadManager;
    private final HopLa hopla;
    private final JScrollBar hBar;
    private final AIConfiguration aiConfiguration;
    DebouncedSwingWorker<List<String>, Void> debouncer = new DebouncedSwingWorker<>();
    private JTextComponent source;
    private int caretStart = 0;
    private int caretPos = 0;

    public AutoCompleteMenu(HopLa hopla, MontoyaApi api, PayloadManager payloadManager, AIConfiguration aiConfiguration) {
        this.api = api;
        this.hopla = hopla;
        this.payloadManager = payloadManager;
        this.aiConfiguration = aiConfiguration;

        frame = generateJWindow();

        suggestionList = new JList<>();
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setLayoutOrientation(JList.VERTICAL);
        suggestionList.setFocusable(false);
        suggestionList.setVisibleRowCount(MIN_VISIBLE_ROWS);
        suggestionList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Double click
                if (e.getClickCount() == 2) {
                    insertSelectedSuggestion();
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(suggestionList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        hBar = scrollPane.getHorizontalScrollBar();
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public void suggest(JTextComponent source, String input, int caretStart, int caretPos, Completer.CaretContext caretContext) {
        this.source = source;
        this.caretStart = caretStart;
        this.caretPos = caretPos;

        List<String> suggestions = this.payloadManager.getSuggestions(input);

        if (hopla.aiAutocompletionEnabled && aiConfiguration.isAIConfigured && input.length() > HopLa.aiConfiguration.config.autocompletion_min_chars) {
            debouncer.trigger(() ->
                    new AICompletion(suggestionList, suggestions, input, caretContext)
            );
        }

        if (suggestions.isEmpty()) {
            hide();
            return;
        }

        suggestionList.setListData(suggestions.toArray(new String[0]));
        suggestionList.setSelectedIndex(0);

        show(suggestions.size());
        hBar.setValue(0);

        if (DEBUG) {
            api.logging().logToOutput("suggestion: " + suggestions);
        }

    }

    private void show(int lines) {
        int rowHeight = suggestionList.getCellBounds(0, 0).height;
        int heightList = ((Math.min(lines, MAX_VISIBLE_ROWS) + 1) * rowHeight) + 25;
        if (heightList < 50) {
            heightList = 50;
        }

        Point np = new Point();
        try {
            np.x = source.modelToView2D(source.getCaretPosition()).getBounds().x + source.getLocationOnScreen().x;
            np.y = source.modelToView2D(source.getCaretPosition()).getBounds().y + source.getLocationOnScreen().y + FRAME_TOP_MARGIN;
            frame.setLocation(np);
        } catch (BadLocationException e) {
            HopLa.montoyaApi.logging().logToError("Suggest suggestion error: " + e.getMessage());
            return;
        }

        Rectangle screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        int height = Math.min(heightList, screenBounds.height - np.y) - FRAME_TOP_MARGIN;
        frame.setPreferredSize(new Dimension(FRAME_WIDTH, height));

        frame.pack();
        frame.setVisible(true);
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
            case KeyEvent.VK_RIGHT:
                hBar.setValue(Math.min(hBar.getValue() + SCROLL_STEP, hBar.getMaximum()));
                break;
            case KeyEvent.VK_LEFT:
                hBar.setValue(Math.min(hBar.getValue() - SCROLL_STEP, 0));
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
        if (val.contains(CUSTOM_KEYWORD_SEPARATOR)) {
            val = val.split(Pattern.quote(CUSTOM_KEYWORD_SEPARATOR))[1];
        }
        if (val.contains(AI_KEYWORD_SEPARATOR)) {
            val = val.split(Pattern.quote(AI_KEYWORD_SEPARATOR))[1];
        }


        if (val == null || source == null) return;
        try {
            Document doc = source.getDocument();
            doc.remove(caretStart, caretPos - caretStart);
            doc.insertString(caretStart, val, null);
            source.setCaretPosition(caretStart + val.length());
        } catch (Exception ex) {
            api.logging().logToError("Insert suggestion error: " + ex.getMessage());
        }
        debouncer.cancel();
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

    public static class DebouncedSwingWorker<T, V> {
        private Supplier<SwingWorker<T, V>> workerSupplier;
        private SwingWorker<T, V> currentWorker;

        public DebouncedSwingWorker() {
        }

        public void cancel() {
            if (currentWorker != null && !currentWorker.isDone()) {
                currentWorker.cancel(true);
            }
        }

        public void trigger(Supplier<SwingWorker<T, V>> workerSupplier) {
            cancel();

            currentWorker = workerSupplier.get();
            currentWorker.execute();
        }

    }

    class AICompletion extends SwingWorker<List<String>, Void> {
        private final JList<String> suggestionList;
        private final List<String> suggestions;
        private final Completer.CaretContext caretContext;
        private final String input;

        public AICompletion(JList<String> suggestionList, List<String> suggestions, String input, Completer.CaretContext caretContext) {
            this.suggestionList = suggestionList;
            this.suggestions = suggestions;
            this.caretContext = caretContext;
            this.input = input;
        }

        @Override
        protected List<String> doInBackground() throws Exception {
            try {
                return HopLa.aiConfiguration.completionProvider.autoCompletion(this.caretContext);
            } catch (Exception e) {
                api.logging().logToError("AI Completion cancelled, input: " + input);
                throw e;
            }
        }

        @Override
        protected void done() {
            try {
                suggestions.addAll(0, get().stream().map(s -> AI_KEYWORD_SEPARATOR + input + s).toList());
                suggestionList.setListData(suggestions.toArray(new String[0]));
                if (!suggestions.isEmpty()) {
                    if (DEBUG) {
                        api.logging().logToOutput("AI suggestion: " + suggestions);
                    }
                    show(suggestions.size());
                }

            } catch (InterruptedException | ExecutionException e) {
                alert("AI Completion error: " + e.getMessage());
                api.logging().logToError("AI Completion: " + e.getMessage());
            } catch (CancellationException exc) {
                if (DEBUG) {
                    api.logging().logToError("AI Completion cancelled, input: " + input);
                }
            }
        }
    }
}
