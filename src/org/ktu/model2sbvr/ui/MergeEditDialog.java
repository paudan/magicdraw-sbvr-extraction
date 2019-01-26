package org.ktu.model2sbvr.ui;

import java.util.ResourceBundle;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public class MergeEditDialog extends JDialog {
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/model2sbvr/ui/Bundle");

    /** Creates new form MergeEditDialog */
    public MergeEditDialog(MergeListDialog parent, boolean modal) {
        super(parent, modal);
        initComponents();
        candidates.setTableHeader(null);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new JButton();
        jButton2 = new JButton();
        lblName = new JLabel();
        nameText = new JTextField();
        jScrollPane2 = new JScrollPane();
        candidates = new JTable();
        jScrollPane3 = new JScrollPane();
        previewLabel = new JEditorPane();
        btnCancel = new JButton();
        btnSave = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        jButton1.setText(bundle.getString("MergeEditDialog.jButton1.text")); // NOI18N

        jButton2.setText(bundle.getString("MergeEditDialog.jButton2.text")); // NOI18N

        lblName.setText(bundle.getString("MergeEditDialog.lblName.text")); // NOI18N

        nameText.setToolTipText(bundle.getString("MergeEditDialog.nameText.toolTipText")); // NOI18N

        candidates.setModel(new DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Candidate", "Candidate name"
            }
        ) {
            Class[] types = new Class [] {
                String.class, String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        candidates.setUpdateSelectionOnSort(false);
        jScrollPane2.setViewportView(candidates);

        jScrollPane3.setBorder(null);
        jScrollPane3.setForeground(UIManager.getDefaults().getColor("Label.background"));
        jScrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        previewLabel.setEditable(false);
        previewLabel.setBackground(UIManager.getDefaults().getColor("control"));
        previewLabel.setContentType("text/html"); // NOI18N
        previewLabel.setText(bundle.getString("MergeEditDialog.previewLabel.text")); // NOI18N
        jScrollPane3.setViewportView(previewLabel);

        btnCancel.setText(bundle.getString("MergeEditDialog.btnCancel.text")); // NOI18N

        btnSave.setText(bundle.getString("MergeEditDialog.btnSave.text")); // NOI18N

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 452, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblName)
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                            .addComponent(jScrollPane3, GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE)
                            .addComponent(nameText))))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSave)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCancel)
                .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(lblName)
                    .addComponent(nameText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel)
                    .addComponent(btnSave))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton btnCancel;
    private JButton btnSave;
    private JTable candidates;
    private JButton jButton1;
    private JButton jButton2;
    private JScrollPane jScrollPane2;
    private JScrollPane jScrollPane3;
    private JLabel lblName;
    private JTextField nameText;
    private JEditorPane previewLabel;
    // End of variables declaration//GEN-END:variables
}
