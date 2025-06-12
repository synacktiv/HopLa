package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;

import static java.awt.SystemColor.menu;

public class PayloadMenu {
    private final PayloadManager payloadManager;
    private final CommonMenu commonMenu;
    private JWindow frame;

    public PayloadMenu(PayloadManager payloadManager, MontoyaApi api) {
        this.payloadManager = payloadManager;
        this.commonMenu = new CommonMenu(api);
    }

    public void show(MessageEditorHttpRequestResponse messageEditor, InputEvent event) {
        if (frame != null && frame.isDisplayable()) {
            frame.dispose();
            frame = null;
            return;
        }
        frame = new JWindow();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setMinimumSize(new Dimension(200, 150));
        frame.setAlwaysOnTop(true);

        JMenuBar menuBar = new JMenuBar();

        menuBar.setLayout(new GridLayout(0, 1));
        frame.setLayout(new BorderLayout());
        frame.add(menuBar);

        PayloadDefinition payloads = payloadManager.getPayloads();
        for (Component c : payloads.buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload.value, event);
            frame.dispose();
        })) {
            menuBar.add(c);
        }

        JMenu customKeywordsMenu = HopLa.localPayloadsManager.buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload, event);
            frame.dispose();
        });
        menuBar.add(customKeywordsMenu);

        for (Component c : this.commonMenu.buildMenu(messageEditor, event, () -> {
            frame.dispose();
        })) {
            menuBar.add(c);
        }

        event.getComponent().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                frame.dispose();
            }
        });

        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        mousePos.x -= 50;
        mousePos.y -= 50;

        frame.setLocation(mousePos);
        frame.pack();
        frame.setVisible(true);


    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }

    }
}
