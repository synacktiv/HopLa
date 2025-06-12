package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.nio.charset.StandardCharsets;

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
        HttpRequest request = messageEditor.requestResponse().request();
        ByteArray original = request.toByteArray();
        ByteArray payloadBytes = ByteArray.byteArray(payload.getBytes(StandardCharsets.UTF_8));

        Component source = (Component) event.getSource();
        int caretPosition = messageEditor.caretPosition();
        if (source instanceof JTextArea textArea) {
            caretPosition = textArea.getCaretPosition();

        }

        int start = Math.min(caretPosition, original.length());
        int end = Math.min(caretPosition, original.length());

        if (messageEditor.selectionOffsets().isPresent()) {
            var selection = messageEditor.selectionOffsets().get();
            start = selection.startIndexInclusive();
            end = selection.endIndexExclusive();
            caretPosition = start;
        }

        ByteArray modified = original.subArray(0, start)
                .withAppended(payloadBytes);

        if (end < original.length()) {
            modified = modified.withAppended(original.subArray(end, original.length()));
        }


        HttpRequest patched_request = HttpRequest.httpRequest(modified);
        messageEditor.setRequest(patched_request);
        if (source instanceof JTextArea textArea) {
            textArea.setCaretPosition(caretPosition + payload.length());
        }
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
}

