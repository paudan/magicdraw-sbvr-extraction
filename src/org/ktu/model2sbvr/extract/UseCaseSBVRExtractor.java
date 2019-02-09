package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.components.mdbasiccomponents.Component;
import com.nomagic.uml2.ext.magicdraw.mdusecases.Actor;
import com.nomagic.uml2.ext.magicdraw.mdusecases.Extend;
import com.nomagic.uml2.ext.magicdraw.mdusecases.ExtensionPoint;
import com.nomagic.uml2.ext.magicdraw.mdusecases.Include;
import com.nomagic.uml2.ext.magicdraw.mdusecases.UseCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conditional;
import org.ktu.model2sbvr.models.SBVRModelException;
import org.ktu.model2sbvr.models.SourceEntry;

public class UseCaseSBVRExtractor extends AbstractSBVRExtractor {

    public UseCaseSBVRExtractor(DiagramPresentationElement diagram, boolean strictOnly, boolean extractMMVoc) {
        super(diagram, strictOnly, extractMMVoc);
    }

    public UseCaseSBVRExtractor(Package model, boolean strictOnly, boolean extractMMVoc) {
        super(model, strictOnly, extractMMVoc);
    }

    @Override
    protected void extractGeneralConceptCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if ((el.getClassType().equals(Actor.class) || isBoundaryElement(el)) && !extractedAuto)
                createGeneralConcept(el, extractElementText(el), true);
            else if (el.getClassType().equals(UseCase.class) && (!extractedAuto || (extractedAuto && extractedStrict))) {
                createGeneralConcept(el, extractActionGC(el), false);
            } else if (el.getClassType().equals(Extend.class)) {
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation())
                    if (extractElementText(ep) != null)
                        gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(ep), Collections.singletonList(getProperName(ep))));
            } else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
        }
    }

    @Override
    protected void extractVerbConceptCandidates() {
        if (strictOnly)
            return;
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if (el.getClassType().equals(Association.class) && !extractedAuto) {
                Collection<Type> endtypes = ((Association) el).getEndType();
                boolean actor_found = false, uc_found = false;
                Element actor = null, uc = null;
                for (Type elem : endtypes)
                    if (elem.getClassType().equals(Actor.class)) {
                        actor = elem;
                        actor_found = true;
                    } else if (elem.getClassType().equals(UseCase.class)) {
                        uc = elem;
                        uc_found = true;
                    }
                if (actor_found && uc_found)
                    createVerbConceptFromAction(actor, (UseCase) uc);
                // Association condition may be embedded in Association name
                if (extractElementText(el) != null)
                    vc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
            } else if (el.getClassType().equals(Generalization.class) && !extractedAuto) {
                Classifier general = ((Generalization) el).getGeneral();
                Classifier specific = ((Generalization) el).getSpecific();
                if (general == null || specific == null)
                    continue;
                if (general.getClassType().equals(Actor.class) && specific.getClassType().equals(Actor.class)) {
                    String gen_name = extractElementText(general);
                    String spec_name = extractElementText(specific);
                    if (gen_name != null && spec_name != null) {
                        SBVRExpressionModel first = getGeneralConcept(gen_name);
                        SBVRExpressionModel second = getGeneralConcept(spec_name);
                        if (first == null || second == null)
                            continue;
                        SBVRExpressionModel candidate = new SBVRExpressionModel();
                        candidate.addIdentifiedExpression(first).addVerbConcept("generalizes", true)
                                .addIdentifiedExpression(second);
                        candidate.setAuto(true);
                        List<String> concept = Arrays.asList(getProperName(general), getProperName(specific));
                        SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(general, specific, el)), concept);
                        vc_candidates.add(source, candidate);
                        vc_candidates.setAutomaticExtraction(source);
                    }
                }
            } else if (el.getClassType().equals(Comment.class) && extractElementText(el) != null)
                vc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
            else if (el instanceof UseCase && (!extractedAuto || (extractedAuto && extractedStrict))) {
                if (extractElementText(el) != null) {
                    // Extract associations from UseCase elements which are not directly associated with Actors
                    UseCase uc = (UseCase) el;
                    if (uc.has_associationOfEndType()) {
                        // Check for model associations which are represented IN THE DIAGRAM
                        boolean hasEnd = false;
                        for (Association assoc : uc.get_associationOfEndType())
                            if (candidateElements.contains(assoc)) {
                                hasEnd = true;
                                for (Type elem : assoc.getEndType())
                                    if (elem.getClassType().equals(Actor.class)) {
                                        createVerbConceptFromAction(elem, uc);
                                        break;
                                    }
                            }
                        if (!hasEnd)
                            createVerbConceptFromAction(uc.getOwner(), uc);
                    } else if (!uc.get_includeOfAddition().isEmpty() || !uc.get_extendOfExtendedCase().isEmpty()) {
                        Collection<Actor> actors = getActorsOfUseCase(uc, null);
                        if (!actors.isEmpty())
                            for (Actor actor : actors)
                                createVerbConceptFromAction(actor, uc);
                        else
                            createVerbConceptFromAction(uc.getOwner(), uc);
                    } else if (uc.get_includeOfAddition().isEmpty() && uc.get_extendOfExtendedCase().isEmpty())
                        // Use case without associations is executed by the system
                        createVerbConceptFromAction(uc.getOwner(), uc);
                    // Also add UseCases for possible manual extraction
                    vc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
                }
            } else if (el.getClassType().equals(Extend.class))
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation()) {
                    createVerbConceptFromCondition(ep);
                    vc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
                }
        }
    }

    @Override
    protected void extractBusinessRuleCandidates() {
        if (strictOnly)
            return;
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if (el.getClassType().equals(Include.class)
                    && (!extractedAuto || (extractedAuto && extractedStrict))) {
                UseCase including = ((Include) el).getIncludingCase();
                UseCase included = ((Include) el).getAddition();
                if (extractElementText(including) == null || extractElementText(included) == null)
                    continue;
                // Must exclude current Include in order not to traverse it
                Collection<Actor> aincluding = getActorsOfUseCase(including, (Include) el);
                Collection<Actor> aincluded = getActorsOfUseCase(included, (Include) el);
                if (aincluding.size() > 0 && aincluded.size() > 0)
                    for (Actor ai : aincluding)
                        for (Actor aai : aincluded)
                            createRuleFromInclude(ai, aai, including, included);
                else if (aincluding.isEmpty() && aincluded.size() > 0)
                    for (Actor aai : aincluded)
                        createRuleFromInclude(including.getOwner(), aai, including, included);
                else if (aincluded.isEmpty() && aincluding.size() > 0)
                    for (Actor ai : aincluding)
                        createRuleFromInclude(ai, ai, including, included);
                else if (aincluded.isEmpty() && aincluding.isEmpty())
                    // UseCases are executed by their owning boundaries
                    createRuleFromInclude(including.getOwner(), included.getOwner(), including, included);
            } else if (el.getClassType().equals(Extend.class)
                    && (!extractedAuto || (extractedAuto && extractedStrict))) {
                UseCase extended = ((Extend) el).getExtendedCase();
                UseCase extension = ((Extend) el).getExtension();
                if (extractElementText(extended) == null || extractElementText(extension) == null)
                    continue;
                Collection<Actor> aextended = getActorsOfUseCase(extended, (Extend) el);
                Collection<Actor> aextension = getActorsOfUseCase(extension, (Extend) el);
                if (aextended.size() > 0 && aextension.size() > 0)
                    for (Actor ai : aextended)
                        for (Actor aai : aextension)
                            createRuleFromExtend(ai, aai, extended, extension);
                else if (aextended.isEmpty() && aextension.size() > 0)
                    for (Actor aai : aextension)
                        createRuleFromExtend(extended.getOwner(), aai, extended, extension);
                else if (aextended.size() > 0 && aextension.isEmpty())
                    for (Actor ai : aextended)
                        createRuleFromExtend(ai, extension.getOwner(), extended, extension);
                else if (aextended.isEmpty() && aextension.isEmpty())
                    // UseCases are executed by their owning boundaries
                    createRuleFromExtend(extended.getOwner(), extension.getOwner(), extended, extension);
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation()) {
                    String epname = extractElementText(ep);
                    if (ep == null)
                        continue;
                    for (Actor aai : aextension) {
                        String aainame = extractElementText(aai);
                        String extnamegc = extractActionGC(extension);
                        String extnamevc = extractActionVC(extension);
                        if (aainame != null && extnamevc != null) {
                            SBVRExpressionModel candidate = new SBVRExpressionModel();
                            candidate.addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
                            if (extnamegc != null) {
                                SBVRExpressionModel binary = getVerbConcept(aainame + " " + extnamevc + " " + extnamegc);
                                if (binary != null)
                                    candidate.addIdentifiedExpression(binary);
                            } else {
                                SBVRExpressionModel unary = getVerbConcept(aainame + " " + extnamevc);
                                if (unary != null)
                                    candidate.addIdentifiedExpression(unary);
                            }
                            candidate.addRuleConditional(Conditional.IF);
                            // Try to map ExtensionPoint to already identified verb concepts
                            SBVRExpressionModel sbvr = getVerbConcept(epname);
                            if (sbvr != null)
                                candidate.addIdentifiedExpression(sbvr);
                            else
                                candidate.addUnidentifiedText(epname);
                            candidate.setAuto(false);
                            SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(ep, aai, extension)), 
                                    Arrays.asList(getProperName(ep), getProperName(aai), getProperName(extension)));
                            br_candidates.add(source, candidate);
                            br_candidates.setManualExtraction(source);
                        }
                    }
                }
            }
            if (el.getClassType().equals(Association.class)) {
                Collection<Type> endtypes = ((Association) el).getEndType();
                boolean actor_found = false, uc_found = false;
                Element actor = null, uc = null;
                for (Type elem : endtypes)
                    if (elem.getClassType().equals(Actor.class)) {
                        actor = elem;
                        actor_found = true;
                    } else if (elem.getClassType().equals(UseCase.class)) {
                        uc = elem;
                        uc_found = true;
                    }
                if (actor_found && uc_found && extractElementText(actor) != null
                        && extractElementText(uc) != null && extractElementText(el) != null) {
                    SourceEntry entry = new SourceEntry(Arrays.asList(actor, uc, el),
                            Arrays.asList(getProperName(actor), getProperName(uc), getProperName(el)));
                    br_candidates.setManualExtraction(entry);
                }
            } else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    br_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
        }
    }

    public static boolean isBoundaryElement(Element el) {
        return el.getClassType().equals(Model.class)
                || el.getClassType().equals(Component.class)
                || el.getClassType().equals(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package.class);
    }

    @Override
    protected void extractModelVocabulary() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if (el.getClassType().equals(Actor.class) || isBoundaryElement(el) || el.getClassType().equals(UseCase.class))
                addGeneralConceptToModelVoc(el);
            else if (el.getClassType().equals(Association.class)) {
                Collection<Type> endtypes = ((Association) el).getEndType();
                boolean actor_found = false, uc_found = false;
                Element actor = null, uc = null;
                for (Type elem : endtypes)
                    if (elem.getClassType().equals(Actor.class)) {
                        actor = elem;
                        actor_found = true;
                    } else if (elem.getClassType().equals(UseCase.class)) {
                        uc = elem;
                        uc_found = true;
                    }
                if (actor_found && uc_found)
                    addVerbConceptToModelVoc(actor, uc, "is_associated_with", "is_associated_with");
                for (Constraint cons : el.get_constraintOfConstrainedElement()) {
                    if (cons.getSpecification() == null || cons.getSpecification().getExpression() == null)
                        continue;
                    // TODO: constraint condition can be defined in several ways (by my QVT) (Paulius)
                    String condition = cons.getSpecification().getExpression().getSymbol();
                    SBVRExpressionModel candidate = new SBVRExpressionModel();
                    candidate.addGeneralConcept(String.format("association_condition '%s'", condition), false);
                    candidate.setModelVocabularyConcept(true);
                    SourceEntry source = new SourceEntry(Collections.singletonList(cons), Collections.singletonList(condition));
                    gc_candidates.add(source, candidate);
                    gc_candidates.setAutomaticExtraction(source);
                    candidate.setAuto(true);
                    candidate.setModelVocabularyConcept(true);
                }
            } else if (el.getClassType().equals(Generalization.class)) {
                Classifier general = ((Generalization) el).getGeneral();
                Classifier specific = ((Generalization) el).getSpecific();
                if (general == null || specific == null)
                    continue;
                if (general.getClassType().equals(Actor.class) && specific.getClassType().equals(Actor.class))
                    addVerbConceptToModelVoc(general, specific, "generalizes", "is_generalized_by");
            } else if (el.getClassType().equals(Include.class))
                addVerbConceptToModelVoc(((Include) el).getIncludingCase(), ((Include) el).getAddition(),
                        "includes", "is_included_by");
            else if (el.getClassType().equals(Extend.class)) {
                UseCase extended = ((Extend) el).getExtendedCase();
                UseCase extension = ((Extend) el).getExtension();
                String extendedName = addGeneralConceptToModelVoc(extended);
                String extensionName = addGeneralConceptToModelVoc(extension);
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation()) {
                    String epName = addGeneralConceptToModelVoc(ep);
                    Map<String, SBVRExpressionModel> map = gc_candidates.getListMap();
                    SBVRExpressionModel candidate = new SBVRExpressionModel();
                    candidate.addIdentifiedExpression(map.get(extendedName)).addVerbConcept("is_extended_by", false)
                            .addIdentifiedExpression(map.get(extensionName)).addVerbConcept("at", false)
                            .addIdentifiedExpression(map.get(epName));
                    candidate.setAuto(true);
                    candidate.setModelVocabularyConcept(true);
                    // Add opposite form of relationship as synonymous form
                    SBVRExpressionModel synonym = new SBVRExpressionModel();
                    synonym.addIdentifiedExpression(map.get(extensionName)).addVerbConcept("extends", false)
                            .addIdentifiedExpression(map.get(extendedName)).addVerbConcept("at", false)
                            .addIdentifiedExpression(map.get(epName));
                    synonym.setAuto(true);
                    synonym.setModelVocabularyConcept(true);
                    candidate.addSynonymousForm(synonym);
                    SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(extended, extension, ep)), 
                            Arrays.asList(extendedName, extensionName, epName));
                    vc_candidates.add(source, candidate);
                    vc_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private String addGeneralConceptToModelVoc(Element el) {
        String gc = extractElementText(el);
        if (gc == null)
            return null;
        Set<String> gclist = gc_candidates.getListMap().keySet();
        String name = String.format("%s", gc);
        if (gclist.contains(name))
            return name;
        SBVRExpressionModel candidate = createModelVocabularyConcept(name);
        String type = null;
        if (el.getClassType().equals(Actor.class))
            type = "uml_actor";
        else if (el.getClassType().equals(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package.class))
            type = "uml_package";
        else if (el.getClassType().equals(Component.class))
            type = "system_boundary";
        else if (el.getClassType().equals(UseCase.class))
            type = "uml_use_case";
        else if (el.getClassType().equals(ExtensionPoint.class))
            type = "uml_extension_point";
        if (type != null)
            try {
                candidate.setGeneralConcept(metamodelVocabulary.get(type));
        } catch (SBVRModelException ex) {
            Logger.getLogger(UseCaseSBVRExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        SourceEntry source = new SourceEntry(Collections.singletonList(el), Collections.singletonList(name));
        gc_candidates.add(source, candidate);
        gc_candidates.setAutomaticExtraction(source);
        return name;
    }

    private void addVerbConceptToModelVoc(Element el1, Element el2, String verb1, String verb2) {
        String e1name = addGeneralConceptToModelVoc(el1);
        String e2name = addGeneralConceptToModelVoc(el2);
        Map<String, SBVRExpressionModel> map = gc_candidates.getListMap();
        SBVRExpressionModel candidate = new SBVRExpressionModel();
        candidate.addIdentifiedExpression(map.get(e1name)).addVerbConcept(verb1, false)
                .addIdentifiedExpression(map.get(e2name));
        candidate.setAuto(true);
        candidate.setModelVocabularyConcept(true);
        SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(el1, el2)), 
                Arrays.asList(e1name, e2name));
        vc_candidates.add(source, candidate);
        vc_candidates.setAutomaticExtraction(source);
        // Add opposite form of relationship as synonymous form
        if (verb2 == null)
            return;
        SBVRExpressionModel synonym = new SBVRExpressionModel();
        synonym.addIdentifiedExpression(map.get(e2name)).addVerbConcept(verb2, false)
                .addIdentifiedExpression(map.get(e1name));
        candidate.addSynonymousForm(synonym);
    }

    @Override
    public void readElements(DiagramPresentationElement diagram) {
        super.readElements(diagram);
        candidateElements.addAll(getDiagramElements(candidateElements, diagram));
    }

    private void createRuleFromInclude(Element ai, Element aai, UseCase including, UseCase included) {
        String ainame = extractElementText(ai);
        String inclname = extractActionVC(including);
        String inclnamegc = extractActionGC(including);
        String aainame = extractElementText(aai);
        String inclname2 = extractActionVC(included);
        String inclnamegc2 = extractActionGC(included);
        SBVRExpressionModel binary1 = inclnamegc2 != null ? getVerbConcept(aainame + " " + inclname2 + " " + inclnamegc2) : null; 
        SBVRExpressionModel unary1 = inclnamegc2 != null ? getVerbConcept(aainame + " " + inclname2) : null;
        SBVRExpressionModel binary2 = inclnamegc != null ? getVerbConcept(ainame + " " + inclname + " " + inclnamegc) : null; 
        SBVRExpressionModel unary2 = inclnamegc != null ? getVerbConcept(ainame + " " + inclname) : null;    
        if (ainame != null && inclname != null && aainame != null && inclname2 != null
                && (binary1 != null || unary1 != null) && (binary2 != null || unary2 != null)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel();
            candidate.addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
            if (binary1 != null)
                candidate.addIdentifiedExpression(binary1);
            else if (unary1 != null)
                candidate.addIdentifiedExpression(unary1);
            candidate.addRuleConditional(Conditional.IF);
            if (binary2 != null)
                candidate.addIdentifiedExpression(binary2);
            else if (unary2 != null)
                candidate.addIdentifiedExpression(unary2);
            candidate.setAuto(true);
            SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(ai, including, aai, included)), 
                    Arrays.asList(getProperName(ai), getProperName(including), getProperName(aai), getProperName(included)));
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private void createRuleFromExtend(Element ai, Element aai, UseCase extended, UseCase extension) {
        String ainame = extractElementText(ai);
        String extname = extractActionVC(extended);
        String extnamegc = extractActionGC(extended);
        String aainame = extractElementText(aai);
        String extname2 = extractActionVC(extension);
        String extnamegc2 = extractActionGC(extension);
        SBVRExpressionModel binary1 = extnamegc2 != null ? getVerbConcept(aainame + " " + extname2 + " " + extnamegc2) : null; 
        SBVRExpressionModel unary1 = extnamegc2 != null ? getVerbConcept(aainame + " " + extname2) : null;
        SBVRExpressionModel binary2 = extnamegc != null ? getVerbConcept(ainame + " " + extname + " " + extnamegc) : null; 
        SBVRExpressionModel unary2 = extnamegc != null ? getVerbConcept(ainame + " " + extname) : null;
        if (ainame != null && extname != null && aainame != null && extname2 != null
                && (binary1 != null || unary1 != null) && (binary2 != null || unary2 != null)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel();
            candidate.addRuleExpression(SBVRExpressionModel.RuleType.PERMISSION);
            if (binary1 != null)
                candidate.addIdentifiedExpression(binary1);
            else if (unary1 != null)
                candidate.addIdentifiedExpression(unary1);
            candidate.addRuleConditional(Conditional.IF);
            if (binary2 != null)
                candidate.addIdentifiedExpression(binary2);
            else if (unary2 != null)
                candidate.addIdentifiedExpression(unary2);
            candidate.setAuto(true);
            SourceEntry source = new SourceEntry(new ArrayList<Object>(Arrays.asList(ai, extended, aai, extension)), 
                    Arrays.asList(getProperName(ai), getProperName(extended), getProperName(aai), getProperName(extension)));
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private Collection<Actor> getActorsOfUseCase(UseCase including, DirectedRelationship exclude) {
        Collection<Actor> actors = new HashSet<>();
        Collection<DirectedRelationship> visited = new HashSet<>();
        actors = addActors(actors, including);
        Collection<DirectedRelationship> includes = new HashSet<>();
        for (Include include : including.get_includeOfAddition())
            if (candidateElements.contains(include))
                includes.add(include);
        for (Extend extend : including.get_extendOfExtendedCase())
            if (candidateElements.contains(extend))
                includes.add(extend);
        if (exclude != null)
            includes.remove(exclude);
        while (!includes.isEmpty()) {
            Collection<DirectedRelationship> newIncludes = new HashSet<>();
            for (DirectedRelationship include : includes) {
                UseCase uc = null;
                if (include instanceof Include)
                    uc = ((Include) include).getIncludingCase();
                else if (include instanceof Extend)
                    uc = ((Extend) include).getExtendedCase();
                if (uc != null) {
                    for (Include incl : uc.get_includeOfAddition())
                        // Exclude previously visited relationships
                        if (candidateElements.contains(incl) && !visited.contains(incl))
                            newIncludes.add(incl);
                    for (Extend extend : uc.get_extendOfExtendedCase())
                        if (candidateElements.contains(extend) && !visited.contains(extend))
                            newIncludes.add(extend);
                    actors = addActors(actors, uc);
                    // The first actors which are found is set to be executing the given UseCase
                    if (!actors.isEmpty())
                        return actors;
                }
            }
            visited.addAll(includes);
            includes = newIncludes;
        }
        return actors;
    }

    private Collection<Actor> addActors(Collection<Actor> actors, UseCase including) {
        if (actors == null)
            actors = new HashSet<>();
        for (Association ai : including.get_associationOfEndType())
            for (Type elem : ai.getEndType())
                if (elem.getClassType().equals(Actor.class) && candidateElements.contains(elem))
                    actors.add((Actor) elem);
        return actors;
    }

    @Override
    public String removeMetaconceptName(String name) {
        return name.startsWith("Extension Point") ? name.substring(16) : name.substring(name.indexOf(" "));
    }

    @Override
    public String[] getMetamodelVocabularyNames() {
        return new String[] {"uml_actor", "uml_package", "system_boundary", "uml_use_case", "uml_extension_point"};
    }

}
