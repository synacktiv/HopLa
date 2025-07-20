package com.hopla;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.Preferences;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.hopla.Constants.DEFAULT_RESOURCE_ENCRYPT_KEY;
import static com.hopla.Utils.isYamlFile;

public class PayloadManager {

    private final MontoyaApi api;
    private final Preferences preferences;
    private final LocalPayloadsManager localPayloadsManager;
    private PayloadDefinition payloads;

    public PayloadManager(MontoyaApi api, LocalPayloadsManager localPayloadsManager) {
        this.api = api;
        this.preferences = api.persistence().preferences();
        this.localPayloadsManager = localPayloadsManager;
        loadPayloads();
    }

    private static List<String> filterSuggestions(String input, Set<String> options) {
        return options.stream()
                .filter(word -> word.startsWith(input))
                .map(word -> new AbstractMap.SimpleEntry<>(word, longestCommonPrefixLength(input, word)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    private static int longestCommonPrefixLength(String a, String b) {
        int len = Math.min(a.length(), b.length());
        int i = 0;
        while (i < len && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    public void export() {
        InputStream inputStream = getClass().getResourceAsStream(Constants.DEFAULT_PAYLOAD_RESOURCE_PATH);
        if (inputStream == null) {
            String exc = "Default Payloads configuration sample not found: " + Constants.DEFAULT_PAYLOAD_RESOURCE_PATH;
            api.logging().logToError(exc);
            Utils.alert(exc);
            return;
        }
        String sample = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            sample = reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            String exc = "Failed to read Payloads configuration sample: " + Constants.DEFAULT_PAYLOAD_RESOURCE_PATH;
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

    public void loadPayloads() {
        String savedPath = preferences.getString(Constants.PREFERENCE_CUSTOM_PATH);

        if (savedPath != null && !savedPath.isEmpty() && !Constants.DEFAULT_PAYLOAD_RESOURCE_PATH.equals(savedPath)) {
            try {
                payloads = loadFromFile(savedPath, false);
                api.logging().logToOutput("Loaded payloads from saved path: " + savedPath);
                return;
            } catch (Exception e) {
                api.logging().logToError("Failed to load payloads from saved path: " + savedPath + ", loading default");
                Utils.alert(Constants.ERROR_INVALID_FILE + e.getMessage());
            }
        }

        // Load from default resource
        try (InputStream in = getClass().getResourceAsStream(Constants.DEFAULT_PAYLOAD_RESOURCE_PATH)) {
            if (in == null) {
                api.logging().logToError("Default payload resource not found. " + Constants.DEFAULT_PAYLOAD_RESOURCE_PATH);
                payloads = new PayloadDefinition();
            } else {
                payloads = loadFromInputStream(in, true);
                preferences.setString(Constants.PREFERENCE_CUSTOM_PATH, Constants.DEFAULT_PAYLOAD_RESOURCE_PATH);
                api.logging().logToOutput("Loaded payloads from default resource. " + Constants.DEFAULT_PAYLOAD_RESOURCE_PATH);
            }
        } catch (Exception e) {
            api.logging().logToError(Constants.ERROR_INVALID_FILE + e.getMessage());
            Utils.alert(Constants.ERROR_INVALID_FILE + e.getMessage());
            api.logging().logToError("Failed to load default payloads resource. " + Constants.DEFAULT_PAYLOAD_RESOURCE_PATH);
            payloads = new PayloadDefinition();
        }
    }


    private PayloadDefinition loadFromFile(String path, boolean decrypt) throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(path))) {
            return loadFromInputStream(in, decrypt);
        }
    }

    private PayloadDefinition loadFromInputStream(InputStream in, boolean decrypt) throws Exception {
        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(PayloadDefinition.class.getName());
        loaderoptions.setTagInspector(taginspector);

        Yaml yaml = new Yaml(new Constructor(PayloadDefinition.class, loaderoptions));
        PayloadDefinition data = null;
        if (decrypt){
            // decrypt embedded resource
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int r;
            while ((r = in.read(buffer)) != -1) {
                baos.write(buffer, 0, r);
            }
            byte[] encryptedData = baos.toByteArray();

            byte[] keyBytes = DEFAULT_RESOURCE_ENCRYPT_KEY.getBytes();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] content = cipher.doFinal(encryptedData);
            data = yaml.load(new String(content));

        }else{
            data = yaml.load(in);
        }

        validateShortcuts(data);
        return data;
    }

    private void validateShortcuts(PayloadDefinition definition) {
        Set<String> shortcuts = new HashSet<>();

        for (PayloadDefinition.Category category : definition.categories) {
            this.recursiveValidateShortcuts(category, shortcuts);
        }

        if (!shortcuts.add(Utils.normalizeShortcut(definition.shortcut_payload_menu))) {
            throw new IllegalArgumentException(
                    "Duplicate shortcut found: " + definition.shortcut_payload_menu
            );
        }
        if (!shortcuts.add(Utils.normalizeShortcut(definition.shortcut_search_and_replace))) {
            throw new IllegalArgumentException(
                    "Duplicate shortcut found: " + definition.shortcut_search_and_replace
            );
        }
    }

    private void recursiveValidateShortcuts(PayloadDefinition.Category category, Set<String> collector) {
        if (category.payloads != null) {
            for (PayloadDefinition.Payload payload : category.payloads) {
                if (payload.shortcut != null && !payload.shortcut.isBlank()) {
                    if (!collector.add(Utils.normalizeShortcut(payload.shortcut))) {
                        throw new IllegalArgumentException(
                                "Duplicate shortcut found: " + payload.shortcut + " (used in payload: " + payload.name + ")"
                        );
                    }
                }
            }
        }

        if (category.categories != null) {
            for (PayloadDefinition.Category sub : category.categories) {
                recursiveValidateShortcuts(sub, collector);
            }
        }
    }

    public List<String> getSuggestions(String input) {
        List<String> suggestions = new ArrayList<>();
        Set<String> localPayloadsSet = new HashSet<>(localPayloadsManager.getPayloads());

        List<String> results = filterSuggestions(input, localPayloadsSet);
        suggestions.addAll(results);
        if (suggestions.size() >= 25) {
            return suggestions.subList(0, 25);
        }

        results = filterSuggestions(input, this.payloads.flattenPayloadValues());
        suggestions.addAll(results);
        if (suggestions.size() >= 25) {
            return suggestions.subList(0, 25);
        }

        results = filterSuggestions(input, this.payloads.flattenKeywordsValues());
        suggestions.addAll(results);

        return suggestions.subList(0, Math.min(25, suggestions.size()));
    }

    public void choosePayloadFile() {
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
                PayloadDefinition loaded = loadFromFile(path, false);
                if (loaded.isEmpty()) {
                    Utils.alert(Constants.ERROR_EMPTY_FILE);
                    return;
                }

                this.payloads = loaded;
                preferences.setString(Constants.PREFERENCE_CUSTOM_PATH, path);
                Utils.success(Constants.FILE_LOADED);
                api.logging().logToOutput(Constants.FILE_LOADED + ": " + path);
            } catch (Exception e) {
                api.logging().logToError(Constants.ERROR_INVALID_FILE + e.getMessage());
                Utils.alert(Constants.ERROR_INVALID_FILE + e.getMessage());
            }
        }
    }

    public String getCurrentPath() {
        String path = preferences.getString(Constants.PREFERENCE_CUSTOM_PATH);
        return (path != null && !path.isEmpty()) ? path : Constants.DEFAULT_PAYLOAD_RESOURCE_PATH;
    }


    public PayloadDefinition getPayloads() {
        return payloads;
    }
}
