package com.hopla.ai;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import com.hopla.Constants;
import com.hopla.PayloadDefinition;
import com.hopla.Utils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static com.hopla.Constants.DEFAULT_AI_CONFIGURATION_PATH;
import static com.hopla.Constants.DEFAULT_BAPP_AI_CONFIGURATION_PATH;
import static com.hopla.Utils.isYamlFile;

public class AIConfiguration {
    private final MontoyaApi api;
    private final Preferences preferences;
    private final Yaml yaml;
    public LLMConfig config;
    public AIProvider defaultChatProvider;
    public AIProvider completionProvider;
    public AIProvider quickActionProvider;
    public boolean isAIConfigured = false;

    public AIConfiguration(MontoyaApi api) {
        this.api = api;
        this.preferences = api.persistence().preferences();
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(PayloadDefinition.class.getName());
        loaderoptions.setTagInspector(taginspector);

        yaml = new Yaml(new Constructor(LLMConfig.class, loaderoptions));
        load();
    }

    public String getCompletionProviderName() {
        return completionProvider != null ? completionProvider.providerName : "Not configured";
    }

    public String getQuickActionProviderName() {
        return quickActionProvider != null ? quickActionProvider.providerName : "Not configured";
    }

    public String getCurrentPath() {
        String path = preferences.getString(Constants.PREFERENCE_AI_CONFIGURATION);
        if (Constants.EXTERNAL_AI){
            return (path != null && !path.isEmpty()) ? (path) : "Not configured";
        }else {
            return (path != null && !path.isEmpty()) ? (path.equals(DEFAULT_BAPP_AI_CONFIGURATION_PATH) ? "Burp default" : path) : "Not configured";
        }
    }

    public boolean load() {
        String savedPath = preferences.getString(Constants.PREFERENCE_AI_CONFIGURATION);

        if ((savedPath == null || savedPath.isEmpty()) && !Constants.EXTERNAL_AI) {
            savedPath = DEFAULT_BAPP_AI_CONFIGURATION_PATH;
        }

        if (savedPath != null && !savedPath.isEmpty() && !DEFAULT_AI_CONFIGURATION_PATH.equals(savedPath)) {
            try {
                config = loadFromFile(savedPath);
                api.logging().logToOutput("Loaded AI configuration from saved path: " + savedPath);
                isAIConfigured = true;
                return true;
            } catch (Exception e) {
                api.logging().logToError("Failed to load AI configuration from saved path: " + savedPath + ", loading default");
                Utils.alert(Constants.ERROR_INVALID_FILE + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public void export() {
        String default_configuration_file = DEFAULT_BAPP_AI_CONFIGURATION_PATH;

        if (Constants.EXTERNAL_AI) {
            default_configuration_file = DEFAULT_AI_CONFIGURATION_PATH;
        }

        InputStream inputStream = getClass().getResourceAsStream(default_configuration_file);
        if (inputStream == null) {
            String exc = "Default AI configuration sample not found: " + default_configuration_file;
            api.logging().logToError(exc);
            Utils.alert(exc);
            return;
        }
        String sample = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            sample = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            String exc = "Failed to read AI configuration sample: " + default_configuration_file;
            api.logging().logToError(exc);
            Utils.alert(exc);
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML files (*.yaml, *.yml)", "yaml", "yml");
        fileChooser.setFileFilter(filter);
        fileChooser.setDialogTitle("Choose export location");

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".yaml") || !fileToSave.getName().toLowerCase().endsWith(".yml")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".yml");
            }


            try {
                Files.writeString(fileToSave.toPath(), sample);
                JOptionPane.showMessageDialog(null, "File saved: " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Write error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private LLMConfig loadFromFile(String path) throws Exception {

        try (InputStream in = path.equals(DEFAULT_BAPP_AI_CONFIGURATION_PATH) ? getClass().getResourceAsStream(path) : Files.newInputStream(Paths.get(path))) {
            LLMConfig config = yaml.load(in);

            if (Constants.EXTERNAL_AI) {
                AIProviderType chatProviderType = AIProviderType.valueOf(config.defaults.chat_provider);
                AIProviderType completionProviderType = AIProviderType.valueOf(config.defaults.completion_provider);
                AIProviderType quickActionProviderType = AIProviderType.valueOf(config.defaults.quick_action_provider);
                defaultChatProvider = AIProviderFactory.createProvider(chatProviderType, config, config.providers.get(chatProviderType));
                completionProvider = AIProviderFactory.createProvider(completionProviderType, config, config.providers.get(completionProviderType));
                quickActionProvider = AIProviderFactory.createProvider(quickActionProviderType, config, config.providers.get(quickActionProviderType));

                new Thread(() -> {
                    defaultChatProvider.testChatConfiguration();
                    completionProvider.testCompletionConfiguration();
                    quickActionProvider.testInstructConfiguration();
                }).start();

            } else {
                defaultChatProvider = AIProviderFactory.createProvider(AIProviderType.BURP, config, config.providers.get(AIProviderType.BURP));
                quickActionProvider = AIProviderFactory.createProvider(AIProviderType.BURP, config, config.providers.get(AIProviderType.BURP));
            }

            return config;
        }
    }

    public void chooseConfigurationFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("YAML files (*.yaml, *.yml)", "yaml", "yml");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();

            if (!isYamlFile(path)) {
                Utils.alert(Constants.ERROR_INVALID_FILE_EXTENSION);
                return;
            }

            try {
                loadFromFile(path);
                preferences.setString(Constants.PREFERENCE_AI_CONFIGURATION, path);
                load();
                Utils.success(Constants.CONFIGURATION_FILE_LOADED);
                api.logging().logToOutput(Constants.CONFIGURATION_FILE_LOADED + ": " + path);
            } catch (Exception e) {
                api.logging().logToError(Constants.ERROR_INVALID_FILE + e.getMessage());
                Utils.alert(Constants.ERROR_INVALID_FILE + e.getMessage());
            }
        }
    }

    public AIProvider getChatProvider(AIProviderType providerType) {
        return AIProviderFactory.createProvider(providerType, config, config.providers.get(providerType));
    }
}
