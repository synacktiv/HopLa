package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse.SelectionContext;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import static com.hopla.Utils.alert;

public class CommonMenu {
    private final MontoyaApi api;

    public CommonMenu(MontoyaApi api) {
        this.api = api;
    }


    public List<Component> buildMenu(MessageEditorHttpRequestResponse messageEditor, InputEvent event, Runnable actionHandler) {
        List<Component> items = new ArrayList<>();

        JMenu customKeywordMenu = new JMenu("Custom Keyword manager");
        items.add(customKeywordMenu);

        JMenuItem addCustomKeywordMenu = new JMenuItem("Add keyword");
        customKeywordMenu.add(addCustomKeywordMenu);
        addCustomKeywordMenu.addActionListener(e -> {
            String input = "";
            if (messageEditor.selectionOffsets().isPresent()) {
                var selection = messageEditor.selectionOffsets().get();
                int start = selection.startIndexInclusive();
                int end = selection.endIndexExclusive();
                String data = "";
                if (messageEditor.selectionContext() == SelectionContext.REQUEST) {
                    data = messageEditor.requestResponse().request().toString();
                } else {
                    data = messageEditor.requestResponse().response().toString();
                }
                input = data.substring(start, end);
            }
            HopLa.localPayloadsManager.add(input);
            actionHandler.run();
        });

        JMenuItem manageCustomKeywordMenu = new JMenuItem("Manage keywords");
        customKeywordMenu.add(manageCustomKeywordMenu);
        manageCustomKeywordMenu.addActionListener(e -> {
            HopLa.localPayloadsManager.manage();
            actionHandler.run();
        });

        JMenu copyPasteRequestResponseMenu = new JMenu("Copy HTTP Request & Response");
        items.add(copyPasteRequestResponseMenu);

        JMenuItem copyPasteRequestResponseFullItem = new JMenuItem("Request (Full) / Response (Full)");
        copyPasteRequestResponseMenu.add(copyPasteRequestResponseFullItem);
        copyPasteRequestResponseFullItem.addActionListener(e -> {
            copyRequestFullResponseFull(messageEditor);
            actionHandler.run();
        });

        JMenuItem copyPasteRequestResponseFullHeaderItem = new JMenuItem("Request (Full) / Response (Header)");
        copyPasteRequestResponseMenu.add(copyPasteRequestResponseFullHeaderItem);
        copyPasteRequestResponseFullHeaderItem.addActionListener(e -> {
            copyRequestFullResponseHeader(messageEditor);
            actionHandler.run();
        });

        JMenuItem copyPasteRequestResponseFullSelectedDataItem = new JMenuItem("Request (Full) / Response (Header + Selected Data)");
        copyPasteRequestResponseMenu.add(copyPasteRequestResponseFullSelectedDataItem);
        copyPasteRequestResponseFullSelectedDataItem.addActionListener(e -> {
            copyRequestFullResponseHeaderData(messageEditor);
            actionHandler.run();
        });

        JMenuItem addCollaboratorMenu = new JMenuItem("Add Collaborator payload [Pro Only]");
        addCollaboratorMenu.setHorizontalTextPosition(SwingConstants.LEFT);
        addCollaboratorMenu.addActionListener(e -> {
            Utils.InsertCollaboratorPayload(api, messageEditor, event);
            actionHandler.run();
        });

        JMenuItem searchReplaceMenu = new JMenuItem("Search & replace");
        searchReplaceMenu.setHorizontalTextPosition(SwingConstants.LEFT);
        items.add(searchReplaceMenu);
        searchReplaceMenu.addActionListener(e -> {
            HopLa.searchReplaceWindow.attach(messageEditor, event);
            actionHandler.run();
        });

        items.add(addCollaboratorMenu);

        JMenuItem askAIMenu = new JMenuItem("Ask AI");
        askAIMenu.setHorizontalTextPosition(SwingConstants.LEFT);
        items.add(askAIMenu);
        askAIMenu.addActionListener(e -> {
            HopLa.aiChatPanel.show();
            actionHandler.run();
        });

        return items;

    }


    private void copyRequestFullResponseFull(MessageEditorHttpRequestResponse messageEditor) {
        HttpRequestResponse reqRes = messageEditor.requestResponse();

        String output = reqRes.request().toString();

        if (reqRes.hasResponse()) {
            output += "\n-----\n\n" + reqRes.response().toString();
        }
        copyToClipboard(output);
    }

    private void copyRequestFullResponseHeader(MessageEditorHttpRequestResponse messageEditor) {
        HttpRequestResponse reqRes = messageEditor.requestResponse();

        String output = reqRes.request().toString();

        if (reqRes.hasResponse()) {
            int httpResponseBodyOffset = reqRes.response().bodyOffset();
            output += "\n-----\n\n" + reqRes.response().toString().substring(0, httpResponseBodyOffset) + "[...]";
        }

        copyToClipboard(output);
    }


    private void copyRequestFullResponseHeaderData(MessageEditorHttpRequestResponse messageEditor) {
        HttpRequestResponse reqRes = messageEditor.requestResponse();
        String output = reqRes.request().toString();

        if (reqRes.hasResponse()) {
            int httpResponseBodyOffset = reqRes.response().bodyOffset();

            if (messageEditor.selectionOffsets().isPresent() && messageEditor.selectionContext() == SelectionContext.RESPONSE) {
                var selection = messageEditor.selectionOffsets().get();
                int start = selection.startIndexInclusive();
                int end = selection.endIndexExclusive();
                String res = reqRes.response().toString();
                output += "\n-----\n\n" + res.substring(0, httpResponseBodyOffset) + "[...]" + "\n";
                output += res.substring(start, end) + "\n[...]";
            } else {
                alert("No selection");
            }
        }
        copyToClipboard(output);
    }

    private void copyToClipboard(String data) {
        StringSelection selection = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, null);
    }
}

