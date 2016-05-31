package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class CandidateTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 5855452082636610070L;

    private final Class[] types = new Class[]{
        Boolean.class, String.class, String.class, Boolean.class, Boolean.class
    };
    private final boolean[] canEdit = new boolean[]{
        true, false, false, false, false
    };

    private List<List<String>> elements;
    private List<SBVRExpressionModel> expressions;
    private List<Boolean> selected;
    private FilteredCandidateConceptModel data;
    private Vector<String> cols;
    private Vector<Object> cgc;

    public CandidateTableModel(FilteredCandidateConceptModel dataset, String[] messages) {
        super();
        this.data = dataset;
        cols = new Vector<>(Arrays.asList(messages).subList(0, 4));
        cols.add(0, ""); //$NON-NLS-1$
        cgc = new Vector<>();
        elements = new ArrayList<>();
        expressions = new ArrayList<>();
        selected = new ArrayList<>();
        setDefaultView();
    }

    public void setDefaultView() {
        clearView();
        for (List<String> concepts : data.getDataset().keySet())
            for (SBVRExpressionModel obj : data.getDataset().get(concepts))
                if (data.isSelected(concepts, obj)) 
                    addCandidate(concepts, obj);
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
                    addCandidate(concepts, obj);
        setDataVector(cgc, cols);
    }

    private void addCandidate(List<String> concepts, SBVRExpressionModel obj) {
        Vector<Object> element = new Vector<>();
        element.add(true);
        element.add(AbstractCandidateConceptModel.getConceptsRepresentation(concepts));
        element.add(obj.toHTMLString(true, null));
        element.add(obj.isAuto());
        element.add(null);
        cgc.add(element);
        elements.add(concepts);
        expressions.add(obj);
        selected.add(true);
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
        return elements.get(index);
    }

    public boolean getSelectedAt(int index) {
        return selected.get(index);
    }

    public SBVRExpressionModel getExpressionModelAt(int index) {
        return expressions.get(index);
    }

}
