package my.log.searcher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Класс, наследующий JPanel и отображающий TextArea с содержимым файла и панель навигации по файлу - NavigationPanel
 * Его основная фишка в том, что файл в случае большого размера добавляется в JTextArea не целиком,
 * а по мере прокрутки JScrollPane
 */
public class TextViewer extends JPanel {
    File openedFile;
    JLabel loading;
    JTextArea textArea;
    JScrollPane scrollPane;
    NavigationPanel navPanel;
    JLabel infoLabel;
    //количество символов, добавляемых при прокрутке до 95% JScrollPane
    int step=5000000;
    int startSymbol=0;
    int finishSymbol=step;
    boolean endOfFile=false;
    boolean contentChanged=false;
    //содержимое файла сразу загружается в fileContent
    String fileContent;

    //delayIsOver и artificialDelay используются для создания задержки при добавлении контента в JTextArea
    boolean delayIsOver=true;
    Runnable artificialDelay=new ArtificialDelay(this);

    /**
     * Конструктор TextViewer, при открытии файла размещает на себе JLabel, используемый для отображения загрузки
     * и запускает поток readFileThread, который считывает файл в fileContent
     * @param file открываемый файл
     */
    public TextViewer(File file){
        openedFile=file;
        setLayout(new BorderLayout());
        try {
            ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/img/loading.gif"));
            loading = new JLabel(imageIcon);
        } catch (Exception e) {
            loading = new JLabel("Ожидайте...");
        } finally{
            add(BorderLayout.CENTER, loading);
        }
        Runnable readFile = new ReadingFileThread(file,this);
        Thread readFileThread = new Thread(readFile);
        readFileThread.start();
    }

    /**
     * Метод вставляет очередную порцию контента в JTextArea
     * @param start - начиная с символа
     * @param finish - до символа
     */
    public void insertContentPart(int start,int finish){
        if(scrollPane==null){
            remove(loading);

            textArea=new JTextArea();
            textArea.getDocument().addDocumentListener(new ContentChangedTrue());
            scrollPane=new JScrollPane(textArea);
            add(BorderLayout.CENTER,scrollPane);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(new appendNewContentTextArea());

            navPanel= new NavigationPanel(this);
            add(BorderLayout.NORTH,navPanel);

            infoLabel= new JLabel("");
            add(BorderLayout.SOUTH,infoLabel);
            revalidate();
        }
        if(finish>=fileContent.length()) {
            finish = fileContent.length();
            endOfFile=true;
            infoLabel.setText("");
        }else{
            infoLabel.setText("Внимание! Файл загружен не полностью, и будет подгружаться по мере просмотра");
        }
        if(start<0)
            start=0;
        boolean localContentChanged=contentChanged;
        textArea.append(fileContent.substring(start,finish));
        contentChanged=localContentChanged;
    }
    /**
     * Обработчик, вставляющий контент в TextArea
     */
    class appendNewContentTextArea implements AdjustmentListener {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if(endOfFile){
                scrollPane.getVerticalScrollBar().removeAdjustmentListener(this);
                System.out.println("Все содержимое контента вставлено в TextArea");
                return;
            }
            if(!delayIsOver)
                return;
            int maxValue=((JScrollBar) e.getSource()).getMaximum();
            int curValue=e.getValue();
            if((float) curValue/maxValue>0.95){
                delayIsOver=false;
                Thread doDelayThread = new Thread(artificialDelay);
                doDelayThread.start();
                startSymbol=finishSymbol;
                finishSymbol+=step;
                insertContentPart(startSymbol,finishSymbol);
                System.out.println(startSymbol+"ОТ ДО"+finishSymbol);
            }
        }
    }
    class ContentChangedTrue implements DocumentListener{

        @Override
        public void insertUpdate(DocumentEvent e) {
            contentChanged=true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            contentChanged=true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            contentChanged=true;
        }
    }
}

/**
 * Класс, реализующий задержку в пол секунды перед изменением флага textViewer.delayIsOver, означающим, что можно вставить часть контента
 */
class ArtificialDelay implements Runnable{
    TextViewer textViewer;
    public ArtificialDelay(TextViewer textViewer){
        this.textViewer=textViewer;
    }
    public synchronized void run(){
        try {
            TimeUnit.MILLISECONDS.sleep(500);
            textViewer.delayIsOver=true;
            textViewer.scrollPane.getVerticalScrollBar().setValue((int) (textViewer.scrollPane.getVerticalScrollBar().getMaximum() * 0.90));
        } catch (InterruptedException e1) {
            textViewer.delayIsOver=true;
        }
    }
}

/**
 * Класс, который читает файл в отдельном потоке в переменную fileContent объекта TextViewer
 */
class ReadingFileThread implements Runnable{
    File file;
    TextViewer textViewer;
    public ReadingFileThread(File file,TextViewer textViewer){
        this.file=file;
        this.textViewer=textViewer;
    }
    public void run(){
        try {
            textViewer.fileContent=read(file);
            textViewer.insertContentPart(textViewer.startSymbol,textViewer.finishSymbol);

        }catch(IOException e){
            //todo закрытие таба
        }
    }
    public synchronized static String read(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()));
            String s;
            while ((s = in.readLine()) != null){
                sb.append(s);
                sb.append("\n");
            }

            in.close();
        } catch (IOException e) {
            throw new IOException(e);
        }
        return sb.toString();
    }
}
