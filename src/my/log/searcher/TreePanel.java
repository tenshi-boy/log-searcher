package my.log.searcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
import java.util.ArrayList;

/**
 * Класс, наследующий JPanel и отображающей древо файлов выбранной дирректори и поле для ввода расширения
 */
public class TreePanel extends JPanel{
    JLabel filterHelper = new JLabel("<html><span style='font-size:11px'>Поиск по критериям:</span></html>");
    JTextFieldPlaceholder extension;
    JTextFieldPlaceholder filterText;
    JButton acceptFilterBtn;
    JTree tree;
    File chosenDir;
    TabbedPanel tabbedPanel;
    public TreePanel(File chosenDirArg, TabbedPanel tabbedPanel){
        this.tabbedPanel=tabbedPanel;
        chosenDir=chosenDirArg;
        setLayout(new BorderLayout());
        tree = new JTree();
        tree.setShowsRootHandles(true);
        tree.addTreeSelectionListener(new SelectedTreeFile());

        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new GridLayout(4, 1,3,3));
        filterPanel.setBorder(new EmptyBorder(0, 2, 3, 2));
        add(filterPanel,BorderLayout.NORTH);

        extension = new JTextFieldPlaceholder("Расширение файла",15);
        extension.setForeground(Color.GRAY);
        extension.addFocusListener(new placeholderHandler());

        filterText = new JTextFieldPlaceholder("Слово в файле",15);
        filterText.setForeground(Color.GRAY);
        filterText.addFocusListener(new placeholderHandler());

        acceptFilterBtn = new JButton("Искать");
        acceptFilterBtn.addActionListener(new AcceptFilter(this));

        filterPanel.add(filterHelper);
        filterPanel.add(extension);
        filterPanel.add(filterText);
        filterPanel.add(acceptFilterBtn);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        setMinimumSize(new Dimension(160,0));

