package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;

import static com.hopla.Utils.generateJWindow;

public class PayloadMenu {
    private static final int MARGIN_PAYLOAD_MENU = 20;
    private static final int PAYLOAD_MENU_WIDTH = 250;
    private static final int PAYLOAD_MENU_HEIGHT = 300;
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

        frame = generateJWindow();
        frame.setMinimumSize(new Dimension(PAYLOAD_MENU_WIDTH, PAYLOAD_MENU_HEIGHT));

        JMenuBar menuBar = new JMenuBar();

        menuBar.setLayout(new GridLayout(0, 1));
        frame.setLayout(new BorderLayout());
        frame.add(menuBar);

        PayloadDefinition payloads = payloadManager.getPayloads();
        for (Component c : payloads.buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload.value, event);
            if (frame != null) {
                frame.dispose();
            }
        })) {
            menuBar.add(c);
        }

        JMenu customKeywordsMenu = HopLa.localPayloadsManager.buildMenu((payload) -> {
            Utils.insertPayload(messageEditor, payload, event);
            if (frame != null) {
                frame.dispose();
            }
        });
        menuBar.add(customKeywordsMenu);

        for (Component c : this.commonMenu.buildMenu(messageEditor, event, () -> {
            if (frame != null) {
                frame.dispose();
            }
        })) {
            menuBar.add(c);
        }

        event.getComponent().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (frame != null) {
                    frame.dispose();
                }
            }
        });

        frame.pack();

        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        mousePos.x -= MARGIN_PAYLOAD_MENU;
        mousePos.y -= frame.getHeight() / 2;

        frame.setLocation(mousePos);

        frame.setVisible(true);


    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }

    }
}
