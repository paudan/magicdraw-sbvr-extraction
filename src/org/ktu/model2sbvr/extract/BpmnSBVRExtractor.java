package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityFinalNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlFlow;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.CentralBufferNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.DecisionNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.JoinNode;
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
import java.util.Set;
import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conditional;
import org.ktu.model2sbvr.models.SourceEntry;

/**
 * @author Paulius Danenas, 2019
 */
public class BpmnSBVRExtractor extends AbstractSBVRExtractor {

    private Project project;
    private Profile bpmnProfile;

    private class TaskTuple {
        ActivityNode taskNode;
        String taskText;
        Map<Element, String> taskSubjects;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        Map<ActivityNode, Map<ActivityEdge, String>> correctionsIncoming, correctionsOutgoing;
        Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;

        private TaskTuple(ActivityNode task) {
            this.taskNode = task;
            taskText = extractElementText(task);
            taskSubjects = getSubjectNames(task);
            incomingConditions = new HashMap<>();
            nullCountIncoming = new HashMap<>();
            for (ActivityEdge edge : task.getIncoming())
                addCondition(incomingConditions, nullCountIncoming, edge, edge.getSource());
            outgoingConditions = new HashMap<>();
            nullCountOutgoing = new HashMap<>();
            for (ActivityEdge edge : task.getOutgoing())
                addCondition(outgoingConditions, nullCountOutgoing, edge, edge.getTarget());
            // Resolve cases when multiple sequence flows are incoming/outgoing from the same node, with contradictions
            correctionsIncoming = createCorrections(cloneConditions(incomingConditions), nullCountIncoming);
            correctionsOutgoing = createCorrections(cloneConditions(outgoingConditions), nullCountOutgoing);
        }

        private Map<ActivityNode, Map<ActivityEdge, String>> cloneConditions(Map<ActivityNode, Map<ActivityEdge, String>> conditions) {
            Map<ActivityNode, Map<ActivityEdge, String>> cloned = new HashMap<>();
            for (Entry<ActivityNode, Map<ActivityEdge, String>> nodeEntry : conditions.entrySet()) {
                Map<ActivityEdge, String> copy = new HashMap<>();
                for (Entry<ActivityEdge, String> condEl : nodeEntry.getValue().entrySet())
                    copy.put(condEl.getKey(), condEl.getValue());
                cloned.put(nodeEntry.getKey(), copy);
            }
            return cloned;
        }

        private void addCondition(Map<ActivityNode, Map<ActivityEdge, String>> conditions,
                                  Map<ActivityNode, Integer> nullCounts, ActivityEdge edge, ActivityNode node) {
            String condition = getCondition(edge.getGuard());
            Map<ActivityEdge, String> condList = conditions.get(node);
            if (condList == null) {
                condList = new HashMap<>();
                conditions.put(node, condList);
            }
            condList.put(edge, condition);
            nullCounts.putIfAbsent(node, 0);
            if (condition == null)
                nullCounts.put(node, nullCounts.get(node) + 1);
        }

