package com.hopla;

import burp.*;
import java.io.PrintWriter;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.util.Iterator;
import java.awt.BorderLayout;

import javax.swing.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Frame;
import java.awt.Dimension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.awt.event.AWTEventListener;
import java.awt.Toolkit;
import java.awt.AWTEvent;
import java.awt.GridLayout;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.awt.Container;

public class HopLa implements IBurpExtender, IContextMenuFactory, IExtensionStateListener, AWTEventListener {

    public static final String APP_NAME = "HopLa";
    public static final double VERSION = 1.0;
    public static final String AUTHOR = "Alexis Danizan";

    public static IBurpExtenderCallbacks callbacks;
    public static IExtensionHelpers helpers;
    public IContextMenuInvocation invocation;

    public static PrintWriter stdout;
    public static PrintWriter stderr;

    private JMenuBar burpMenuBar;
    private JMenu hoplaMenuBar;

    private JFileChooser fileChooser;
    private String filepath;
    private String temppath;

    private ArrayList<Completer> listeners = new ArrayList<>();

    private Set<String> keywordsSet =  new HashSet<String>() ;
    private ArrayList<String> keywords = new ArrayList<>();
    private boolean enableCompletion = true;

    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {

        this.callbacks = callbacks;
        this.helpers = callbacks.getHelpers();
        this.stdout = new PrintWriter(callbacks.getStdout(), true);
        this.stderr = new PrintWriter(callbacks.getStderr(), true);

        callbacks.setExtensionName(this.APP_NAME);
        callbacks.registerContextMenuFactory(this);
        callbacks.registerExtensionStateListener(this);

        // Allow only json files for config.json
        this.fileChooser = new JFileChooser();
        this.fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON files (*.json)", "json");
        this.fileChooser.addChoosableFileFilter(filter);

        this.filepath = callbacks.loadExtensionSetting(this.APP_NAME);
        if (this.filepath == null) {
            File extension_file = new File(callbacks.getExtensionFilename());
            File config_file = new File(extension_file.getParentFile() + "/config.json");
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json");
            try {
                FileWriter config_writer = new FileWriter(config_file);
                config_writer.write( IOUtils.toString( inputStream, "UTF-8" ));
                config_writer.close();
                stdout.println("Default config write to file:" + config_file);

            } catch (IOException e) {
                stdout.println(e.getMessage());
            }

        }
        stdout.println("Current config file:" + this.filepath);

        // Add our custom configuration menu bar to burp
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    
                    burpMenuBar = getBurpFrame().getJMenuBar();
                    hoplaMenuBar = new JMenu("HopLa");
                    JMenuItem PayloadFileMenu = new JMenuItem("Payload file");
                    PayloadFileMenu.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            EditPayloadFile();
                        }
                    });

                    JCheckBoxMenuItem enableAutoCompletion = new JCheckBoxMenuItem("AutoCompletion",enableCompletion);
                    enableAutoCompletion.addItemListener(new ItemListener() {
                        public void itemStateChanged(ItemEvent e) {
                            if (enableAutoCompletion.getState()) {
                                enableCompletion = true;
                                stdout.println("Completion enable");

                            } else {
                                enableCompletion = false;
                                stdout.println("Completion disable");
                                for(Completer listener : listeners) {
                                    listener.detachFromSource();
                                    listener.getSource().getDocument().removeDocumentListener(listener);
                                    listener.getSource().putClientProperty("hasListener",false);
                                }
                                listeners = new ArrayList<>();
                            }
                        }
                    });

                    hoplaMenuBar.add(PayloadFileMenu);
                    hoplaMenuBar.add(enableAutoCompletion);
                    burpMenuBar.add(hoplaMenuBar);
                    stdout.println("Menubar loaded");
                }catch (Exception e){
                    stderr.println(e.getMessage());
                    stderr.println("Failed to start HopLa");
                }
            }
        });

        Toolkit.getDefaultToolkit().addAWTEventListener(this,AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
        this.buildMenu(this.getActionListener());
        this.stdout.println("HopLa initialized.\nHappy hacking !\nAlexis Danizan from Synacktiv\n--------------");

    }

    @Override
    public void extensionUnloaded() {
        // Remove menu on unload
        burpMenuBar.remove(hoplaMenuBar);
        burpMenuBar.repaint();

        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        // Remove all listeners on unload
        for(Completer listener : this.listeners) {
            listener.detachFromSource();
            listener.getSource().getDocument().removeDocumentListener(listener);
            listener.getSource().putClientProperty("hasListener",false);
        }

        this.stdout.println("HopLa unloaded");
    }

    /**
     * This hooks keyboard events for the entire application. Only textareas are considered. Practically, this includes
     * Repeater, Intruder, and any extension which uses JTextArea.
     * @param event keyboard event
     */
    @Override
    public void eventDispatched(AWTEvent event) {
        if(event.getSource() instanceof JTextArea) {
            JTextArea source = ((JTextArea)event.getSource());
	    // hooks only message editor and intruder

            Container is_editor = SwingUtilities.getAncestorNamed("requestResponseViewer",source);
            //Container is_repeater = SwingUtilities.getAncestorNamed("httpRequestMessageAnalyser",source);
            Container is_intruder = SwingUtilities.getAncestorNamed("intruderControlPanelTabBar",source);

            if (is_intruder == null && is_editor == null){
                return;
            }
	    if (!source.isEditable()){
                return;
            }
		
            if(source.getClientProperty("hasListener") ==  null || !((Boolean) source.getClientProperty("hasListener"))) {
                if (enableCompletion){
                    Completer t = new Completer(source, keywords, this.stdout, this.stderr);
                    this.createPayloadMenuFrame(t);
                    source.getDocument().addDocumentListener(t);
                    source.putClientProperty("hasListener",true);
                    this.listeners.add(t);
                    stdout.println("New completer added");
                }
            }
        }
    }

    public void createPayloadMenuFrame(Completer t){
        
        JFrame payloadMenu = new JFrame("Payload Menu");
        payloadMenu.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        payloadMenu.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                payloadMenu.setVisible(false);
                payloadMenu.dispose();
            }
        });

        payloadMenu.setLayout(new BorderLayout());
        JMenuBar menuBar = new JMenuBar();
        menuBar.setLayout(new GridLayout(0,1));

        for (JMenuItem menu: this.buildMenu(t.getActionListener()) ){
            menuBar.add(menu); 
        }
        payloadMenu.setResizable(false);
        payloadMenu.add(menuBar);
        payloadMenu.pack();
        payloadMenu.setLocationRelativeTo(null);
        payloadMenu.setVisible(false);
        t.payloadMenuFrame = payloadMenu;


        menuBar.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                    "showPayloads");

        menuBar.getActionMap().put("showPayloads", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                payloadMenu.setVisible(false);
            }
        });
        stdout.println("Payload menu fram built");
    }


    public void EditPayloadFile() {
        
        temppath = null;
        JPanel pane = new JPanel(new GridLayout(2,2));

        JLabel nameLabel = new JLabel("Payload file: ");
        pane.add(nameLabel);

        JLabel fileLabel = new JLabel(this.filepath);
        pane.add(fileLabel);

        JButton reloadButton = new JButton("Reload");
        pane.add(reloadButton);

        JButton fileButton = new JButton("Browse");
        pane.add(fileButton);

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (fileChooser.showOpenDialog(pane) == JFileChooser.APPROVE_OPTION) {
                    temppath = fileChooser.getSelectedFile().getAbsolutePath();
                    fileLabel.setText(temppath);
                }
            }
        });


        reloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                for(Completer t : listeners) {
                    createPayloadMenuFrame(t);
                    t.keywords = keywords;
                }
                temppath = fileLabel.getText();
            }
        });

        int result = JOptionPane.showConfirmDialog(null, pane, 
               "Payload file path", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String oldpath = this.filepath;
            this.filepath = temppath;
            ArrayList<JMenuItem> items = buildMenu(getActionListener());
            if (items != null && temppath != null){
                callbacks.saveExtensionSetting(APP_NAME, temppath);
               
            }else {
                this.filepath = oldpath;
            }
        }
        stdout.println("Filepath change to: " + this.filepath);

    }

    private static JFrame getBurpFrame() {
        for (Frame f : Frame.getFrames()) {
            if (f.isVisible() && f.getTitle().startsWith(("Burp Suite"))) {
                return (JFrame) f;
            }
        }
        return null;
    }

    public CompleterActionListener getActionListener() {
        return new CompleterActionListener() {
            public void actionPerformed(ActionEvent event) {
                String payload = event.getActionCommand();
                if(this.values.get(payload).size() > 0){
                    payload = this.prompt(payload, this.values.get(payload));
                }
                try{                    
                    int[] bounds = invocation.getSelectionBounds();
                    byte[] message = invocation.getSelectedMessages()[0].getRequest();
                    
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
                    outputStream.write(Arrays.copyOfRange(message, 0, bounds[0]));
                    outputStream.write(helpers.stringToBytes(payload));
                    outputStream.write(Arrays.copyOfRange(message,bounds[0], bounds[1]));
                    outputStream.write(Arrays.copyOfRange(message, bounds[1],message.length));
                    outputStream.flush();
                    invocation.getSelectedMessages()[0].setRequest(outputStream.toByteArray());
                
                }
                catch (Exception exc)
                {
                    stderr.println(exc.getMessage());
                }
            }
        };
    }
    

    private JMenu recursionMenu(String name, JSONArray values, CompleterActionListener action ){
        JMenu menu = new JMenu(name);
        MenuScroller.setScrollerFor(menu,30);
        Iterator<JSONObject> iterator = values.iterator();
        while (iterator.hasNext()) {
            JSONObject obj = iterator.next();
            String child_name = (String) obj.get("name");

            JSONArray child_values = (JSONArray) obj.get("values");
            if (child_values != null){
                menu.add(this.recursionMenu(child_name, child_values, action));
            }

            String child_value = (String) obj.get("value");
            if (child_value != null){
                this.keywordsSet.add(child_value);
                String item_name = child_value;
                if (child_name.length() > 0){
                    item_name = child_name + ": " + child_value;
                }

                if (item_name.length() > 80){
                    item_name = item_name.substring(0,80);
                }

                JMenuItem item = new JMenuItem(item_name);
                item.setActionCommand(child_value);

                JSONArray prompt_values = (JSONArray) obj.get("prompt");
                ArrayList<String> prompt = new ArrayList<String>();  
                if (prompt_values != null){
                    for (Object c : prompt_values) {
                        prompt.add((String) c);
                    }
                    
                }
                action.add(child_value,prompt);


                item.addActionListener(action);
                menu.add(item);
            }
        }
        return menu;
    }
    public ArrayList<JMenuItem> buildMenu(CompleterActionListener action){
        ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();
        try{
            this.keywordsSet =  new HashSet<String>();
            String content = "";
            if (this.filepath == null) {
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json");
                content = IOUtils.toString( inputStream, "UTF-8" );
            } else {
                content = new String(Files.readAllBytes(Paths.get(this.filepath)), StandardCharsets.UTF_8);
            }

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(content);
            JSONObject jsonObject = (JSONObject) obj;

            JSONArray categories = (JSONArray) jsonObject.get("categories");
           
            Iterator<JSONObject> iterator = categories.iterator();
            while (iterator.hasNext()) {
                JSONObject categorie = iterator.next();
                String categorie_name = (String) categorie.get("name");
                JSONArray categorie_values = (JSONArray) categorie.get("values");
                items.add(this.recursionMenu(categorie_name, categorie_values, action));
            }


            JSONArray keywords = (JSONArray) jsonObject.get("keywords");
            Iterator<JSONObject> iterator_keywords = keywords.iterator();
            while (iterator_keywords.hasNext()) {
                JSONObject keyword = iterator_keywords.next();
                JSONArray keywords_values = (JSONArray) keyword.get("values");
                if (keywords_values != null){
                    for (Object c : keywords_values) {
                        this.keywordsSet.add((String) c);
                    }
                }
            }


            this.keywords = new ArrayList<String>();
            this.keywords.addAll(this.keywordsSet);

        }catch (Exception e) {
			this.stderr.println(e.getMessage());
            this.alert("Invalid payload file, sry bro");
            return null;
		}
        stdout.println("Menu built");
        return items;
    }

    @Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();
        this.invocation = invocation;
        switch (invocation.getInvocationContext()) {
            case IContextMenuInvocation.CONTEXT_INTRUDER_PAYLOAD_POSITIONS:
            case IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST:
            case IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST:
                JMenu base_menu = new JMenu("HopLa");
                items.add(base_menu);
                for (JMenuItem menu: this.buildMenu(this.getActionListener())){
                    base_menu.add(menu);    
                }
                break;
        }
        
        return items;
	}

    public void alert(String message) {
        JOptionPane.showMessageDialog(null, message,"HopLa Error",JOptionPane.ERROR_MESSAGE);
        HopLa.callbacks.issueAlert(message);
    }

}

