package my.log.searcher;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Основной класс программы, содержащий JFrame, на которой размещаются все остальные компоненты
 */
public class MainWindow{
    JFrame mainWindow = new JFrame();
    JSplitPane splitPane = new JSplitPane();
    TreePanel treePanel;
    TabbedPanel tabbedPanel;

    public MainWindow(){
        mainWindow.setTitle("Поиск заданного текста в файлах");
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.setSize(800,600);
        mainWindow.setLocationRelativeTo(null);
        addMenuButtons();
        mainWindow.setVisible(true);
    }

    /**
     * Метод добавляет пункт меню выбора дирректории для поиска файлов с обработчиком SelectDirListener
     */
    private void addMenuButtons(){
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Меню");
        JMenuItem menuItemSelectDir = new JMenuItem("Выбрать локальную папку");
        menu.add(menuItemSelectDir);
        menuBar.add(menu);
        menuItemSelectDir.addActionListener(new SelectDirListener());
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
            if (result == JFileChooser.APPROVE_OPTION ) {
                File chosenFile = fileChooserSelectDir.getSelectedFile();
                tabbedPanel = new TabbedPanel();
                treePanel = new TreePanel(chosenFile,tabbedPanel);
                mainWindow.add(splitPane);
                splitPane.setLeftComponent(treePanel);
                splitPane.setRightComponent(tabbedPanel);
                mainWindow.revalidate();
                mainWindow.repaint();
            }
        }
    }
}
