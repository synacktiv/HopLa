package com.hopla;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import java.awt.event.*;

import static com.hopla.Constants.DEBUG;

public class Completer {
    private final JTextArea source;
    private final MontoyaApi api;

    private final AutoCompleteMenu autoCompleteMenu;
    private KeyListener keyListener;
    private CaretListener caretListener;
    private int caretPositionStart = 0;
    private boolean manual_move = false;
    private boolean backspace = false;
    private boolean escape = false;
    private int in_selection = 0;
    private FocusListener focusListener;

    public Completer(MontoyaApi api, JTextArea source, AutoCompleteMenu autoCompleteMenu) {
        this.source = source;
        this.api = api;
        this.autoCompleteMenu = autoCompleteMenu;
        this.addListenersDetectManualCaretMove();

    }

    public static CaretContext getCaretContext(JTextArea source, int caretPosition) {
        String[] lines = source.getText().split("\n");

        int charCount = 0;
        int caretLineIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            charCount += lines[i].length() + 1; // +1 for \n
            if (caretPosition < charCount) {
                caretLineIndex = i;
                break;
            }
        }

        int bodyStartIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                bodyStartIndex = i + 1;
                break;
            }
        }

        HttpSection section;
        if (caretLineIndex == 0) {
            section = HttpSection.REQUEST_LINE;
        } else if (bodyStartIndex == -1) {
            section = HttpSection.HEADERS;
        } else if (caretLineIndex < bodyStartIndex) {
            section = HttpSection.HEADERS;
        } else {
            section = HttpSection.BODY;
        }

        String textBeforeCaret = source.getText().substring(0, caretPosition);
        String textAfterCaret = source.getText().substring(caretPosition);
        if (textAfterCaret.isEmpty()) {
            textAfterCaret = "\n";
        }
        int lastNewline = textBeforeCaret.lastIndexOf('\n');
        String lineUpToCaret = textBeforeCaret.substring(lastNewline + 1);


        return new CaretContext(section, lineUpToCaret, textBeforeCaret, textAfterCaret);
    }

    public JTextArea getSource() {
        return this.source;
    }

    public boolean isPrintableChar(char c) {
        if (Character.isLetterOrDigit(c)) {
            return true;
        }

        String specialChars = "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|`~";

        return specialChars.contains(Character.toString(c));
    }

    private void addListenersDetectManualCaretMove() {
        keyListener = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (DEBUG) {
                    api.logging().logToOutput("Input: " + e.getKeyChar());
                }
                if (isPrintableChar(e.getKeyChar())) {
                    manual_move = false;
                }
            }

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    backspace = true;
                    return;
                }

                if (autoCompleteMenu.isVisible()) {
                    int keyCode = e.getKeyCode();
                    if (DEBUG) {
                        api.logging().logToOutput("Key caught");
                    }
                    autoCompleteMenu.handleKey(keyCode);
                    e.consume();
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        if (DEBUG) {
                            api.logging().logToOutput("Escape key caught");
                        }
                        escape = true;
                    }
                }
            }
        };
        caretListener = new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {

                int pos = e.getDot();

                // selection create 3 events, skip the next 2
                if (e.getMark() != e.getDot()) {
                    if (DEBUG) {
                        api.logging().logToOutput("selection " + e.getMark() + "  " + pos);
                    }

                    // min depend on selection direction
                    caretPositionStart = Math.min(e.getMark(), e.getDot());
                    in_selection = 2;
                }

                if (in_selection > 0 && backspace) {
                    in_selection = -1;
                } else if (in_selection > 0) {
                    in_selection -= 1;
                    return;
                }

                if (DEBUG) {
                    api.logging().logToOutput("caret manual move: " + manual_move);
                    api.logging().logToOutput("caret start: " + caretPositionStart + " end: " + pos);
                }

                if ((manual_move || escape) && !backspace) {
                    if (DEBUG) {
                        api.logging().logToOutput("manual move or escape");
                    }

                    caretPositionStart = pos;
                    autoCompleteMenu.hide();
                } else {
                    if (backspace) {
                        if (DEBUG) {
                            api.logging().logToOutput("backspace");
                        }

                        if (pos <= caretPositionStart) {
                            caretPositionStart = pos;
                            autoCompleteMenu.hide();
                        }

                    }

                    try {
                        String content = source.getText(0, pos);
                        String text = content.substring(caretPositionStart);
                        CaretContext caretContext = getCaretContext(source, pos);
                        if (DEBUG) {
                            api.logging().logToOutput("complete: " + text);
                            api.logging().logToOutput("Caret context: " + caretContext);
                        }
                        autoCompleteMenu.suggest(source, text, caretPositionStart, pos, caretContext);

                    } catch (BadLocationException ex) {
                        if (DEBUG) {
                            api.logging().logToError("Bad location user completion input" + ex.getMessage());
                        }
                    } catch (StringIndexOutOfBoundsException ex) {
                        if (DEBUG) {
                            api.logging().logToError("Out of bound error start: " + caretPositionStart + " end: " + source.getCaretPosition());
                        }
                    } catch (Exception ex) {
                        if (DEBUG) {
                            api.logging().logToError("Completion input error: " + ex.getMessage());
                        }
                    }
                }
                backspace = false;
                escape = false;
                manual_move = true;

                if (DEBUG) {
                    api.logging().logToOutput("-----------------");
                }

            }
        };


        focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                autoCompleteMenu.hide();
            }
        };

        source.addCaretListener(caretListener);
        source.addKeyListener(keyListener);
        source.addFocusListener(focusListener);
    }

    public void detach() {
        source.removeKeyListener(keyListener);
        source.removeCaretListener(caretListener);
        source.removeFocusListener(focusListener);
    }

    public enum HttpSection {
        REQUEST_LINE,
        HEADERS,
        BODY,
        UNKNOWN
    }

    public static class CaretContext {
        public final HttpSection section;
        public final String lineUpToCaret;
        public final String textBeforeCaret;
        public final String textAfterCaret;

        public CaretContext(HttpSection section, String lineUpToCaret, String textBeforeCaret, String textAfterCaret) {
            this.section = section;
            this.lineUpToCaret = lineUpToCaret;
            this.textBeforeCaret = textBeforeCaret;
            this.textAfterCaret = textAfterCaret;
        }

        @Override
        public String toString() {
            return "Section: " + section + ", Line: \"" + lineUpToCaret + "\"";
        }
    }

}
