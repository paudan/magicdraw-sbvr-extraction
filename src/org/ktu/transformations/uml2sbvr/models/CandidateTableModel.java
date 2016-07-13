package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class CandidateTableModel extends DefaultTableModel {

    private final Class[] types = new Class[]{
        Boolean.class, String.class, String.class, Boolean.class, Boolean.class
    };
    private final boolean[] canEdit = new boolean[]{
        true, false, false, false, true
    };

    private final List<List<String>> elements;
    private final List<SBVRExpressionModel> expressions;
    private final List<Boolean> selected, traces;
    private final FilteredCandidateConceptModel data;
    private final Vector<String> cols;
    private final Vector<Object> cgc;

    public CandidateTableModel(FilteredCandidateConceptModel dataset, String[] messages) {
        super();
        this.data = dataset;
        cols = new Vector<>(Arrays.asList(messages).subList(0, 4));
        cols.add(0, "");
        cgc = new Vector<>();
        elements = new ArrayList<>();
        expressions = new ArrayList<>();
        selected = new ArrayList<>();
        traces = new ArrayList<>();
        setDefaultView();
    }

    public void setDefaultView() {
        clearView();
        for (List<String> concepts : data.getDataset().keySet())
            for (SBVRExpressionModel obj : data.getDataset().get(concepts))
                if (data.isSelected(concepts, obj))
                    addCandidate(concepts, obj, data.isCreateTrace(concepts, obj));
        this.setDataVector(cgc, cols);
    }

    public void setBusinessVocabularyView() {
        setVocabularyView(false);
    }

    public void setModelVocabularyView() {
        setVocabularyView(true);
    }

    private void setVocabularyView(boolean modelVoc) {
        clearView();
        for (List<String> concepts : data.getDataset().keySet())
            for (SBVRExpressionModel obj : data.getDataset().get(concepts))
                if (data.isSelected(concepts, obj) && obj.isModelVocabularyConcept() == modelVoc)
                    addCandidate(concepts, obj, data.isCreateTrace(concepts, obj));
        setDataVector(cgc, cols);
    }

    private void addCandidate(List<String> concepts, SBVRExpressionModel obj, Boolean trace) {
        Vector<Object> element = new Vector<>();
        element.add(true);
        element.add(AbstractCandidateConceptModel.getConceptsRepresentation(concepts));
        element.add(obj.toHTMLString(true, null));
        element.add(obj.isAuto());
        element.add(trace);
        cgc.add(element);
        addEntries(concepts, obj, trace);
    }
    
    private void addEntries(List<String> concepts, SBVRExpressionModel obj, Boolean trace) {
        elements.add(concepts);
        expressions.add(obj);
        selected.add(true);
        traces.add(trace);
    }

    @Override
    public void addRow(Object[] rowData) {
        if (rowData.length != 5)
            return;
        Object [] repr = new Object[5];
        repr[0] = rowData[0];
        if (rowData[1] instanceof List)
            repr[1] = AbstractCandidateConceptModel.getConceptsRepresentation((List<String>) rowData[1]);
        if (rowData[2] instanceof SBVRExpressionModel)
            repr[2] = ((SBVRExpressionModel)rowData[2]).toHTMLString(true, Boolean.TRUE);
        repr[3] = rowData[3];
        repr[4] = rowData[4];
        super.addRow(repr);
        if (!(rowData[1] instanceof List && rowData[2] instanceof SBVRExpressionModel
                && rowData[3] instanceof Boolean))
            return;
        addEntries((List<String>) rowData[1], (SBVRExpressionModel) rowData[2], (Boolean) rowData[4]);
    }

    private void clearView() {
        cgc.clear();
        elements.clear();
        expressions.clear();
        selected.clear();
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return types[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
    }

    public List<String> getElementsAt(int index) {
        if (index >= elements.size())
            return new ArrayList<>();
        return elements.get(index);
    }

    public boolean getSelectedAt(int index) {
        if (index >= selected.size())
            return false;
        return selected.get(index);
    }

    public SBVRExpressionModel getExpressionModelAt(int index) {
        if (index >= expressions.size())
            return null;
        return expressions.get(index);
    }

    @Override
    public void setValueAt(Object rowData, int row, int column) {
        Object repr = column == 2 ? ((SBVRExpressionModel) rowData).toHTMLString(true, Boolean.TRUE) : rowData;
        super.setValueAt(repr, row, column);
        switch (column) {
            case 1:
                elements.set(row, (List<String>) rowData);
                break;
            case 2:
                expressions.set(row, (SBVRExpressionModel) rowData);
                break;
            case 3:
                selected.set(row, (Boolean) rowData);
                break;
            case 4:
                data.setCreateTrace(getElementsAt(row), getExpressionModelAt(row),
                        Boolean.parseBoolean(getValueAt(row, column).toString()));
                break;
            default:
                break;
        }
    }

}
