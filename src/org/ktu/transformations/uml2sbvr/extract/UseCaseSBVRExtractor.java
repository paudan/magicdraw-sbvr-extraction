package org.ktu.transformations.uml2sbvr.extract;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel;

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
                createGeneralConcept(el, extractElementText(el));
            else if (el.getClassType().equals(UseCase.class) && (!extractedAuto || (extractedAuto && extractedStrict))) {
                createGeneralConcept(el, extractUseCaseGC(el));
                gc_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
            } else if (el.getClassType().equals(Extend.class)) {
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation())
                    if (extractElementText(ep) != null)
                        gc_candidates.setManualExtraction(Arrays.asList(getProperName(ep)));
            } else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    gc_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
        }
    }

    @Override
    protected void extractVerbConceptCandidates() {
        if (strictOnly)
            return;
        Map<String, SBVRExpressionModel> map = gc_candidates.getListMap();
        Set<String> gclist = map.keySet();
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
                    createVerbConceptFromUseCase(actor, (UseCase) uc);
                // Association condition may be embedded in Association name
                if (extractElementText(el) != null)
                    vc_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
            } else if (el.getClassType().equals(Generalization.class) && !extractedAuto) {
                Classifier general = ((Generalization) el).getGeneral();
                Classifier specific = ((Generalization) el).getSpecific();
                if (general.getClassType().equals(Actor.class) && specific.getClassType().equals(Actor.class)) {
                    String gen_name = extractElementText(general);
                    String spec_name = extractElementText(specific);
                    if (gen_name != null && spec_name != null && gclist.contains(gen_name) && gclist.contains(spec_name)) {
                        SBVRExpressionModel candidate = new SBVRExpressionModel();
                        candidate.addIdentifiedExpression(map.get(gen_name)).addVerbConcept("generalizes", true)
                                .addIdentifiedExpression(map.get(spec_name));
                        candidate.setAuto(true);
                        List<String> concept = Arrays.asList(getProperName(general), getProperName(specific));
                        vc_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(general, specific, el)));
                        vc_candidates.setAutomaticExtraction(concept);
                    }
                }
            } else if (el.getClassType().equals(Comment.class) && extractElementText(el) != null)
                vc_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
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
                                        createVerbConceptFromUseCase(elem, uc);
                                        break;
                                    }
                            }
                        if (!hasEnd)
                            createVerbConceptFromUseCase(uc.getOwner(), uc);
                    } else if (!uc.get_includeOfAddition().isEmpty() || !uc.get_extendOfExtendedCase().isEmpty()) {
                        Collection<Actor> actors = getActorsOfUseCase(uc, null);
                        if (!actors.isEmpty())
                            for (Actor actor : actors)
                                createVerbConceptFromUseCase(actor, uc);
                        else
                            createVerbConceptFromUseCase(uc.getOwner(), uc);
                    } else if (uc.get_includeOfAddition().isEmpty() && uc.get_extendOfExtendedCase().isEmpty())
                        // Use case without associations is executed by the system
                        createVerbConceptFromUseCase(uc.getOwner(), uc);
                    // Also add UseCases for possible manual extraction
                    vc_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
                }
            } else if (el.getClassType().equals(Extend.class))
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation()) {
                    String name = getProperName(ep);
                    String eptext = extractElementText(ep);
                    if (eptext == null)
                        continue;
                    List<String> idgcs = new ArrayList<>();
                    for (String gc : gclist)
                        if (eptext.contains(gc))
                            idgcs.add(gc);
                    if (idgcs.size() > 0 && (!extractedAuto || (extractedAuto && extractedStrict))) {
                        SBVRExpressionModel candidate = new SBVRExpressionModel();
                        if (idgcs.size() == 1) {
                            String gc = idgcs.get(0);
                            if (eptext.startsWith(gc))
                                candidate.addIdentifiedExpression(map.get(gc))
                                        .addUnidentifiedText(eptext.substring(gc.length()).trim());
                            else if (eptext.endsWith(gc))
                                candidate.addUnidentifiedText(eptext.substring(0, eptext.length() - gc.length()))
                                        .addIdentifiedExpression(map.get(gc));
                        } else if (idgcs.size() == 2) {
                            String gc1 = idgcs.get(0);
                            String gc2 = idgcs.get(1);
                            if (eptext.startsWith(gc1) && eptext.endsWith(gc2)
                                    && eptext.length() - gc2.length() > gc1.length() + 1)
                                candidate.addIdentifiedExpression(map.get(gc1))
                                        .addUnidentifiedText(eptext.substring(gc1.length(), eptext.length() - gc2.length()))
                                        .addIdentifiedExpression(map.get(gc2));
                            else if (eptext.startsWith(gc2) && eptext.endsWith(gc1)
                                    && eptext.length() - gc1.length() > gc2.length() + 1)
                                candidate.addIdentifiedExpression(map.get(gc2))
                                        .addUnidentifiedText(eptext.substring(gc2.length() + 1, eptext.length() - gc1.length()))
                                        .addIdentifiedExpression(map.get(gc1));
                        }
                        vc_candidates.add(Arrays.asList(name), candidate, Arrays.asList((Object)ep));
                    }
                    vc_candidates.setManualExtraction(Arrays.asList(getProperName(ep)));
                }
        }
    }

    @Override
    protected void extractBusinessRuleCandidates() {
        if (strictOnly)
            return;
        Map<String, SBVRExpressionModel> vcmap = vc_candidates.getListMap();
        Collection<String> vclist = vcmap.keySet();
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
                        createRuleFromInclude(extended.getOwner(), aai, extended, extension);
                else if (aextended.size() > 0 && aextension.isEmpty())
                    for (Actor ai : aextended)
                        createRuleFromExtend(ai, ai, extended, extension);
                else if (aextended.isEmpty() && aextension.isEmpty())
                    // UseCases are executed by their owning boundaries
                    createRuleFromExtend(extended.getOwner(), extension.getOwner(), extended, extension);
                for (ExtensionPoint ep : ((Extend) el).getExtensionLocation()) {
                    String epname = extractElementText(ep);
                    if (ep == null)
                        continue;
                    for (Actor aai : aextension) {
                        String aainame = extractElementText(aai);
                        String extnamegc = extractUseCaseGC(extension);
                        String extnamevc = extractUseCaseVC(extension);
                        if (aainame != null && extnamevc != null) {
                            SBVRExpressionModel candidate = new SBVRExpressionModel();
                            candidate.addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
                            if (extnamegc != null && vclist.contains(aainame + " " + extnamevc + " " + extnamegc))
                                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + extnamevc + " " + extnamegc));
                            else if (extnamegc == null && vclist.contains(aainame + " " + extnamevc))
                                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + extnamevc));
                            candidate.addIfExpression();
                            // Try to map ExtensionPoint to already identified verb concepts
                            boolean found = false;
                            for (String str : vclist)
                                if (epname.startsWith(str)) {
                                    found = true;
                                    candidate.addIdentifiedExpression(vcmap.get(str));
                                }
                            if (!found)
                                candidate.addUnidentifiedText(epname);
                            candidate.setAuto(false);
                            List<String> concept = Arrays.asList(getProperName(ep), getProperName(aai), getProperName(extension));
                            br_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(ep, aai, extension)));
                            br_candidates.setManualExtraction(concept);
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
                        && extractElementText(uc) != null && extractElementText(el) != null)
                    br_candidates.setManualExtraction(Arrays.asList(getProperName(actor), getProperName(uc), getProperName(el)));
            } else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    br_candidates.setManualExtraction(Arrays.asList(getProperName(el)));
        }
    }

    public static boolean isBoundaryElement(Element el) {
        return el.getClassType().equals(Model.class)
                || el.getClassType().equals(Component.class)
                || el.getClassType().equals(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package.class);
    }

    private String extractUseCaseVC(Element el) {
        String name = el.getHumanName();
        if (name == null || name.length() == 0)
            return null;
        String[] parts = name.split(" ");
        if (parts.length < 2)
            return null;
        return (parts[1].trim().length() > 0 ? parts[1].trim().toLowerCase() : null);
    }

    private String extractUseCaseGC(Element el) {
        String proper = getProperName(el);
        if (proper == null || proper.length() == 0)
            return null;
        String[] parts = proper.split(" ");
        if (parts.length < 3)
            return null;
        String name = "";
        for (int i = 2; i < parts.length; i++)
            name += parts[i] + " ";
        return name.trim();
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
                    // TODO: constraint condition can be defined in several ways (by my QVT) 
                    String condition = cons.getSpecification().getExpression().getSymbol();
                    SBVRExpressionModel candidate = new SBVRExpressionModel();
                    candidate.addGeneralConcept(String.format("association_condition '%s'", condition), false);
                    candidate.setModelVocabularyConcept(true);
                    List<String> concepts = Arrays.asList(condition);
                    gc_candidates.add(concepts, candidate, Arrays.asList((Object)cons));
                    gc_candidates.setAutomaticExtraction(concepts);
                    candidate.setAuto(true);
                    candidate.setModelVocabularyConcept(true);
                }
            } else if (el.getClassType().equals(Generalization.class)) {
                Classifier general = ((Generalization) el).getGeneral();
                Classifier specific = ((Generalization) el).getSpecific();
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
                    List<String> concept = Arrays.asList(extendedName, extensionName, epName);
                    // Add opposite form of relationship as synonymous form
                    SBVRExpressionModel synonym = new SBVRExpressionModel();
                    synonym.addIdentifiedExpression(map.get(extensionName)).addVerbConcept("extends", false)
                            .addIdentifiedExpression(map.get(extendedName)).addVerbConcept("at", false)
                            .addIdentifiedExpression(map.get(epName));
                    synonym.setAuto(true);
                    synonym.setModelVocabularyConcept(true);
                    candidate.addSynonymousForm(synonym);
                    vc_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(extended, extension, ep)));
                    vc_candidates.setAutomaticExtraction(concept);
                }
            }
        }
    }

    private String addGeneralConceptToModelVoc(Element el) {
        String gc = extractElementText(el);
        if (gc == null)
            return null;
        Set<String> gclist = gc_candidates.getListMap().keySet();
        SBVRExpressionModel candidate = new SBVRExpressionModel();
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
        String name = String.format("%s '%s'", type, gc);
        if (gclist.contains(name))
            return name;
        candidate.addGeneralConcept(name, false);
        candidate.setAuto(true);
        candidate.setModelVocabularyConcept(true);
        List<String> concepts = Arrays.asList(name);
        gc_candidates.add(concepts, candidate, Arrays.asList((Object)el));
        gc_candidates.setAutomaticExtraction(concepts);
        return name;
    }

    private void addVerbConceptToModelVoc(Element el1, Element el2, String verb1, String verb2) {
        String e1name = addGeneralConceptToModelVoc(el1);
        String e2name = addGeneralConceptToModelVoc(el2);
        Map<String, SBVRExpressionModel> map = gc_candidates.getListMap();
        SBVRExpressionModel candidate = new SBVRExpressionModel();
        candidate.addIdentifiedExpression(map.get(e1name)).addVerbConcept(verb1, false)
                .addIdentifiedExpression(map.get(e2name));
        List<String> concept = Arrays.asList(e1name, e2name);
        candidate.setAuto(true);
        candidate.setModelVocabularyConcept(true);
        vc_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(el1, el2)));
        vc_candidates.setAutomaticExtraction(concept);
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

    private Collection<Element> getDiagramElements(Collection<Element> colelem, DiagramPresentationElement diagram) {
        Collection<Element> newelem = new HashSet<>();
        Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> packages = new HashSet<>();
        for (Element element : colelem)
            if (element instanceof com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package
                    && diagram.findPresentationElement(element, null) != null)
                packages.add((com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package) element);
        while (!packages.isEmpty()) {
            Collection<com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package> newPack = new HashSet<>();
            for (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package pack : packages) {
                for (Element el : pack.getOwnedElement())
                    if (diagram.findPresentationElement(el, null) != null)
                        newelem.add(el);
                for (com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package innerPack : pack.getNestedPackage())
                    if (diagram.findPresentationElement(innerPack, null) != null)
                        newPack.add(innerPack);
            }
            packages = newPack;
        }
        return newelem;
    }

    private void createVerbConceptFromUseCase(Element actor, UseCase usecase) {
        Map<String, SBVRExpressionModel> map = gc_candidates.getListMap();
        Set<String> gclist = map.keySet();
        String uc = extractUseCaseGC(usecase);
        String bname = extractElementText(actor);
        String vname = extractUseCaseVC(usecase);
        if (bname != null && vname != null && gclist.contains(bname)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel();
            candidate.addIdentifiedExpression(map.get(bname)).addVerbConcept(vname, false);
            if (uc != null && gclist.contains(uc))
                candidate.addIdentifiedExpression(map.get(uc));
            candidate.setAuto(true);
            List<String> concept = Arrays.asList(getProperName(actor), getProperName(usecase));
            vc_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(actor, usecase)));
            vc_candidates.setAutomaticExtraction(concept);
        }
    }

    private void createRuleFromInclude(Element ai, Element aai, UseCase including, UseCase included) {
        Map<String, SBVRExpressionModel> vcmap = vc_candidates.getListMap();
        Collection<String> vclist = vcmap.keySet();
        String ainame = extractElementText(ai);
        String inclname = extractUseCaseVC(including);
        String inclnamegc = extractUseCaseGC(including);
        String aainame = extractElementText(aai);
        String inclname2 = extractUseCaseVC(included);
        String inclnamegc2 = extractUseCaseGC(included);
        boolean hasBinary1 = inclnamegc2 != null && vclist.contains(aainame + " " + inclname2 + " " + inclnamegc2);
        boolean hasUnary1 = inclnamegc2 == null && vclist.contains(aainame + " " + inclname2);
        boolean hasBinary2 = inclnamegc != null && vclist.contains(ainame + " " + inclname + " " + inclnamegc);
        boolean hasUnary2 = inclnamegc == null && vclist.contains(ainame + " " + inclnamegc);
        if (ainame != null && inclname != null && aainame != null && inclname2 != null
                && (hasBinary1 || hasUnary1) && (hasBinary2 || hasUnary2)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel();
            candidate.addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
            if (hasBinary1)
                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + inclname2 + " " + inclnamegc2));
            else if (hasUnary1)
                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + inclnamegc2));
            candidate.addIfExpression();
            if (hasBinary2)
                candidate.addIdentifiedExpression(vcmap.get(ainame + " " + inclname + " " + inclnamegc));
            else if (hasUnary2)
                candidate.addIdentifiedExpression(vcmap.get(ainame + " " + inclnamegc));
            candidate.setAuto(true);
            List<String> concept = Arrays.asList(getProperName(ai), getProperName(including),
                    getProperName(aai), getProperName(included));
            br_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(ai, including, aai, included)));
            br_candidates.setAutomaticExtraction(concept);
        }
    }

    private void createRuleFromExtend(Element ai, Element aai, UseCase extended, UseCase extension) {
        Map<String, SBVRExpressionModel> vcmap = vc_candidates.getListMap();
        Collection<String> vclist = vcmap.keySet();
        String ainame = extractElementText(ai);
        String extname = extractUseCaseVC(extended);
        String extnamegc = extractUseCaseGC(extended);
        String aainame = extractElementText(aai);
        String extname2 = extractUseCaseVC(extension);
        String extnamegc2 = extractUseCaseGC(extension);
        boolean hasBinary1 = extnamegc2 != null && vclist.contains(aainame + " " + extname2 + " " + extnamegc2);
        boolean hasUnary1 = extnamegc2 == null && vclist.contains(aainame + " " + extname2);
        boolean hasBinary2 = extnamegc != null && vclist.contains(ainame + " " + extname + " " + extnamegc);
        boolean hasUnary2 = extnamegc == null && vclist.contains(ainame + " " + extname);
        if (ainame != null && extname != null && aainame != null && extname2 != null
                && (hasBinary1 || hasUnary1) && (hasBinary2 || hasUnary2)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel();
            candidate.addRuleExpression(SBVRExpressionModel.RuleType.PERMISSION);
            if (hasBinary1)
                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + extname2 + " " + extnamegc2));
            else if (hasUnary1)
                candidate.addIdentifiedExpression(vcmap.get(aainame + " " + extname2));
            candidate.addIfExpression();
            if (hasBinary2)
                candidate.addIdentifiedExpression(vcmap.get(ainame + " " + extname + " " + extnamegc));
            else if (hasUnary2)
                candidate.addIdentifiedExpression(vcmap.get(ainame + " " + extname));
            candidate.setAuto(true);
            List<String> concept = Arrays.asList(getProperName(ai), getProperName(extended),
                    getProperName(aai), getProperName(extension));
            br_candidates.add(concept, candidate, new ArrayList<Object>(Arrays.asList(ai, extended, aai, extension)));
            br_candidates.setAutomaticExtraction(concept);
        }
    }

    private Collection<Actor> getActorsOfUseCase(UseCase including, DirectedRelationship exclude) {
        Collection<Actor> actors = new HashSet<>();
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
                        if (candidateElements.contains(incl))
                            newIncludes.add(incl);
                    for (Extend extend : uc.get_extendOfExtendedCase())
                        if (candidateElements.contains(extend))
                            newIncludes.add(extend);
                    actors = addActors(actors, uc);
                    // The first actors which are found is set to be executing the given UseCase
                    if (!actors.isEmpty())
                        return actors;
                }
            }
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

    private SBVRExpressionModel createGeneralConcept(Element el, String name) {
        if (name == null)
            return null;
        SBVRExpressionModel candidate = new SBVRExpressionModel();
        candidate.addGeneralConcept(name, false);
        candidate.setAuto(true);
        List<String> concepts = Arrays.asList(getProperName(el));
        gc_candidates.add(concepts, candidate, Arrays.asList((Object)el));
        gc_candidates.setAutomaticExtraction(concepts);
        return candidate;
    }
}
