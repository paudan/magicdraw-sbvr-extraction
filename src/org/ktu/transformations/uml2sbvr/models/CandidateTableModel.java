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
    
    public static class CandidateEntry {
        
        private List<String> elements;
        private SBVRExpressionModel expression;
        private Boolean selected, trace;
        private String original;

        public CandidateEntry(List<String> elements, SBVRExpressionModel expression, 
                Boolean selected, Boolean trace, String original) {
            this.elements = elements;
            this.expression = expression;
            this.selected = selected;
            this.trace = trace;
            this.original = original;
        }

        public List<String> getElements() {
            return elements;
        }

        public SBVRExpressionModel getExpression() {
            return expression;
        }

        public Boolean getSelected() {
            return selected;
        }

        public Boolean getTrace() {
            return trace;
        }

        public String getOriginalString() {
            return original;
        }

    }
    
    protected final FilteredCandidateConceptModel data;
    protected List<CandidateEntry> entries;
    protected final Vector<String> cols;
    protected final Vector<Object> cgc;

    public CandidateTableModel(FilteredCandidateConceptModel dataset, String[] messages) {
        super();
        this.data = dataset;
        entries = new ArrayList<>();
        cols = new Vector<>(Arrays.asList(messages).subList(0, 4));
        cols.add(0, "");
        cgc = new Vector<>();
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
        entries.add(new CandidateEntry(concepts, obj, true, trace, obj.toString()));
    }
    
    public CandidateEntry getEntryAt(Integer index) {
        return entries.get(index);
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
        entries.clear();
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return types[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return canEdit[columnIndex];
    }

    @Override
    public void setValueAt(Object rowData, int row, int column) {
        Object repr = column == 2 ? ((SBVRExpressionModel) rowData).toHTMLString(true, Boolean.TRUE) : rowData;
        super.setValueAt(repr, row, column);
        CandidateEntry entry = getEntryAt(row);
        switch (column) {
            case 1:
                entry.elements = (List<String>) rowData;
                break;
            case 2:
                entry.expression = (SBVRExpressionModel) rowData;
                break;
            case 3:
                entry.selected = (Boolean) rowData;
                break;
            case 4:
                entry.trace = (Boolean) rowData;
                data.setCreateTrace(entry.elements, entry.expression,
                        Boolean.parseBoolean(getValueAt(row, column).toString()));
                break;
            default:
                break;
        }
    }

}
