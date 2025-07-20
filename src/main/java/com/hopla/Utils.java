package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Map;

import static com.hopla.Constants.ERROR_TITLE;
import static com.hopla.Constants.EXTENSION_NAME;

public final class Utils {
    private Utils() {
    }

    public static void alert(String message) {
        JOptionPane.showMessageDialog(null, message, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
    }

    public static void success(String message) {
        JOptionPane.showMessageDialog(null, message, EXTENSION_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void insertPayload(MessageEditorHttpRequestResponse messageEditor, String payload, InputEvent event) {
        Component source = (Component) event.getSource();
        if (!(source instanceof JTextArea textArea)) {
            HopLa.montoyaApi.logging().logToError("Invalid component: " + source.getClass().getName());
            return;
        }

        int caretPosition = textArea.getCaretPosition();
        int requestLength = textArea.getText().length();

        int start = Math.min(caretPosition, requestLength);
        int end = Math.min(caretPosition, requestLength);

        if (messageEditor.selectionOffsets().isPresent()) {
            var selection = messageEditor.selectionOffsets().get();
            start = selection.startIndexInclusive();
            end = selection.endIndexExclusive();
            caretPosition = start;
        }

        try {
            Document doc = textArea.getDocument();
            doc.remove(start, end - start);
            doc.insertString(start, payload, null);
            textArea.setCaretPosition(caretPosition + payload.length());
        } catch (BadLocationException e) {
            HopLa.montoyaApi.logging().logToError("Inserting payload: " + e.getMessage());
        }
    }

    public static String getRequest(MessageEditorHttpRequestResponse messageEditor) {
        return messageEditor.requestResponse().request().toString();
    }

    public static String getResponse(MessageEditorHttpRequestResponse messageEditor) {
        if (messageEditor.requestResponse().hasResponse()) {
            return messageEditor.requestResponse().response().toString();
        }
        return "";
    }

    public static boolean isYamlFile(String path) {
        return path.toLowerCase().endsWith(".yaml") || path.toLowerCase().endsWith(".yml");
    }


    public static String getSelectedText(MessageEditorHttpRequestResponse messageEditor) {
        String input = "";
        if (messageEditor.selectionOffsets().isPresent()) {
            var selection = messageEditor.selectionOffsets().get();
            int start = selection.startIndexInclusive();
            int end = selection.endIndexExclusive();
            String data = "";
            if (messageEditor.selectionContext() == MessageEditorHttpRequestResponse.SelectionContext.REQUEST) {
                data = messageEditor.requestResponse().request().toString();
            } else {
                data = messageEditor.requestResponse().response().toString();
            }
            input = data.substring(start, end);
        }
        return input;
    }

    public static String normalizeShortcut(String shortcut) {
        if (shortcut == null || shortcut.isBlank()) {
            return null;
        }

        String s = shortcut.toLowerCase().trim();

        s = s.replaceAll("\\s+", " ");

        s = s.replace("ctrl", "Ctrl");
        s = s.replace("control", "Ctrl");
        s = s.replace("command", "Meta");
        s = s.replace("alt", "Alt");

        String[] parts = s.split("\\+");

        if (parts.length == 0) {
            return null;
        }
        parts[parts.length - 1] = parts[parts.length - 1].toUpperCase();
        return String.join("+", parts);
    }

    public static Boolean isBurpPro(MontoyaApi api) {
        return api.burpSuite().version().edition() == burp.api.montoya.core.BurpSuiteEdition.PROFESSIONAL;
    }

    public static void InsertCollaboratorPayload(MontoyaApi api, MessageEditorHttpRequestResponse messageEditor, InputEvent event) {
        if (!Utils.isBurpPro(api)) {
            return;
        }
        String value = api.collaborator().defaultPayloadGenerator().generatePayload().toString();
        Utils.insertPayload(messageEditor, value, event);
    }

    public static JFrame generateJFrame() {
        JFrame frame = new JFrame();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(true);
        frame.setFocusableWindowState(true);
        frame.setAlwaysOnTop(true);
        return frame;
    }

    public static JWindow generateJWindow() {
        JWindow frame = new JWindow();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(false);
        frame.setAlwaysOnTop(true);
        return frame;
    }

    public static JsonObject mapToJson(Map<String, Object> map) {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(map);
        return element.getAsJsonObject();
    }
}

