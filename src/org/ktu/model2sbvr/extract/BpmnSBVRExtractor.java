package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdcompleteactions.AcceptEventAction;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityFinalNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlFlow;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ObjectFlow;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.CentralBufferNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.DecisionNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.JoinNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdstructuredactivities.StructuredActivityNode;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
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
import org.ktu.model2sbvr.models.SBVRExpressionModel.RuleType;
import org.ktu.model2sbvr.models.SourceEntry;

/**
 * @author Paulius Danenas, 2019
 */
public class BpmnSBVRExtractor extends AbstractSBVRExtractor {

    private Project project;
    private Profile bpmnProfile;

    private static String[] taskStereotypes = {"Task", "ServiceTask", "SendTask", "ReceiveTask", "UserTask", "ManualTask", "BusinessRuleTask", "ScriptTask"};
    private static String[] activityStereotypes = {"SubProcess", "Transaction", "AdHocSubProcess"};
    private static String[] boundaryStereotypes = {"MessageBoundaryEvent", "ErrorBoundaryEvent",  "TimerBoundaryEvent",
            "EscalationBoundaryEvent", "CancelBoundaryEvent", "CompensationBoundaryEvent", "ConditionalBoundaryEvent",
            "SignalBoundaryEvent", "MultipleBoundaryEvent", "ParallelMultipleBoundaryEvent"};

    private class TaskTuple {
        ActivityNode taskNode;
        String taskText;
        Map<Element, String> taskSubjects;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        Map<ActivityNode, Map<ActivityEdge, String>> correctionsIncoming, correctionsOutgoing;
        Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;
        int nullsTotalIncoming, nullsTotalOutgoing;

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
            String condition = getCondition(edge);
            nullCounts.putIfAbsent(node, 0);
            if (condition == null) {
                nullCounts.put(node, nullCounts.get(node) + 1);
                if (conditions == this.incomingConditions)
                    nullsTotalIncoming += 1;
                else
                    nullsTotalOutgoing += 1;
            }
            Map<ActivityEdge, String> condList = conditions.get(node);
            if (condList == null) {
                condList = new HashMap<>();
                conditions.put(node, condList);
            }
            condList.put(edge, condition);
        }

