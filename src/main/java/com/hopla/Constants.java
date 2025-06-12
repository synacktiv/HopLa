package com.hopla;

public final class Constants {
    public static final String EXTENSION_NAME = "HopLa";
    public static final String INIT_MESSAGE = "HopLa initialized.\n\nFor i3, add the following line to $HOME/.config/i3/config for floating frame:\n" +
            "    for_window [class=\".*burp-StartBurp.*\" title=\"^ $\"] floating enable\n\nHappy hacking !\nAlexis Danizan from Synacktiv\n--------------";
    public static final String PREFERENCE_CUSTOM_PATH = "HOPLA_PAYLOAD_PATH";
    public static final String PREFERENCE_AUTOCOMPLETION = "HOPLA_AUTOCOMPLETION";
    public static final String PREFERENCE_SHORTCUTS = "HOPLA_SHORTCUTS";
    public static final String PREFERENCE_IA = "HOPLA_IA";
    public static final String PREFERENCE_LOCAL_DICT = "HOPLA_LOCAL_DICT";
    public static final String PREFERENCE_IA_CONFIGURATION = "PREFERENCE_IA_CONFIGURATION";
    public static final String DEFAULT_PAYLOAD_RESOURCE_PATH = "/default-payloads.enc.yaml";
    public static final String MENU_ITEM_CHOOSE_PAYLOAD = "Choose payload file";
    public static final String MENU_ITEM_RELOAD_PAYLOADS = "Reload Payloads";
    public static final String MENU_ITEM_AUTOCOMPLETION = "Enable Autocompletion";
    public static final String MENU_ITEM_SHORTCUTS = "Enable Shortcuts";
    public static final String MENU_ITEM_AI_AUTOCOMPLETION = "Enable AI autocompletion (Beta)";
    public static final String MENU_ITEM_CONFIGURE_AI = "Configure AI";
    public static final String ERROR_INVALID_FILE_EXTENSION = "Please select a .yaml or .yml file.";
    public static final String ERROR_EMPTY_FILE = "The selected YAML file is empty or invalid.";
    public static final String ERROR_INVALID_FILE = "Failed to load the file:\n";
    public static final String FILE_LOADED = "Payload file loaded";
    public static final String ERROR_TITLE = "HopLa Error";
    public static final String DEFAULT_RESOURCE_ENCRYPT_KEY = "1234567890123456";
    public static boolean DEBUG = false;
    public static boolean AWT_DEBUG = false;

    private Constants() {
    }
}
