package org.ktu.transformations.uml2sbvr.ui;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import org.ktu.transformations.uml2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.transformations.uml2sbvr.extract.FactDiagramGenerator;
import org.ktu.transformations.uml2sbvr.models.CandidateTableModel;
import org.ktu.transformations.uml2sbvr.models.CandidateTableModel.CandidateEntry;
import org.ktu.transformations.uml2sbvr.models.FilteredConceptModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel.ExpressionType;
import org.ktu.transformations.uml2sbvr.models.SourceEntry;
import org.ktu.transformations.uml2sbvr.ui.EditCandidateDialog.ConceptType;
import org.ktu.transformations.uml2sbvr.ui.EditCandidateDialog.Operation;

/**
 *
 * @author Paulius
 */
@SuppressWarnings("serial")
public class ExtractionWizardDialog extends javax.swing.JDialog {

    public FilteredConceptModel gcCandidates, vcCandidates, brCandidates;
    private FilteredConceptModel copy_gcCandidates, copy_vcCandidates, copy_brCandidates;
    public FilteredConceptModel temp_vcCandidates, temp_brCandidates;
    private AbstractSBVRExtractor extractor;
    private boolean extractModelVoc;
    private NamedElement diagramPackage;
    private OptionsDialog optDlg;

    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/ui/Bundle");

    private static final String pformat = "<p style=\"margin-top:2px; margin-bottom: 2px;\">";
    private static final String typeformat = "<span style=\"padding-left: 20px; margin-top: 0px; color: grey;\">%s:</span> ";

    private static final String[] messages_gcTable = {bundle.getString("ExtractionWizardDialog_3"), bundle.getString("ExtractionWizardDialog_4"),
        bundle.getString("ExtractionWizardDialog_47"), bundle.getString("ExtractionWizardDialog_5"),
        bundle.getString("ExtractionWizardDialog_6"), bundle.getString("ExtractionWizardDialog_7")};
    private static final String[] messages_vcTable = {bundle.getString("ExtractionWizardDialog_12"), bundle.getString("ExtractionWizardDialog_13"),
        bundle.getString("ExtractionWizardDialog_47"), bundle.getString("ExtractionWizardDialog_14"),
        bundle.getString("ExtractionWizardDialog_13"), bundle.getString("ExtractionWizardDialog_14")};
    private static final String[] messages_brTable = {bundle.getString("ExtractionWizardDialog_20"), bundle.getString("ExtractionWizardDialog_21"),
        bundle.getString("ExtractionWizardDialog_47"), bundle.getString("ExtractionWizardDialog_22"),
        bundle.getString("ExtractionWizardDialog_21"), bundle.getString("ExtractionWizardDialog_22")};

