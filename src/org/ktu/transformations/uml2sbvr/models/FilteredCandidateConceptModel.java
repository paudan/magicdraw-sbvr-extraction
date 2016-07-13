package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilteredCandidateConceptModel extends AbstractCandidateConceptModel  {

    private Map<List<String>, List<Boolean>> selected, traceOpt;

    public FilteredCandidateConceptModel() {
        super();
        selected = new HashMap<>();
        traceOpt = new HashMap<>();
    }

    public void setSelectedState(List<String> concepts, SBVRExpressionModel model, Boolean state) {
        selected.get(concepts).set(data.get(concepts).indexOf(model), state);
    }

    public Boolean isSelected(List<String> concepts, SBVRExpressionModel model) {
        return selected.get(concepts).get(data.get(concepts).indexOf(model));
    }
    
    public void setCreateTrace(List<String> concepts, SBVRExpressionModel model, Boolean state) {
        traceOpt.get(concepts).set(data.get(concepts).indexOf(model), state);
    }

    public Boolean isCreateTrace(List<String> concepts, SBVRExpressionModel model) {
        if (traceOpt.get(concepts) == null)
            return null;
        return traceOpt.get(concepts).get(data.get(concepts).indexOf(model));
    }

    @Override
    public boolean add(List<String> concepts, SBVRExpressionModel candidate, List<Object> source) {
        boolean added = super.add(concepts, candidate, source);
        if (added) {
            List<Boolean> res = selected.get(concepts);
            if (res == null) {
                res = new ArrayList<>();
                selected.put(concepts, res);
            }
            res.add(Boolean.TRUE);
            List<Boolean> trace = traceOpt.get(concepts);
            if (trace == null) {
                trace = new ArrayList<>();
                traceOpt.put(concepts, trace);
            }
            trace.add(Boolean.FALSE);
        }
        return added;
    }

    @Override
    public void remove(List<String> concepts, SBVRExpressionModel candidate) {
        super.remove(concepts, candidate);
        List<Boolean> res = selected.get(concepts);
        if (res != null)
            res.remove(data.get(concepts).indexOf(candidate));
        super.remove(concepts, candidate);
    }

    @Override
    public void removeAll() {
        super.removeAll();
        for (List<String> concepts : selected.keySet())
            selected.get(concepts).clear();
        selected.clear();
    }

    @Override
    public Set<String> getCandidatesList() {
        Set<String> candidates = new HashSet<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (int i = 0; i < data.get(concept).size(); i++)
                    if (selected.get(concept).get(i))
                        candidates.add(data.get(concept).get(i).toHTMLString(true, null));
        return candidates;
    }

    @Override
    public Set<String> getCandidatesListText() {
        Set<String> candidates = new HashSet<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (int i = 0; i < data.get(concept).size(); i++)
                    if (selected.get(concept).get(i))
                        candidates.add(data.get(concept).get(i).toString());
        return candidates;
    }

    @Override
    public FilteredCandidateConceptModel clone() {
        FilteredCandidateConceptModel copy = new FilteredCandidateConceptModel();
        super.copyInstance(copy);
        copy.selected = new HashMap<>();
        for (List<String> concept : selected.keySet()) {
            List<Boolean> bool = new ArrayList<>();
            for (int i = 0; i < data.get(concept).size(); i++)
                bool.add(selected.get(concept).get(i));
            copy.selected.put(concept, bool);
        }
        return copy;
    }

    @Override
    public Set<String> getCandidatesListOrigText() {
        Set<String> candidates = new HashSet<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (int i = 0; i < data.get(concept).size(); i++)
                    if (selected.get(concept).get(i))
                        candidates.add(data.get(concept).get(i).toOriginalString());
        return candidates;
    }

    @Override
    public HashMap<String, SBVRExpressionModel> getListMap() {
        HashMap<String, SBVRExpressionModel> map = new HashMap<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (int i = 0; i < data.get(concept).size(); i++)
                    if (selected.get(concept).get(i))
                        map.put(data.get(concept).get(i).toOriginalString(), data.get(concept).get(i));
        return map;
    }

}
