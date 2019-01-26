package org.ktu.model2sbvr.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import org.ktu.model2sbvr.models.CandidateTableModel;
import org.ktu.model2sbvr.models.CandidateTableModel.CandidateEntry;
import org.ktu.model2sbvr.models.FilteredConceptModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.RuleType;
import org.ktu.model2sbvr.models.SourceEntry;

@SuppressWarnings("serial")
public class EditCandidateDialog extends JDialog {

    public enum ConceptType {

        GENERAL_CONCEPT, VERB_CONCEPT, BUSINESS_RULE
    }

    public enum Operation {

        NEW, EDIT
    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/model2sbvr/ui/Bundle");
    private FilteredConceptModel data;
    private CandidateTableModel tablemodel;
    private int candidateIndex;
    private Set<String> gcTempList, vcTempList;
    private final ConceptType type;
    private final Operation operation;
    private Vector<Object[]> gcTemp, vcTemp;
    private final ExtractionWizardDialog parentDlg;
    private Map<String, SourceEntry> map;
    private SBVRExpressionModel editedConcept;
    private CandidateEntry item;

    public EditCandidateDialog(ExtractionWizardDialog parent, String title, boolean modal,
            Operation operation, ConceptType type) {
        super(parent, title, modal);
        this.parentDlg = parent;
        this.type = type;
        if (type == ConceptType.GENERAL_CONCEPT) {
            this.data = parent.gcCandidates;
            this.tablemodel = parent.getGCTableModel();
        } else if (type == ConceptType.VERB_CONCEPT) {
            this.data = parent.vcCandidates;
            this.tablemodel = parent.getVCTableModel();
        } else if (type == ConceptType.BUSINESS_RULE) {
            this.data = parent.brCandidates;
            this.tablemodel = parent.getBRTableModel();
        }
        this.operation = operation;
        map = new HashMap<>();
        initComponents();
        for (SourceEntry element : data.manualExtractionCandidates())
            map.put(element.toString(), element);
        comboBox.setModel(new DefaultComboBoxModel(map.keySet().toArray()));
        if (operation == Operation.NEW)
            init_new();
        else
            init_edit();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new JLabel();
        comboBox = new JComboBox();
        jLabel2 = new JLabel();
        jScrollPane1 = new JScrollPane();
        textArea = new JTextArea();
        btnCancel = new JButton();
        btnSave = new JButton();
        jScrollPane3 = new JScrollPane();
        previewLabel = new JEditorPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("EditCandidateDialog.title")); // NOI18N
        setType(Type.UTILITY);

        jLabel1.setText(bundle.getString("EditCandidateDialog.jLabel1.text")); // NOI18N

        jLabel2.setText(bundle.getString("EditCandidateDialog.jLabel2.text")); // NOI18N

        textArea.setColumns(20);
        textArea.setFont(new Font("Tahoma", 0, 11)); // NOI18N
        textArea.setRows(5);
        textArea.setWrapStyleWord(true);
        textArea.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                textAreaKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(textArea);

        btnCancel.setText(bundle.getString("EditCandidateDialog.btnCancel.text")); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        btnSave.setText(bundle.getString("EditCandidateDialog.btnSave.text")); // NOI18N

