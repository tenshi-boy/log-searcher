package my.log.searcher;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Основной класс программы, содержащий JFrame, на которой размещаются все остальные компоненты
 */
public class MainWindow {
    JFrame mainWindow = new JFrame();
    JSplitPane splitPane = new JSplitPane();
    TreePanel treePanel;
    TabbedPanel tabbedPanel;
    JMenuItem menuItemSaveFile;

    public MainWindow() {
        mainWindow.setTitle("Поиск заданного текста в файлах");
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.setSize(800, 600);
        mainWindow.setLocationRelativeTo(null);
        try {
            ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/img/icon-log.png"));
            mainWindow.setIconImage(imageIcon.getImage());
        } catch (Exception e) {}
        addMenuButtons();
        mainWindow.setVisible(true);
    }

    /**
     * Метод добавляет пункт меню выбора дирректории для поиска файлов с обработчиком SelectDirListener
     */
    private void addMenuButtons() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Меню");
        JMenuItem menuItemSelectDir = new JMenuItem("Выбрать локальную папку");
        menuItemSaveFile = new JMenuItem("Сохранить файл");
        menu.add(menuItemSelectDir);
        menu.add(menuItemSaveFile);
        menuBar.add(menu);
        menu.addMenuListener(new CheckOpenedFilesListener());
        menuItemSelectDir.addActionListener(new SelectDirListener());
        menuItemSaveFile.addActionListener(new SaveFileListener());
        mainWindow.setJMenuBar(menuBar);
    }

    /**
     * Обработчик пункта меню, после выбора дирректории добавляет в mainWindow панель древа выбранного каталога и панель табов
     */
    class SelectDirListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            JFileChooser fileChooserSelectDir = new JFileChooser();
            fileChooserSelectDir.setDialogTitle("Выбор директории");
            fileChooserSelectDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooserSelectDir.showOpenDialog(mainWindow);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosenFile = fileChooserSelectDir.getSelectedFile();
                tabbedPanel = new TabbedPanel();
                treePanel = new TreePanel(chosenFile, tabbedPanel);
                mainWindow.add(splitPane);
                splitPane.setLeftComponent(treePanel);
                splitPane.setRightComponent(tabbedPanel);
                mainWindow.revalidate();
                mainWindow.repaint();
            }
        }
    }

    /**
     * Обработчик меню, проверяет есть ли открытые файлы, и активирует/деактивирует кнопку сохранения
     */
    class CheckOpenedFilesListener implements MenuListener {
        public void menuSelected(MenuEvent e) {
            if (tabbedPanel == null || tabbedPanel.openedFiles.isEmpty()) {
                menuItemSaveFile.setEnabled(false);
            } else {
                menuItemSaveFile.setEnabled(true);
            }
        }

        public void menuDeselected(MenuEvent e) {
        }

        public void menuCanceled(MenuEvent e) {
        }
    }

    /**
     * Обработчик пункта меню сохранить файл
     */
    class SaveFileListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            TextViewer selectedTextViewer = (TextViewer) tabbedPanel.tabbedPane.getSelectedComponent();
            String contentToSave;
            if (selectedTextViewer.endOfFile) {
                contentToSave = selectedTextViewer.textArea.getText();
            } else {
                contentToSave = selectedTextViewer.textArea.getText() +
                        selectedTextViewer.fileContent.substring(selectedTextViewer.finishSymbol);
            }
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(selectedTextViewer.openedFile);
            String extension = getFileExtension(selectedTextViewer.openedFile);
            if (extension.length() > 0) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter("*." + extension, extension);
                fc.setFileFilter(filter);
            }
            if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                    fw.write(contentToSave);
                    JOptionPane.showMessageDialog(null, "Файл \"" + fc.getSelectedFile() + "\" успешно сохранен!");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
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
}