        private Map<ActivityNode, Map<ActivityEdge, String>> createCorrections(Map<ActivityNode, Map<ActivityEdge, String>> conditions,
                                                                               Map<ActivityNode, Integer> nullCounts) {
            for (Entry<ActivityNode, Integer> nodeEntry : nullCounts.entrySet()) {
                Integer nullStats = nodeEntry.getValue();
                Map<ActivityEdge, String> condList = conditions.get(nodeEntry.getKey());
                if (nullStats >= 1 && condList.size() > 1)
                    // We have contradictions, transition condition is undefined
                    condList.clear();
            }
            return conditions;
        }
    }

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
        Set<Class> gatewayClasses = new HashSet<>(Arrays.asList(ForkNode.class, JoinNode.class, DecisionNode.class));
        return gatewayClasses.contains(el.getClassType()) && BPMNHelper.getGatewayStereotype(el).getName().compareToIgnoreCase(stereotype) == 0;
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
            if ((el.getClassType().equals(ActivityPartition.class) && hasAnyStereotype(el, "Lane", "LaneSet")) || isDataObject(el) || isResourceElement(el))
                createGeneralConcept(el, extractElementText(el), true);
            else if ((el.getClassType().equals(InformationFlow.class) && hasAnyStereotype(el, "MessageFlow"))) {
                Collection<Classifier> conveyed = ((InformationFlow) el).getConveyed();
                for (Classifier classifier : conveyed)
                    if (classifier.getClassType().equals(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class) && hasAnyStereotype(classifier, "BPMNMessage"))
                        createGeneralConcept(classifier, extractElementText(classifier), true);
            } else if (isTaskElement(el) && !extractedAuto)
                createGeneralConcept(el, extractActionGC(el), false);
            else if (isSequenceFlow(el)) {
                String condition = getCondition(((ActivityEdge) el).getGuard());
                if (condition != null)
                    gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(condition)));
            } else if ((isStartEventElement(el) || isEndEventElement(el)) && extractElementText(el) != null)
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
                Map<Element, String> subjects = getSubjectNames(el);
                for (Entry<Element, String> subject: subjects.entrySet())
                    createVerbConceptFromAction(subject.getKey(), el);
            } else if (isStartEventElement(el) || isEndEventElement(el) && !strictOnly)
                createVerbConceptFromCondition(el, getProperName(el));
            else if (isSequenceFlow(el))
                createVerbConceptFromCondition(el, getCondition(((ActivityEdge) el).getGuard()));
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
            extractRuleT2(el);
            extractRuleT4(el);
        }
    }

    private Map<Element, String> getSubjectNames(Element element) {
        Map<Element, String> names = new HashMap<>();
        // Should be modelled as unary concepts
        if (isStartEventElement(element) || isEndEventElement(element))
            return names;
        Collection<ActivityPartition> parts = null;
        if (element instanceof ActivityNode && ((ActivityNode) element).hasInPartition())
            parts = ((ActivityNode) element).getInPartition();
        else if (element instanceof ActivityEdge && ((ActivityEdge) element).hasInPartition())
            parts = ((ActivityEdge) element).getInPartition();
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

    private SBVRExpressionModel addCondition(SBVRExpressionModel model, String condition) {
        condition = condition.replaceAll("\n", " ").replaceAll("_", " ").replaceAll("  ", " ").trim();
        SBVRExpressionModel binary2 = getVerbConcept(condition);
        return binary2 != null ? model.addIdentifiedExpression(binary2) : model.addUnidentifiedText(condition);
    }

    private void extractRuleT4(Element el) {
        if (isGatewayOfType(el, "ParallelGateway")) {
            Map<ActivityEdge, TaskTuple> outgoingElements = new HashMap<>();
            for (ActivityEdge edge : ((ControlNode) el).getOutgoing()) {
                TaskTuple taskTuple = new TaskTuple(edge.getTarget());
                outgoingElements.put(edge, taskTuple);
            }
            for (Entry<ActivityEdge, TaskTuple> outTaskNode: outgoingElements.entrySet()) {
                ActivityNode outgoingTask = outTaskNode.getValue().taskNode;
                if (!(isTaskElement(outgoingTask) || isEndEventElement(outgoingTask)))
                    continue;
                String taskText = outTaskNode.getValue().taskText;
                if (taskText == null)
                    continue;
                for (Entry<Element, String> entry: outTaskNode.getValue().taskSubjects.entrySet()) {
                    Element part = entry.getKey();
                    String subject = entry.getValue();
                    SBVRExpressionModel candidate = new SBVRExpressionModel()
                            .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION)
                            .addUnidentifiedText(",")
                            .addRuleConditional(Conditional.AFTER);
                    boolean added_first = true;
                    Set<ActivityNode> incomingTasks = outTaskNode.getValue().incomingConditions.keySet();
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
                    List<Element> srcObj = new ArrayList<>(Arrays.asList(part, outTaskNode.getKey(), el));
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
            ActivityNode before = ((ActivityEdge) el).getSource();
            ActivityNode after = ((ActivityEdge) el).getTarget();
            if (isTaskElement(before) && isTaskElement(after)) {
                Map<Element, String> subjects = getSubjectNames(el);
                for (Entry<Element, String> subject : subjects.entrySet()) {
                    Element part = subject.getKey();
                    SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
                    candidate = addTask(candidate, after, subject.getValue());
                    candidate = candidate.addRuleConditional(Conditional.AFTER);
                    candidate = addTask(candidate, before, subject.getValue());
                    SourceEntry source = new SourceEntry(Arrays.asList(part, after, part, before),
                            Arrays.asList(getProperName(part), getProperName(after), getProperName(part), getProperName(before)));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT2(Element el) {
        if (isGatewayOfType(el, "ExclusiveGateway")) {
            Map<String, TaskTuple> incomingTasks = new HashMap<>();
            for (ActivityEdge edge : ((ControlNode) el).getIncoming()) {
                TaskTuple taskTuple = new TaskTuple(edge.getSource());
                incomingTasks.put(taskTuple.taskText, taskTuple);
            }
            Map<String, TaskTuple> outgoingTasks = new HashMap<>();
            for (ActivityEdge edge : ((ControlNode) el).getOutgoing()) {
                TaskTuple taskTuple = new TaskTuple(edge.getTarget());
                outgoingTasks.put(taskTuple.taskText, taskTuple);
            }
            if (incomingTasks.isEmpty() && outgoingTasks.isEmpty())
                return;
            List<Object> objects = new ArrayList<>();
            List<String> representations = new ArrayList<>();
            SBVRExpressionModel candidate = new SBVRExpressionModel()
                    .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
            // No incoming or outgoing sequences flows - bad practice, but must be processed as well
            if (!incomingTasks.isEmpty()) {
                if (!outgoingTasks.isEmpty())
                    //TODO: check incoming conditions from tasks which are "incoming"
                    candidate = candidate.addUnidentifiedText(",").addRuleConditional(Conditional.AFTER);
                candidate = addTasksWithConditions(candidate, incomingTasks, el, objects, representations, false);
                if (!outgoingTasks.isEmpty())
                    candidate = candidate.addUnidentifiedText(",");
            }
            if (!outgoingTasks.isEmpty())
                candidate = addTasksWithConditions(candidate, outgoingTasks, el, objects, representations, true);
            SourceEntry source = new SourceEntry(objects, representations);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private SBVRExpressionModel addTasksWithConditions(SBVRExpressionModel candidate, Map<String, TaskTuple> tasksData, Element el,
                                                       List<Object> objects, List<String> representations, boolean incomingConditions) {
        List<Object> tasksDefault = new ArrayList<>();
        boolean added_first = true;
        boolean rules_added = false;
        for (Entry<String, TaskTuple> entryOut : tasksData.entrySet()) {
            Map<Element, String> subjectsOut = entryOut.getValue().taskSubjects;
            Map<ActivityNode, Map<ActivityEdge, String>> conditionsOut = incomingConditions ? entryOut.getValue().correctionsIncoming : entryOut.getValue().correctionsOutgoing;
            Map<ActivityNode, Integer> nullStats = incomingConditions ? entryOut.getValue().nullCountIncoming : entryOut.getValue().nullCountOutgoing;
            for (Entry<Element, String> subjectOut : subjectsOut.entrySet()) {
                // Add verb concept from rule and subject (lane, resource, etc.)
                objects.add(subjectOut.getKey());
                objects.add(entryOut.getValue().taskNode);
                representations.add(subjectOut.getValue());
                representations.add(entryOut.getValue().taskText);
                Map<ActivityEdge, String> conditionsTask = conditionsOut.get(el);
                if (conditionsTask != null && !conditionsTask.isEmpty() && nullStats.get(el) == 0) {
                    if (!added_first)
                        candidate = candidate.addUnidentifiedText(",").addOrExpression();
                    else
                        added_first = false;
                    candidate = addTask(candidate, entryOut.getValue().taskNode, subjectOut.getValue());
                    // Add verb concepts from conditions
                    boolean added_or_first = true;
                    candidate = candidate.addRuleConditional(Conditional.IF);
                    rules_added = true;
                    for (Entry<ActivityEdge, String> cond : conditionsTask.entrySet()) {
                        if (!added_or_first)
                            candidate = candidate.addOrExpression();
                        else
                            added_or_first = false;
                        String condition = cond.getValue();
                        if (condition != null) {
                            candidate = addCondition(candidate, cond.getValue());
                            objects.add(cond.getKey());
                            representations.add(cond.getValue());
                        }
                    }
                } else {
                    // Process default conditions
                    String outTaskText = extractElementText(entryOut.getValue().taskNode);
                    SBVRExpressionModel taskModel = getVerbConcept(subjectOut.getValue() + " " + outTaskText);
                    tasksDefault.add(taskModel != null ? taskModel : subjectOut.getValue() + " " + outTaskText);
                }
            }
        }
        if (!tasksDefault.isEmpty()) {
            if (rules_added)
                candidate = candidate.addUnidentifiedText(",").addRuleConditional(Conditional.OTHERWISE);
            added_first = true;
            for (Object model : tasksDefault) {
                if (!added_first)
                    candidate = candidate.addOrExpression();
                else
                    added_first = false;
                if (model instanceof SBVRExpressionModel)
                    candidate = candidate.addIdentifiedExpression((SBVRExpressionModel) model);
                else
                    candidate = candidate.addUnidentifiedText(model.toString());
            }
        }
        return candidate;
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
