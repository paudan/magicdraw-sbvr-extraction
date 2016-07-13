package org.ktu.transformations.uml2sbvr.extract;

import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.util.Collection;
import java.util.HashSet;
import org.ktu.transformations.uml2sbvr.models.AbstractCandidateConceptModel;
import org.ktu.transformations.uml2sbvr.models.DefaultCandidateConceptModel;

public abstract class AbstractSBVRExtractor {

    protected Collection<DiagramPresentationElement> diagrams;
    protected AbstractCandidateConceptModel gc_candidates, vc_candidates, br_candidates;
    protected boolean strictOnly, extractMMVoc;
    protected boolean extractedStrict, extractedAuto;
    protected Collection<Element> candidateElements;

    protected AbstractSBVRExtractor(DiagramPresentationElement diagram, boolean strictOnly, boolean extractMMVoc) {
        this(diagram);
        this.strictOnly = strictOnly;
        this.extractMMVoc = extractMMVoc;
    }
    
    protected AbstractSBVRExtractor(DiagramPresentationElement diagram) {
        this.diagrams = new HashSet<>();
        diagrams.add(diagram);
        init();
        readElements(diagram);
    }
    
    protected AbstractSBVRExtractor(Collection<DiagramPresentationElement> diagrams) {
        init();
        setExtractedDiagrams(diagrams);
    }
    
    protected AbstractSBVRExtractor(Package model) {
        init();
        candidateElements = getPackageElements(model);
    }
    
    protected AbstractSBVRExtractor(Collection<DiagramPresentationElement> diagrams, boolean strictOnly, boolean extractMMVoc) {
        this(diagrams);
        this.strictOnly = strictOnly;
        this.extractMMVoc = extractMMVoc;
    }
    
    protected AbstractSBVRExtractor(Package model, boolean strictOnly, boolean extractMMVoc) {
        this(model);
        this.strictOnly = strictOnly;
        this.extractMMVoc = extractMMVoc;
    }
    
    private void init() {
        gc_candidates = null;
        vc_candidates = null;
        br_candidates = null;
        extractedStrict = false;
        extractedAuto = false;
    }

    protected abstract void extractGeneralConceptCandidates();

    protected void extractModelVocabularyCandidates() {
        if (extractedAuto)
            return;
        extractModelVocabulary();
    }

    protected abstract void extractVerbConceptCandidates();

    protected abstract void extractBusinessRuleCandidates();
    
    protected abstract void extractModelVocabulary();
    
    protected void readElements(DiagramPresentationElement diagram) {
        if (candidateElements == null)
            candidateElements = new HashSet<>();
        candidateElements = diagram.getUsedModelElements(true);
    }
    
    public static String getProperName(BaseElement el) {
        String name = el.getHumanName();
        if (name.trim().length() == 0)
            return null;
        return name.replaceAll("\n", " ").replaceAll("  ", " ").trim();
    }

    public void createGeneralConceptCandidates() {
        if (gc_candidates == null)
            gc_candidates = new DefaultCandidateConceptModel();
        else
            gc_candidates.removeAll();
        extractGeneralConceptCandidates();
    }

    public void createVerbConceptCandidates() {
        if (gc_candidates == null)
            createGeneralConceptCandidates();
        if (vc_candidates == null)
            vc_candidates = new DefaultCandidateConceptModel();
        else
            vc_candidates.removeAll();
        extractVerbConceptCandidates();
    }

    public void createBusinessRuleCandidates() {
        if (gc_candidates == null)
            extractGeneralConceptCandidates();
        if (vc_candidates == null)
            createVerbConceptCandidates();
        if (br_candidates == null)
            br_candidates = new DefaultCandidateConceptModel();
        else
            br_candidates.removeAll();
        extractBusinessRuleCandidates();
    }

    public void createModelVocabularyCandidates() {
        if (gc_candidates == null)
            gc_candidates = new DefaultCandidateConceptModel();
        if (vc_candidates == null)
            vc_candidates = new DefaultCandidateConceptModel();
        extractModelVocabularyCandidates();
    }

    public static String extractElementText(BaseElement el) {
        String name = getProperName(el);
        if (name == null || name.trim().length() == 0)
            return null;
        if (name.startsWith(el.getHumanType()))
            return name.substring(el.getHumanType().length()).trim();
        return name.trim();
    }

    public void removeAll() {
        gc_candidates.removeAll();
        vc_candidates.removeAll();
        br_candidates.removeAll();
    }

    public void extractAll() {
        createGeneralConceptCandidates();
        createVerbConceptCandidates();
        createBusinessRuleCandidates();
        if (extractMMVoc)
            createModelVocabularyCandidates();
    }

    public AbstractCandidateConceptModel getGCCandidateModel() {
        return gc_candidates;
    }

    public void setGCCandidateModel(AbstractCandidateConceptModel gc_candidates) {
        this.gc_candidates = gc_candidates;
    }

    public AbstractCandidateConceptModel getVCCandidateModel() {
        return vc_candidates;
    }

    public void setVCCandidateModel(AbstractCandidateConceptModel vc_candidates) {
        this.vc_candidates = vc_candidates;
    }

    public AbstractCandidateConceptModel getBRCandidateModel() {
        return br_candidates;
    }

    public void setBRCandidateModel(AbstractCandidateConceptModel br_candidates) {
        this.br_candidates = br_candidates;
    }

    public boolean isStrictOnly() {
        return strictOnly;
    }

    public void setStrictOnly(boolean strictOnly) {
        this.strictOnly = strictOnly;
    }

    public boolean isExtractModelVocabulary() {
        return extractMMVoc;
    }

    public void setExtractModelVocabulary(boolean extractMMVoc) {
        this.extractMMVoc = extractMMVoc;
    }

    public boolean isExtractedStrict() {
        return extractedStrict;
    }

    public void setExtractedStrict(boolean extractedStrict) {
        this.extractedStrict = extractedStrict;
    }

    public boolean isExtractedAuto() {
        return extractedAuto;
    }

    public void setExtractedAuto(boolean extractedAuto) {
        this.extractedAuto = extractedAuto;
    }

    public Collection<DiagramPresentationElement> getExtractedDiagrams() {
        return diagrams;
    }

    public void setExtractedDiagrams(Collection<DiagramPresentationElement> diagrams) {
        this.diagrams = diagrams;
        if (candidateElements == null)
            candidateElements = new HashSet<>();
        else
            candidateElements.clear();
        for (DiagramPresentationElement diagram: diagrams)
            readElements(diagram);
    }
    
    // TODO: Needed recursive support for reading deeper element trees 
    protected Collection<Element> getPackageElements(Package model) {
        Collection<Element> newelem = new HashSet<>();
        newelem.add(model);
        newelem.addAll(model.getOwnedElement());
        /*for (Element e : model.getOwnedElement())
            if (!(e instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package))
                newelem.addAll(e.getOwnedElement());*/
        Collection<Package> packages = new HashSet<>();
        packages.addAll(model.getNestedPackage());
        while (!packages.isEmpty()) {
            Collection<Package> newPack = new HashSet<>();
            for (Package pack : packages) {
                newelem.addAll(pack.getOwnedElement());
                for (Element e : pack.getOwnedElement())
                    if (!(e instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package))
                        newelem.addAll(e.getOwnedElement());
                newPack.addAll(pack.getNestedPackage());
            }
            packages = newPack;
        }
        return newelem;
    }

}