        Runnable fillTree = new FillTreeThread(this);
        Thread fillTreeThread = new Thread(fillTree);
        fillTreeThread.start();
    }

    /**
     * Обработчик, перерисовывающий JTree
     */
    class AcceptFilter implements ActionListener{
        TreePanel treePanel;
        public AcceptFilter(TreePanel treePanel){
            this.treePanel=treePanel;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            treePanel.acceptFilterBtn.setEnabled(false);
            Runnable fillTree = new FillTreeThread(treePanel);
            Thread fillTreeThread = new Thread(fillTree);
            fillTreeThread.start();
        }
    }

    /**
     * Класс, который в отдельном потоке применяет к JTree модель с узлами(дирректориями и файлами) соответствующи фильтру
     */
    class FillTreeThread implements Runnable{
        TreePanel treePanel;

        /**
         * @param treePanel - объект основного класса, содержащий JTree, выбранную дирректорию и параметры фильтра
         */
        public FillTreeThread(TreePanel treePanel){
            this.treePanel=treePanel;
        }

        @Override
        public void run(){
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(chosenDir));
            DefaultTreeModel treeModel = new DefaultTreeModel(root);
            fillTree(treePanel.chosenDir,treePanel.extension.getText(),treePanel.filterText.getText(),root);
            treePanel.acceptFilterBtn.setEnabled(true);
            tree.setModel(treeModel);
        }

        /**
         * Рекурсивный метод, проверяющий дирректории в chosenDirArg, и добавляющий в дерево узлы с этими дирректориями,
         * если в них найдены файлы соответствующие фильтру(проверяется возвращаемое значение метода)
         * @param chosenDirArg
         * @param extensionArg
         * @param filterTextArg
         * @param node
         * @return
         */
        public boolean fillTree(File chosenDirArg,String extensionArg, String filterTextArg,DefaultMutableTreeNode node){
            boolean filterMatching=false;
            //цикл по дирректориям, вызывающий рекурсию
            for (File file : chosenDirArg.listFiles()) {
                if (file.isDirectory() && file.listFiles()!=null && file.listFiles().length>0) {
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
                    if(fillTree(file,extensionArg,filterTextArg,childNode)){
                        filterMatching=true;
                        node.add(childNode);
                    }
                }
            }
            //цикл, собирающий файлы соответствующие расширению в ArrayList<File> extensionMatching
            ArrayList<File> extensionMatching = new ArrayList();
            if(!extensionArg.isEmpty()) {
                for (File file : chosenDirArg.listFiles(new ExtMaskFilter(extensionArg))) {
                    if(!file.isDirectory()) {
                        extensionMatching.add(file);
                    }
                }
            }
            //цикл, собирающий файлы содержащие искомую строку в ArrayList<File> filterTextMatching
            ArrayList<File> filterTextMatching = new ArrayList();
            if(!filterTextArg.isEmpty()) {
                ArrayList<Thread> threadsTextMatching = new ArrayList();
                for (File file : chosenDirArg.listFiles()) {
                    if(!file.isDirectory()) {
                        Runnable readFile = new CheckForFilterMatchingThread(file, filterTextArg, filterTextMatching);
                        Thread readFileThread = new Thread(readFile);
                        threadsTextMatching.add(readFileThread);
                        readFileThread.start();
                    }
                }
                for(Thread thread : threadsTextMatching){
                    try {
                        thread.join();
                    } catch ( InterruptedException e ) {
                        JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
                    }
                }
            }

            if(extensionArg.isEmpty() && filterTextArg.isEmpty()){
                for (File file : chosenDirArg.listFiles()) {
                    if(!file.isDirectory()) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
                        node.add(childNode);
                        filterMatching=true;
                    }
                }
            }else if(extensionArg.isEmpty()){
                for (File file : filterTextMatching){
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
                    node.add(childNode);
                    filterMatching=true;
                }
            }else if(filterTextArg.isEmpty()){
                for (File file : extensionMatching){
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
                    node.add(childNode);
                    filterMatching=true;
                }
            }else{
                for (File file : filterTextMatching){
                    if(extensionMatching.contains(file)) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
                        node.add(childNode);
                        filterMatching = true;
                    }
                }
            }
            return filterMatching;
        }
    }

    /**
     * Класс фильтр по расширению
     */
    class ExtMaskFilter implements FilenameFilter {
        String mask;

        ExtMaskFilter(String Mask) {
            if(Mask.substring(0,1).equals(".")){
                mask=Mask;
            }else{
                mask = "." + Mask;
            }
        }
        @Override
        public boolean accept(File f, String name) {
            return(name.indexOf(mask) > 0);
        }
    }

    /**
     * Класс, который ищет строку в файле в отдельном потоке, и, в случае нахождения, заполняет ArrayList<File> filterTextMatching
     */
    class CheckForFilterMatchingThread implements Runnable{
        File file;
        String filterText;
        ArrayList<File> filterTextMatching;

        /**
         * @param file - файл, в котором производится поиск
         * @param filterText - искомая строка
         * @param filterTextMatching - список файлов, содержащих строку
         */
        public CheckForFilterMatchingThread(File file,String filterText, ArrayList<File> filterTextMatching){
            this.file=file;
            this.filterText=filterText;
            this.filterTextMatching=filterTextMatching;
        }
        @Override
        public void run(){
            try {
                BufferedReader in = new BufferedReader(new FileReader(file.getAbsoluteFile()));
                String s;
                while ((s = in.readLine()) != null){
                    if(s.toUpperCase().indexOf(filterText.toUpperCase())>=0){
                        filterTextMatching.add(file);
                        break;
                    }
                }
                in.close();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
            }
        }
    }

    /**
     * Обработчик, который показывает/прячет плейсхолдер при фокусе и расфукосировке на JTextFieldPlaceholder
     */
    class placeholderHandler implements FocusListener{
        @Override
        public void focusGained(FocusEvent e) {
            JTextFieldPlaceholder placeholder = (JTextFieldPlaceholder)e.getComponent();
            if (placeholder.getText().isEmpty()) {
                placeholder.setText("");
                placeholder.setForeground(Color.BLACK);
            }
        }
        @Override
        public void focusLost(FocusEvent e) {
            JTextFieldPlaceholder placeholder = (JTextFieldPlaceholder)e.getComponent();
            if (placeholder.getText().isEmpty()) {
                placeholder.setForeground(Color.GRAY);
                placeholder.setText(placeholder.getPlaceholder());
            }
        }
    }

    /**
     * Обработчик древа, после выбора узла дерева(файла) добавляет в tabbedPanel новую панель tabPanel с содержимым файла
     */
    class SelectedTreeFile implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath treePath = e.getNewLeadSelectionPath();
            if(treePath==null)
                return;
            StringBuilder pathFromRoot = new StringBuilder();
            Object[] nodes = treePath.getPath();
            for(int i=1;i<nodes.length;i++) {
                pathFromRoot.append(File.separatorChar).append(nodes[i].toString());
            }
            tabbedPanel.openFileInNewTab(new File(chosenDir+pathFromRoot.toString()));
        }
    }
}

/**
 * Класс, дополняющий JTextField полем placeholder
 */
class JTextFieldPlaceholder extends JTextField {
    private String placeholder;
    JTextFieldPlaceholder(String placeholder, int columns){
        super(placeholder,columns);
        setPlaceholder(placeholder);
    }

    public void setPlaceholder(String placeholder){
        this.placeholder=placeholder;
    }

    public String getPlaceholder(){
        return placeholder;
    }

    @Override
    public String getText(){
        if(super.getText().equals(placeholder)){
            return "";
        }else{
            return super.getText();
        }
    }
}

/**
 * Класс, использующийся для наименования узлов древа
 */
class FileNode {
    private File file;

    public FileNode(File file) {
        this.file = file;
    }
    @Override
    public String toString() {
        String name = file.getName();
        if (name.isEmpty()) {
            return file.getAbsolutePath();
        } else {
            return name;
        }
    }
}


