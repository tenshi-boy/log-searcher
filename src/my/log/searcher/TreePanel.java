package my.log.searcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Класс, наследующий JPanel и отображающей древо файлов выбранной дирректори и поле для ввода расширения
 */
public class TreePanel extends JPanel {
    JLabel filterHelper = new JLabel("<html><span style='font-size:11px'>Поиск по критериям:</span></html>");
    JTextFieldPlaceholder extension;
    JTextFieldPlaceholder filterText;
    JButton acceptFilterBtn;
    JTree tree;
    File chosenDir;
    TabbedPanel tabbedPanel;
    TreeExpansionListener treeExpansionListener;

    public TreePanel(File chosenDirArg, TabbedPanel tabbedPanel) {
        this.tabbedPanel = tabbedPanel;
        chosenDir = chosenDirArg;
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(new OpenTextFile());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayout(4, 1, 3, 3));
        filterPanel.setBorder(new EmptyBorder(0, 2, 3, 2));
        add(filterPanel, BorderLayout.NORTH);

        extension = new JTextFieldPlaceholder("Расширение файла", 15);
        extension.setForeground(Color.GRAY);
        extension.addFocusListener(new placeholderHandler());

        filterText = new JTextFieldPlaceholder("Слово в файле", 15);
        filterText.setForeground(Color.GRAY);
        filterText.addFocusListener(new placeholderHandler());

        acceptFilterBtn = new JButton("Искать");
        acceptFilterBtn.addActionListener(new AcceptFilter());

