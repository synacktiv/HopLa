package com.hopla;

public final class Constants {
    public static final String VERSION = "2.1.0";
    public static final String EXTENSION_NAME = "HopLa";
    public static final String INIT_MESSAGE = "HopLa initialized v" + VERSION + "\n\nFor i3, add the following line to $HOME/.config/i3/config for floating frame:\n" +
            "    for_window [class=\".*burp-StartBurp.*\" title=\"^ $\"] floating enable\n\nHappy hacking !\n@alexisdanizan\n--------------";
    public static final String PREFERENCE_CUSTOM_PATH = "HOPLA_PAYLOAD_PATH";
    public static final String PREFERENCE_AUTOCOMPLETION = "HOPLA_AUTOCOMPLETION";
    public static final String PREFERENCE_SHORTCUTS = "HOPLA_SHORTCUTS";
    public static final String PREFERENCE_AI = "HOPLA_AI";
    public static final String PREFERENCE_LOCAL_DICT = "HOPLA_LOCAL_DICT";
    public static final String PREFERENCE_AI_CONFIGURATION = "HOPLA_AI_CONFIGURATION";
    public static final String PREFERENCE_AI_CHATS = "HOPLA_AI_CHATS";
    public static final String DEFAULT_PAYLOAD_RESOURCE_PATH = "/default-payloads.enc.yaml";
    public static final String DEFAULT_AI_CONFIGURATION_PATH = "/ai-configuration-sample.yaml";
    public static final String DEFAULT_BAPP_AI_CONFIGURATION_PATH = "/ai-bapp-configuration-sample.yaml";
    public static final String MENU_ITEM_CHOOSE_PAYLOAD = "Choose payloads file";
    public static final String MENU_ITEM_CHOOSE_AI_CONFIGURATION = "Choose AI configuration file";
    public static final String MENU_ITEM_RELOAD_PAYLOADS = "Reload Payloads";
    public static final String MENU_ITEM_RELOAD_AI_CONFIGURATION = "Reload AI Configuration";
    public static final String MENU_ITEM_AUTOCOMPLETION = "Enable Autocompletion";
    public static final String MENU_ITEM_SHORTCUTS = "Enable Shortcuts";
    public static final String MENU_ITEM_AI_AUTOCOMPLETION = "Enable AI autocompletion";
    public static final String MENU_ITEM_EXPORT_DEFAULT_AI_CONF = "Export default AI configuration";
    public static final String MENU_ITEM_CLEAR_PREFERENCES = "Clear preferences";
    public static final String MENU_ITEM_EXPORT_DEFAULT_PAYLOADS = "Export default payloads";
    public static final String ERROR_INVALID_FILE_EXTENSION = "Please select a .yaml or .yml file.";
    public static final String ERROR_EMPTY_FILE = "The selected YAML file is empty or invalid.";
    public static final String ERROR_INVALID_FILE = "Failed to load the file:\n";
    public static final String ERROR_BURP_AI_DISABLED = "This feature is only available on the AI version of Burp.";
    public static final String FILE_LOADED = "Payloads file loaded";
    public static final String CONFIGURATION_FILE_LOADED = "AI configuration file loaded";
    public static final String ERROR_TITLE = "HopLa Error";
    public static final String DEFAULT_RESOURCE_ENCRYPT_KEY = "1234567890123456";
    // Allow external AI providers
    public static final boolean EXTERNAL_AI = true;
    public static boolean DEBUG = false;
    public static boolean DEBUG_AI = false;
    public static boolean AWT_DEBUG = false;

    private Constants() {
    }
}