        jScrollPane3.setBorder(null);
        jScrollPane3.setForeground(UIManager.getDefaults().getColor("Label.background"));
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        previewLabel.setEditable(false);
        previewLabel.setBackground(UIManager.getDefaults().getColor("control"));
        previewLabel.setContentType("text/html"); // NOI18N
        previewLabel.setText(bundle.getString("EditCandidateDialog.previewLabel.text")); // NOI18N
        jScrollPane3.setViewportView(previewLabel);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(42, 42, 42)
                        .addComponent(comboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnSave)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
                            .addComponent(jScrollPane3))))
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnSave))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public JButton getBtnCancel() {
        return btnCancel;
    }

    private void init_new() {
        final EditCandidateDialog dlg = this;
        dlg.setTitle("Add new candidate concept");
        comboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox combo = (JComboBox) e.getItemSelectable();
                String text = combo.getSelectedItem().toString();
                SourceEntry elemdata = map.get(text);
                dlg.getTextArea().setText(dlg.extractCandidateText(elemdata));
                updatePreviewLabel(dlg);
            }
        });
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (performUpdate(dlg, map.get(dlg.getComboBox().getSelectedItem())))
                    dlg.setVisible(false);
            }
        });
    }

    private void init_edit() {
        final EditCandidateDialog dlg = this;
        dlg.setTitle("Edit candidate concept");
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (performUpdate(dlg, item.getElements()))
                    dlg.setVisible(false);
            }
        });

    }

    private boolean performUpdate(EditCandidateDialog dlg, SourceEntry elements) {
        editedConcept = item != null ? item.getExpression() : null;
        String candidate = dlg.getTextArea().getText().trim();
        if (candidate.trim().length() == 0) {
            JOptionPane.showMessageDialog(dlg, bundle.getString("EditCandidateDialog_3"),
                    bundle.getString("EditCandidateDialog_6"), JOptionPane.OK_OPTION);
            return false;
        }
        // Replace multiple whitespaces
        candidate = candidate.replaceAll("\\s+", " ");
        // Fix processing for general concepts
        if (type == ConceptType.GENERAL_CONCEPT)
            candidate = candidate.replaceAll(" ", "_");
        boolean exists = false;
        boolean no_mod = (operation == Operation.EDIT && candidate.compareTo(editedConcept.toUnderscoreString()) != 0);
        if (type == ConceptType.GENERAL_CONCEPT) {
            Set<String> gcCandidates = parentDlg.gcCandidates.getCandidatesListText();
            if (gcCandidates != null && no_mod)
                exists = gcCandidates.contains(candidate.replaceAll("_", " "));
        } else if (type == ConceptType.VERB_CONCEPT) {
            Set<String> vcCandidates = parentDlg.vcCandidates.getCandidatesListText();
            if (vcCandidates != null && no_mod)
                exists = vcCandidates.contains(candidate.replaceAll("_", " "));
        } else if (type == ConceptType.BUSINESS_RULE) {
            Set<String> brCandidates = parentDlg.brCandidates.getCandidatesListText();
            if (brCandidates != null && no_mod)
                exists = brCandidates.contains(candidate.replaceAll("_", " "));
        }
        if (exists)
            JOptionPane.showMessageDialog(dlg, bundle.getString("EditCandidateDialog_7"),
                    bundle.getString("EditCandidateDialog_8"), JOptionPane.OK_OPTION);
        else {
            // Check for new concepts
            boolean canadd = true;
            gcTemp = new Vector<>();
            vcTemp = new Vector<>();
            gcTempList = new HashSet<>();
            vcTempList = new HashSet<>();
            if (type == ConceptType.VERB_CONCEPT) {
                String split[] = candidate.split("\\s+");
                Vector<String> gcs = new Vector<>();
                for (int i = 0; i < split.length; i += 2)		// Again, we follow VC pattern
                    gcs.add(split[i]);
                String[] arr = new String[gcs.size()];
                canadd = checkGCCandidates(gcs.toArray(arr), elements);
            } else if (type == ConceptType.BUSINESS_RULE) {
                String cand = candidate.replace(RuleType.OBLIGATION.toString(), "").replace(RuleType.PERMISSION.toString(), "").trim();
                String[] vcs = cand.split("(if|and)");
                for (int i = 0; i < vcs.length; i++)
                    vcs[i] = vcs[i].trim();
                Vector<String> gcs = new Vector<>();
                for (String vc : vcs) {
                    String[] items = vc.split("\\s+");
                    for (int i = 0; i < items.length; i += 2)
                        gcs.add(items[i].trim());
                }
                String[] arr = new String[gcs.size()];
                canadd = checkGCCandidates(gcs.toArray(arr), elements);
                if (canadd)
                    canadd = checkVCCandidates(vcs, elements);
            }

            if (!canadd)
                return false;
            if (gcTemp.size() > 0)
                for (Object[] obj : gcTemp) {
                    SBVRExpressionModel gcModel = new SBVRExpressionModel();
                    gcModel.addGeneralConcept((String) obj[1], true);
                    gcModel.setAuto(false);
                    SourceEntry candidates = (SourceEntry) obj[0];
                    parentDlg.gcCandidates.add(candidates, gcModel);
                    parentDlg.getGCTableModel().addRow(new Object[]{true,
                        map.get(dlg.getComboBox().getSelectedItem().toString()), gcModel, Boolean.FALSE, Boolean.FALSE});

                }
            if (vcTemp.size() > 0)
                for (Object[] obj : vcTemp) {
                    SBVRExpressionModel vcModel = new SBVRExpressionModel();
                    String[] items = ((String) obj[1]).split("\\s+");
                    for (int i = 0; i < items.length; i++)
                        if (i % 2 == 0)
                            vcModel.addGeneralConcept(items[i].replaceAll("_", ""), true);
                        else
                            vcModel.addVerbConcept(items[i].replaceAll("_", ""), true);
                    vcModel.setAuto(false);
                    SourceEntry candidates = (SourceEntry) obj[0];
                    parentDlg.gcCandidates.add(candidates, vcModel);
                    parentDlg.getVCTableModel().addRow(new Object[]{true,
                        map.get(dlg.getComboBox().getSelectedItem().toString()), vcModel, Boolean.FALSE, Boolean.FALSE});

                }
            SBVRExpressionModel model = new SBVRExpressionModel();
            if (type == ConceptType.GENERAL_CONCEPT || type == ConceptType.VERB_CONCEPT) {
                String split[] = candidate.split("\\s+");
                for (int i = 0; i < split.length; i++)
                    if (i % 2 == 0)
                        model.addGeneralConcept(split[i].replace("_", " "), true);
                    else
                        model.addVerbConcept(split[i].replace("_", " "), true);
            } else if (type == ConceptType.BUSINESS_RULE){
                String cand = candidate;
                if (candidate.startsWith(RuleType.OBLIGATION.toString())) {
                    model.addRuleExpression(RuleType.OBLIGATION);
                    cand = candidate.replace(RuleType.OBLIGATION.toString(), "");
                } else if (candidate.startsWith(RuleType.PERMISSION.toString())) {
                    model.addRuleExpression(RuleType.PERMISSION);
                    cand = candidate.replace(RuleType.PERMISSION.toString(), "");
                }
                String split[] = cand.split("\\s+");
                int s = 0;
                for (int i = 0; i < split.length; i++) {
                    if (split[i].compareToIgnoreCase("and") == 0) {
                        model.addAndExpression();
                        s = 0;
                    } else if (split[i].compareToIgnoreCase("or") == 0) {
                        model.addOrExpression();
                        s = 0;
                    } else s++;
                    if (s % 2 == 0)
                        model.addGeneralConcept(split[i].replace("_", " "), true);
                    else
                        model.addVerbConcept(split[i].replace("_", " "), true);
                }
            }
            model.setAuto(false);
            if (operation == Operation.NEW) {
                data.add(elements, model);
                tablemodel.addRow(new Object[]{true,
                    map.get(dlg.getComboBox().getSelectedItem().toString()), model, Boolean.FALSE, Boolean.FALSE});
            } else {   
                parentDlg.addReplacement(editedConcept.toString(), candidate.replaceAll("_", " "), model, type);
                editedConcept.replace(model);
                tablemodel.setValueAt(model, candidateIndex, 2);
            }
        }
        return !exists;
    }

    private void updatePreviewLabel(EditCandidateDialog dlg) {
        if (type == ConceptType.GENERAL_CONCEPT)
            dlg.getPreviewLabel().setText("<html>"
                    + String.format(SBVRExpressionModel.CGC_FORMAT, dlg.getTextArea().getText()) + "</html>");
        else if (type == ConceptType.VERB_CONCEPT)
            dlg.getPreviewLabel().setText("<html>" + underscoreToHTML(dlg.getTextArea().getText()) + "</html>");
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public JEditorPane getPreviewLabel() {
        return previewLabel;
    }

    @SuppressWarnings("unchecked")
    public void setCandidateIndex(int candidateIndex) {
        this.candidateIndex = candidateIndex;
        item = tablemodel.getEntryAt(candidateIndex);
        comboBox.setModel(new DefaultComboBoxModel(new String[]{tablemodel.getValueAt(candidateIndex, 1).toString()}));
        comboBox.setEnabled(false);
        editedConcept = item.getExpression();
        textArea.setText(editedConcept.toUnderscoreString());
        previewLabel.setText(editedConcept.toHTMLString(true, null));
        updatePreviewLabel(this);
    }

    private String extractCandidateText(SourceEntry elemdata) {
        StringBuilder str = new StringBuilder();
        List<String> names = elemdata.getSourceNames();
        str.append(removeElementName(names.get(0)));
        for (int i = 1; i < names.size(); i++)
            str.append(" ").append(removeElementName(names.get(i)).toLowerCase());
        return str.toString();
    }

    private String removeElementName(String str) {
        return parentDlg.getExtractor().removeMetaconceptName(str);
    }

    private boolean checkGCCandidates(String cand_concepts[], SourceEntry elements) {
        Set<String> gcCandidates = parentDlg.gcCandidates.getCandidatesListText();
        for (int i = 0; i < cand_concepts.length; i++)
            if (!gcCandidates.contains(cand_concepts[i].replaceAll("_", " ")))
                gcTempList.add(cand_concepts[i]);
        if (gcTempList.isEmpty())
            return true;
        String conceptstr = new String();
        for (String str : gcTempList)
            conceptstr += str + "\n"; //$NON-NLS-1$
        if (JOptionPane.showConfirmDialog(this,
                String.format(bundle.getString("EditCandidateDialog_15"), conceptstr),
                bundle.getString("EditCandidateDialog_16"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            for (String str : gcTempList)
                gcTemp.add(new Object[]{elements, str, false});
        else {
            JOptionPane.showMessageDialog(this, bundle.getString("EditCandidateDialog_17"),
                    bundle.getString("EditCandidateDialog_18"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean checkVCCandidates(String concepts[], SourceEntry elements) {
        Set<String> vcCandidates = parentDlg.vcCandidates.getCandidatesListText();
        for (int i = 0; i < concepts.length; i++)
            if (!vcCandidates.contains(concepts[i].replaceAll("_", " ").trim()))
                vcTempList.add(concepts[i].trim());
        String conceptstr = new String();
        for (String str : vcTempList)
            conceptstr += str + "\n";
        if (JOptionPane.showConfirmDialog(this,
                String.format(bundle.getString("EditCandidateDialog_20"), conceptstr),
                bundle.getString("EditCandidateDialog_21"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            for (String str : vcTempList)
                vcTemp.add(new Object[]{elements, str, false});
        else {
            JOptionPane.showMessageDialog(this, bundle.getString("EditCandidateDialog_22"),
                    bundle.getString("EditCandidateDialog_23"), JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private String underscoreToHTML(String text) {
        if (this.type == ConceptType.GENERAL_CONCEPT)
            return String.format(SBVRExpressionModel.CGC_FORMAT, text.replaceAll(" ", "_"));
        else {
            String result = new String();
            String split[] = text.trim().split("\\s+");
            for (int i = 0; i < split.length; i++) {
                // Apply pattern <general concept><verb concept><general concept><verb concept>....
                String tmp = split[i].trim().replace("_", " ");
                result += (i % 2 == 0 ? String.format(SBVRExpressionModel.CGC_FORMAT, tmp)
                        : String.format(SBVRExpressionModel.CVC_FORMAT, tmp)).trim() + " ";
            }
            return result.trim();
        }
    }
    private void btnCancelActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    private void textAreaKeyReleased(KeyEvent evt) {//GEN-FIRST:event_textAreaKeyReleased
        updatePreviewLabel(this);
    }//GEN-LAST:event_textAreaKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnCancel;
    private JButton btnSave;
    private JComboBox comboBox;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane3;
    private JEditorPane previewLabel;
    private JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
