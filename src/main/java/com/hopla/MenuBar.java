package com.hopla;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;

import static com.hopla.Constants.PREFERENCE_IA;

public class MenuBar {

    private final MontoyaApi api;
    private final PayloadManager payloadManager;
    private final HopLa hopla;

    public MenuBar(MontoyaApi api, HopLa hopla, PayloadManager payloadManager) {
        this.api = api;
        this.hopla = hopla;
        this.payloadManager = payloadManager;
        buildAndRegisterMenu();
    }

    private void buildAndRegisterMenu() {

        JMenu menu = new JMenu(Constants.EXTENSION_NAME);

        JMenuItem pathItem = new JMenuItem("Loaded: " + payloadManager.getCurrentPath());
        pathItem.setEnabled(false);

        JMenuItem chooseItem = new JMenuItem(Constants.MENU_ITEM_CHOOSE_PAYLOAD);
        chooseItem.addActionListener(e -> {
            payloadManager.choosePayloadFile();
        });

        JMenuItem reloadItem = new JMenuItem(Constants.MENU_ITEM_RELOAD_PAYLOADS);
        reloadItem.addActionListener(e -> {
            payloadManager.loadPayloads();
            api.logging().logToOutput("Payload file reloaded: " + payloadManager.getCurrentPath());
            Utils.success(payloadManager.getCurrentPath() + " reloaded");
            api.logging().logToOutput("Reloading shortcuts");
            hopla.disableShortcuts();
            hopla.enableShortcuts();
        });

        JCheckBoxMenuItem enableAutoCompletionItem = new JCheckBoxMenuItem(Constants.MENU_ITEM_AUTOCOMPLETION, hopla.autocompletionEnabled);
        enableAutoCompletionItem.addActionListener(e -> {
            if (hopla.autocompletionEnabled) {
                hopla.disableAutocompletion();
            } else {
                hopla.enableAutocompletion();
            }
            api.logging().logToOutput("Autocompletion: " + (hopla.autocompletionEnabled ? "enabled" : "disabled"));

        });

        JCheckBoxMenuItem enableShortcutsItem = new JCheckBoxMenuItem(Constants.MENU_ITEM_SHORTCUTS, hopla.shortcutsEnabled);
        enableShortcutsItem.addActionListener(e -> {
            if (hopla.shortcutsEnabled) {
                hopla.disableShortcuts();
            } else {
                hopla.enableShortcuts();
            }
            api.logging().logToOutput("Shortcuts: " + (hopla.shortcutsEnabled ? "enabled" : "disabled"));
        });

        JCheckBoxMenuItem enableIAItem = new JCheckBoxMenuItem(Constants.MENU_ITEM_AI_AUTOCOMPLETION, hopla.aiAutocompletionEnabled);
        enableIAItem.addActionListener(e -> {
            hopla.aiAutocompletionEnabled = !hopla.aiAutocompletionEnabled;
            api.persistence()
                    .preferences()
                    .setBoolean(PREFERENCE_IA, hopla.aiAutocompletionEnabled);
            api.logging().logToOutput("AI autocompletion: " + (hopla.aiAutocompletionEnabled ? "enabled" : "disabled"));
        });

        JMenuItem configureIAItem = new JMenuItem(Constants.MENU_ITEM_CONFIGURE_AI);
        configureIAItem.addActionListener(e -> {
            HopLa.aiConfiguration.show();
        });

        menu.add(enableIAItem);
        menu.add(configureIAItem);
        menu.add(pathItem);
        menu.add(chooseItem);
        menu.add(reloadItem);
        menu.add(enableAutoCompletionItem);
        menu.add(enableShortcutsItem);

        api.userInterface().menuBar().registerMenu(menu);
    }


}