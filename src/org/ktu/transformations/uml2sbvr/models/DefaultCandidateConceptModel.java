package org.ktu.transformations.uml2sbvr.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultCandidateConceptModel extends AbstractCandidateConceptModel {

    public DefaultCandidateConceptModel() {
        super();
    }

    @Override
    public Set<String> getCandidatesListText() {
        Set<String> candidates = new HashSet<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept))
                    candidates.add(sbvr.toString());
        return candidates;
    }

    @Override
    public Set<String> getCandidatesList() {
        Set<String> candidates = new HashSet<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept))
                    candidates.add(sbvr.toHTMLString(true, null));
        return candidates;
    }

    @Override
    public DefaultCandidateConceptModel clone() {
        DefaultCandidateConceptModel copy = new DefaultCandidateConceptModel();
        this.copyInstance(copy);
        return copy;
    }

    @Override
    public Set<String> getCandidatesListOrigText() {
        Set<String> candidates = new HashSet<String>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept))
                    candidates.add(sbvr.toOriginalString());
        return candidates;
    }

    @Override
    public Map<String, SBVRExpressionModel> getListMap() {
        Map<String, SBVRExpressionModel> map = new HashMap<>();
        for (List<String> concept : data.keySet())
            if (data.get(concept) != null)
                for (SBVRExpressionModel sbvr : data.get(concept))
                    map.put(sbvr.toOriginalString(), sbvr);
        return map;
    }
}
