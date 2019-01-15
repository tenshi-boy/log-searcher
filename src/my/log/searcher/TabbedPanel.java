package my.log.searcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Класс, наследующий JPanel и отображающей панель с вкладками - содержимым файлов
 */
public class TabbedPanel extends JPanel {
    JTabbedPane tabbedPane = new JTabbedPane();
    static ArrayList<File> openedFiles = new ArrayList();

    /**
     * Простой конструктор
     */
    public TabbedPanel(){
        setLayout(new BorderLayout());
        add(tabbedPane);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.setFocusable(false);
    }

    /**
     * Метод для удаления записи из списка открытых файлов
     * @param file
     */
    public static void removeFromOpenedFiles(File file){
        openedFiles.remove(file);
    }

    /**
     * Метод, добавляющий в TabbedPanel новый таб с панелью открытого файла - экземпляром TextViewer и табкомпонентом - экземпляром TabPanel
     * @param file
     */
    public void openFileInNewTab(File file){
        if(file.isDirectory())
            return;
        if(openedFiles.contains(file))
            return;
        openedFiles.add(file);
        tabbedPane.addTab(file.getName(),new TextViewer(file));
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount()-1,new TabPanel(file,tabbedPane));
    }
}

/**
 * Класс, экземпляры которого используются как табкомпоненты. Имеет возможность закрывать открытые табы
 */
class TabPanel extends JPanel{
    JTabbedPane tabbedPane;
    File file;

    /**
     * Конструктор класса, добавляющий кнопку закрытия
     * @param file
     * @param tabbedPane - передается экземпляр панели, что бы была возможность удалять таб с этой панели
     */
    public TabPanel(File file,JTabbedPane tabbedPane){
        if (tabbedPane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.file=file;
        this.tabbedPane = tabbedPane;
        setOpaque(false);
        add(new JLabel(file.getName()));
        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setFocusable(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(1,1,3,3));
        closeButton.setBorderPainted(false);
        closeButton.setRolloverEnabled(true);
        closeButton.addActionListener(new CloseButton());
        add(closeButton);
    }

    /**
     * Обработчик, закрывающий панель
     */
    class CloseButton implements ActionListener{
        public void actionPerformed(ActionEvent e){
            tabbedPane.remove(tabbedPane.indexOfTabComponent(TabPanel.this));
            TabbedPanel.removeFromOpenedFiles(file);
        }
    }
}

