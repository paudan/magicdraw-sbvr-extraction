package org.ktu.transformations.uml2sbvr.ui;

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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import org.ktu.transformations.uml2sbvr.models.AbstractCandidateConceptModel;
import org.ktu.transformations.uml2sbvr.models.CandidateTableModel;
import org.ktu.transformations.uml2sbvr.models.FilteredCandidateConceptModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel;

@SuppressWarnings("serial")
public class EditCandidateDialog extends JDialog {

    public enum ConceptType {

        GENERAL_CONCEPT, VERB_CONCEPT, BUSINESS_RULE
    }

    public enum Operation {

        NEW, EDIT
    }

    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/ui/Bundle");

    private FilteredCandidateConceptModel data;
    private CandidateTableModel tablemodel;
    private String entry;
    private int candidateIndex;
    private Set<String> gcTempList, vcTempList;
    private final ConceptType type;
    private final Operation operation;
    private Vector<Object[]> gcTemp, vcTemp;
    private SBVRExpressionModel editedConcept;
    private final ExtractionWizardDialog parentDlg;
    private List<String> elements;
    private int previousHeight;

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
        initComponents();
        if (operation == Operation.NEW)
            init_new();
        else
            init_edit();
        previousHeight = getPreviewLabel().getHeight();
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
        previewLabel = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("EditCandidateDialog.title")); // NOI18N
        setAlwaysOnTop(true);
        setType(Type.UTILITY);

        jLabel1.setText(bundle.getString("EditCandidateDialog.jLabel1.text")); // NOI18N

        jLabel2.setText(bundle.getString("EditCandidateDialog.jLabel2.text")); // NOI18N

        textArea.setColumns(20);
        textArea.setRows(5);
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

        previewLabel.setText(bundle.getString("EditCandidateDialog.previewLabel.text")); // NOI18N

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
                            .addComponent(previewLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE))))
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
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(previewLabel, GroupLayout.DEFAULT_SIZE, 18, Short.MAX_VALUE)
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
        final Map<String, List<String>> map = new HashMap<>();
        for (List<String> element : data.manualExtractionCandidates())
            map.put(AbstractCandidateConceptModel.getConceptsRepresentation(element), elements);
        comboBox.setModel(new DefaultComboBoxModel(map.keySet().toArray()));
        comboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox combo = (JComboBox) e.getItemSelectable();
                String text = combo.getSelectedItem().toString();
                List<String> elemdata = map.get(text);
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
                if (performUpdate(dlg, elements))
                    dlg.setVisible(false);
            }
        });
    }

    private boolean performUpdate(EditCandidateDialog dlg, List<String> elements) {
        String candidate = dlg.getTextArea().getText().trim();
        if (candidate.trim().length() == 0) {
            JOptionPane.showMessageDialog(dlg, bundle.getString("EditCandidateDialog_3"),
                    bundle.getString("EditCandidateDialog_6"), JOptionPane.OK_OPTION);
            return false;
        }
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
                String split[] = candidate.split(" ");
                Vector<String> gcs = new Vector<>();
                for (int i = 0; i < split.length; i += 2)		// Again, we follow VC pattern
                    gcs.add(split[i]);
                String[] arr = new String[gcs.size()];
                canadd = checkGCCandidates(gcs.toArray(arr), elements);
            } /*else if (type == ConceptType.BUSINESS_RULE) {
             String [] split = candidate.replaceAll("</*html>", "").split("</span>"); 
             Vector<String> gcs = new Vector<String>();
             for (int i = 0; i < split.length; i++) 
             if (split[i].contains("color: teal")) //$NON-NLS-1$
             gcs.add(split[i]);
             String[] arr = new String[gcs.size()];
             canadd = checkGCCandidates(gcs.toArray(arr), elements, dlg.getComboBox().getSelectedItem().toString());
        		
             // If all possible GC candidates are in GC candidates we check verb concept candidates
             if (canadd) {
             gcs = new Vector<String>();
             for (int i = 0; i < split.length-1; i++) 
             if (split[i].contains("color: teal") && split[i+1].contains("color: blue")) 
             gcs.add(split[i] + " " + split[i+1]); 
             arr = new String[gcs.size()];
             canadd = checkVCCandidates(gcs.toArray(arr), elements, dlg.getComboBox().getSelectedItem().toString());
             }
             }*/

            if (!canadd)
                return false;
            else {
                if (gcTemp.size() > 0)
                    for (Object[] obj : gcTemp) {
                        SBVRExpressionModel model = new SBVRExpressionModel();
                        model.addGeneralConcept((String) obj[1], true);
                        model.setAuto(false);
                        List<String> candidates = (List<String>) obj[0];
                        parentDlg.gcCandidates.add(candidates, model, data.getSourceData().get(candidates));
                        parentDlg.getGCTableModel().addRow(new Object[]{true, 
                            dlg.getComboBox().getSelectedItem().toString(), model.toHTMLString(true, null), false});

                    }
            }
            SBVRExpressionModel model = new SBVRExpressionModel();
            String split[] = candidate.split(" ");
            for (int i = 0; i < split.length; i++)
                if (i % 2 == 0)
                    model.addGeneralConcept(split[i].replace("_", " "), true);
                else
                    model.addVerbConcept(split[i].replace("_", " "), true);
            model.setAuto(false);
            if (operation == Operation.NEW) {
                data.add(elements, model, data.getSourceData().get(elements));
                tablemodel.addRow(new Object[]{true, dlg.getComboBox().getSelectedItem(), model.toHTMLString(true, null), false});
            } else {
                editedConcept.modify(model);
                tablemodel.setValueAt(dlg.getPreviewLabel().getText(), candidateIndex, 2);
            }
        }
        return !exists;
    }

    private void updatePreviewLabel(EditCandidateDialog dlg) {
        if (type == ConceptType.GENERAL_CONCEPT)
            dlg.getPreviewLabel().setText("<html>" + 
                    String.format(SBVRExpressionModel.CGC_FORMAT, dlg.getTextArea().getText()) + "</html>"); 
        else if (type == ConceptType.VERB_CONCEPT)
            dlg.getPreviewLabel().setText("<html>" + underscoreToHTML(dlg.getTextArea().getText()) + "</html>"); 
        int height = dlg.getPreviewLabel().getHeight();
        if (height != previousHeight)
            previousHeight = height;
        dlg.setSize(dlg.getWidth(), 240 + previousHeight);
    }

    public JComboBox getComboBox() {
        return comboBox;
    }

    public JTextArea getTextArea() {
        return textArea;
    }
    
    public JLabel getPreviewLabel() {
        return previewLabel;
    }

    @SuppressWarnings("unchecked")
    public void setCandidateIndex(int candidateIndex) {
        this.candidateIndex = candidateIndex;
        editedConcept = tablemodel.getExpressionModelAt(candidateIndex);
        elements = tablemodel.getElementsAt(candidateIndex);
        entry = tablemodel.getValueAt(candidateIndex, 1).toString();
        comboBox.setModel(new DefaultComboBoxModel(new String[]{entry}));
        comboBox.setEnabled(false);
        textArea.setText(editedConcept.toUnderscoreString());
        previewLabel.setText(editedConcept.toHTMLString(true, null));
        updatePreviewLabel(this);
    }

    private String extractCandidateText(List<String> elemdata) {
        StringBuilder str = new StringBuilder();
        str.append(removeElementName(elemdata.get(0)));
        for (int i = 1; i < elemdata.size(); i++)
            str.append(" ").append(removeElementName(elemdata.get(i)).toLowerCase());
        return str.toString();
    }

    private String removeElementName(String str) {
        return str.startsWith("Extension Point") ? str.substring(16) : str.substring(str.indexOf(" "));
    }

    private boolean checkGCCandidates(String cand_concepts[], List<String> elements) {
        Set<String> gcCandidates = parentDlg.gcCandidates.getCandidatesListText();
        for (int i = 0; i < cand_concepts.length; i++)
            if (!gcCandidates.contains(cand_concepts[i].replaceAll("_", " ")))
                gcTempList.add(cand_concepts[i]);
        if (gcTempList.size() == 0)
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

    private boolean checkVCCandidates(String concepts[], String elements[]) {
        Set<String> vcCandidates = parentDlg.vcCandidates.getCandidatesListText();
        for (int i = 0; i < concepts.length; i++)
            if (!vcCandidates.contains(concepts[i].replaceAll("_", " ")))
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
        String result = new String();
        String split[] = text.trim().split(" ");
        for (int i = 0; i < split.length; i++) {
            // Apply pattern <general concept><verb concept><general concept><verb concept>....
            String tmp = split[i].trim().replace("_", " ");
            result += (i % 2 == 0 ? String.format(SBVRExpressionModel.CGC_FORMAT, tmp)
                    : String.format(SBVRExpressionModel.CVC_FORMAT, tmp)).trim() + " ";
        }
        return result.trim();
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
    private JLabel previewLabel;
    private JTextArea textArea;
    // End of variables declaration//GEN-END:variables
}
