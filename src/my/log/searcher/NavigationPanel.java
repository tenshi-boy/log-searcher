package my.log.searcher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс, наследующий JPanel и отображающий поисковую строку и кнопки для навигации и вызова окна замены вхождений
 */
public class NavigationPanel extends JPanel {
    TextViewer textViewer;
    JTextField searchStr;
    JButton searchBtn;
    JButton nextBtn;
    JButton prevBtn;
    JButton replaceBtn;
    int searchResultPointer;
    ArrayList<Integer> searchResult = new ArrayList();

    /**
     * Простой конструктор класса с размещением кнопок
     *
     * @param textViewer - передается экземпляр панели с содержимым файла, для навигации
     */
    public NavigationPanel(TextViewer textViewer) {
        this.textViewer = textViewer;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        searchStr = new JTextField(20);
        searchStr.getDocument().addDocumentListener(new DisableButtons());
        searchBtn = new JButton("Искать");
        searchBtn.addActionListener(new SearchTextInContent());
        nextBtn = new JButton("Далее");
        nextBtn.setEnabled(false);
        nextBtn.addActionListener(new NextButtonClick());
        prevBtn = new JButton("Назад");
        prevBtn.setEnabled(false);
        prevBtn.addActionListener(new PrevButtonClick());
        replaceBtn = new JButton("Заменить");
        replaceBtn.setEnabled(false);
        replaceBtn.addActionListener(new ShowReplaceWindow());

        this.textViewer.textArea.addFocusListener(new DisableReplaceButtonsOnFocus());

        add(searchStr);
        add(searchBtn);
        add(nextBtn);
        add(prevBtn);
        add(replaceBtn);
    }

    /**
     * Обработчик, отключающий кнопки панели, при фокусе на содержимое файла
     */
    class DisableReplaceButtonsOnFocus implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            textViewer.textArea.getHighlighter().removeAllHighlights();
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            replaceBtn.setEnabled(false);
        }

        @Override
        public void focusLost(FocusEvent e) {

        }
    }

    /**
     * Обработчик, который ищет вхождения подстроки и записывает их в searchResult
     */
    class SearchTextInContent implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (searchStr.getText().length() > 0) {
                searchResultPointer = 0;
                searchResult.clear();
                Pattern pattern = Pattern.compile(searchStr.getText());
                Matcher matcher = pattern.matcher(textViewer.textArea.getText()+
                        textViewer.fileContent.substring(textViewer.finishSymbol));
                while (matcher.find()) {
                    searchResult.add(matcher.start());
                }
                textViewer.textArea.getHighlighter().removeAllHighlights();
                if (searchResult.size() > 0) {
                    if(searchResult.get(searchResultPointer)>textViewer.finishSymbol) {
                        textViewer.startSymbol=textViewer.finishSymbol;
                        textViewer.finishSymbol=searchResult.get(searchResultPointer)+textViewer.step;
                        textViewer.insertContentPart();
                    }
                    textViewer.infoLabel.setText("Найдено вхождений: " + searchResult.size());
                    nextBtn.setEnabled(true);
                    if (textViewer.endOfFile)
                        prevBtn.setEnabled(true);
                    replaceBtn.setEnabled(true);
                    textViewer.textArea.getHighlighter().removeAllHighlights();
                    for (int position : searchResult) {
                        try {
                            if (searchResult.indexOf(position) == searchResultPointer) {
                                textViewer.textArea.getHighlighter().addHighlight(position, position +
                                        searchStr.getText().length(), new DefaultHighlighter.DefaultHighlightPainter(Color.gray));
                                textViewer.textArea.setCaretPosition(position);
                            }else {
                                textViewer.textArea.getHighlighter().addHighlight(position, position +
                                        searchStr.getText().length(), DefaultHighlighter.DefaultPainter);
                            }
                        } catch (BadLocationException e) {
                            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                        }
                    }
                } else {
                    textViewer.infoLabel.setText("<html><font color=red>Вхождений не найдено</font></html>");
                }
            }
        }
    }

    /**
     * Обработчик, увеличивающий searchResultPointer(следующее вхождение)
     */
    class NextButtonClick implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (searchResult.size() == 0) {
                textViewer.infoLabel.setText("<html><font color=red>Нет совпадений</font></html>");
                return;
            }
            if (searchResultPointer == searchResult.size() - 1)
                searchResultPointer = 0;
            else
                searchResultPointer++;
            if(searchResult.get(searchResultPointer)>textViewer.finishSymbol) {
                textViewer.startSymbol=textViewer.finishSymbol;
                textViewer.finishSymbol=searchResult.get(searchResultPointer)+textViewer.step;
                textViewer.insertContentPart();
            }

            textViewer.textArea.getHighlighter().removeAllHighlights();
            for (int position : searchResult) {
                try {
                    if (searchResult.indexOf(position) == searchResultPointer) {
                        textViewer.textArea.getHighlighter().addHighlight(position, position +
                                searchStr.getText().length(), new DefaultHighlighter.DefaultHighlightPainter(Color.gray));
                        textViewer.textArea.setCaretPosition(position);
                    } else {
                        textViewer.textArea.getHighlighter().addHighlight(position, position +
                                searchStr.getText().length(), DefaultHighlighter.DefaultPainter);
                    }
                } catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Обработчик, уменьшающий searchResultPointer(предыдущее вхождение)
     */
    class PrevButtonClick implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            if (searchResult.size() == 0) {
                textViewer.infoLabel.setText("<html><font color=red>Нет совпадений</font></html>");
                return;
            }
            if (searchResultPointer == 0)
                searchResultPointer = searchResult.size() - 1;
            else
                searchResultPointer--;

            textViewer.textArea.getHighlighter().removeAllHighlights();
            for (int position : searchResult) {
                try {
                    if (searchResult.indexOf(position) == searchResultPointer) {
                        textViewer.textArea.getHighlighter().addHighlight(position, position +
                                searchStr.getText().length(), new DefaultHighlighter.DefaultHighlightPainter(Color.gray));
                        textViewer.textArea.setCaretPosition(position);
                    } else {
                        textViewer.textArea.getHighlighter().addHighlight(position, position +
                                searchStr.getText().length(), DefaultHighlighter.DefaultPainter);
                    }
                } catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Обработчик, отключающий кнопки навигации и замены при изменении поисковой строки
     */
    class DisableButtons implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
            change();
        }

        public void removeUpdate(DocumentEvent e) {
            change();
        }

        public void insertUpdate(DocumentEvent e) {
            change();
        }

        private void change() {
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
            replaceBtn.setEnabled(false);

            textViewer.textArea.getHighlighter().removeAllHighlights();
        }
    }

    /**
     * Обработчик, создающий экземпляр класса ReplaceWindow - панели замены вхождений
     */
    class ShowReplaceWindow implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            JDialog.setDefaultLookAndFeelDecorated(true);
            ReplaceWindow replaceWindow = new ReplaceWindow(textViewer);
        }
    }
}
