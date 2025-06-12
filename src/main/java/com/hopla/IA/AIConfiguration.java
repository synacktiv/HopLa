package com.hopla.IA;

import com.hopla.HopLa;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.Proxy;
import java.util.Objects;

import static com.hopla.Constants.PREFERENCE_IA_CONFIGURATION;
import static com.hopla.Utils.success;

public class AIConfiguration {
    private final JFrame frame;
    private final JComboBox<AIProviderType> providerComboBox;
    private final JTextField apiUrlField;
    private final JTextField apiKeyField;
    private final JTextField modelField;
    private final JComboBox<Proxy.Type> proxyTypeComboBox;
    private final JTextField proxyUrlField;
    private final JTextField proxyPortField;
    private final Yaml yaml;
    private final JLabel proxyUrlLabel;
    private final JLabel proxyPortLabel;
    public AIProvider aiProvider;
    private UserAiConfiguration userAiConfiguration = new UserAiConfiguration();

    public AIConfiguration() {


        var loaderoptions = new LoaderOptions();
        TagInspector taginspector =
                tag -> tag.getClassName().equals(UserAiConfiguration.class.getName());
        loaderoptions.setTagInspector(taginspector);

        this.yaml = new Yaml(new Constructor(UserAiConfiguration.class, loaderoptions));

        frame = new JFrame();
        frame.getRootPane().putClientProperty("windowTitle", "");
        frame.setName("");
        frame.setLocationRelativeTo(null);
        frame.setAutoRequestFocus(true);
        frame.setFocusableWindowState(true);
        frame.setAlwaysOnTop(true);
        frame.setPreferredSize(new Dimension(500, 250));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel providerLabel = new JLabel("Provider :");
        providerLabel.setPreferredSize(new Dimension(100, 20));
        formPanel.add(providerLabel);

        providerComboBox = new JComboBox<>(AIProviderType.values());
        providerComboBox.setPreferredSize(new Dimension(350, 20));
        formPanel.add(providerComboBox);

        JLabel apiUrlLabel = new JLabel("API URL :");
        formPanel.add(apiUrlLabel);
        apiUrlLabel.setPreferredSize(new Dimension(100, 20));


        apiUrlField = new JTextField("http://localhost:11434/api/generate");
        apiUrlField.setPreferredSize(new Dimension(350, 20));
        formPanel.add(apiUrlField);

        JLabel apiKeyLabel = new JLabel("API Key:");
        formPanel.add(apiKeyLabel);
        apiKeyLabel.setPreferredSize(new Dimension(100, 20));

        apiKeyField = new JPasswordField();
        formPanel.add(apiKeyField);
        apiKeyField.setPreferredSize(new Dimension(350, 20));


        JLabel modelLabel = new JLabel("Model :");
        formPanel.add(modelLabel);
        modelLabel.setPreferredSize(new Dimension(100, 20));

        modelField = new JTextField("qwen2.5-coder:3b");
        modelField.setPreferredSize(new Dimension(350, 20));
        formPanel.add(modelField);


        JLabel proxyTypeLabel = new JLabel("Proxy Type:");
        formPanel.add(proxyTypeLabel);
        proxyTypeLabel.setPreferredSize(new Dimension(100, 20));

        Proxy.Type[] proxyTypes = {Proxy.Type.DIRECT, Proxy.Type.HTTP, Proxy.Type.SOCKS};
        proxyTypeComboBox = new JComboBox<>(proxyTypes);
        proxyTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == Proxy.Type.DIRECT) {
                    setText("No proxy");
                }
                return this;
            }
        });
        formPanel.add(proxyTypeComboBox);
        proxyTypeComboBox.setPreferredSize(new Dimension(350, 20));


        proxyUrlLabel = new JLabel("Proxy URL :");
        formPanel.add(proxyUrlLabel);
        proxyUrlLabel.setPreferredSize(new Dimension(100, 20));
        proxyUrlField = new JTextField();
        formPanel.add(proxyUrlField);
        proxyUrlField.setPreferredSize(new Dimension(350, 20));


        proxyPortLabel = new JLabel("Proxy Port :");
        proxyPortLabel.setPreferredSize(new Dimension(100, 20));
        formPanel.add(proxyPortLabel);
        proxyPortField = new JTextField();
        formPanel.add(proxyPortField);
        proxyPortField.setPreferredSize(new Dimension(350, 20));


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton testButton = new JButton("Test");
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        frame.add(formPanel);
        frame.add(buttonPanel, BorderLayout.SOUTH);


        ActionListener proxyAction = e -> updateProxyFieldsState();
        proxyTypeComboBox.addActionListener(proxyAction);

        saveButton.addActionListener(e -> saveConfiguration());
        cancelButton.addActionListener(e -> hide());
        testButton.addActionListener(e -> test());

        updateProxyFieldsState();
        loadConfiguration();

        frame.pack();
        frame.setLocationRelativeTo(null);

    }

    public void show() {
        frame.setVisible(true);
    }

    private void loadConfiguration() {
        String config = HopLa.montoyaApi.persistence().preferences().getString(PREFERENCE_IA_CONFIGURATION);
        if (config == null || config.isEmpty()) {
            return;
        }

        userAiConfiguration = yaml.load(config);
        proxyPortField.setText(String.valueOf(userAiConfiguration.proxyPort));
        proxyUrlField.setText(userAiConfiguration.proxyUrl);
        proxyTypeComboBox.setSelectedItem(userAiConfiguration.proxyType);
        providerComboBox.setSelectedItem(userAiConfiguration.aiProviderType);
        apiUrlField.setText(userAiConfiguration.apiUrl);
        apiKeyField.setText(userAiConfiguration.apiKey);
        modelField.setText(userAiConfiguration.model);
        updateProxyFieldsState();
        aiProvider = AIProviderFactory.createProvider((AIProviderType) Objects.requireNonNull(providerComboBox.getSelectedItem()), userAiConfiguration);
    }

    private void test() {
        UserAiConfiguration config = getJFrameConfiguration();
        AIProvider currentProvider = AIProviderFactory.createProvider((AIProviderType) Objects.requireNonNull(providerComboBox.getSelectedItem()), config);
        currentProvider.testCommunication();
    }

    private UserAiConfiguration getJFrameConfiguration() {
        UserAiConfiguration config = new UserAiConfiguration();
        config.proxyPort = proxyPortField.getText().isEmpty() ? 8080 : Integer.parseInt(proxyPortField.getText());
        config.proxyUrl = proxyUrlField.getText().trim();
        config.proxyType = Proxy.Type.valueOf(Objects.requireNonNull(proxyTypeComboBox.getSelectedItem()).toString());
        config.apiUrl = apiUrlField.getText().trim();
        config.apiKey = apiKeyField.getText().trim();
        config.aiProviderType = (AIProviderType) providerComboBox.getSelectedItem();
        config.model = modelField.getText().trim();
        return config;
    }

    public void saveConfiguration() {
        userAiConfiguration = getJFrameConfiguration();
        aiProvider = AIProviderFactory.createProvider((AIProviderType) Objects.requireNonNull(providerComboBox.getSelectedItem()), userAiConfiguration);
        String output = yaml.dump(userAiConfiguration);
        HopLa.montoyaApi.persistence().preferences().setString(PREFERENCE_IA_CONFIGURATION, output);
        success("Configuration saved");
        hide();
    }

    private void updateProxyFieldsState() {
        Proxy.Type selectedType = (Proxy.Type) proxyTypeComboBox.getSelectedItem();
        boolean isProxyEnabled = (selectedType != Proxy.Type.DIRECT);

        proxyUrlLabel.setEnabled(isProxyEnabled);
        proxyUrlField.setEnabled(isProxyEnabled);
        proxyPortLabel.setEnabled(isProxyEnabled);
        proxyPortField.setEnabled(isProxyEnabled);
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }

    public void hide() {
        frame.setVisible(false);
    }
}
