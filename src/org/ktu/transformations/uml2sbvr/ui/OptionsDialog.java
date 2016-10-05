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
import javax.swing.JTabbedPane;
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
        btnTransform = new JButton();
        btnCancel = new JButton();
        tbpOptions = new JTabbedPane();
        jPanel4 = new JPanel();
        jPanel1 = new JPanel();
        chkVocabulary = new JCheckBox();
        chkMMVocabulary = new JCheckBox();
        jPanel2 = new JPanel();
        radioM2M = new JRadioButton();
        radioM2T = new JRadioButton();
        chkStrict = new JCheckBox();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        tabAdditional = new JPanel();
        chkUseNLP = new JCheckBox();
        jPanel6 = new JPanel();
        chkAllCaps = new JCheckBox();
        chkStartsCap = new JCheckBox();
        jLabel3 = new JLabel();
        chkWordnet = new JCheckBox();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(bundle.getString("OptionsDialog.title")); // NOI18N
        setResizable(false);
        setType(Type.UTILITY);

        btnTransform.setText(bundle.getString("OptionsDialog.btnTransform.text")); // NOI18N

        btnCancel.setText(bundle.getString("OptionsDialog.btnCancel.text")); // NOI18N
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        jPanel1.setBorder(BorderFactory.createEtchedBorder());

        chkVocabulary.setSelected(true);
        chkVocabulary.setText(bundle.getString("OptionsDialog.chkVocabulary.text")); // NOI18N
        chkVocabulary.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                chkVocabularyActionPerformed(evt);
            }
        });

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
                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(chkVocabulary)
                    .addComponent(chkMMVocabulary))
                .addGap(0, 120, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(chkVocabulary)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkMMVocabulary))
        );

        jPanel2.setBorder(BorderFactory.createEtchedBorder());

        buttonGroup1.add(radioM2M);
        radioM2M.setSelected(true);
        radioM2M.setText(bundle.getString("OptionsDialog.radioM2M.text")); // NOI18N

        buttonGroup1.add(radioM2T);
        radioM2T.setText(bundle.getString("OptionsDialog.radioM2T.text")); // NOI18N

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(radioM2M)
                .addGap(18, 18, 18)
                .addComponent(radioM2T, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 124, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(radioM2M)
                    .addComponent(radioM2T)))
        );

        chkStrict.setText(bundle.getString("OptionsDialog.chkStrict.text")); // NOI18N
        chkStrict.setToolTipText(bundle.getString("OptionsDialog.chkStrict.toolTipText")); // NOI18N

        jLabel1.setText(bundle.getString("OptionsDialog.jLabel1.text")); // NOI18N

        jLabel2.setText(bundle.getString("OptionsDialog.jLabel2.text")); // NOI18N

        GroupLayout jPanel4Layout = new GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(chkStrict))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jLabel1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(2, 2, 2)
                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkStrict)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tbpOptions.addTab(bundle.getString("OptionsDialog.jPanel4.TabConstraints.tabTitle"), jPanel4); // NOI18N

        chkUseNLP.setText(bundle.getString("OptionsDialog.chkUseNLP.text")); // NOI18N
        chkUseNLP.setToolTipText(bundle.getString("OptionsDialog.chkUseNLP.toolTipText")); // NOI18N

        jPanel6.setBorder(BorderFactory.createEtchedBorder());

        chkAllCaps.setText(bundle.getString("OptionsDialog.chkAllCaps.text")); // NOI18N

        chkStartsCap.setText(bundle.getString("OptionsDialog.chkStartsCap.text")); // NOI18N

        GroupLayout jPanel6Layout = new GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(chkStartsCap)
                    .addComponent(chkAllCaps))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(chkStartsCap)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkAllCaps)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel3.setText(bundle.getString("OptionsDialog.jLabel3.text")); // NOI18N
        jLabel3.setToolTipText(bundle.getString("OptionsDialog.jLabel3.toolTipText")); // NOI18N

        chkWordnet.setText(bundle.getString("OptionsDialog.chkWordnet.text")); // NOI18N

        GroupLayout tabAdditionalLayout = new GroupLayout(tabAdditional);
        tabAdditional.setLayout(tabAdditionalLayout);
        tabAdditionalLayout.setHorizontalGroup(tabAdditionalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(tabAdditionalLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(tabAdditionalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(tabAdditionalLayout.createSequentialGroup()
                        .addGroup(tabAdditionalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(chkWordnet)
                            .addComponent(chkUseNLP)
                            .addComponent(jLabel3))
                        .addGap(0, 36, Short.MAX_VALUE)))
                .addContainerGap())
        );
        tabAdditionalLayout.setVerticalGroup(tabAdditionalLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(tabAdditionalLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(chkUseNLP)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chkWordnet)
                .addGap(11, 11, 11)
                .addComponent(jLabel3)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        tbpOptions.addTab(bundle.getString("OptionsDialog.tabAdditional.TabConstraints.tabTitle"), tabAdditional); // NOI18N

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(tbpOptions)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnCancel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnTransform)
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tbpOptions, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
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
    private JCheckBox chkAllCaps;
    private JCheckBox chkMMVocabulary;
    private JCheckBox chkStartsCap;
    private JCheckBox chkStrict;
    private JCheckBox chkUseNLP;
    private JCheckBox chkVocabulary;
    private JCheckBox chkWordnet;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JPanel jPanel4;
    private JPanel jPanel6;
    private JRadioButton radioM2M;
    private JRadioButton radioM2T;
    private JPanel tabAdditional;
    public JTabbedPane tbpOptions;
    // End of variables declaration//GEN-END:variables
}