    public ExtractionWizardDialog(Frame parent, OptionsDialog optDlg, boolean modal, NamedElement diagramPackage, AbstractSBVRExtractor extractor) {
        super(parent, modal);
        this.diagramPackage = diagramPackage;
        this.extractor = extractor;
        this.extractModelVoc = extractor.isExtractModelVocabulary();
        this.optDlg = optDlg;
        initComponents();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ExtractionWizardDialog.class.getName()).log(Level.WARNING, null, ex);
        }
        SwingUtilities.updateComponentTreeUI(this);
        extractor.setGCCandidateModel(new FilteredConceptModel());
        extractor.setVCCandidateModel(new FilteredConceptModel());
        extractor.setBRCandidateModel(new FilteredConceptModel());
        extractor.extractAll();
        gcCandidates = (FilteredConceptModel) extractor.getGCCandidateModel();
        vcCandidates = (FilteredConceptModel) extractor.getVCCandidateModel();
        brCandidates = (FilteredConceptModel) extractor.getBRCandidateModel();
        copy_gcCandidates = gcCandidates.clone();
        copy_vcCandidates = vcCandidates.clone();
        copy_brCandidates = brCandidates.clone();
        vcCandidates.removeAll();
        brCandidates.removeAll();
        updateTableModel(JTable1, messages_gcTable, gcCandidates,
                jRadioButton3.isSelected() ? null : jRadioButton2.isSelected(), false);
        int tabcount = jTabbedPane1.getTabCount();
        jTabbedPane1.setEnabledAt(tabcount - 2, false);
        jTabbedPane1.setEnabledAt(tabcount - 1, false);
        JButton3.setEnabled(false);
        final ExtractionWizardDialog thisDlg = this;
        JTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = JTable1.getSelectedRow();
                    int actual = JTable1.convertRowIndexToModel(row);
                    if (getGCTableModel().getEntryAt(actual).getExpression().isModelVocabularyConcept())
                        JOptionPane.showMessageDialog(thisDlg, bundle.getString("ExtractionWizardDialog_56"),
                                bundle.getString("ExtractionWizardDialog_57"), JOptionPane.WARNING_MESSAGE);
                    else {
                        EditCandidateDialog dlg = new EditCandidateDialog(thisDlg, bundle.getString("ExtractionWizardDialog_32"),
                                true, Operation.EDIT, ConceptType.GENERAL_CONCEPT);
                        dlg.setCandidateIndex(actual);
                        dlg.setVisible(true);
                    }
                } else if (e.getClickCount() == 1)
                    applyFilter(JTable1, gcCandidates);
            }
        });
        JTable4.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = JTable4.getSelectedRow();
                    int actual = JTable4.convertRowIndexToModel(row);
                    if (getVCTableModel().getEntryAt(actual).getExpression().isModelVocabularyConcept())
                        JOptionPane.showMessageDialog(thisDlg, bundle.getString("ExtractionWizardDialog_56"),
                                bundle.getString("ExtractionWizardDialog_57"), JOptionPane.WARNING_MESSAGE);
                    else {
                        EditCandidateDialog dlg = new EditCandidateDialog(thisDlg, bundle.getString("ExtractionWizardDialog_33"), true,
                                Operation.EDIT, ConceptType.VERB_CONCEPT);
                        dlg.setCandidateIndex(actual);
                        dlg.setVisible(true);
                    }
                } else if (e.getClickCount() == 1)
                    applyFilter(JTable4, vcCandidates);
            }
        });
        JTable5.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    EditCandidateDialog dlg = new EditCandidateDialog(thisDlg, bundle.getString("ExtractionWizardDialog_34"), true,
                            Operation.EDIT, ConceptType.BUSINESS_RULE);
                    int row = JTable5.getSelectedRow();
                    int actual = JTable5.convertRowIndexToModel(row);
                    dlg.setCandidateIndex(actual);
                    dlg.setVisible(true);
                } else if (e.getClickCount() == 1)
                    applyFilter(JTable5, brCandidates);
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new ButtonGroup();
        buttonGroup2 = new ButtonGroup();
        jTabbedPane1 = new JTabbedPane();
        jPanel2 = new JPanel();
        jRadioButton1 = new JRadioButton();
        jLabel1 = new JLabel();
        jScrollPane2 = new JScrollPane();
        JTable1 = new JTable();
        JButton6 = new JButton();
        jButton5 = new JButton();
        jButton4 = new JButton();
        jRadioButton2 = new JRadioButton();
        jRadioButton3 = new JRadioButton();
        btnMerge = new JButton();
        jPanel3 = new JPanel();
        jScrollPane3 = new JScrollPane();
        JTable4 = new JTable();
        jRadioButton4 = new JRadioButton();
        jLabel2 = new JLabel();
        jButton7 = new JButton();
        jButton8 = new JButton();
        jRadioButton5 = new JRadioButton();
        jRadioButton6 = new JRadioButton();
        JButton9 = new JButton();
        jPanel4 = new JPanel();
        jButton10 = new JButton();
        jScrollPane4 = new JScrollPane();
        JTable5 = new JTable();
        JButton12 = new JButton();
        jButton11 = new JButton();
        JButton2 = new JButton();
        JButton3 = new JButton();
        btnCancel = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("ExtractionWizardDialog.title")); // NOI18N
        setName("Form"); // NOI18N

        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanel2.setName("jPanel2"); // NOI18N

        buttonGroup1.add(jRadioButton1);
        jRadioButton1.setText(bundle.getString("ExtractionWizardDialog.jRadioButton1.text")); // NOI18N
        jRadioButton1.setName("jRadioButton1"); // NOI18N
        jRadioButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        jLabel1.setText(bundle.getString("ExtractionWizardDialog.jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jScrollPane2.setName("jScrollPane2"); // NOI18N

        JTable1.setAutoCreateRowSorter(true);
        JTable1.setName("JTable1"); // NOI18N
        jScrollPane2.setViewportView(JTable1);

        JButton6.setText(bundle.getString("ExtractionWizardDialog.JButton6.text")); // NOI18N
        JButton6.setName("JButton6"); // NOI18N
        JButton6.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JButton6ActionPerformed(evt);
            }
        });

        jButton5.setText(bundle.getString("ExtractionWizardDialog.jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N
        jButton5.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton4.setText(bundle.getString("ExtractionWizardDialog.jButton4.text")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N
        jButton4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText(bundle.getString("ExtractionWizardDialog.jRadioButton2.text")); // NOI18N
        jRadioButton2.setName("jRadioButton2"); // NOI18N
        jRadioButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton2ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setSelected(true);
        jRadioButton3.setText(bundle.getString("ExtractionWizardDialog.jRadioButton3.text")); // NOI18N
        jRadioButton3.setName("jRadioButton3"); // NOI18N
        jRadioButton3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        btnMerge.setText(bundle.getString("ExtractionWizardDialog.btnMerge.text")); // NOI18N
        btnMerge.setName("btnMerge"); // NOI18N

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jRadioButton1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton3))
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnMerge)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JButton6)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton1)
                    .addComponent(jLabel1)
                    .addComponent(jRadioButton2)
                    .addComponent(jRadioButton3))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(JButton6)
                    .addComponent(jButton5)
                    .addComponent(jButton4)
                    .addComponent(btnMerge))
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("ExtractionWizardDialog.jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        jPanel3.setName("jPanel3"); // NOI18N

        jScrollPane3.setName("jScrollPane3"); // NOI18N

        JTable4.setAutoCreateRowSorter(true);
        JTable4.setName("JTable4"); // NOI18N
        jScrollPane3.setViewportView(JTable4);

        buttonGroup2.add(jRadioButton4);
        jRadioButton4.setText(bundle.getString("ExtractionWizardDialog.jRadioButton4.text")); // NOI18N
        jRadioButton4.setName("jRadioButton4"); // NOI18N
        jRadioButton4.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton4ActionPerformed(evt);
            }
        });

        jLabel2.setText(bundle.getString("ExtractionWizardDialog.jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jButton7.setText(bundle.getString("ExtractionWizardDialog.jButton7.text")); // NOI18N
        jButton7.setName("jButton7"); // NOI18N
        jButton7.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButton8.setText(bundle.getString("ExtractionWizardDialog.jButton8.text")); // NOI18N
        jButton8.setName("jButton8"); // NOI18N
        jButton8.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });

        buttonGroup2.add(jRadioButton5);
        jRadioButton5.setText(bundle.getString("ExtractionWizardDialog.jRadioButton5.text")); // NOI18N
        jRadioButton5.setName("jRadioButton5"); // NOI18N
        jRadioButton5.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton5ActionPerformed(evt);
            }
        });

        buttonGroup2.add(jRadioButton6);
        jRadioButton6.setSelected(true);
        jRadioButton6.setText(bundle.getString("ExtractionWizardDialog.jRadioButton6.text")); // NOI18N
        jRadioButton6.setName("jRadioButton6"); // NOI18N
        jRadioButton6.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jRadioButton6ActionPerformed(evt);
            }
        });

        JButton9.setText(bundle.getString("ExtractionWizardDialog.JButton9.text")); // NOI18N
        JButton9.setName("JButton9"); // NOI18N
        JButton9.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JButton9ActionPerformed(evt);
            }
        });

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jRadioButton4)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton5)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton6))
                    .addGroup(GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton8)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton7)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JButton9)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButton4)
                    .addComponent(jLabel2)
                    .addComponent(jRadioButton5)
                    .addComponent(jRadioButton6))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(JButton9)
                    .addComponent(jButton7)
                    .addComponent(jButton8))
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("ExtractionWizardDialog.jPanel3.TabConstraints.tabTitle"), jPanel3); // NOI18N

        jPanel4.setName("jPanel4"); // NOI18N

        jButton10.setText(bundle.getString("ExtractionWizardDialog.jButton10.text")); // NOI18N
        jButton10.setName("jButton10"); // NOI18N
        jButton10.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        JTable5.setAutoCreateRowSorter(true);
        JTable5.setName("JTable5"); // NOI18N
        jScrollPane4.setViewportView(JTable5);

        JButton12.setText(bundle.getString("ExtractionWizardDialog.JButton12.text")); // NOI18N
        JButton12.setName("JButton12"); // NOI18N
        JButton12.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JButton12ActionPerformed(evt);
            }
        });

        jButton11.setText(bundle.getString("ExtractionWizardDialog.jButton11.text")); // NOI18N
        jButton11.setName("jButton11"); // NOI18N
        jButton11.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(0, 338, Short.MAX_VALUE)
                        .addComponent(jButton11)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton10)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JButton12)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 295, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(JButton12)
                    .addComponent(jButton10)
                    .addComponent(jButton11))
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("ExtractionWizardDialog.jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        JButton2.setText(bundle.getString("ExtractionWizardDialog.JButton2.text")); // NOI18N
        JButton2.setName("JButton2"); // NOI18N
        JButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JButton2ActionPerformed(evt);
            }
        });

        JButton3.setText(bundle.getString("ExtractionWizardDialog.JButton3.text")); // NOI18N
        JButton3.setName("JButton3"); // NOI18N
        JButton3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JButton3ActionPerformed(evt);
            }
        });

        btnCancel.setText(bundle.getString("ExtractionWizardDialog.btnCancel.text")); // NOI18N
        btnCancel.setName("btnCancel"); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1)
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCancel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JButton3)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(JButton2)))
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(JButton2)
                    .addComponent(JButton3)
                    .addComponent(btnCancel))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void jButton11ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        new EditCandidateDialog(this, bundle.getString("ExtractionWizardDialog_34"), true,
                Operation.NEW, ConceptType.BUSINESS_RULE).setVisible(true);
    }//GEN-LAST:event_jButton11ActionPerformed

    private void jButton4ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        new EditCandidateDialog(this, bundle.getString("ExtractionWizardDialog_32"), true,
                Operation.NEW, ConceptType.GENERAL_CONCEPT).setVisible(true);
    }//GEN-LAST:event_jButton4ActionPerformed

    private void JButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_JButton2ActionPerformed
        if (diagramPackage == null && extractor.getExtractedDiagrams().isEmpty())
            return;
        String projName = diagramPackage != null ? diagramPackage.getName()
                : extractor.getExtractedDiagrams().toArray(new DiagramPresentationElement[]{})[0].getDiagram().getName();
        if (jTabbedPane1.getSelectedIndex() == jTabbedPane1.getTabCount() - 1) {
            if (JOptionPane.showConfirmDialog(this, bundle.getString("ExtractionWizardDialog_50"),
                    bundle.getString("ExtractionWizardDialog_51"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                new VetisExporter(projName) {

                    protected void printCandidate(PrintWriter writer, FilteredConceptModel dataset,
                            SBVRExpressionModel obj, SourceEntry concepts, String conceptType) {
                        if (dataset.isSelected(concepts, obj)) {
                            writer.println(obj.toUnderscoreString());
                            if (conceptType != null)
                                writer.format("\t%s: %s\n", PluginUtilities.CONCEPT_TYPE_STRING, conceptType);
                        }
                    }
                }.exportProject(null, gcCandidates, vcCandidates, brCandidates, extractModelVoc);
            Project proj = Application.getInstance().getProject();
            if (PluginUtilities.getCustomizationsProfile(proj) == null)
                PluginUtilities.addSBVRProfiles();
            FactDiagramGenerator generator = new FactDiagramGenerator(projName, extractModelVoc);
            generator.setGCCandidates(gcCandidates);
            generator.setVCCandidates(vcCandidates);
            generator.setBRCandidates(brCandidates);
            generator.generate();
            setVisible(false);
            if (optDlg != null)
                optDlg.setVisible(false);
        } else
            nextStep(1);
    }//GEN-LAST:event_JButton2ActionPerformed

    private void JButton3ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_JButton3ActionPerformed
        nextStep(-1);
    }//GEN-LAST:event_JButton3ActionPerformed

    private void jButton7ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        vcCandidates = copy_vcCandidates.clone();
        extractor.getVCReplacements().clear();
        updateTableModel(JTable4, messages_vcTable, vcCandidates, jRadioButton6.isSelected() ? null : jRadioButton5.isSelected(), true);
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton5ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        gcCandidates = copy_gcCandidates.clone();
        extractor.getGCReplacements().clear();
        updateTableModel(JTable1, messages_gcTable, gcCandidates, jRadioButton3.isSelected() ? null : jRadioButton2.isSelected(), true);
        extractor.createVerbConceptCandidates();
        extractor.createBusinessRuleCandidates();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton10ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        brCandidates = copy_brCandidates.clone();
        updateTableModel(JTable5, messages_brTable, brCandidates, null, true);
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton8ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        new EditCandidateDialog(this, bundle.getString("ExtractionWizardDialog_33"), true,
                Operation.NEW, ConceptType.VERB_CONCEPT).setVisible(true);
    }//GEN-LAST:event_jButton8ActionPerformed

    private void jRadioButton1ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        if (jRadioButton1.isSelected())
            updateTableModel(JTable1, messages_gcTable, gcCandidates, false, false);
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void jRadioButton2ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton2ActionPerformed
        if (jRadioButton2.isSelected())
            updateTableModel(JTable1, messages_gcTable, gcCandidates, true, false);
    }//GEN-LAST:event_jRadioButton2ActionPerformed

    private void jRadioButton3ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        if (jRadioButton3.isSelected())
            updateTableModel(JTable1, messages_gcTable, gcCandidates, null, false);
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    private void jRadioButton4ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton4ActionPerformed
        if (jRadioButton4.isSelected())
            updateTableModel(JTable4, messages_vcTable, vcCandidates, false, false);
    }//GEN-LAST:event_jRadioButton4ActionPerformed

    private void jRadioButton5ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton5ActionPerformed
        if (jRadioButton5.isSelected())
            updateTableModel(JTable4, messages_vcTable, vcCandidates, true, false);
    }//GEN-LAST:event_jRadioButton5ActionPerformed

    private void jRadioButton6ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_jRadioButton6ActionPerformed
        if (jRadioButton6.isSelected())
            updateTableModel(JTable4, messages_vcTable, vcCandidates, null, false);
    }//GEN-LAST:event_jRadioButton6ActionPerformed

    private void JButton6ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_JButton6ActionPerformed
        preview(1);
    }//GEN-LAST:event_JButton6ActionPerformed

    private void JButton9ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_JButton9ActionPerformed
        preview(2);
    }//GEN-LAST:event_JButton9ActionPerformed

    private void JButton12ActionPerformed(ActionEvent evt) {//GEN-FIRST:event_JButton12ActionPerformed
        preview(3);
    }//GEN-LAST:event_JButton12ActionPerformed

    public void addReplacement(String original, String replacement, SBVRExpressionModel expression, ConceptType type) {
        if (type == ConceptType.GENERAL_CONCEPT)
            extractor.getGCReplacements().put(original, new SimpleImmutableEntry<>(replacement, expression));
        else if (type == ConceptType.VERB_CONCEPT)
            extractor.getVCReplacements().put(original, new SimpleImmutableEntry<>(replacement, expression));
    }
    
    private void nextStep(int step) {
        int index = jTabbedPane1.getSelectedIndex() + step;
        jTabbedPane1.setSelectedIndex(index);
        final int tabcount = jTabbedPane1.getTabCount();
        for (int i = 0; i < tabcount; i++)
            jTabbedPane1.setEnabledAt(i, index == i);
        boolean identifiedFlag = step > 0;
        if (index == tabcount - 3) {
            gcCandidates.setAllIdentified(identifiedFlag);
            updateTableModel(JTable1, messages_gcTable, gcCandidates,
                    jRadioButton3.isSelected() ? null : jRadioButton2.isSelected(), false);
        } else if (index == tabcount - 2) {
            gcCandidates.setAllIdentified(identifiedFlag);
            if (step > 0) {
                extractor.getGCCandidateModel().setAllIdentified(identifiedFlag);
                extractor.createVerbConceptCandidates();
                if (extractModelVoc)
                    extractor.createModelVocabularyCandidates();
                vcCandidates = (FilteredConceptModel) extractor.getVCCandidateModel();
                updateTableModel(JTable4, messages_vcTable, vcCandidates,
                        jRadioButton6.isSelected() ? null : jRadioButton5.isSelected(), false);
            }
        } else if (index == tabcount - 1) {
            vcCandidates.setAllIdentified(identifiedFlag);
            if (step > 0) {
                extractor.getVCCandidateModel().setAllIdentified(identifiedFlag);
                extractor.createBusinessRuleCandidates();
                brCandidates = (FilteredConceptModel) extractor.getBRCandidateModel();
                updateTableModel(JTable5, messages_brTable, brCandidates, null, false);
            }
        }
        JButton3.setEnabled(index != 0);
        if (index == jTabbedPane1.getTabCount() - 1)
            JButton2.setText(bundle.getString("ExtractionWizardDialog_38"));
        else
            JButton2.setText(bundle.getString("ExtractionWizardDialog_29"));
    }

    private void updateTableModel(final JTable table, String[] messages, FilteredConceptModel modeldata,
            Boolean modelVoc, boolean restore) {
        TableModel tmodel = table.getModel();
        if (tmodel == null || !(tmodel instanceof CandidateTableModel) || restore)
            table.setModel(new CandidateTableModel(modeldata, messages));
        CandidateTableModel model = (CandidateTableModel) table.getModel();
        if (modelVoc == null)
            model.setDefaultView();
        else if (modelVoc.equals(Boolean.TRUE))
            model.setModelVocabularyView();
        else if (modelVoc.equals(Boolean.FALSE))
            model.setBusinessVocabularyView();
        table.getColumnModel().getColumn(0).setMinWidth(20);
        table.getColumnModel().getColumn(0).setPreferredWidth(20);
        table.getColumnModel().getColumn(0).setMaxWidth(20);
        table.getColumnModel().getColumn(3).setMinWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setMaxWidth(75);
        TableColumn tc = table.getColumnModel().getColumn(4);
        tc.setMinWidth(75);
        tc.setPreferredWidth(75);
        tc.setMaxWidth(75);
        tc.setCellEditor(table.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(table.getDefaultRenderer(Boolean.class));
        tc.setHeaderRenderer(new CheckBoxHeader(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                Object source = e.getSource();
                if (source instanceof AbstractButton == false)
                    return;
                for (int x = 0, y = table.getRowCount(); x < y; x++)
                    table.setValueAt(e.getStateChange() == ItemEvent.SELECTED, x, 4);
            }

        }, "Create trace"));
        table.updateUI();
    }

    private void preview(int step) {
        if (step < 1 || step > 3)
            return;
        PreviewDialog dlg = new PreviewDialog(this, true);
        JTextPane bvCtl = dlg.getBusinessVocEditor();
        JTextPane mvCtl = dlg.getModelVocEditor();
        bvCtl.setContentType("text/html");
        bvCtl.setText(null);
        mvCtl.setContentType("text/html");
        mvCtl.setText(null);
        if (step <= 1) {
            display(bvCtl, gcCandidates, ExpressionType.GENERAL_CONCEPT, false);
            if (extractModelVoc)
                display(mvCtl, gcCandidates, ExpressionType.GENERAL_CONCEPT, true);
            dlg.setVisible(true);
            return;
        }
        if (step >= 2) {
            bvCtl.setText(verbConceptsString(false));
            if (extractModelVoc)
                mvCtl.setText(verbConceptsString(true));
        }
        if (step == 3)
            display(dlg.getRulesEditor(), brCandidates, ExpressionType.RULE_TYPE, false);
        if (!extractModelVoc)
            dlg.disableModelVocTab();
        dlg.setVisible(true);
    }

    private String verbConceptsString(boolean showModelVoc) {
        StringBuilder voc = new StringBuilder();
        voc.append("<html>");
        Map<String, SBVRExpressionModel> model = gcCandidates.getListMap();
        for (String gc : model.keySet()) {
            SBVRExpressionModel sbvr = model.get(gc);
            if (sbvr.isModelVocabularyConcept() == showModelVoc) {
                voc.append(pformat).append(sbvr.toHTMLString(false, true)).append("<br />")
                        .append(String.format(typeformat, PluginUtilities.CONCEPT_TYPE_STRING))
                        .append(String.format(SBVRExpressionModel.CGC_FORMAT, PluginUtilities.GC_UNDERSCORE_STRING));
                for (SBVRExpressionModel synonym : sbvr.getSynonymousForms())
                    voc.append("<br />").append(String.format(typeformat, PluginUtilities.SYNONYMOUS_FORM_STRING))
                            .append(synonym.toHTMLString(false, true));
                voc.append("</p>");
            }
        }
        model = vcCandidates.getListMap();
        for (String vc : model.keySet()) {
            SBVRExpressionModel sbvr = model.get(vc);
            if (sbvr.isModelVocabularyConcept() == showModelVoc) {
                voc.append(pformat).append(sbvr.toHTMLString(false, true)).append("<br />")
                        .append(String.format(typeformat, PluginUtilities.CONCEPT_TYPE_STRING))
                        .append(String.format(SBVRExpressionModel.CVC_FORMAT, PluginUtilities.VC_UNDERSCORE_STRING));
                for (SBVRExpressionModel synonym : sbvr.getSynonymousForms())
                    voc.append("<br />").append(String.format(typeformat, PluginUtilities.SYNONYMOUS_FORM_STRING))
                            .append(synonym.toHTMLString(false, true));
                voc.append("</p>");
            }
        }
        voc.append("</html>");
        return voc.toString();
    }

    private void display(JTextPane bvCtl, FilteredConceptModel candidates, ExpressionType type, boolean modelVoc) {
        StringBuilder voc = new StringBuilder();
        voc.append("<html>");
        HashMap<String, SBVRExpressionModel> model = candidates.getListMap();
        for (String gc : model.keySet())
            if (model.get(gc).isModelVocabularyConcept() == modelVoc) {
                voc.append(pformat).append(model.get(gc).toHTMLString(false, true));
                if (type == ExpressionType.GENERAL_CONCEPT)
                    voc.append("<br />").append(String.format(typeformat, PluginUtilities.CONCEPT_TYPE_STRING))
                            .append(String.format(SBVRExpressionModel.CGC_FORMAT, PluginUtilities.GC_UNDERSCORE_STRING));
                voc.append("</p>");
            }
        voc.append("</html>");
        bvCtl.setText(voc.toString());
    }

    public CandidateTableModel getGCTableModel() {
        return (CandidateTableModel) JTable1.getModel();
    }

    public CandidateTableModel getVCTableModel() {
        return (CandidateTableModel) JTable4.getModel();
    }

    public CandidateTableModel getBRTableModel() {
        return (CandidateTableModel) JTable5.getModel();
    }

    private void applyFilter(JTable table, FilteredConceptModel data) {
        if (table.getSelectedColumn() == 0) {
            CandidateTableModel model = (CandidateTableModel) table.getModel();
            int row = table.getSelectedRow();
            row = table.convertRowIndexToModel(row);
            CandidateEntry entry = model.getEntryAt(row);
            boolean value = (Boolean) model.getValueAt(row, table.getSelectedColumn());
            data.setSelectedState(entry.getElements(), entry.getExpression(), value);
        }
    }

    public AbstractSBVRExtractor getExtractor() {
        return extractor;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton JButton12;
    private JButton JButton2;
    private JButton JButton3;
    private JButton JButton6;
    private JButton JButton9;
    private JTable JTable1;
    private JTable JTable4;
    private JTable JTable5;
    private JButton btnCancel;
    private JButton btnMerge;
    private ButtonGroup buttonGroup1;
    private ButtonGroup buttonGroup2;
    private JButton jButton10;
    private JButton jButton11;
    private JButton jButton4;
    private JButton jButton5;
    private JButton jButton7;
    private JButton jButton8;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JPanel jPanel2;
    private JPanel jPanel3;
    private JPanel jPanel4;
    private JRadioButton jRadioButton1;
    private JRadioButton jRadioButton2;
    private JRadioButton jRadioButton3;
    private JRadioButton jRadioButton4;
    private JRadioButton jRadioButton5;
    private JRadioButton jRadioButton6;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JScrollPane jScrollPane4;
    private JTabbedPane jTabbedPane1;
    // End of variables declaration//GEN-END:variables

}
