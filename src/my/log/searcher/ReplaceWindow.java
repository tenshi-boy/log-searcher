package my.log.searcher;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import static javax.swing.GroupLayout.Alignment.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;

/**
 * Класс, наследующий JDialog содержащий кнопки замены и осуществляющий замену вхождений
 */
public class ReplaceWindow extends JDialog {
    TextViewer textViewer;
    JPanel mainPanel=new JPanel();
    GroupLayout layout = new GroupLayout(mainPanel);
    JLabel replaceToLabel=new JLabel("Заменить на:");
    JTextField replaceTo=new JTextField();
    JButton nextBtn=new JButton("Далее");
    JButton replaceBtn=new JButton("Заменить");
    JButton replaceAllBtn=new JButton("Заменить все");
    /**
     * Простой конструктор класса с размещением кнопок
     * @param textViewer - передается экземпляр панели с содержимым файла, что бы была возможность заменять вхождения
     */
    public ReplaceWindow(TextViewer textViewer){
        this.textViewer=textViewer;
        setLayout(new FlowLayout());
        setTitle("Заменить найденный текст в файле");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(440,160);
        setResizable(false);
        setLocationRelativeTo(null);

        replaceAllBtn.addActionListener(new ReplaceAllBtnClick());
        nextBtn.addActionListener(new NextBtnClick());
        replaceBtn.addActionListener(new ReplaceBtnClick());

        mainPanel.setLayout(layout);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20));
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(replaceToLabel)
                .addGroup(layout.createParallelGroup(LEADING)
                        .addComponent(replaceTo)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(replaceBtn)
                                .addComponent(replaceAllBtn)
                                .addComponent(nextBtn)))
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(replaceToLabel)
                        .addComponent(replaceTo))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(nextBtn)
                        .addComponent(replaceAllBtn)
                        .addComponent(replaceBtn))
        );

        add(mainPanel);
        setModal(false);
        addWindowFocusListener(new CloseModal());
        setVisible(true);
    }

    /**
     * Обработчик, закрывающий окно замены в случае клика мыши вне окна
     */
    class CloseModal implements WindowFocusListener{
        public void windowLostFocus(WindowEvent e) {
            setVisible(false);
        }
        public void windowGainedFocus(WindowEvent e) {
        }
    }

    /**
     * Класс, заменяющий все вхождения и отключающий кнопки навигации
     */
    class ReplaceAllBtnClick implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if(textViewer.navPanel.searchResult.size()==0){
                textViewer.infoLabel.setText("<html><font color=red>Нет совпадений для замены</font></html>");
                return;
            }
            int strLengthDiff=replaceTo.getText().length()-textViewer.navPanel.searchStr.getText().length();
            int replaceCount=0;
            ArrayList<Integer> newSearchResult = new ArrayList();
            for(int position:textViewer.navPanel.searchResult){
                position=position+strLengthDiff*replaceCount;
                replaceCount++;
                newSearchResult.add(position);
            }
            textViewer.navPanel.searchResult=newSearchResult;
            textViewer.textArea.getHighlighter().removeAllHighlights();

            for(int position:textViewer.navPanel.searchResult){
                textViewer.textArea.replaceRange(replaceTo.getText(),position,position+textViewer.navPanel.searchStr.getText().length());
            }

            textViewer.navPanel.searchResult.clear();
            textViewer.fileContent=textViewer.fileContent.replace(textViewer.navPanel.searchStr.getText(), replaceTo.getText());

            textViewer.navPanel.nextBtn.setEnabled(false);
            textViewer.navPanel.prevBtn.setEnabled(false);
            textViewer.navPanel.replaceBtn.setEnabled(false);
        }
    }

    /**
     * Класс, увеличивающий указатель textViewer.navPanel.searchResultPointer(следущее вхождение)
     */
    class NextBtnClick implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if(textViewer.navPanel.searchResult.size()==0){
                textViewer.infoLabel.setText("<html><font color=red>Нет совпадений</font></html>");
                return;
            }
            if(textViewer.navPanel.searchResultPointer==textViewer.navPanel.searchResult.size()-1)
                textViewer.navPanel.searchResultPointer = 0;
            else
                textViewer.navPanel.searchResultPointer++;
            textViewer.textArea.getHighlighter().removeAllHighlights();
            for(int position:textViewer.navPanel.searchResult){
                try{
                    if(textViewer.navPanel.searchResult.indexOf(position)==textViewer.navPanel.searchResultPointer)
                        textViewer.textArea.getHighlighter().addHighlight(position, position+textViewer.navPanel.searchStr.getText().length(), new DefaultHighlighter.DefaultHighlightPainter(Color.gray));
                    else
                        textViewer.textArea.getHighlighter().addHighlight(position, position+textViewer.navPanel.searchStr.getText().length(), DefaultHighlighter.DefaultPainter);
                } catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                }
            }
        }
    }
    /**
     * Класс, заменяющий текущее вхождение - значение textViewer.navPanel.searchResult по индексу textViewer.navPanel.searchResultPointer
     */
    class ReplaceBtnClick implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if(textViewer.navPanel.searchResult.size()==0){
                textViewer.infoLabel.setText("<html><font color=red>Нет совпадений для замены</font></html>");
                return;
            }
            int strLengthDiff=replaceTo.getText().length()-textViewer.navPanel.searchStr.getText().length();
            ArrayList<Integer> newSearchResult = new ArrayList();
            for(int position:textViewer.navPanel.searchResult){
                if(textViewer.navPanel.searchResult.indexOf(position)>textViewer.navPanel.searchResultPointer)
                    position=position+strLengthDiff;
                newSearchResult.add(position);
            }
            textViewer.navPanel.searchResult=newSearchResult;
            int curPosition=textViewer.navPanel.searchResult.get(textViewer.navPanel.searchResultPointer);
            textViewer.textArea.replaceRange(replaceTo.getText(),curPosition,curPosition+textViewer.navPanel.searchStr.getText().length());
            textViewer.navPanel.searchResult.remove(textViewer.navPanel.searchResultPointer);
            if(textViewer.navPanel.searchResultPointer==textViewer.navPanel.searchResult.size())
                textViewer.navPanel.searchResultPointer=0;
            if(textViewer.navPanel.searchResult.size()==0){
                textViewer.navPanel.nextBtn.setEnabled(false);
                textViewer.navPanel.prevBtn.setEnabled(false);
                textViewer.navPanel.replaceBtn.setEnabled(false);
            }
            textViewer.textArea.getHighlighter().removeAllHighlights();
            for(int position:textViewer.navPanel.searchResult){
                try{
                    if(textViewer.navPanel.searchResult.indexOf(position)==textViewer.navPanel.searchResultPointer)
                        textViewer.textArea.getHighlighter().addHighlight(position, position+textViewer.navPanel.searchStr.getText().length(), new DefaultHighlighter.DefaultHighlightPainter(Color.gray));
                    else
                        textViewer.textArea.getHighlighter().addHighlight(position, position+textViewer.navPanel.searchStr.getText().length(), DefaultHighlighter.DefaultPainter);
                } catch (BadLocationException e) {
                    JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                }
            }
        }
    }
}
