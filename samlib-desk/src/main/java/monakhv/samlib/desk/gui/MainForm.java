/*
 * Created by JFormDesigner on Fri Feb 13 18:11:31 MSK 2015
 */

package monakhv.samlib.desk.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import monakhv.samlib.db.SQLController;
import monakhv.samlib.db.entity.Author;
import monakhv.samlib.desk.Main;
import monakhv.samlib.desk.data.Settings;
import monakhv.samlib.desk.sql.AuthorController;
import monakhv.samlib.desk.sql.BookController;
import monakhv.samlib.exception.SamlibParseException;
import monakhv.samlib.http.HttpClientController;
import monakhv.samlib.log.Log;

/**
 * @author Dmitry Monakhov
 */
public class MainForm extends JFrame {
    private static final String DEBUG_TAG="MainForm";
    private final DefaultListModel<Author> authorsModel;
    //private final DefaultListModel<Book> booksModel;

    private final SQLController sql;
    private Settings settings;
    private BookList bkList;
    private String selection=null;
    private String sortOrder=SQLController.COL_NAME;
    private Author selectedAuthor;

    public MainForm( Settings settings ) {
        this.settings=settings;
        SQLController sql1;

        try {
            sql1 = SQLController.getInstance( settings.getDataDirectoryPath()  );
        } catch (Exception e) {
            Log.e(DEBUG_TAG,"Error SQL init");
            sql1 =null;
        }
        sql = sql1;
        authorsModel = new DefaultListModel<>();
//        booksModel = new DefaultListModel<>();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                Main.exit(1);
            }
        });

        initComponents();


        addSortedAuthorList();
        jAuthorList.setModel(authorsModel);
        jAuthorList.setCellRenderer(new AuthorRenderer());
        jAuthorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        //TODO: we can move the constant 20 to settings
        bookScrolPanel.getVerticalScrollBar().setUnitIncrement(20);

        bkList = new BookList(bookPanel);

        jAuthorList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                JList lsm = (JList) e.getSource();


                if (lsm.isSelectionEmpty()|| !e.getValueIsAdjusting() ) {
                    return;
                }
                if (lsm.getMinSelectionIndex() != lsm.getMaxSelectionIndex()) {
                    Log.i(DEBUG_TAG, "selection " + lsm.getMinSelectionIndex() + " - " + lsm.getMaxSelectionIndex());
                    return;
                }
                selectedAuthor=authorsModel.get(lsm.getMaxSelectionIndex());
                Log.i(DEBUG_TAG, "selection " +selectedAuthor.getName()+"  "+e.getValueIsAdjusting());
                loadBookList(selectedAuthor);
            }
        });



    }

    /**
     * Construct Author list
     */
    private void addSortedAuthorList() {
        AuthorController ctl = new AuthorController(sql);
         authorsModel.removeAllElements();


        for (Author a : ctl.getAll(selection,sortOrder) ){
            authorsModel.addElement(a);
        }

    }

    /**
     * Construct Book List
     * @param a
     */
    private void loadBookList(Author a){
        BookController ctl = new BookController(sql);

        if (a != null){
            bkList.load(ctl.getAll(a, null));
        }


        bookPanel.setComponentPopupMenu(bookPopup);
        this.getContentPane().validate();
        this.getContentPane().repaint();


    }


    private void menuItemExitActionPerformed(ActionEvent e) {
        Main.exit(0);
    }

    private void reFreshActionPerformed(ActionEvent e) {
        bookPanel.revalidate();
        this.repaint();
    }

    private void buttonUpdateActionPerformed(ActionEvent e) {
        buttonUpdate.setEnabled(false);
        Update update = new Update();
        update.execute();
    }

    private void menuItemSettingsActionPerformed(ActionEvent e) {
        SettingsForm.show(this,settings);
    }



    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        menuBar1 = new JMenuBar();
        menu1 = new JMenu();
        menuItemSettings = new JMenuItem();
        menuItemExit = new JMenuItem();
        panelMain = new JPanel();
        toolBar = new JPanel();
        buttonUpdate = new JButton();
        progressBar1 = new JProgressBar();
        reFresh = new JButton();
        scrollPane1 = new JScrollPane();
        jAuthorList = new JList();
        bookScrolPanel = new JScrollPane();
        bookPanel = new JPanel();
        bookPopup = new JPopupMenu();
        menuItem3 = new JMenuItem();
        menuItem4 = new JMenuItem();
        menuItem5 = new JMenuItem();
        menuItem6 = new JMenuItem();

        //======== this ========
        setMinimumSize(new Dimension(20, 70));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== menuBar1 ========
        {

            //======== menu1 ========
            {
                menu1.setText("File");

                //---- menuItemSettings ----
                menuItemSettings.setText("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438");
                menuItemSettings.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        menuItemSettingsActionPerformed(e);
                    }
                });
                menu1.add(menuItemSettings);

                //---- menuItemExit ----
                menuItemExit.setText("Exit");
                menuItemExit.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        menuItemExitActionPerformed(e);
                    }
                });
                menu1.add(menuItemExit);
            }
            menuBar1.add(menu1);
        }
        setJMenuBar(menuBar1);

        //======== panelMain ========
        {
            panelMain.setMinimumSize(new Dimension(800, 100));
            panelMain.setBorder(Borders.DLU4);
            panelMain.setLayout(new FormLayout(
                "[200dlu,default]:grow, 5dlu, [350dlu,default]:grow(0.8), default",
                "default, fill:[400dlu,default]:grow, $lgap, default"));

            //======== toolBar ========
            {
                toolBar.setLayout(new GridBagLayout());
                ((GridBagLayout)toolBar.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout)toolBar.getLayout()).rowHeights = new int[] {0, 5, 0};
                ((GridBagLayout)toolBar.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout)toolBar.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                //---- buttonUpdate ----
                buttonUpdate.setText("Update");
                buttonUpdate.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        buttonUpdateActionPerformed(e);
                    }
                });
                toolBar.add(buttonUpdate, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));
                toolBar.add(progressBar1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 5), 0, 0));

                //---- reFresh ----
                reFresh.setText("Refresh");
                reFresh.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        reFreshActionPerformed(e);
                    }
                });
                toolBar.add(reFresh, new GridBagConstraints(24, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 0), 0, 0));
            }
            panelMain.add(toolBar, CC.xywh(1, 1, 3, 1));

            //======== scrollPane1 ========
            {
                scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

                //---- jAuthorList ----
                jAuthorList.setComponentPopupMenu(null);
                scrollPane1.setViewportView(jAuthorList);
            }
            panelMain.add(scrollPane1, CC.xy(1, 2));

            //======== bookScrolPanel ========
            {
                bookScrolPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                bookScrolPanel.setDoubleBuffered(true);
                bookScrolPanel.setAutoscrolls(true);
                bookScrolPanel.setComponentPopupMenu(null);

                //======== bookPanel ========
                {
                    bookPanel.setAutoscrolls(true);
                    bookPanel.setComponentPopupMenu(bookPopup);
                    bookPanel.setLayout(new GridBagLayout());
                    ((GridBagLayout)bookPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
                    ((GridBagLayout)bookPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                    ((GridBagLayout)bookPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                    ((GridBagLayout)bookPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                }
                bookScrolPanel.setViewportView(bookPanel);
            }
            panelMain.add(bookScrolPanel, CC.xy(3, 2));
        }
        contentPane.add(panelMain, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //======== bookPopup ========
        {

            //---- menuItem3 ----
            menuItem3.setText("text");
            bookPopup.add(menuItem3);

            //---- menuItem4 ----
            menuItem4.setText("text");
            bookPopup.add(menuItem4);
        }

        //---- menuItem5 ----
        menuItem5.setText("text");

        //---- menuItem6 ----
        menuItem6.setText("text");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JMenuBar menuBar1;
    private JMenu menu1;
    private JMenuItem menuItemSettings;
    private JMenuItem menuItemExit;
    private JPanel panelMain;
    private JPanel toolBar;
    private JButton buttonUpdate;
    private JProgressBar progressBar1;
    private JButton reFresh;
    private JScrollPane scrollPane1;
    private JList jAuthorList;
    private JScrollPane bookScrolPanel;
    private JPanel bookPanel;
    private JPopupMenu bookPopup;
    private JMenuItem menuItem3;
    private JMenuItem menuItem4;
    private JMenuItem menuItem5;
    private JMenuItem menuItem6;
    // JFormDesigner - End of variables declaration  //GEN-END:variables


    class Update extends SwingWorker<Void,Void>{

        @Override
        protected Void doInBackground() throws Exception {
            AuthorController ctl = new AuthorController(sql);
            HttpClientController http = HttpClientController.getInstance(settings);
            for (Author a : ctl.getAll(selection,sortOrder) ){
                String url = a.getUrl();

                Author newA;


                try {
                    newA = http.getAuthorByURL(url);
                }
                catch (IOException ex ){
                    Log.e(DEBUG_TAG, "Connection Error: "+url, ex);

                    return null;

                }
                catch (SamlibParseException ex){
                    Log.e(DEBUG_TAG, "Error parsing url: " + url + " skip update author ", ex);

                    //++skippedAuthors;
                    newA = a;
                }

                if (a.update(newA)) {//we have update for the author

                    Log.i(DEBUG_TAG, "We need update author: " + a.getName());
                    ctl.update(a);
                }

            }

            return null;
        }

        @Override
        protected void done() {
            super.done();
            buttonUpdate.setEnabled(true);
            loadBookList(selectedAuthor);

        }
    }
}
