package org.ktu.transformations.uml2sbvr.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author Paulius Danenas
 */
@SuppressWarnings("serial")
public class OptionsDialog extends javax.swing.JDialog {
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/ui/Bundle");

    public OptionsDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new ButtonGroup();
        jPanel1 = new JPanel();
        chkVocabulary = new JCheckBox();
        jLabel1 = new JLabel();
        chkMMVocabulary = new JCheckBox();
        chkStrict = new JCheckBox();
        jPanel2 = new JPanel();
        radioM2M = new JRadioButton();
        radioM2T = new JRadioButton();
        jLabel2 = new JLabel();
        btnTransform = new JButton();
        btnCancel = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("OptionsDialog.title")); // NOI18N
        setResizable(false);
        setType(Type.UTILITY);

        jPanel1.setBorder(BorderFactory.createEtchedBorder());

        chkVocabulary.setSelected(true);
        chkVocabulary.setText(bundle.getString("OptionsDialog.chkVocabulary.text")); // NOI18N
        chkVocabulary.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                chkVocabularyActionPerformed(evt);
            }
        });

        jLabel1.setText(bundle.getString("OptionsDialog.jLabel1.text")); // NOI18N

        chkMMVocabulary.setText(bundle.getString("OptionsDialog.chkMMVocabulary.text")); // NOI18N
        chkMMVocabulary.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                chkMMVocabularyActionPerformed(evt);
            }
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(chkMMVocabulary)
                    .addComponent(chkVocabulary)
                    .addComponent(jLabel1))
                .addContainerGap(110, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkVocabulary)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkMMVocabulary))
        );

        chkStrict.setText(bundle.getString("OptionsDialog.chkStrict.text")); // NOI18N
        chkStrict.setToolTipText(bundle.getString("OptionsDialog.chkStrict.toolTipText")); // NOI18N

        jPanel2.setBorder(BorderFactory.createEtchedBorder());

        buttonGroup1.add(radioM2M);
        radioM2M.setSelected(true);
        radioM2M.setText(bundle.getString("OptionsDialog.radioM2M.text")); // NOI18N

        buttonGroup1.add(radioM2T);
        radioM2T.setText(bundle.getString("OptionsDialog.radioM2T.text")); // NOI18N

        jLabel2.setText(bundle.getString("OptionsDialog.jLabel2.text")); // NOI18N

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(radioM2M)
                        .addGap(18, 18, 18)
                        .addComponent(radioM2T, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 4, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(radioM2M)
                    .addComponent(radioM2T)))
        );

        btnTransform.setText(bundle.getString("OptionsDialog.btnTransform.text")); // NOI18N

        btnCancel.setText(bundle.getString("OptionsDialog.btnCancel.text")); // NOI18N
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
                    .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnCancel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnTransform))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jPanel2, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel1, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(chkStrict))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkStrict)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTransform)
                    .addComponent(btnCancel))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void chkMMVocabularyActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkMMVocabularyActionPerformed
        testStrict();
    }//GEN-LAST:event_chkMMVocabularyActionPerformed

    private void chkVocabularyActionPerformed(ActionEvent evt) {//GEN-FIRST:event_chkVocabularyActionPerformed
        testStrict();
    }//GEN-LAST:event_chkVocabularyActionPerformed

    public void testStrict() {
        if (chkMMVocabulary.isSelected() && !chkVocabulary.isSelected()) {
            chkStrict.setSelected(false);
            chkStrict.setEnabled(false);
        } else {
            chkStrict.setEnabled(true);
        }
    }
    
    public JButton getCancelButton() {
        return btnCancel;
    }

    public JButton getTransformButton() {
        return btnTransform;
    }
    
    public boolean isStrictSelected() {
        return chkStrict.isSelected();
    }
    
    public boolean isM2MSelected() {
        return radioM2M.isSelected();
    }
    
    public boolean isM2TSelected() {
        return radioM2T.isSelected();
    }
    
    public void setM2MEnabled(boolean value) {
        radioM2M.setEnabled(value);
    }
    
    public void setM2TEnabled(boolean value) {
        radioM2T.setEnabled(value);
    }
    
    public void setM2MSelected(boolean value) {
        radioM2M.setSelected(value);
    }
    
    public void setM2TSelected(boolean value) {
        radioM2T.setSelected(value);
    }

    public boolean isMMVocabularySelected() {
        return chkMMVocabulary.isSelected();
    }

    public boolean isVocabularySelected() {
        return chkVocabulary.isSelected();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnCancel;
    private JButton btnTransform;
    private ButtonGroup buttonGroup1;
    private JCheckBox chkMMVocabulary;
    private JCheckBox chkStrict;
    private JCheckBox chkVocabulary;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JRadioButton radioM2M;
    private JRadioButton radioM2T;
    // End of variables declaration//GEN-END:variables
}
