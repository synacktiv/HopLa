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
import java.util.ArrayList;
import java.util.Arrays;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.FocusAdapter;
import java.util.List;
import java.awt.Color;
import java.io.PrintWriter;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.MouseInfo;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import javax.swing.text.DefaultCaret;

public class Completer implements DocumentListener, CaretListener {
    
    private JTextArea source;
    private int pos;
    private int start;

    private JFrame suggestionPane;
   
    //List model to hold the candidate autocompletions
    private DefaultListModel<String> suggestionsModel = new DefaultListModel<>();

    // The content of the source document we will be replacing
    private String content;
    
    // Burp debug output 
    private PrintWriter stdout;
    private PrintWriter stderr;

    // Caret initial position
    private int startPos = -1;
    
    // Payload menu reference
    public JFrame payloadMenuFrame;

    // Keywords for completion
    public ArrayList<String> keywords;
    private Boolean mode_insert = false;
    private Boolean in_selection = false;
    private int selection_size = -1;

    /**
     * This listener follows the caret and updates where we should draw the suggestions box
     */
    @Override
    public void caretUpdate(CaretEvent e) {
        pos = e.getDot();
        if (e.getMark() != e.getDot()){
            in_selection = true;
            selection_size =  e.getDot() - e.getMark();
        }else{
            if (selection_size != -1 ){
                startPos = -1;
                //suggestionPane.setVisible(false);

            }
            selection_size = -1;
            in_selection = false;
        }
        /*Point p = source.getCaret().getMagicCaretPosition();
        if(p != null) {
            Point np = new Point();
            np.x = p.x + source.getLocationOnScreen().x;
            np.y = p.y + source.getLocationOnScreen().y + 25;
            suggestionPane.setLocation(np);
        }*/

        try{
           Point np = new Point();
           np.x = source.modelToView( source.getCaretPosition() ).x + source.getLocationOnScreen().x;
           np.y = source.modelToView( source.getCaretPosition() ).y + source.getLocationOnScreen().y + 25;
           suggestionPane.setLocation(np);

        }catch(Exception exc){
            stderr.println(exc);
        }
        stdout.println("Position: " + String.valueOf(pos) + " Start position: " + String.valueOf(startPos));
    }


