package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SourceEntry implements Cloneable {

    private List<Object> source;
    private List<String> sourceText;

    public SourceEntry() {
        this.source = new ArrayList<>();
        this.sourceText = new ArrayList<>();
    }

    public SourceEntry(List<Object> source, List<String> sourceText) {
        this.source = source;
        this.sourceText = sourceText;
    }

    public SourceEntry(List<String> sourceText) {
        this.source = new ArrayList<>();
        for (String str : sourceText)
            this.source.add(null);
        this.sourceText = sourceText;
    }

    public void addEntry(Object source, String sourceText) {
        this.source.add(source);
        this.sourceText.add(sourceText);
    }

    public void addEntry(String sourceText) {
        addEntry(null, sourceText);
    }

    public List<Object> getSourceObjects() {
        return source;
    }

    public List<String> getSourceNames() {
        return sourceText;
    }
    
    @Override
    public SourceEntry clone() {
        SourceEntry copy = new SourceEntry();
        copy.source = new ArrayList<>();
        Collections.copy(source, copy.source);
        copy.sourceText = new ArrayList<>();
        Collections.copy(sourceText, copy.sourceText);
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(sourceText.get(0));
        for (int i = 1; i < sourceText.size(); i++)
            str.append(", ").append(sourceText.get(i));
        return str.toString();
    }

}
