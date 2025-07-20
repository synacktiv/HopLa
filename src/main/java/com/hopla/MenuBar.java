package com.hopla;

import burp.api.montoya.MontoyaApi;
import com.hopla.ai.AIConfiguration;

import javax.swing.*;

import static com.hopla.Constants.*;
import static com.hopla.Utils.success;

public class MenuBar {

    private final MontoyaApi api;
    private final PayloadManager payloadManager;
    private final AIConfiguration aiConfiguration;
    private final HopLa hopla;

    public MenuBar(MontoyaApi api, HopLa hopla, PayloadManager payloadManager, AIConfiguration aiConfiguration) {
        this.api = api;
        this.hopla = hopla;
        this.payloadManager = payloadManager;
        this.aiConfiguration = aiConfiguration;
        buildAndRegisterMenu();
    }

    private String getPayloadsFilename() {
        return payloadManager.getCurrentPath().equals(DEFAULT_PAYLOAD_RESOURCE_PATH) ? "Built-in default payloads" : payloadManager.getCurrentPath();
    }

    private void buildAndRegisterMenu() {
        JMenu menu = new JMenu(Constants.EXTENSION_NAME);
        JMenuItem pathItem = new JMenuItem("Loaded payloads: " + getPayloadsFilename());
        pathItem.setEnabled(false);

        JMenuItem chooseItem = new JMenuItem(Constants.MENU_ITEM_CHOOSE_PAYLOAD);
        chooseItem.addActionListener(e -> {
            payloadManager.choosePayloadFile();
            pathItem.setText("Loaded payloads: " +getPayloadsFilename());
            reloadShortcuts();
        });

        JMenuItem reloadItem = new JMenuItem(Constants.MENU_ITEM_RELOAD_PAYLOADS);
        reloadItem.addActionListener(e -> {
            payloadManager.loadPayloads();
            api.logging().logToOutput("Payloads file reloaded: " + payloadManager.getCurrentPath());
            success(getPayloadsFilename() + " reloaded");
            api.logging().logToOutput("Reloading shortcuts");
            reloadShortcuts();
        });

        JMenuItem aiConfigurationPathItem = new JMenuItem("Loaded AI conf: " + aiConfiguration.getCurrentPath());
        aiConfigurationPathItem.setEnabled(false);

        JMenuItem completionProviderItem = new JMenuItem("AI completion provider: " + aiConfiguration.getCompletionProviderName());
        completionProviderItem.setEnabled(false);

        JMenuItem quickActionProviderItem = new JMenuItem("AI quick action provider: " + aiConfiguration.getQuickActionProviderName());
        quickActionProviderItem.setEnabled(false);


        JMenuItem aiConfigurationChooseItem = new JMenuItem(Constants.MENU_ITEM_CHOOSE_AI_CONFIGURATION);
        aiConfigurationChooseItem.addActionListener(e -> {
            aiConfiguration.chooseConfigurationFile();
            aiConfigurationPathItem.setText("Loaded AI conf: " + aiConfiguration.getCurrentPath());
            completionProviderItem.setText("AI completion provider: " +  aiConfiguration.getCompletionProviderName());
            quickActionProviderItem.setText("AI quick action provider: " + aiConfiguration.getQuickActionProviderName());
            reloadShortcuts();
        });

        JMenuItem aiConfigurationReloadItem = new JMenuItem(Constants.MENU_ITEM_RELOAD_AI_CONFIGURATION);
        aiConfigurationReloadItem.addActionListener(e -> {
            if (aiConfiguration.load()) {
                api.logging().logToOutput("AI configuration file reloaded: " + aiConfiguration.getCurrentPath());
                success(aiConfiguration.getCurrentPath() + " reloaded");
                api.logging().logToOutput("Reloading shortcuts");
                reloadShortcuts();
            }
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

        JCheckBoxMenuItem enableAIItem = new JCheckBoxMenuItem(Constants.MENU_ITEM_AI_AUTOCOMPLETION, hopla.aiAutocompletionEnabled);
        enableAIItem.addActionListener(e -> {
            hopla.aiAutocompletionEnabled = !hopla.aiAutocompletionEnabled;
            api.persistence()
                    .preferences()
                    .setBoolean(PREFERENCE_AI, hopla.aiAutocompletionEnabled);
            api.logging().logToOutput("AI autocompletion: " + (hopla.aiAutocompletionEnabled ? "enabled" : "disabled"));
        });

        JMenuItem exportDefaultAIConfItem = new JMenuItem(Constants.MENU_ITEM_EXPORT_DEFAULT_AI_CONF);
        exportDefaultAIConfItem.addActionListener(e -> {
            HopLa.aiConfiguration.export();
        });

        JMenuItem exportDefaultPayloadsItem = new JMenuItem(Constants.MENU_ITEM_EXPORT_DEFAULT_PAYLOADS);
        exportDefaultPayloadsItem.addActionListener(e -> {
            payloadManager.export();
        });


        JMenuItem clearPreferencesItem = new JMenuItem(Constants.MENU_ITEM_CLEAR_PREFERENCES);
        clearPreferencesItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(menu, "Clear preferences ?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                api.persistence().preferences().deleteBoolean(PREFERENCE_SHORTCUTS);
                api.persistence().preferences().deleteBoolean(PREFERENCE_AUTOCOMPLETION);
                api.persistence().preferences().deleteString(PREFERENCE_CUSTOM_PATH);
                api.persistence().preferences().deleteBoolean(PREFERENCE_AI);
                api.persistence().preferences().deleteString(PREFERENCE_LOCAL_DICT);
                api.persistence().preferences().deleteString(PREFERENCE_AI_CONFIGURATION);
                api.persistence().preferences().deleteString(PREFERENCE_AI_CHATS);
                success("Preferences cleared. Please reload the extension");
            }

        });

        menu.add(enableAIItem);
        menu.add(exportDefaultAIConfItem);
        menu.add(aiConfigurationChooseItem);
        menu.add(aiConfigurationReloadItem);
        menu.add(aiConfigurationPathItem);
        menu.add(completionProviderItem);
        menu.add(quickActionProviderItem);
        menu.add(new JSeparator());
        menu.add(pathItem);
        menu.add(chooseItem);
        menu.add(reloadItem);
        menu.add(exportDefaultPayloadsItem);
        menu.add(new JSeparator());
        menu.add(enableAutoCompletionItem);
        menu.add(enableShortcutsItem);
        menu.add(new JSeparator());
        menu.add(clearPreferencesItem);
        api.userInterface().menuBar().registerMenu(menu);
    }

    private void reloadShortcuts() {
        hopla.disableShortcuts();
        hopla.enableShortcuts();
    }

}