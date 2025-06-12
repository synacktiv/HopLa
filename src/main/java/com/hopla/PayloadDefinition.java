package com.hopla;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class PayloadDefinition {
    public List<Category> categories;
    public List<KeywordCategory> keywords;
    public String shortcut_search_and_replace;
    public String shortcut_payload_menu;
    public String shortcut_collaborator;
    public String shortcut_ia_chat;

    public Boolean isEmpty() {
        return this.categories == null || this.categories.isEmpty();
    }

    public List<Component> buildMenu(Consumer<PayloadDefinition.Payload> actionHandler) {
        List<Component> items = new ArrayList<>();

        if (categories != null) {
            for (Category cat : categories) {
                items.add(buildCategoryMenu(cat, actionHandler));
            }
        }
        return items;
    }

    private JMenuItem buildCategoryMenu(Category category, Consumer<PayloadDefinition.Payload> actionHandler) {
        JMenu menu = new JMenu(category.name);
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        MenuScroller.setScrollerFor(menu, 30);

        if (category.categories != null) {
            for (Category subcat : category.categories) {
                menu.add(buildCategoryMenu(subcat, actionHandler));
            }
        }

        if (category.payloads != null) {
            for (Payload payload : category.payloads) {
                String itemName;

                if (payload.name != null && !payload.name.isEmpty()) {
                    itemName = payload.name + ": " + payload.value;
                } else {
                    itemName = payload.value;
                }

                if (itemName.length() > 80) {
                    itemName = itemName.substring(0, 77) + "...";
                }

                JMenuItem item = new JMenuItem(itemName);
                item.addActionListener(e -> actionHandler.accept(payload));
                menu.add(item);
            }
        }
        return menu;
    }

    public Set<String> flattenPayloadValues() {
        Set<String> values = new HashSet<>();
        if (categories == null) return values;

        for (PayloadDefinition.Category category : categories) {
            collectValues(category, values);
        }
        return values;
    }

    public Set<String> flattenKeywordsValues() {
        Set<String> values = new HashSet<>();
        for (PayloadDefinition.KeywordCategory category : keywords) {
            values.addAll(category.values);
        }
        return values;
    }

    private void collectValues(PayloadDefinition.Category category, Set<String> collector) {
        if (category.payloads != null) {
            for (PayloadDefinition.Payload payload : category.payloads) {
                if (payload != null && payload.value != null) {
                    collector.add(payload.value);
                }
            }
        }

        if (category.categories != null) {
            for (PayloadDefinition.Category sub : category.categories) {
                collectValues(sub, collector);
            }
        }
    }

    public static class Category {
        public String name;
        public List<Payload> payloads;
        public List<Category> categories;

        public boolean isEmpty() {
            boolean noPayloads = (payloads == null || payloads.isEmpty());
            boolean noSubs = (categories == null || categories.isEmpty());
            return noPayloads && noSubs;
        }
    }

    public static class Payload {
        public String name;
        public String value;
        public String shortcut; // could be null
    }

    public static class KeywordCategory {
        public String name;
        public List<String> values;
    }
}