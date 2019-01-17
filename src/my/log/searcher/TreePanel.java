package my.log.searcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.io.File;
import java.io.FilenameFilter;

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
        //extension.getDocument().addDocumentListener(new extensionChange());
        extension.addFocusListener(new placeholderHandler());

        filterText = new JTextFieldPlaceholder("Слово в файле",15);
        filterText.setForeground(Color.GRAY);
        filterText.addFocusListener(new placeholderHandler());

        acceptFilterBtn = new JButton("Искать");
        acceptFilterBtn.addActionListener(new acceptFilter());

        filterPanel.add(filterHelper);
        filterPanel.add(extension);
        filterPanel.add(filterText);
        filterPanel.add(acceptFilterBtn);

        add(new JScrollPane(tree), BorderLayout.CENTER);
        setMinimumSize(new Dimension(160,0));
        fillTreeModel();
    }

    /**
     * Метод пере/создает древо для компонента JTree
     */
    public void fillTreeModel(){
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new FileNode(chosenDir));
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        if(extension.getText().isEmpty() || extension.getText().equals(extension.getPlaceholder())){
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
     * Обработчик, применяющий фильтр
     */
    class acceptFilter implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            System.out.println(extension.getText());
            System.out.println(filterText.getText());
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
        if (name.isEmpty()) {
            return file.getAbsolutePath();
        } else {
            return name;
        }
    }
}

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

    public String getText(){
        if(super.getText().equals(placeholder)){
            return "";
        }else{
            return super.getText();
        }
    }
}