        private Map<ActivityNode, Map<ActivityEdge, String>> createCorrections(Map<ActivityNode, Map<ActivityEdge, String>> conditions,
                                                                               Map<ActivityNode, Integer> nullCounts) {
            for (Entry<ActivityNode, Integer> nodeEntry : nullCounts.entrySet()) {
                Integer nullStats = nodeEntry.getValue();
                Map<ActivityEdge, String> condList = conditions.get(nodeEntry.getKey());
                if (nullStats >= 1 && condList != null && condList.size() > 1)
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

    private Stereotype getActivityStereotype(Element el) {
        if (el == null)
            return null;
        List<String> stereotypes = new ArrayList<>(Arrays.asList(taskStereotypes));
        stereotypes.add("CallActivity");
        stereotypes.addAll(Arrays.asList(activityStereotypes));
        for (String st : stereotypes) {
            Stereotype stereotype = StereotypesHelper.getStereotype(project, st, bpmnProfile);
            if (StereotypesHelper.hasStereotype(el, stereotype))
                return stereotype;
        }
        return null;
    }

    private boolean isTaskElement(Element el) {
        if (el == null)
            return false;
        return hasAnyStereotype(el, taskStereotypes);
    }

    private boolean isActivityElement(Element el) {
        if (el == null)
            return false;
        return isTaskElement(el)
                || (el.getClassType().equals(CallBehaviorAction.class) && hasAnyStereotype(el, "CallActivity"))
                || (el.getClassType().equals(StructuredActivityNode.class) && hasAnyStereotype(el, activityStereotypes));
    }

    private boolean isStartEventElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(InitialNode.class)
                && hasAnyStereotype(el, "StartEvent", "MessageStartEvent", "TimerStartEvent", "ErrorStartEvent", "EscalationStartEvent",
                "CompensationStartEvent", "ConditionalStartEvent", "SignalStartEvent", "MultipleStartEvent", "ParallelMultipleStartEvent");
    }

    private boolean isBoundaryEvent(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(AcceptEventAction.class) && hasAnyStereotype(el, boundaryStereotypes);
    }

    private boolean isEndEventElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ActivityFinalNode.class)
                && hasAnyStereotype(el, "EndEvent", "MessageEndEvent", "ErrorEndEvent", "EscalationEndEvent",
                "CompensationEndEvent", "SignalEndEvent", "MultipleEndEvent", "TerminateEndEvent");
    }

    private boolean isEventElement(Element el) {
        return isStartEventElement(el) || isBoundaryEvent(el) || isEndEventElement(el);
    }

    private boolean isDataObject(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(CentralBufferNode.class) && hasAnyStereotype(el, "DataObject", "DataStore", "DataInput", "DataOutput");
    }

    private boolean isDataStore(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(CentralBufferNode.class) && hasAnyStereotype(el, "DataStore");
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

    private boolean isMessageFlow(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(InformationFlow.class) && hasAnyStereotype(el, "MessageFlow");
    }

    private boolean isDataAssociation(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ObjectFlow.class) && hasAnyStereotype(el, "DataAssociation");
    }

    private boolean isLaneElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ActivityPartition.class) && hasAnyStereotype(el, "Lane", "LaneSet");
    }

    protected Stereotype getStereotypeInList(Element el, String[] stereotypesList) {
        return getStereotypeInList(el, stereotypesList, project, bpmnProfile);
    }

    private String getCondition(ActivityEdge el) {
        String cond = getCondition(el.getGuard());
        if (cond != null)
            return cond;
        cond = el.getName().trim();
        return cond.length() > 0 ? cond : null;
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
                String condition = getCondition((ActivityEdge) el);
                if (condition != null)
                    gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(condition)));
            } else if ((isStartEventElement(el) || isEndEventElement(el)) && extractElementText(el) != null)
                gc_candidates.setManualExtraction(new SourceEntry(Collections.singletonList(el), Collections.singletonList(getProperName(el))));
            else if (isDataObject(el) || isDataStore(el))
                for (State state : ((CentralBufferNode) el).getInState()) {
                    String stateText = extractElementText(state);
                    String elText = extractElementText(el);
                    if (stateText != null && elText != null)
                        createGeneralConcept(el, stateText + " " + elText, true);
                }
            if (isMessageFlow(el)) {
                for (Classifier convObj : ((InformationFlow) el).getConveyed())
                    if (hasAnyStereotype(convObj, "BPMNMessage"))
                        createGeneralConcept(el, extractElementText(el), false);
            } else if (el.getClassType().equals(Comment.class))
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
                createVerbConceptFromCondition(el, getCondition((ActivityEdge) el));
            else if (isDataObject(el) || isDataStore(el))
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
            /*extractRuleT1(el);
            extractRuleT2(el);
            extractRuleT3(el);
            extractRuleT4(el);
            extractRuleT5(el);
            extractRuleT6(el);
            extractRuleT7(el);*/
            extractRuleT8(el);
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

    private SBVRExpressionModel addActivity(SBVRExpressionModel model, ActivityNode task, String subject) {
        String outTaskText = extractElementText(task);
        String verbText = subject + " " + outTaskText;
        if (isEventElement(task))
            verbText = outTaskText;
        SBVRExpressionModel binary2 = getVerbConcept(verbText);
        return binary2 != null ? model.addIdentifiedExpression(binary2) : model.addUnidentifiedText(verbText);
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
                        candidate = addActivity(candidate, outTask, subject);
                    }
                    candidate = candidate.addUnidentifiedText(",");
                    candidate = addActivity(candidate, outTaskNode.getValue().taskNode, subject);
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
                    candidate = addActivity(candidate, after, subject.getValue());
                    candidate = candidate.addRuleConditional(Conditional.AFTER);
                    candidate = addActivity(candidate, before, subject.getValue());
                    SourceEntry source = new SourceEntry(Arrays.asList(part, after, part, before),
                            Arrays.asList(getProperName(part), getProperName(after), getProperName(part), getProperName(before)));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT2(Element el) {
        extractRuleWithGateways(el, "ExclusiveGateway", "or");
    }

    private void extractRuleT3(Element el) {
        extractRuleWithGateways(el, "InclusiveGateway", "and");
    }

    private void extractRuleWithGateways(Element el, String gatewayStereotype, String conjunction) {
        if (isGatewayOfType(el, gatewayStereotype)) {
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
                candidate = addTasksWithConditions(candidate, incomingTasks, el, objects, representations, conjunction);
                if (!outgoingTasks.isEmpty())
                    candidate = candidate.addUnidentifiedText(",");
            }
            if (!outgoingTasks.isEmpty())
                candidate = addTasksWithConditions(candidate, outgoingTasks, el, objects, representations, conjunction);
            SourceEntry source = new SourceEntry(objects, representations);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private SBVRExpressionModel addTasksWithConditions(SBVRExpressionModel candidate, Map<String, TaskTuple> tasksData, Element el,
                                                       List<Object> objects, List<String> representations, String conjunction) {
        List<Object> tasksDefault = new ArrayList<>();
        boolean added_first = true;
        boolean rules_added = false;
        for (Entry<String, TaskTuple> entryOut : tasksData.entrySet()) {
            Map<Element, String> subjectsOut = entryOut.getValue().taskSubjects;
            Map<ActivityNode, Map<ActivityEdge, String>> conditionsOut = entryOut.getValue().correctionsIncoming;
            int nullTotal = entryOut.getValue().nullsTotalIncoming;
            for (Entry<Element, String> subjectOut : subjectsOut.entrySet()) {
                // Add verb concept from rule and subject (lane, resource, etc.)
                objects.add(subjectOut.getKey());
                objects.add(entryOut.getValue().taskNode);
                representations.add(subjectOut.getValue());
                representations.add(entryOut.getValue().taskText);
                if (!conditionsOut.values().isEmpty()) {
                    for (Map<ActivityEdge, String> conditionsTask: conditionsOut.values())
                        if (conditionsTask != null && !conditionsTask.isEmpty() && nullTotal == 0) {
                            if (!added_first) {
                                candidate = candidate.addUnidentifiedText(",");
                                if (conjunction.equalsIgnoreCase("or"))
                                    candidate = candidate.addOrExpression();
                                else if (conjunction.equalsIgnoreCase("and"))
                                    candidate = candidate.addAndExpression();
                            }
                            else
                                added_first = false;
                            candidate = addActivity(candidate, entryOut.getValue().taskNode, subjectOut.getValue());
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
                            // No conditions are present, process as default
                            String outTaskText = extractElementText(entryOut.getValue().taskNode);
                            SBVRExpressionModel taskModel = getVerbConcept(subjectOut.getValue() + " " + outTaskText);
                            tasksDefault.add(taskModel != null ? taskModel : subjectOut.getValue() + " " + outTaskText);
                        }
                } else {
                    // No conditions are present, process as default
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

    private void extractRuleT5(Element el) {
        if (isActivityElement(el)) {
            List<Element> boundaryElements = BPMNHelper.getBoundaryEventRefs(el, getActivityStereotype(el));
            if (boundaryElements.isEmpty())
                return;
            for (Element boundary: boundaryElements) {
                if (!isBoundaryEvent(boundary))
                    continue;
                boolean isInterrupting = false;
                Stereotype st = getStereotypeInList(boundary, boundaryStereotypes);
                if (st == null)
                    continue;
                List cancelActivity = StereotypesHelper.getStereotypePropertyValue(boundary, st, "cancelActivity");
                if (!cancelActivity.isEmpty()) {
                    Object valueObj = cancelActivity.get(0);
                    if (valueObj instanceof EnumerationLiteral) {
                        String value = ((EnumerationLiteral) valueObj).getName();
                        if (value != null)
                            isInterrupting = Boolean.parseBoolean(value);
                    }
                }
                Map<Element, String> activitySubjects = getSubjectNames(el);
                for (Entry<Element, String> subject: activitySubjects.entrySet()) {
                    SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PERMISSION);
                    candidate = addCondition(candidate, getProperName(boundary))
                            .addRuleConditional(Conditional.WHEN);
                    candidate = addActivity(candidate, (ActivityNode) el, subject.getValue());
                    SourceEntry source = new SourceEntry(Arrays.asList(subject.getKey(), el, boundary),
                            Arrays.asList(getProperName(subject.getKey()), getProperName(el), getProperName(boundary)));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);

                    for (ActivityEdge outNode: ((AcceptEventAction)boundary).getOutgoing()) {
                        ActivityNode outTask = outNode.getTarget();
                        if (isTaskElement(outTask)) {
                            candidate = new SBVRExpressionModel().addRuleExpression(RuleType.OBLIGATION);
                            candidate = addActivity(candidate, outTask, subject.getValue())
                                    .addRuleConditional(Conditional.AFTER)
                                    .addUnidentifiedText("(");
                            candidate = addCondition(candidate, getProperName(boundary))
                                    .addRuleConditional(Conditional.AFTER);
                            candidate = addActivity(candidate, (ActivityNode) el, subject.getValue())
                                    .addUnidentifiedText(")");
                            source = new SourceEntry(Arrays.asList(subject.getKey(), el, boundary, outTask),
                                    Arrays.asList(getProperName(subject.getKey()), getProperName(el), getProperName(boundary), getProperName(outTask)));
                            br_candidates.add(source, candidate);
                            br_candidates.setAutomaticExtraction(source);
                        }
                    }

                    if (!isInterrupting) {
                        candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PROHIBITION);
                        candidate = addActivity(candidate, (ActivityNode) el, subject.getValue())
                                .addRuleConditional(Conditional.AFTER);
                        candidate = addCondition(candidate, getProperName(boundary));
                        source = new SourceEntry(Arrays.asList(subject.getKey(), el, boundary),
                                Arrays.asList(getProperName(subject.getKey()), getProperName(el), getProperName(boundary)));
                        br_candidates.add(source, candidate);
                        br_candidates.setAutomaticExtraction(source);
                    }
                }
            }
        }
    }

    private void extractRuleT6(Element el) {
        if (isTaskElement(el))
            extractTasksWithData(el, false, "is produced", "is provided to");
    }

    private void extractRuleT7(Element el) {
        if (isActivityElement(el))
            extractTasksWithData(el, true, "is available to", "is provided with data");
    }

    private void extractTasksWithData(Element el, boolean checkDataStore, String reservedVerb1, String reservedVerb2) {
        // Outgoing tasks with data objects
        List<Element> dataObjects = new ArrayList<>();
        Map<Element, String> taskSubjects = getSubjectNames(el);
        for (ActivityEdge outAssoc: ((ActivityNode)el).getOutgoing())
            if (isDataAssociation(outAssoc)) {
                ActivityNode dataObj = outAssoc.getTarget();
                if (checkDataStore ? isDataStore(dataObj) : isDataObject(el))
                    dataObjects.add(dataObj);
            }
        for (Entry<Element, String> taskSubject : taskSubjects.entrySet()) {
            SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.OBLIGATION);
            boolean added_first_obj = true;
            for (Element dataObj: dataObjects) {
                Collection<State> states = ((CentralBufferNode) dataObj).getInState();
                String objText = extractElementText(dataObj);
                if (states.isEmpty()) {
                    if (!added_first_obj)
                        candidate = candidate.addAndExpression();
                    else
                        added_first_obj = false;
                    candidate = addGeneralConcept(candidate, dataObj);
                    candidate = candidate.addVerbConcept(reservedVerb1, true);
                } else {
                    boolean added_first_state = true;
                    for (State state: states) {
                        if (!added_first_state)
                            candidate = candidate.addAndExpression();
                        else
                            added_first_state = false;
                        String stateText = extractElementText(state);
                        SBVRExpressionModel objConcept = getGeneralConcept(stateText + " " + objText);
                        candidate = objConcept != null ? candidate.addIdentifiedExpression(objConcept) : candidate.addUnidentifiedText(objText);
                        candidate = candidate.addVerbConcept(reservedVerb1, true);
                    }
                }
            }
            candidate = candidate.addRuleConditional(Conditional.WHEN);
            candidate = addActivity(candidate, (ActivityNode) el, taskSubject.getValue());
            List<Object> srcElements = new ArrayList<>(dataObjects);
            srcElements.add(taskSubject.getKey());
            srcElements.add(el);
            List<String> names = new ArrayList<>();
            for (Element dataObject: dataObjects)
                names.add(getProperName(dataObject));
            names.add(getProperName(taskSubject.getKey()));
            names.add(getProperName(el));
            SourceEntry source = new SourceEntry(srcElements, names);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
        // Incoming tasks with data objects
        dataObjects.clear();
        for (ActivityEdge outAssoc: ((ActivityNode)el).getIncoming())
            if (isDataAssociation(outAssoc)) {
                ActivityNode dataObj = outAssoc.getSource();
                if (checkDataStore ? isDataStore(dataObj) : isDataObject(el))
                    dataObjects.add(dataObj);
            }
        for (Entry<Element, String> taskSubject : taskSubjects.entrySet()) {
            SBVRExpressionModel subjectConcept = getGeneralConcept(taskSubject.getValue());
            SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PERMISSION);
            candidate = addActivity(candidate, (ActivityNode) el, taskSubject.getValue())
                    .addRuleConditional(Conditional.ONLY_IF);
            boolean added_first_obj = true;
            for (Element dataObj: dataObjects) {
                Collection<State> states = ((CentralBufferNode) dataObj).getInState();
                String objText = getProperName(dataObj);
                if (states.isEmpty()) {
                    if (!added_first_obj)
                        candidate = candidate.addAndExpression();
                    else
                        added_first_obj = false;
                    SBVRExpressionModel objConcept = getGeneralConcept(objText);
                    candidate = objConcept != null ? candidate.addIdentifiedExpression(objConcept) : candidate.addUnidentifiedText(objText);
                    candidate = candidate.addVerbConcept(reservedVerb2, true);
                    candidate = subjectConcept != null ? candidate.addIdentifiedExpression(subjectConcept) : candidate.addUnidentifiedText(taskSubject.getValue());
                } else {
                    boolean added_first_state = true;
                    for (State state: states) {
                        if (!added_first_state)
                            candidate = candidate.addAndExpression();
                        else
                            added_first_state = false;
                        String stateText = getProperName(state);
                        SBVRExpressionModel objConcept = getGeneralConcept(stateText + " " + objText);
                        candidate = objConcept != null ? candidate.addIdentifiedExpression(objConcept) : candidate.addUnidentifiedText(objText);
                        candidate = candidate.addVerbConcept(reservedVerb2, true);
                        candidate = subjectConcept != null ? candidate.addIdentifiedExpression(subjectConcept) : candidate.addUnidentifiedText(taskSubject.getValue());
                    }
                }
            }
            List<Object> srcElements = new ArrayList<>();
            srcElements.add(el);
            srcElements.add(taskSubject.getKey());
            srcElements.addAll(dataObjects);
            List<String> names = new ArrayList<>();
            names.add(getProperName(taskSubject.getKey()));
            names.add(getProperName(el));
            for (Element dataObject: dataObjects)
                names.add(getProperName(dataObject));
            SourceEntry source = new SourceEntry(srcElements, names);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }


    private void extractRuleT8(Element el) {
        if (isMessageFlow(el)) {
            Collection<Classifier> conveyed = ((InformationFlow) el).getConveyed();
            if (conveyed.isEmpty())
                extractMessageFlow(el, null);
            else {
                for (Classifier convObj : conveyed)
                    if (hasAnyStereotype(convObj, "BPMNMessage"))
                        extractMessageFlow(el, convObj);
            }
        }
    }

    private void extractMessageFlow(Element el, Element convObj) {
        Collection<NamedElement> sources = ((InformationFlow) el).getInformationSource();
        Collection<NamedElement> targets = ((InformationFlow) el).getInformationTarget();
        NamedElement source = null, target = null;
        if (!sources.isEmpty() && !targets.isEmpty()) {
            source = sources.stream().findFirst().get();
            target = targets.stream().findFirst().get();
        }
        if (source == null)
            return;
        if (isLaneElement(source) && isLaneElement(target)) {
            addMessageFlowBetweenLanes(el, convObj, source, null, target, null, "sends", "to", RuleType.PERMISSION);
            addMessageFlowBetweenLanes(el, convObj, target, null, source, null, "receives", "from", RuleType.PERMISSION);
        } else if (isActivityElement(source) && isLaneElement(target)) {
            Map<Element, String> subjects = getSubjectNames(source);
            RuleType ruleType = RuleType.PERMISSION;
            if (hasAnyStereotype(source, "SendTask", "ReceiveTask"))
                ruleType = RuleType.OBLIGATION;
            for (Element subject: subjects.keySet()) {
                addMessageFlowBetweenLanes(el, convObj, subject, (ActivityNode) source, target, null, "sends", "to", ruleType);
                addMessageFlowBetweenLanes(el, convObj, target, null, subject, null, "receives", "from", RuleType.PERMISSION);
            }
        } else if (isLaneElement(source) && isActivityElement(target)) {
            Map<Element, String> subjects = getSubjectNames(target);
            for (Element subject: subjects.keySet())
                addMessageFlowBetweenLanes(el, convObj, source, null, subject, (ActivityNode)target, "sends", "to", RuleType.PERMISSION);
            addReceivingNodeEventRules(el, convObj, source, (ActivityNode) target);
        } else if (isActivityElement(source) && isActivityElement(target)) {
            RuleType ruleType = RuleType.PERMISSION;
            if (hasAnyStereotype(source, "SendTask", "ReceiveTask"))
                ruleType = RuleType.OBLIGATION;
            Map<Element, String> subjects = getSubjectNames(source);
            Map<Element, String> subjectsT = getSubjectNames(target);
            for (Element subject: subjects.keySet())
                for (Element subjectT: subjectsT.keySet())
                addMessageFlowBetweenLanes(el, convObj, subject, (ActivityNode)source, subjectT, (ActivityNode)target, "sends", "to", ruleType);
            addReceivingNodeEventRules(el, convObj, source, (ActivityNode) target);
        }
    }

    private void addMessageFlowBetweenLanes(Element el, Element convObj, Element subject1, ActivityNode task1,
                                            Element subject2, ActivityNode task2, String verb1, String verb2, RuleType ruleType) {
        SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(ruleType);
        String objText = extractElementText(subject1);
        if (task1 != null)
            candidate = addActivity(candidate, task1, objText).addRuleConditional(Conditional.WHEN);
        else
            candidate = addGeneralConcept(candidate, subject1);
        candidate = addConveyedObject(candidate, convObj, verb1, verb2);
        candidate = addGeneralConcept(candidate, subject2);
        objText = extractElementText(subject2);
        if (task2 != null) {
            candidate = candidate.addRuleConditional(Conditional.WHEN);
            candidate = addActivity(candidate, task2, objText);
        }
        List<Object> source = new ArrayList<>(Arrays.asList(subject1, el, subject2));
        if (task1 != null)
            source.add(task1);
        if (task2 != null)
            source.add(task2);
        List<String> names = new ArrayList<>(Arrays.asList(getProperName(subject1), getProperName(convObj), getProperName(subject2)));
        if (task1 != null)
            source.add(getProperName(task1));
        if (task2 != null)
            source.add(getProperName(task2));
        SourceEntry src = new SourceEntry(source, names);
        br_candidates.add(src, candidate);
        br_candidates.setAutomaticExtraction(src);
    }

    private SBVRExpressionModel addConveyedObject(SBVRExpressionModel candidate, Element convObj, String verb1, String verb2) {
        if (convObj != null) {
            candidate = candidate.addVerbConcept(verb1, true);
            candidate = addGeneralConcept(candidate, convObj);
            candidate = candidate.addVerbConcept(verb2, true);
        } else
            candidate = candidate.addVerbConcept(verb1 + " message " + verb2, true);
        return candidate;
    }

    private void addReceivingNodeEventRules(Element el, Element convObj, Element source, ActivityNode target) {
        Map<Element, String> subjects = getSubjectNames(target);
        RuleType ruleType = RuleType.PERMISSION;
        if (isTaskElement(target)) {
            if (hasAnyStereotype(target, "SendTask", "ReceiveTask"))
                ruleType = RuleType.OBLIGATION;
            for (Element subject : subjects.keySet())
                if (source instanceof ActivityNode) {
                    Map<Element, String> subjectsS = getSubjectNames(source);
                    for (Element subjectS: subjectsS.keySet())
                        addMessageFlowBetweenLanes(el, convObj, subject, null, subjectS, target, "receives", "from", ruleType);
                } else
                    addMessageFlowBetweenLanes(el, convObj, subject, null, source, target, "receives", "from", ruleType);
        } else if (isEventElement(target))
            for (Element subject : subjects.keySet()) {
                SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(ruleType);
                candidate = addActivity(candidate, target, null).addRuleConditional(Conditional.ONLY_WHEN);
                candidate = addGeneralConcept(candidate, subject);
                candidate = addConveyedObject(candidate, convObj, "receives", "from");
                candidate = addGeneralConcept(candidate, source);
                SourceEntry src = new SourceEntry(Arrays.asList(el, subject, source, target),
                        Arrays.asList(getProperName(el), getProperName(subject), getProperName(source), getProperName(target)));
                br_candidates.add(src, candidate);
                br_candidates.setAutomaticExtraction(src);
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
