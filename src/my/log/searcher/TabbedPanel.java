package my.log.searcher;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    public TabbedPanel() {
        setLayout(new BorderLayout());
        add(tabbedPane);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.setFocusable(false);
    }

    /**
     * Метод для удаления записи из списка открытых файлов
     *
     * @param file
     */
    public static void removeFromOpenedFiles(File file) {
        openedFiles.remove(file);
    }

    /**
     * Метод, добавляющий в TabbedPanel новый таб с панелью открытого файла - экземпляром TextViewer и табкомпонентом - экземпляром TabPanel
     *
     * @param file
     */
    public void openFileInNewTab(File file) {
        if (file.isDirectory())
            return;
        if (openedFiles.contains(file))
            return;
        openedFiles.add(file);
        tabbedPane.addTab(file.getName(), new TextViewer(file));
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new TabPanel(file, tabbedPane));
    }
}

/**
 * Класс, экземпляры которого используются как табкомпоненты. Имеет возможность закрывать открытые табы
 */
class TabPanel extends JPanel {
    JTabbedPane tabbedPane;
    File file;

    /**
     * Конструктор класса, добавляющий кнопку закрытия
     *
     * @param file
     * @param tabbedPane - передается экземпляр панели, что бы была возможность удалять таб с этой панели
     */
    public TabPanel(File file, JTabbedPane tabbedPane) {
        if (tabbedPane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.file = file;
        this.tabbedPane = tabbedPane;
        setOpaque(false);
        add(new JLabel(file.getName()));
        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(16, 16));
        closeButton.setFocusable(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(1, 1, 3, 3));
        closeButton.setBorderPainted(false);
        closeButton.setRolloverEnabled(true);
        closeButton.addActionListener(new CloseButton());
        add(closeButton);
    }

    /**
     * Обработчик, закрывающий панель
     */
    class CloseButton implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            TextViewer currentTextViewer = (TextViewer) tabbedPane.getSelectedComponent();
            if (currentTextViewer.contentChanged) {
                int dialogButton = JOptionPane.YES_NO_OPTION;
                int saveFile = JOptionPane.showConfirmDialog(null,
                        "Файл был изменен, хотите сохранить изменения?", "Внимание", dialogButton);
                if (saveFile == JOptionPane.YES_OPTION) {
                    String contentToSave;
                    if (currentTextViewer.endOfFile) {
                        contentToSave = currentTextViewer.textArea.getText();
                    } else {
                        contentToSave = currentTextViewer.textArea.getText() +
                                currentTextViewer.fileContent.substring(currentTextViewer.finishSymbol);
                    }
                    JFileChooser fc = new JFileChooser();
                    fc.setSelectedFile(currentTextViewer.openedFile);
                    String extension = getFileExtension(currentTextViewer.openedFile);
                    if (extension.length() > 0) {
                        FileNameExtensionFilter filter = new FileNameExtensionFilter("*." + extension, extension);
                        fc.setFileFilter(filter);
                    }
                    if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                            fw.write(contentToSave);
                            JOptionPane.showMessageDialog(null, "Файл \"" + fc.getSelectedFile() +
                                    "\" успешно сохранен!");
                        } catch (IOException exception) {
                            JOptionPane.showMessageDialog(null, exception.getLocalizedMessage());
                        }
                    }
                    tabbedPane.remove(tabbedPane.indexOfTabComponent(TabPanel.this));
                    TabbedPanel.removeFromOpenedFiles(file);
                } else {
                    tabbedPane.remove(tabbedPane.indexOfTabComponent(TabPanel.this));
                    TabbedPanel.removeFromOpenedFiles(file);
                }
            } else {
                tabbedPane.remove(tabbedPane.indexOfTabComponent(TabPanel.this));
                TabbedPanel.removeFromOpenedFiles(file);
            }
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }
}

