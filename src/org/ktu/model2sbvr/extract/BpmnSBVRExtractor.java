package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityFinalNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlFlow;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.CentralBufferNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.statemachines.mdbehaviorstatemachines.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SourceEntry;

/**
 * @author Paulius Danenas, 2019
 */
public class BpmnSBVRExtractor extends AbstractSBVRExtractor {

    private Project project;
    private Profile bpmnProfile;

    public BpmnSBVRExtractor(DiagramPresentationElement diagram, boolean strictOnly, boolean extractMMVoc) {
        super(diagram, strictOnly, extractMMVoc);
        setProfile();
    }

    public BpmnSBVRExtractor(Package model, boolean strictOnly, boolean extractMMVoc) {
        super(model, strictOnly, extractMMVoc);
        setProfile();
    }

    private void setProfile() {
        project = Application.getInstance().getProject();
        bpmnProfile = PluginUtilities.getBPMNProfile(project);
    }

    private boolean hasAnyStereotype(Element el, String... stereotypes) {
        if (el == null)
            return false;
        for (String st : stereotypes)
            if (StereotypesHelper.hasStereotype(el, StereotypesHelper.getStereotype(project, st, bpmnProfile)))
                return true;
        return false;
    }

    private boolean isTaskElement(Element el) {
        if (el == null)
            return false;
        return hasAnyStereotype(el, "Task", "ServiceTask", "SendTask", "ReceiveTask", "UserTask", "ManualTask", "BusinessRuleTask", "ScriptTask");
    }

