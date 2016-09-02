package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCandidateConceptModel implements Cloneable {

    protected Map<List<String>, List<SBVRExpressionModel>> data;
    // Decision whether particular combinations of element rumblings can be used for manual extraction
    protected Map<List<String>, Boolean> flag;
    protected Map<SBVRExpressionModel, List<Object>> source;

    public AbstractCandidateConceptModel() {
        data = new HashMap<>();
        flag = new HashMap<>();
        source = new HashMap<>();
    }

    protected static AbstractCandidateConceptModel createInstance() {
        return null;
    }

    public boolean add(List<String> concepts, SBVRExpressionModel candidate, List<Object> sourceElements) {
        if (concepts == null || concepts.isEmpty())
            return false;
        List<SBVRExpressionModel> res = data.get(concepts);
        if (res == null) {
            res = new ArrayList<>();
            data.put(concepts, res);
        } else
            for (SBVRExpressionModel sbvr : res)
                if (sbvr.originalEqualsTo(candidate))
                    return false;
        res.add(candidate);
        source.put(candidate, sourceElements);
        return true;
    }

    public void remove(List<String> concepts, SBVRExpressionModel candidate) {
        if (concepts == null || concepts.isEmpty())
            return;
        List<SBVRExpressionModel> res = data.get(concepts);
        if (res == null)
            return;
        res.remove(candidate);
        if (res.isEmpty()) {
            data.remove(concepts);
            source.remove(concepts);
        }
    }

    public void removeAll() {
        for (List<String> concepts : data.keySet())
            data.get(concepts).clear();
        data.clear();
        flag.clear();
        source.clear();
    }

    public void set(List<String> concepts, int index, SBVRExpressionModel candidate) {
        if (concepts == null || concepts.isEmpty() || data.get(concepts) == null)
            return;
        data.get(concepts).set(index, candidate);
    }

    public SBVRExpressionModel get(String concepts[], int index) {
        if (concepts == null || concepts.length == 0 || data.get(Arrays.asList(concepts)) == null)
            return null;
        return data.get(Arrays.asList(concepts)).get(index);
    }

    public int indexOf(List<String> concepts, String candidate) {
        if (concepts == null || concepts.isEmpty())
            return -1;
        List<SBVRExpressionModel> res = data.get(concepts);
        if (res == null)
            return -1;
        for (int i = 0; i < res.size(); i++)
            if (candidate.compareTo(res.get(i).toString()) == 0)
                return i;
        return -1;
    }

    public int indexOf(List<String> concepts, SBVRExpressionModel model) {
        if (concepts == null || concepts.isEmpty())
            return -1;
        return data.get(concepts).indexOf(model);
    }

    public abstract Set<String> getCandidatesList();

    public abstract Set<String> getCandidatesListText();

    public abstract Map<String, SBVRExpressionModel> getListMap();

    public int size() {
        return data.size();
    }
    
    public boolean isEmpty() {
        return data.isEmpty();
    }

    public Map<List<String>, List<SBVRExpressionModel>> getDataset() {
        return data;
    }

    public Map<SBVRExpressionModel, List<Object>> getSourceData() {
        return source;
    }

    public boolean manualExtractionPossible(ArrayList<String> concepts) {
        return flag.get(concepts) != null ? flag.get(concepts) : false;
    }

    public Set<List<String>> manualExtractionCandidates() {
        Set<List<String>> list = new HashSet<>();
        for (List<String> concepts : flag.keySet())
            if (flag.get(concepts))
                list.add(concepts);
        return list;
    }

    public void setManualExtraction(List<String> concepts) {
        if (concepts == null || concepts.isEmpty())
            return;
        flag.put(concepts, Boolean.TRUE);
    }

    public void setAutomaticExtraction(List<String> concepts) {
        if (concepts == null || concepts.isEmpty())
            return;
        flag.put(concepts, Boolean.FALSE);
    }

    public void setAllIdentified(Boolean value) {
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept)) {
                    if (value.equals(Boolean.TRUE))
                        sbvr.setIdentified(value);
                    else if (value.equals(Boolean.FALSE) && sbvr.isAuto())
                        sbvr.setIdentified(value);
                }
    }

    protected AbstractCandidateConceptModel copyInstance(AbstractCandidateConceptModel copy) {
        copy.data = new HashMap<>();
        for (List<String> concept : data.keySet()) {
            List<SBVRExpressionModel> obj = new ArrayList<>();
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept))
                    obj.add(sbvr.clone());
            copy.data.put(concept, obj);  
        }
        for (SBVRExpressionModel sbvr: source.keySet()) {
            List<Object> src = new ArrayList<>();
            if (source.get(sbvr) != null)
                for (Object o : source.get(sbvr))
                    src.add(o);
            copy.source.put(sbvr, src);
        }
        copy.flag = new HashMap<>();
        for (List<String> concept : flag.keySet())
            copy.flag.put(concept, flag.get(concept));
        return copy;
    }

    public static String getConceptsRepresentation(List<String> elemdata) {
        StringBuilder str = new StringBuilder();
        str.append(elemdata.get(0));
        for (int i = 1; i < elemdata.size(); i++)
            str.append(", ").append(elemdata.get(i));
        return str.toString();
    }

}
