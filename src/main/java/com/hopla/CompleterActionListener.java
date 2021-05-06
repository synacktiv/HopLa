package com.hopla;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusEvent;
import java.awt.MouseInfo;
import java.awt.Dimension;

import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Handle the Dialog for placeholders and menu action 
 */
public class CompleterActionListener implements ActionListener {
    public Map<String,ArrayList<String>> values;

    public CompleterActionListener() {
        this.values = new HashMap<String,ArrayList<String>>();
    }

    public String prompt(String payload, ArrayList<String> placeholder){
   
        JPanel pane = new JPanel(new GridLayout(placeholder.size()+1,2));
        HashMap<String,JTextField> fields = new HashMap<String,JTextField>();

        for (String name : placeholder) { 		
            JLabel label = new JLabel(name);
            label.setPreferredSize(new Dimension(100, 20));      
            JTextField field = new JTextField();
            field.setPreferredSize(new Dimension(300, 20));
            pane.add(label);
            pane.add(field);      
            fields.put(name, field);	
        }

        int result = JOptionPane.showConfirmDialog(null, pane, 
               "Payload edit", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
                String key = entry.getKey();
                JTextField value = entry.getValue();
                payload = payload.replace('§'+ key + '§', value.getText());
            }
        }else{
            payload = "";
        }
        return payload;
    }

    public void add(String payload, ArrayList<String> placeholder) {
        this.values.put(payload, placeholder);
    }

    public void actionPerformed(ActionEvent e) {
    }
}