package com.hopla;

import burp.api.montoya.MontoyaApi;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.hopla.Constants.PREFERENCE_LOCAL_DICT;

public class LocalPayloadsManager {
    private final MontoyaApi api;
    private final Yaml yaml = new Yaml();
    private JFrame frameAdd;
    private JFrame frameManage;
    private HashMap<String, String> localPayloads = new HashMap<>();
    private DefaultListModel<String> listModel;

    public LocalPayloadsManager(MontoyaApi api) {
        this.api = api;
        this.loadLocalPayloads();
    }

    public Set<String> getPayloads() {
        Set<String> concatenatedSet = new HashSet<>();

        for (Map.Entry<String, String> entry : localPayloads.entrySet()) {
            String combined = entry.getKey() + " [CUSTOM]-> " + entry.getValue();
            concatenatedSet.add(combined);
            concatenatedSet.add(entry.getValue());
        }
        return concatenatedSet;
    }

    private void loadLocalPayloads() {
        String payloads = this.api.persistence().preferences().getString(PREFERENCE_LOCAL_DICT);
        if (payloads == null) {
            return;
        }
        localPayloads = yaml.load(payloads);
    }

    public void saveLocalPayload() {
        String output = yaml.dump(localPayloads);
        this.api.persistence().preferences().setString(PREFERENCE_LOCAL_DICT, output);
    }

    public void add(String input) {
        if (frameAdd != null) {
            frameAdd.dispose();
        }
        frameAdd = new JFrame();
        frameAdd.getRootPane().putClientProperty("windowTitle", "");
        frameAdd.setName("");
        frameAdd.setLocationRelativeTo(null);
        frameAdd.setAutoRequestFocus(true);
        frameAdd.setFocusableWindowState(true);
        frameAdd.setAlwaysOnTop(true);


        JTextField keyField = new JTextField(50);
        JTextField valueField = new JTextField(50);
        valueField.setText(input);
        JButton addButton = new JButton("Add");
        addButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label1 = new JLabel("Key:");
        label1.setPreferredSize(new Dimension(80, 20));
        row1.add(label1);
        row1.add(keyField);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label2 = new JLabel("Value:");
        label2.setPreferredSize(new Dimension(80, 20));

        row2.add(label2);
        row2.add(valueField);

        panel.add(row1);
        panel.add(row2);
        panel.add(addButton);

        addButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();
            if (localPayloads.containsKey(key)) {
                JOptionPane.showMessageDialog(panel, "Key already exists");
                return;
            }
            if (!key.isEmpty() && !value.isEmpty()) {
                localPayloads.put(key, value);
                saveLocalPayload();

                JOptionPane.showMessageDialog(panel, "Added: " + key + " → " + value);
                keyField.setText("");
                valueField.setText("");
                frameAdd.dispose();
                updateListModel(localPayloads, listModel);
                frameManage.invalidate();
                frameManage.repaint();

            } else {
                JOptionPane.showMessageDialog(panel, "Key and value required");
            }
        });

        frameAdd.getContentPane().add(panel);
        frameAdd.pack();
        frameAdd.setVisible(true);
    }

    public void manage() {
        if (frameManage != null) {
            frameManage.dispose();
        }
        frameManage = new JFrame();
        frameManage.getRootPane().putClientProperty("windowTitle", "");
        frameManage.setName("");
        frameManage.setLocationRelativeTo(null);
        frameManage.setAutoRequestFocus(false);

        frameManage.setSize(400, 300);

        JPanel panel = new JPanel(new BorderLayout());

        listModel = new DefaultListModel<>();
        JList<String> payloadList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(payloadList);

        updateListModel(localPayloads, listModel);
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton deleteAllButton = new JButton("Delete all");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(deleteAllButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> {
            this.add("");
        });

        editButton.addActionListener(e -> {
            int selectedIndex = payloadList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedEntry = payloadList.getSelectedValue();
                String key = selectedEntry.split(" = ")[0];
                String currentValue = localPayloads.get(key);

                String newValue = JOptionPane.showInputDialog(panel, "New value for \"" + key + "\":", currentValue);
                if (newValue != null) {
                    localPayloads.put(key, newValue);
                    updateListModel(localPayloads, listModel);
                    saveLocalPayload();
                }
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedIndex = payloadList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedEntry = payloadList.getSelectedValue();
                String key = selectedEntry.split(" = ")[0];
                int confirm = JOptionPane.showConfirmDialog(panel, "Delete \"" + key + "\" ?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    localPayloads.remove(key);
                    updateListModel(localPayloads, listModel);
                    saveLocalPayload();
                }
            }
        });

        deleteAllButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(panel, "Delete all entries ?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                localPayloads.clear();
                updateListModel(localPayloads, listModel);
                saveLocalPayload();
            }
        });

        frameManage.add(panel);
        frameManage.setVisible(true);

    }

    public void dispose() {
        if (frameManage != null) {
            frameManage.dispose();

        }
        if (frameAdd != null) {
            frameAdd.dispose();

        }
    }

    private void updateListModel(HashMap<String, String> map, DefaultListModel<String> model) {
        model.clear();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            model.addElement(entry.getKey() + " = " + entry.getValue());
        }
    }

    public JMenu buildMenu(Consumer<String> actionHandler) {

        JMenu menu = new JMenu("Custom keywords");

        for (Map.Entry<String, String> entry : localPayloads.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String itemName = key + ": " + value;

            if (itemName.length() > 80) {
                itemName = itemName.substring(0, 77) + "...";
            }

            JMenuItem item = new JMenuItem(itemName);
            item.addActionListener(e -> actionHandler.accept(value));
            menu.add(item);

        }
        return menu;

    }
}
