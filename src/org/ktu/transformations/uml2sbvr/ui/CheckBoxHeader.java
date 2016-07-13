package org.ktu.transformations.uml2sbvr.ui;

import java.awt.Component;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * @author Paulius Danenas
 * adopted from http://www.coderanch.com/t/343795/GUI/java/Check-Box-JTable-header
 */
@SuppressWarnings("serial")
class CheckBoxHeader extends JCheckBox implements TableCellRenderer, MouseListener {

    //protected CheckBoxHeader rendererComponent;
    protected int column;
    protected boolean mousePressed = false;
    protected String headerString;

    public CheckBoxHeader(ItemListener itemListener) {
        addItemListener(itemListener);
        headerString = "Check All";
    }

    public CheckBoxHeader(ItemListener itemListener, String headerString) {
        this(itemListener);
        this.headerString = headerString;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setForeground(header.getForeground());
                setBackground(header.getBackground());
                setFont(header.getFont());
                header.addMouseListener(this);
            }
        }
        setColumn(column);
        setText(headerString);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return this;
    }

    protected void setColumn(int column) {
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    protected void handleClickEvent(MouseEvent e) {
        if (mousePressed) {
            mousePressed = false;
            JTableHeader header = (JTableHeader) (e.getSource());
            JTable tableView = header.getTable();
            TableColumnModel columnModel = tableView.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column_ = tableView.convertColumnIndexToModel(viewColumn);
            if (viewColumn == this.column && e.getClickCount() == 1 && column_ != -1)
                doClick();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        handleClickEvent(e);
        ((JTableHeader) e.getSource()).repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
