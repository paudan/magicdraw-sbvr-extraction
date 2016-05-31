package org.ktu.transformations.uml2sbvr.models;

import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class DiagramFilterModel extends DefaultTableModel {

    private Class[] types = new Class[]{
        java.lang.Boolean.class, java.lang.String.class
    };
    private boolean[] canEdit = new boolean[]{
        true, false
    };
    private List<DiagramPresentationElement> diagrams = new ArrayList<>();

    public DiagramFilterModel(Collection<DiagramPresentationElement> diagramList) {
        super();
        diagrams.addAll(diagramList);
        Vector<Object> elements = new Vector<>();
        for (int i = 0; i < diagrams.size(); i++) {
            Vector<Object> element = new Vector<>();
            element.add(true);
            element.add(diagrams.get(i).getDiagram().getName());
            elements.add(element);
        }
        this.setDataVector(elements, new Vector<String>(Arrays.asList("", "Name")));
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return types[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
    }
    
    public Collection<DiagramPresentationElement> getSelectedDiagrams() {
        Collection<DiagramPresentationElement> selected = new HashSet<>();
        for (int i = 0; i < this.getRowCount(); i++)
            if (Boolean.parseBoolean(this.getValueAt(i, 0).toString()))
                selected.add(diagrams.get(i));
        return selected;
    }

}
