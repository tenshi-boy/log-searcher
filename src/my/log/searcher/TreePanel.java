package my.log.searcher;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Класс наследующий JPanel и отображающей древо файлов выбранной дирректори и поле для ввода расширения
 */
public class TreePanel extends JPanel{
    JTextField extension;
    String extensionPlaceholder="Введите расширение";
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
        extension = new JTextField(extensionPlaceholder,15);
        extension.setForeground(Color.GRAY);
        extension.getDocument().addDocumentListener(new extensionChange());
        extension.addFocusListener(new extensionPlaceholder());
        add(extension,BorderLayout.NORTH);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        fillTreeModel();
    }

    /**
     * Метод пере/создает древо для компонента JTree
     */
    public void fillTreeModel(){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(chosenDir));
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        if(extension.getText().isEmpty() || extension.getText().equals(extensionPlaceholder)){
            fillTreeNodeNoExtension(chosenDir,root);
        }else{
            fillTreeNodeWithExtension(chosenDir,extension.getText(),root);
        }
        tree.setModel(treeModel);
    }


    /**
     * Метод для построения дерева из всех файлов выбранной дирректории
     * @param chosenDirArg - выбранная дирректория
     * @param node - древо
     */
    public void fillTreeNodeNoExtension(File chosenDirArg,DefaultMutableTreeNode node){
        for (File file : chosenDirArg.listFiles()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            node.add(childNode);
            if(file.isDirectory() && file.listFiles()!=null && file.listFiles().length>0) {
                fillTreeNodeNoExtension(file, childNode);
            }
        }
    }
    /**
     * Метод для построения дерева из файлов выбранной дирректории, соответствующих расширению extension
     * @param chosenDirArg - выбранная дирректория
     * @param extensionArg - расширение, используемое для фильтрации
     * @param node - древо
     */
    public boolean fillTreeNodeWithExtension(File chosenDirArg,String extensionArg,DefaultMutableTreeNode node){
        boolean extFilesFound=false;
        for (File file : chosenDirArg.listFiles()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            if (file.isDirectory() && file.listFiles()!=null && file.listFiles().length>0) {
                if(fillTreeNodeWithExtension(file,extensionArg,childNode)){
                    extFilesFound=true;
                    node.add(childNode);
                }
            }
        }
        for (File file : chosenDirArg.listFiles(new ExtMaskFilter(extensionArg))) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new FileNode(file));
            node.add(childNode);
            extFilesFound=true;
        }
        return extFilesFound;
    }
    class ExtMaskFilter implements FilenameFilter {
        String mask;

        ExtMaskFilter(String Mask) {
            if(Mask.substring(0,1).equals(".")){
                mask=Mask;
            }else{
                mask = "." + Mask;
            }
        }

        public boolean accept(File f, String name) {
            return(name.indexOf(mask) > 0);
        }
    }

    /**
     * Обработчик, вызывающий метод перерисовывающий древо
     */
    class extensionChange implements DocumentListener{
        @Override
        public void insertUpdate(DocumentEvent e) {
            fillTreeModel();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            fillTreeModel();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            fillTreeModel();
        }
    }

    /**
     * Обработчик, выводящий placeholder
     */
    class extensionPlaceholder implements FocusListener{
        @Override
        public void focusGained(FocusEvent e) {
            if (extension.getText().equals(extensionPlaceholder)) {
                extension.setText("");
                extension.setForeground(Color.BLACK);
            }
        }
        @Override
        public void focusLost(FocusEvent e) {
            if (extension.getText().isEmpty()) {
                extension.setForeground(Color.GRAY);
                extension.setText(extensionPlaceholder);
            }
        }
    }
    /**
     * Обработчик древа, после выбора узла дерева(файла) добавляет в tabbedPanel новую панель tabPanel с содержимым файла
     */
    class SelectedTreeFile implements TreeSelectionListener {
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
class FileNode {
    private File file;

    public FileNode(File file) {
        this.file = file;
    }
    @Override
    public String toString() {
        String name = file.getName();
        if (name.equals("")) {
            return file.getAbsolutePath();
        } else {
            return name;
        }
    }
}

