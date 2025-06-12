package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContextMenu implements ContextMenuItemsProvider {

    private final PayloadManager payloadManager;
    private final CommonMenu commonMenu;


    public ContextMenu(MontoyaApi api, PayloadManager payloadManager) {
        this.payloadManager = payloadManager;
        this.commonMenu = new CommonMenu(api);

    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        if (event.messageEditorRequestResponse().isEmpty()) {
            return null;
        }

        MessageEditorHttpRequestResponse messageEditor = event.messageEditorRequestResponse().get();

        List<Component> items = new ArrayList<>();

        for (Component c : payloadManager.getPayloads().buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload.value, event.inputEvent());
        })) {
            items.add(c);
        }

        JMenu customKeywordsMenu = HopLa.localPayloadsManager.buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload, event.inputEvent());
        });
        items.add(customKeywordsMenu);


        for (Component c : this.commonMenu.buildMenu(messageEditor, event.inputEvent(), () -> {
        })) {
            items.add(c);
        }

        return items;
    }
}