        filterPanel.add(filterHelper);
        filterPanel.add(extension);
        filterPanel.add(filterText);
        filterPanel.add(acceptFilterBtn);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        setMinimumSize(new Dimension(160, 0));

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(chosenDir.getName());
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        tree.setModel(treeModel);
        Runnable fillTree = new NoFilteredFillTreeThread(root);
        Thread noFilteredFillTreeThread = new Thread(fillTree);
        noFilteredFillTreeThread.start();
        treeExpansionListener = new AddChildrenToNode();
        tree.addTreeExpansionListener(treeExpansionListener);
    }

    /**
     * Обработчик, добавляющий в tabbedPanel новую панель tabPanel с содержимым файла
     */
    class OpenTextFile implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath treePath = e.getNewLeadSelectionPath();
            if (treePath == null)
                return;
            StringBuilder pathFromRoot = new StringBuilder();
            Object[] nodes = treePath.getPath();
            for (int i = 1; i < nodes.length; i++) {
                pathFromRoot.append(File.separatorChar).append(nodes[i].toString());
            }
            tabbedPanel.openFileInNewTab(new File(chosenDir + pathFromRoot.toString()));
        }
    }

    /**
     * Класс, который в отдельном потоке добавляет в родительский узел дочерние.
     * Поток создается при раскрытии родительского узла(если отсутствуют критерии для фильтра)
     */
    class NoFilteredFillTreeThread implements Runnable {
        DefaultMutableTreeNode parentNode;

        public NoFilteredFillTreeThread(DefaultMutableTreeNode parentNode) {
            this.parentNode = parentNode;
        }

        @Override
        public void run() {
            StringBuilder pathFromRoot = new StringBuilder();
            Object[] nodes = parentNode.getPath();
            for (int i = 1; i < nodes.length; i++) {
                pathFromRoot.append(File.separatorChar).append(nodes[i].toString());
            }
            File parentNodeDir = new File(chosenDir + pathFromRoot.toString());
            ArrayList<String> alDirs = new ArrayList<>();
            ArrayList<String> alFiles = new ArrayList<>();
            for (File file : parentNodeDir.listFiles(new NotBinaryFileFilter())) {
                if (!file.canRead())
                    continue;
                if (file.isDirectory()) {
                    if (file.listFiles() == null)
                        continue;
                    alDirs.add(file.getName());
                } else {
                    alFiles.add(file.getName());
                }
            }
            for (String filename : alDirs) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(filename);
                parentNode.add(node);
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode("Ожидайте...");
                node.add(childNode);
            }
            for (String filename : alFiles) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(filename);
                parentNode.add(node);
            }
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
            treeModel.reload(parentNode);
        }
    }

    /**
     * Обработчик, который по раскрытию создает поток NoFilteredFillTreeThread.
     * Вставляет в родительский узел дочерние(если отсутствуют критерии для фильтра)
     */
    class AddChildrenToNode implements TreeExpansionListener {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            TreePath treePath = event.getPath();
            if (treePath == null)
                return;
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            parentNode.removeAllChildren();
            Runnable fillTree = new NoFilteredFillTreeThread(parentNode);
            Thread noFilteredFillTreeThread = new Thread(fillTree);
            noFilteredFillTreeThread.start();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
        }
    }

    /**
     * Класс фильтр по типу файла(только текстовые и дирректории)
     * todo так и не нашел универсального способа разделить бинарные файлы от текстовых
     */
    class NotBinaryFileFilter implements FileFilter {
        boolean isTextFile(File file) {
            try {
                String type = Files.probeContentType(file.toPath());
                if (type == null) {
                    return true;
                } else if (type.startsWith("application") ||
                        type.startsWith("video") ||
                        type.startsWith("audio") ||
                        type.startsWith("image")) {
                    return false;
                } else {
                    return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public boolean accept(File file) {
            return (isTextFile(file) || file.isDirectory());
        }
    }

    /**
     * Класс, который в отдельном потоке применяет к модели древо узлов(дирректории и файлы) соответствующие фильтру
     * Поток создается при нажатии на кнопки "Найти" (если есть критерии для фильтра)
     */
    class FilteredFillTreeThread implements Runnable {
        @Override
        public void run() {
            DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
            root.removeAllChildren();
            acceptFilterBtn.setEnabled(false);
            tree.setEnabled(false);
            filteredFillTree(chosenDir, extension.getText(), filterText.getText(), root);
            treeModel.reload();
            acceptFilterBtn.setEnabled(true);
            tree.setEnabled(true);
        }

        /**
         * Рекурсивный метод, проверяющий дирректории в chosenDirArg, и добавляющий в дерево узлы с этими дирректориями,
         * если в них найдены файлы соответствующие фильтру(проверяется возвращаемое значение метода)
         *
         * @param chosenDirArg  - проверяемая дирректория
         * @param extensionArg  - проверяемое расширение файлов
         * @param filterTextArg - текст, на вхождение которого проверяются файлы
         * @param node          - узел, в который вставляются соотетствующие фильру файлы
         * @return - возвращает true, если файлы соответствующие фильру найдены в chosenDirArg
         */

        public boolean filteredFillTree(File chosenDirArg, String extensionArg, String filterTextArg, DefaultMutableTreeNode node) {
            if (extensionArg.isEmpty() && filterTextArg.isEmpty()) {
                return false;
            }
            boolean filterMatching = false;
            //цикл по дирректориям, вызывающий рекурсию
            for (File file : chosenDirArg.listFiles()) {
                if (file.isDirectory() &&
                        file.listFiles(new NotBinaryFileFilter()) != null &&
                        file.listFiles(new NotBinaryFileFilter()).length > 0) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                    if (filteredFillTree(file, extensionArg, filterTextArg, childNode)) {
                        filterMatching = true;
                        node.add(childNode);
                    }
                }
            }
            //цикл, собирающий файлы соответствующие расширению в ArrayList<File> extensionMatching
            ArrayList<File> extensionMatching = new ArrayList();
            if (!extensionArg.isEmpty()) {
                for (File file : chosenDirArg.listFiles(new ExtensionFileFilter(extensionArg))) {
                    if (!file.isDirectory()) {
                        extensionMatching.add(file);
                    }
                }
            }
            //цикл, собирающий файлы содержащие искомую строку в ArrayList<File> filterTextMatching
            ArrayList<File> filterTextMatching = new ArrayList();
            if (!filterTextArg.isEmpty()) {
                ArrayList<Thread> threadsTextMatching = new ArrayList();
                for (File file : chosenDirArg.listFiles(new NotBinaryFileFilter())) {
                    if (!file.isDirectory()) {
                        Runnable readFile = new CheckForFilterMatchingThread(file, filterTextArg, filterTextMatching);
                        Thread readFileThread = new Thread(readFile);
                        threadsTextMatching.add(readFileThread);
                        readFileThread.start();
                    }
                }
                for (Thread thread : threadsTextMatching) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                    }
                }
            }
            if (!extensionArg.isEmpty() && !filterTextArg.isEmpty()) {
                for (File file : filterTextMatching) {
                    if (extensionMatching.contains(file)) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                        node.add(childNode);
                        filterMatching = true;
                    }
                }
            } else if (!filterTextArg.isEmpty()) {
                for (File file : filterTextMatching) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                    node.add(childNode);
                    filterMatching = true;
                }
            } else if (!extensionArg.isEmpty()) {
                for (File file : extensionMatching) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                    node.add(childNode);
                    filterMatching = true;
                }
            }
            return filterMatching;
        }
    }

    /**
     * Класс фильтр по расширению и типу файла(только текстовые)
     */
    class ExtensionFileFilter extends NotBinaryFileFilter implements FileFilter {
        String extension;

        ExtensionFileFilter(String extension) {
            if (extension.substring(0, 1).equals(".")) {
                this.extension = extension;
            } else {
                this.extension = "." + extension;
            }
        }

        boolean isExtensionFiltered(File file) {
            String fileName = file.toString();
            return (fileName.substring(fileName.length() - extension.length()).equals(extension));
        }

        @Override
        public boolean accept(File file) {
            return (isTextFile(file) && isExtensionFiltered(file));
        }
    }

    /**
     * Класс, который ищет строку в файле в отдельном потоке, и, в случае нахождения, заполняет ArrayList<File> filterTextMatching
     */
    class CheckForFilterMatchingThread implements Runnable {
        File file;
        String filterText;
        ArrayList<File> filterTextMatching;

        /**
         * @param file               - файл, в котором производится поиск вхождения filterText
         * @param filterText         - искомая строка
         * @param filterTextMatching - список файлов, содержащих строку
         */
        public CheckForFilterMatchingThread(File file, String filterText, ArrayList<File> filterTextMatching) {
            this.file = file;
            this.filterText = filterText;
            this.filterTextMatching = filterTextMatching;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                String s;
                while ((s = in.readLine()) != null) {
                    if (s.toUpperCase().indexOf(filterText.toUpperCase()) >= 0) {
                        filterTextMatching.add(file);
                        break;
                    }
                }
                in.close();
            } catch (IOException e) {
                /*JOptionPane.showMessageDialog(null, e.getLocalizedMessage());*/
            }
        }
    }

    /**
     * Обработчик, перерисовывающий JTree.
     * Если нет критериев для фильтрации, создается поток NoFilteredFillTreeThread и привязывается обработчик treeExpansionListener.
     * Если есть критерии для фильтрации, создается поток FilteredFillTreeThread и отвязывается обработчик treeExpansionListener.
     */
    class AcceptFilter implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean listenerFound = false;
            if (extension.getText().isEmpty() && filterText.getText().isEmpty()) {
                for (TreeExpansionListener listener : tree.getTreeExpansionListeners()) {
                    if (listener == treeExpansionListener)
                        listenerFound = true;
                }
                if (!listenerFound) {
                    tree.addTreeExpansionListener(treeExpansionListener);
                }
                DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
                root.removeAllChildren();
                Runnable fillTree = new NoFilteredFillTreeThread(root);
                Thread noFilteredFillTreeThread = new Thread(fillTree);
                noFilteredFillTreeThread.start();
            } else {
                for (TreeExpansionListener listener : tree.getTreeExpansionListeners()) {
                    if (listener == treeExpansionListener)
                        listenerFound = true;
                }
                if (listenerFound) {
                    tree.removeTreeExpansionListener(treeExpansionListener);
                }
                Runnable fillTree = new FilteredFillTreeThread();
                Thread filteredFillTreeThread = new Thread(fillTree);
                filteredFillTreeThread.start();
            }
        }
    }

    /**
     * Обработчик, который показывает/прячет плейсхолдер при фокусе и расфукосировке на JTextFieldPlaceholder
     */
    class placeholderHandler implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            JTextFieldPlaceholder placeholder = (JTextFieldPlaceholder) e.getComponent();
            if (placeholder.getText().isEmpty()) {
                placeholder.setText("");
                placeholder.setForeground(Color.BLACK);
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            JTextFieldPlaceholder placeholder = (JTextFieldPlaceholder) e.getComponent();
            if (placeholder.getText().isEmpty()) {
                placeholder.setForeground(Color.GRAY);
                placeholder.setText(placeholder.getPlaceholder());
            }
        }
    }
}

/**
 * Класс, дополняющий JTextField полем placeholder
 */
class JTextFieldPlaceholder extends JTextField {
    private String placeholder;

    JTextFieldPlaceholder(String placeholder, int columns) {
        super(placeholder, columns);
        setPlaceholder(placeholder);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public String getText() {
        if (super.getText().equals(placeholder)) {
            return "";
        } else {
            return super.getText();
        }
    }
}