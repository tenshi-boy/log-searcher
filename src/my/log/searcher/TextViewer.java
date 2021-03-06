package my.log.searcher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.*;
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
    final int step = 5000000;
    private int startSymbol = 0;
    private int finishSymbol = step;
    boolean endOfFile = false;
    boolean contentChanged = false;
    //содержимое файла сразу загружается в fileContent
    String fileContent;

    //delayIsOver и artificialDelay используются для создания задержки при добавлении контента в JTextArea
    boolean delayIsOver = true;
    Runnable artificialDelay = new ArtificialDelay(this);

    /**
     * Конструктор TextViewer, при открытии файла размещает на себе JLabel, используемый для отображения загрузки
     * и запускает поток readFileThread, который считывает файл в fileContent
     *
     * @param file открываемый файл
     */
    public TextViewer(File file) {
        openedFile = file;
        setLayout(new BorderLayout());
        try {
            ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/img/loading.gif"));
            loading = new JLabel(imageIcon);
        } catch (Exception e) {
            loading = new JLabel("Ожидайте...");
        } finally {
            add(BorderLayout.CENTER, loading);
        }
        Runnable readFile = new ReadingFileThread(file, this);
        Thread readFileThread = new Thread(readFile);
        readFileThread.start();
    }

    /**
     * Метод вставляет очередную порцию контента в JTextArea
     */
    public void insertContentPart() {
        if (scrollPane == null) {
            remove(loading);

            textArea = new JTextArea();
            textArea.getDocument().addDocumentListener(new ContentChangedTrue());
            scrollPane = new JScrollPane(textArea);
            add(BorderLayout.CENTER, scrollPane);
            scrollPane.getVerticalScrollBar().addAdjustmentListener(new appendNewContentTextArea());

            navPanel = new NavigationPanel(this);
            add(BorderLayout.NORTH, navPanel);

            infoLabel = new JLabel("");
            add(BorderLayout.SOUTH, infoLabel);
            revalidate();
        }
        if (finishSymbol >= fileContent.length()) {
            finishSymbol = fileContent.length();
            endOfFile = true;
            if (navPanel.searchResult.size() > 0)
                navPanel.prevBtn.setEnabled(true);
            infoLabel.setText("");
        } else {
            infoLabel.setText("Внимание! Файл загружен не полностью, и будет подгружаться по мере просмотра");
        }
        if (startSymbol < 0)
            startSymbol = 0;
        boolean localContentChanged = contentChanged;
        textArea.append(fileContent.substring(startSymbol, finishSymbol));
        contentChanged = localContentChanged;
    }

    /**
     * Обработчик, вставляющий контент в TextArea
     */
    class appendNewContentTextArea implements AdjustmentListener {
        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (endOfFile) {
                scrollPane.getVerticalScrollBar().removeAdjustmentListener(this);
                System.out.println("Все содержимое контента вставлено в TextArea");
                return;
            }
            if (!delayIsOver)
                return;
            int maxValue = ((JScrollBar) e.getSource()).getMaximum();
            int curValue = e.getValue();
            if ((float) curValue / maxValue > 0.95) {
                delayIsOver = false;
                Thread doDelayThread = new Thread(artificialDelay);
                doDelayThread.start();
                startSymbol = finishSymbol;
                finishSymbol += step;
                insertContentPart();
                System.out.println(startSymbol + "ОТ ДО" + finishSymbol);
            }
        }
    }

    /**
     * Обработчик, выставляющий флаг contentChanged, указывающий на то, что файл изменен и его необходимо сохранить
     */
    class ContentChangedTrue implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            contentChanged = true;
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            contentChanged = true;
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            contentChanged = true;
        }
    }

    //геттеры и сеттеры предупреждающие ошибки с fileContent.substring()
    public int getStartSymbol() {
        return startSymbol;
    }

    public int getFinishSymbol() {
        return finishSymbol;
    }

    public void setStartSymbol(int startSymbolArg) {
        if (startSymbolArg > finishSymbol)
            startSymbol = finishSymbol;
        else
            startSymbol = startSymbolArg;
    }

    public void setFinishSymbol(int finishSymbolArg) {
        if (finishSymbolArg > startSymbol)
            finishSymbol = finishSymbolArg;
        else
            finishSymbol = startSymbol + step;
        if (finishSymbol > fileContent.length())
            finishSymbol = fileContent.length();
    }
}

/**
 * Класс, реализующий задержку в пол секунды перед изменением флага textViewer.delayIsOver, означающим, что можно вставить часть контента
 */
class ArtificialDelay implements Runnable {
    TextViewer textViewer;

    public ArtificialDelay(TextViewer textViewer) {
        this.textViewer = textViewer;
    }

    public synchronized void run() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
            textViewer.delayIsOver = true;
            textViewer.scrollPane.getVerticalScrollBar().setValue((int) (textViewer.scrollPane.getVerticalScrollBar().getMaximum() * 0.90));
        } catch (InterruptedException e1) {
            textViewer.delayIsOver = true;
        }
    }
}

/**
 * Класс, который читает файл в отдельном потоке в переменную fileContent объекта TextViewer
 */
class ReadingFileThread implements Runnable {
    File file;
    TextViewer textViewer;

    public ReadingFileThread(File file, TextViewer textViewer) {
        this.file = file;
        this.textViewer = textViewer;
    }

    public void run() {
        try {
            textViewer.fileContent = read(file);
            textViewer.insertContentPart();

        } catch (IOException e) {
            //todo закрытие таба
        }
    }

    public synchronized static String read(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String s;
            while ((s = in.readLine()) != null) {
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