    /**
     * Initializes the suggestion pane and attaches our listeners
     * @param s the source to provide autocompletions for
     */
    public Completer(JTextArea s, ArrayList<String> keywords, PrintWriter stdout, PrintWriter stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.keywords = keywords;
        this.source = s;
        
        this.pos = this.source.getCaret().getDot();
        //DefaultCaret caret = (DefaultCaret) this.source.getCaret();
        //caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);  
        
        this.startPos = this.pos-1;
        this.source.addCaretListener(this);
        this.source.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                stdout.println("Mouse move");
                suggestionPane.setVisible(false);
                startPos = pos-1;//-1;
                
            }
        });
       
        suggestionPane = new JFrame();
        suggestionPane.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        suggestionPane.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                suggestionPane.setVisible(false);
                suggestionPane.dispose();
            }
        });
        suggestionPane.setUndecorated(true);
        suggestionPane.setAutoRequestFocus(false);
        JPanel pane = new JPanel(new BorderLayout());

        JList<String> suggestions = new JList(suggestionsModel);
        suggestions.setVisibleRowCount(10);
        suggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestions.setLayoutOrientation(JList.VERTICAL);

        JScrollPane scroller = new JScrollPane(suggestions,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = suggestions.getPreferredSize();

                if (d.height > 250){
                    d.height = 250;
                }

                if (d.width > 500) {
                    d.width = 500;
                }
                // scrollbar hacky
                d.height += 5;
                return d;
            }
        };
        JScrollBar vertical = scroller.getVerticalScrollBar();
        
        // completion shortcut
        InputMap im = vertical.getInputMap(JComponent.WHEN_FOCUSED);
        im.put(KeyStroke.getKeyStroke("DOWN"), "positiveUnitIncrement");
        im.put(KeyStroke.getKeyStroke("UP"), "negativeUnitIncrement");

        pane.add(scroller, BorderLayout.CENTER);

        suggestionPane.setLayout(new BorderLayout());
        suggestionPane.add(pane, BorderLayout.CENTER);
        suggestionPane.pack();

        //Completions mouse listerner, double clicks to add payload
        suggestions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList list = (JList)e.getSource();
                if (e.getClickCount() == 2) {
                    try{
                        // Double-click detected
                        int index = list.locationToIndex(e.getPoint());
                        String selectedCompletion = suggestionsModel.elementAt(index);
                        mode_insert = true;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                source.select(start+1,pos);
                                source.replaceSelection(selectedCompletion);
                                source.setCaretPosition(source.getSelectionEnd());
                                suggestionPane.setVisible(false);
                                startPos = -1;
                                mode_insert = false;
                            }
                        });
                    }  
                    catch (Exception exc)
                    {
                        stderr.println(exc.getMessage());
                    }  
                }
            }
        });

        // Completions key listerner, enter to add payload
        suggestions.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_ENTER ){
                    try{
                        JList list = (JList)e.getSource();
                        List<String> values = list.getSelectedValuesList();
                        String val = values.get(0).toString();
                        mode_insert = true;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                source.select(start+1,pos);
                                source.replaceSelection(val);
                                source.setCaretPosition(source.getSelectionEnd());
                                suggestionPane.setVisible(false);
                                startPos = -1;
                                mode_insert = false;
                            }
                        }); 
                    }  
                    catch (Exception exc)
                    {
                        stderr.println(exc.getMessage());
                    }                           
                }else if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    // close suggestion on escape key
                    s.requestFocus();
                    suggestionPane.setVisible(false);
                }else if(
                    e.getKeyCode() != KeyEvent.VK_UP &&
                    e.getKeyCode() != KeyEvent.VK_DOWN &&
                    e.getKeyCode() != KeyEvent.VK_LEFT &&
                    e.getKeyCode() != KeyEvent.VK_RIGHT &&
                    e.getKeyCode() != KeyEvent.VK_TAB
                ) {
                    if (Character.isUnicodeIdentifierPart(e.getKeyChar())){
                        source.select(pos,pos);
                        source.replaceSelection(Character.toString(e.getKeyChar()));
                        source.setCaretPosition(source.getSelectionEnd());
                    }
                    
                    suggestionPane.setVisible(false);
                    s.requestFocus();
                }                
            }
        });

        // Colorize suggestion on focus
        suggestions.addFocusListener(new FocusAdapter(){
            public void focusGained(FocusEvent e) {
                suggestions.setBackground(new Color(216,227,231));
            }
            
            public void focusLost(FocusEvent e) {
                suggestions.setBackground(Color.white);
            }
        });



        // Show Payload library on ctrl + q
        this.source.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                    "showPayloads");

        this.source.getActionMap().put("showPayloads", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (payloadMenuFrame.isVisible()){
                    payloadMenuFrame.setVisible(false);
                }else {
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    // marging position, for better clicks
                    p.x += 20;
                    p.y += 20;
                    payloadMenuFrame.setLocation(p);
                    payloadMenuFrame.setVisible(true);
                }
                
                
            }
        });

        // Source JTextarea listener
        this.source.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e){
                if (e.getModifiers() == KeyEvent.CTRL_MASK){
                    mode_insert = true;
                    startPos = -1;
                    pos -= 1;
                    suggestionPane.setVisible(false);
                }else {
                    mode_insert = false;
                }


                int keyCode = e.getKeyCode();
                switch( keyCode ) { 
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT :
                    case KeyEvent.VK_ENTER :
                        // Change start completion position when user move carret
                        stdout.println("Arrow / Enter move");
                        if (suggestionPane.isVisible() && keyCode != KeyEvent.VK_LEFT &&  keyCode != KeyEvent.VK_RIGHT){
                            suggestions.setSelectedIndex(0);
                            suggestions.grabFocus();
                            e.consume();      
                        }else{
                            suggestionPane.setVisible(false);
                            startPos = -1;
                        }
                        break;

                    case  KeyEvent.VK_TAB:
                        // Pick the first payload on tab completion
                        suggestions.setSelectedIndex(0);
                        List<String> values = suggestions.getSelectedValuesList();
                        mode_insert = true;

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                source.select(start+1,pos);
                                source.replaceSelection(values.get(0).toString());
                                source.setCaretPosition(source.getSelectionEnd());
                                suggestionPane.setVisible(false);
                                startPos = -1;
                                mode_insert = false;

                            }
                        });
                        suggestionPane.setVisible(false);
                        s.requestFocus();
                        e.consume();  
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestionPane.setVisible(false);
                        s.requestFocus();
                        break;

                    case KeyEvent.VK_BACK_SPACE:
                    stdout.println(in_selection);
                    stdout.println(selection_size);
                    stdout.println(pos);
                        if (in_selection){
                            mode_insert = true;
                        }else{
                            if(startPos == -1 ){ // corner case
                                pos = pos-1;
                            }else {
                                pos = pos-2;
                            }
                        }
                        
                        break;
                    case KeyEvent.VK_DELETE:
                        suggestionPane.setVisible(false);
                        mode_insert = true;
                        break;
                }
            }
        });
    }


    public CompleterActionListener getActionListener() {
        return new CompleterActionListener() {
            public void actionPerformed(ActionEvent event) {
                String payload = event.getActionCommand();
                
                // Ask for input if placeholder 
                if(this.values.get(payload).size() > 0){
                    payload = this.prompt(payload, this.values.get(payload));
                }
                // Update the source with our payload
                source.select(pos+2,pos+2);
                source.replaceSelection(payload);
                source.setCaretPosition(source.getSelectionEnd());
                payloadMenuFrame.setVisible(false);
            }
        };
    }

   
    public JTextArea getSource() {
        return this.source;
    }

    public void detachFromSource(){
        this.suggestionPane.dispose();
        this.source.removeCaretListener(this);
        this.source.getDocument().removeDocumentListener(this);
    }

    /**
     * Searches the autocompletions for candidates. Exact matches are ignored.
     */
    private ArrayList<String> prefixSearcher(String search) {
        ArrayList<String> results = new ArrayList<>();
        for(String in : this.keywords) {
            if( !in.toLowerCase().equals(search.trim()) && in.toLowerCase().startsWith(search.trim()) ) {
                
                results.add(in);
            }
        }
        return results;
    }



    @Override
    public void insertUpdate(DocumentEvent e) {
        try {
            Completions(pos);
        }
        catch (Exception exc)
		{
            this.stderr.println(exc.getMessage());
		}
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        try {
            Completions(pos);
        }
        catch (Exception exc)
		{
            this.stderr.println(exc.getMessage());
		}
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
       
    }



    private void Completions(int offset) {

        if (mode_insert){
            return;
        }

        content = null;
        try {
            content = this.source.getText(0, offset+1);
        } catch (BadLocationException e) {
            this.stderr.println(e.getMessage());
        }
        catch (Exception exc)
		{
            this.stderr.println(exc.getMessage());
		}

        // Here is the magics
        int start = offset;
        if (start < startPos && startPos != -1){
            startPos = start;
        }else{
            if (startPos == -1) {
                start -= 1;         
                startPos = start;
            }
            while (start >= 0) {
                
                if (start == startPos){
                    break;
                }
                if (Character.isWhitespace(content.charAt(start))){
                    startPos = start;
                    break;
                }
                start--;
            }
        }
        
        
        this.start = start;

        stdout.println(start);

        if (offset - start < 1){
            suggestionPane.setVisible(false);
            return;
        }
        
        // corner case for http method
        if (startPos == 0){
            start = -1;
        }
        String prefix = content.substring(start +1);
        stdout.println(prefix);

        if (prefix.trim().length() == 0 ) { //|| prefix.trim().length() == 1
            suggestionPane.setVisible(false);
        } else {
           
            ArrayList<String> matches = prefixSearcher(prefix.toLowerCase());
            if (matches.size() != 0) {
                SwingUtilities.invokeLater(
                        new CompletionTask(matches));
            } else {
                suggestionPane.setVisible(false);
            }
        }

    }

    /**
     * Updates the suggestion pane with the new options
     */
    private class CompletionTask
            implements Runnable {

        CompletionTask(ArrayList<String> completions) {
            suggestionsModel.removeAllElements();
            for(String completion : completions) {
                suggestionsModel.addElement(completion);
            }

            // Force refresh JList
            suggestionPane.pack();
        }

        @Override
        public void run() {
            suggestionPane.setVisible(true);
        }
    }
}