    private boolean isStartEventElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(InitialNode.class)
                && hasAnyStereotype(el, "StartEvent", "MessageStartEvent", "TimerStartEvent", "ErrorStartEvent", "EscalationStartEvent",
                        "CompensationStartEvent", "ConditionalStartEvent", "SignalStartEvent", "MultipleStartEvent", "ParallelMultipleStartEvent");
    }

    private boolean isEndEventElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ActivityFinalNode.class)
                && hasAnyStereotype(el, "EndEvent", "MessageEndEvent", "ErrorEndEvent", "EscalationEndEvent",
                        "CompensationEndEvent", "SignalEndEvent", "MultipleEndEvent", "TerminateEndEvent");
    }

    private boolean isDataObject(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(CentralBufferNode.class) && hasAnyStereotype(el, "DataObject", "DataStore", "DataInput", "DataOutput");
    }

    private boolean isResourceElement(Element el) {
        if (el == null)
            return false;
        return hasAnyStereotype(el, "Resource", "PartnerRole");
    }

    private boolean isGatewayOfType(Element el, String stereotype) {
        if (el == null)
            return false;
        return el.getClassType().equals(ForkNode.class) && BPMNHelper.getGatewayStereotype(el).getName().compareToIgnoreCase(stereotype) == 0;
    }

    private boolean isSequenceFlow(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ControlFlow.class) && hasAnyStereotype(el, "SequenceFlow");
    }

    @Override
    protected void extractGeneralConceptCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if ((el.getClassType().equals(ActivityPartition.class) && hasAnyStereotype(el, "Lane", "LaneSet"))
                    || isDataObject(el) || isResourceElement(el))
                createGeneralConcept(el, extractElementText(el), true);
            else if ((el.getClassType().equals(InformationFlow.class) && hasAnyStereotype(el, "MessageFlow"))) {
                Collection<Classifier> conveyed = ((InformationFlow) el).getConveyed();
                for (Classifier classifier : conveyed)
                    if (classifier.getClassType().equals(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class) && hasAnyStereotype(classifier, "BPMNMessage"))
                        createGeneralConcept(classifier, extractElementText(classifier), true);
            } else if (isTaskElement(el) && !extractedAuto)
                createGeneralConcept(el, extractActionGC(el), false);
            else if ((isStartEventElement(el) || isEndEventElement(el)) && extractElementText(el) != null)
                gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
            else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
        }
    }

    @Override
    protected void extractVerbConceptCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            if (isTaskElement(el) && el instanceof ActivityNode && !strictOnly) {
                if (((ActivityNode) el).hasInPartition())
                    for (ActivityPartition part : ((ActivityNode) el).getInPartition())
                        createVerbConceptFromAction(part, el);
            } else if (isStartEventElement(el) || isEndEventElement(el) && !strictOnly)
                createVerbConceptFromCondition(el);
            else if (isDataObject(el))
                for (State state : ((CentralBufferNode) el).getInState())
                    createCharacteristic(el, state);
            else if (el.getClassType().equals(Comment.class) && extractElementText(el) != null && !strictOnly)
                vc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
        }
    }

    @Override
    protected void extractBusinessRuleCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            extractRuleT1(el);
            extractRuleT4(el);
        }
    }

    private Map<Element, String> getPartitionNames(Element element) {
        Map<Element, String> names = new HashMap<>();
        Collection<ActivityPartition> parts = null;
        if (element instanceof ActivityNode && ((ActivityNode)element).hasInPartition())
            parts = ((ActivityNode)element).getInPartition();
        else if (element instanceof ActivityEdge && ((ActivityEdge)element).hasInPartition())
            parts = ((ActivityEdge)element).getInPartition();
        if (parts == null)
            return names;
        for (ActivityPartition part : parts) {
            String subject = extractElementText(part);
            if (subject == null)
                continue;
            names.put(part, subject);
        }
        return names;
    }

    private SBVRExpressionModel addTask(SBVRExpressionModel model, ActivityNode task, String subject) {
        String outTaskText = extractElementText(task);
        SBVRExpressionModel binary2 = getVerbConcept(subject + " " + outTaskText);
        return binary2 != null ? model.addIdentifiedExpression(binary2) : model.addUnidentifiedText(subject + " " + outTaskText);
    }

    private void extractRuleT4(Element el) {
        if (isGatewayOfType(el, "ParallelGateway")) {
            List<ActivityNode> incomingTasks = new ArrayList<>();
            for (ActivityEdge edge : ((ForkNode) el).getIncoming())
                incomingTasks.add(edge.getSource());
            for (ActivityEdge edge : ((ForkNode) el).getOutgoing()) {
                ActivityNode outgoingTask = edge.getTarget();
                if (!(isTaskElement(outgoingTask) || isStartEventElement(el)))
                    continue;
                String taskText = extractElementText(outgoingTask);
                if (taskText == null)
                    continue;
                Map<Element, String> subjects = getPartitionNames(outgoingTask);
                for (Entry<Element, String> entry: subjects.entrySet()) {
                    Element part = entry.getKey();
                    String subject = entry.getValue();
                    SBVRExpressionModel candidate = new SBVRExpressionModel()
                            .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION)
                            .addUnidentifiedText(",")
                            .addAfterExpression();
                    boolean added_first = true;
                    for (ActivityNode outTask : incomingTasks) {
                        if (!added_first)
                            candidate = candidate.addAndExpression();
                        else
                            added_first = false;
                        candidate = addTask(candidate, outTask, subject);
                    }
                    candidate = candidate.addUnidentifiedText(",");
                    String vc_expression = subject + " " + taskText;
                    if (isStartEventElement(el))
                        vc_expression = taskText;
                    SBVRExpressionModel binary1 = getVerbConcept(vc_expression);
                    candidate = binary1 != null ? candidate.addIdentifiedExpression(binary1) : candidate.addUnidentifiedText(vc_expression);
                    candidate.setAuto(true);
                    List<Element> srcObj = new ArrayList<>(Arrays.asList(part, edge, el));
                    for (ActivityNode outTask : incomingTasks) {
                        srcObj.add(part);
                        srcObj.add(outTask);
                    }
                    List<String> names = new ArrayList<>(Arrays.asList(getProperName(part), getProperName(outgoingTask), null));
                    for (ActivityNode incTask : incomingTasks)
                        names.addAll(Arrays.asList(getProperName(part), getProperName(incTask)));
                    SourceEntry source = new SourceEntry(new ArrayList<Object>(srcObj), names);
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT1(Element el) {
        if (isSequenceFlow(el)) {
            ActivityNode before = ((ActivityEdge)el).getSource();
            ActivityNode after = ((ActivityEdge)el).getTarget();
            if (isTaskElement(before) && isTaskElement(after)) {
                Map<Element, String> subjects = getPartitionNames(el);
                for (Entry<Element, String> subject: subjects.entrySet()) {
                    Element part = subject.getKey();
                    SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
                    candidate = addTask(candidate, after, subject.getValue());
                    candidate = candidate.addAfterExpression();
                    candidate = addTask(candidate, before, subject.getValue());
                    SourceEntry source = new SourceEntry(Arrays.asList(part, after, part, before),
                            Arrays.asList(getProperName(part), getProperName(after), getProperName(part), getProperName(before)));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    @Override
    protected void extractModelVocabulary() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] getMetamodelVocabularyNames() {
        return null;    // Not implemented yet!
    }

    @Override
    public String removeMetaconceptName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static Collection<DiagramPresentationElement> getBPMNDiagrams(Package model, Collection<DiagramPresentationElement> diagrams) {
        Project project = Application.getInstance().getProject();
        if (project == null || model == null)
            return diagrams;
        for (Diagram diag : model.getOwnedDiagram()) {
            DiagramPresentationElement pres = project.getDiagram(diag);
            if (pres != null && PluginUtilities.isBPMNDiagram(pres))
                diagrams.add(pres);
        }
        for (Activity el : BPMNHelper.getBPMNProcesses(model))
            for (Diagram diag : el.getOwnedDiagram()) {
                DiagramPresentationElement pres = project.getDiagram(diag);
                if (pres != null && PluginUtilities.isBPMNDiagram(pres))
                    diagrams.add(pres);
            }
        for (Package pkg : model.getNestedPackage())
            diagrams = getBPMNDiagrams(pkg, diagrams);
        return diagrams;
    }

    public static Collection<DiagramPresentationElement> getBPMNDiagrams(Package root) {
        Collection<DiagramPresentationElement> diagrams = new HashSet<>();
        return BpmnSBVRExtractor.getBPMNDiagrams(root, diagrams);
    }

}
