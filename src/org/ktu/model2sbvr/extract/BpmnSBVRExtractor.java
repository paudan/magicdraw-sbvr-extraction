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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conditional;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conjunction;
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

    Map<ControlNode, GatewayNeighborhood> gatewayNeighborhoods, gatewayNeighborhoods2;
    private Set<ActivityNode> boundaryActivities;


    class ActivityNeighborhood {
        ActivityNode taskNode;
        String taskText;
        Map<Element, String> taskSubjects;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        Map<ActivityNode, Map<ActivityEdge, String>> correctionsIncoming, correctionsOutgoing;
        private Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;
        int nullsTotalIncoming, nullsTotalOutgoing;

        private ActivityNeighborhood(ActivityNode task) {
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

        private String formatPadding(String str) {
            return StringUtils.removeEnd(str, "\n").replaceAll("\n", "\n\t");
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Task element:").append(taskNode.getHumanName()).append("\n")
                    .append("Extracted text: ").append(taskText).append("\n")
                    .append("Subjects (executing elements): ")
                    .append(String.join(",", taskSubjects.keySet().stream().map(Element::getHumanName).collect(Collectors.toList())));
            if (!incomingConditions.isEmpty()) {
                sb.append("\nConditions from incoming edges:\n");
                sb.append(formatPadding(getConditionsRepresentation(incomingConditions)));
            }
            if (!outgoingConditions.isEmpty()) {
                sb.append("\nConditions from outgoing edges:\n");
                sb.append(formatPadding(getConditionsRepresentation(outgoingConditions)));
            }
            if (!correctionsIncoming.isEmpty()) {
                sb.append("\nConditions from incoming edges after resolving default conditions:\n");
                sb.append(formatPadding(getConditionsRepresentation(correctionsIncoming)));
            }
            if (!correctionsOutgoing.isEmpty()) {
                sb.append("\nConditions from outgoing edges after resolving default conditions:\n");
                sb.append(formatPadding(getConditionsRepresentation(correctionsOutgoing)));
            }
            if (!nullCountIncoming.isEmpty()) {
                sb.append("\nNumber of incoming edges with null conditions:\n");
                for (Entry<ActivityNode, Integer> nullEntry: nullCountIncoming.entrySet())
                    sb.append("\t").append(nullEntry.getKey().getHumanName()).append(": ").append(nullEntry.getValue()).append("\n");
            }
            if (!nullCountOutgoing.isEmpty()) {
                sb.append("Number of outgoing edges with null conditions: ").append("\n");
                for (Entry<ActivityNode, Integer> nullEntry: nullCountOutgoing.entrySet())
                    sb.append(nullEntry.getKey().getHumanName()).append(": ").append(nullEntry.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    class GatewayNeighborhood {
        ControlNode gatewayNode;
        Map<ActivityNode, ActivityNeighborhood> incomingActivities, outgoingActivities;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        private Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;
        Map<ControlNode, GatewayNeighborhood> incomingGateways, outgoingGateways;
        int nullsTotalIncoming, nullsTotalOutgoing;
        SBVRExpressionModel partialRule;
        List<Object> partialRuleSource;
        List<String> partialRuleNames;

        public GatewayNeighborhood(ControlNode gatewayNode, boolean fillIncoming) {
            this.gatewayNode = gatewayNode;
            incomingActivities = new HashMap<>();
            outgoingActivities = new HashMap<>();
            incomingGateways = new HashMap<>();
            outgoingGateways = new HashMap<>();
            incomingConditions = new HashMap<>();
            outgoingConditions = new HashMap<>();
            nullCountIncoming = new HashMap<>();
            nullCountOutgoing = new HashMap<>();
            partialRule = new SBVRExpressionModel();
            partialRuleSource = new ArrayList<>();
            partialRuleNames = new ArrayList<>();
            for (ActivityEdge edge : gatewayNode.getIncoming()) {
                addIncomingCondition(edge, edge.getSource());
                if (isActivityElement(edge.getSource())) {
                    ActivityNeighborhood taskTuple = new ActivityNeighborhood(edge.getSource());
                    incomingActivities.put(taskTuple.taskNode, taskTuple);
                } else if (isGatewayElement(edge.getSource())) {
                    ControlNode node = (ControlNode) edge.getSource();
                    GatewayNeighborhood nnode = null;
                    if (fillIncoming)
                        nnode = new GatewayNeighborhood(node, fillIncoming);
                    incomingGateways.put(node, nnode);
                }
            }
            outgoingActivities = new HashMap<>();
            for (ActivityEdge edge : gatewayNode.getOutgoing()) {
                addOutgoingCondition(edge, edge.getTarget());
                if (isActivityElement(edge.getTarget())) {
                    ActivityNeighborhood taskTuple = new ActivityNeighborhood(edge.getTarget());
                    outgoingActivities.put(taskTuple.taskNode, taskTuple);
                } else if (isGatewayElement(edge.getTarget())) {
                    ControlNode node = (ControlNode) edge.getTarget();
                    GatewayNeighborhood nnode = null;
                    if (!fillIncoming)
                        nnode = new GatewayNeighborhood(node, fillIncoming);
                    outgoingGateways.put(node, nnode);
                }
            }
        }

        private void addIncomingCondition(ActivityEdge edge, ActivityNode node) {
            String condition = getCondition(edge);
            nullCountIncoming.putIfAbsent(node, 0);
            if (condition == null) {
                nullCountIncoming.put(node, nullCountIncoming.get(node) + 1);
                nullsTotalIncoming += 1;
            }
            Map<ActivityEdge, String> condList = incomingConditions.get(node);
            if (condList == null) {
                condList = new HashMap<>();
                incomingConditions.put(node, condList);
            }
            condList.put(edge, condition);
        }

        private void addOutgoingCondition(ActivityEdge edge, ActivityNode node) {
            String condition = getCondition(edge);
            nullCountOutgoing.putIfAbsent(node, 0);
            if (condition == null) {
                nullCountOutgoing.put(node, nullCountOutgoing.get(node) + 1);
                nullsTotalOutgoing += 1;
            }
            Map<ActivityEdge, String> condList = outgoingConditions.get(node);
            if (condList == null) {
                condList = new HashMap<>();
                outgoingConditions.put(node, condList);
            }
            condList.put(edge, condition);
        }

        private String formatPadding(String str) {
            return StringUtils.removeEnd(str, "\n").replaceAll("\n", "\n\t");
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Gateway element:").append(gatewayNode.getHumanName());
            if (!incomingActivities.isEmpty()) {
                sb.append("\nIncoming activity nodes:\n");
                for (ActivityNeighborhood taskNode: incomingActivities.values())
                    sb.append("\t").append(formatPadding(taskNode.toString())).append("\n\n");
            }
            if (!outgoingActivities.isEmpty()) {
                sb.append("\nOutgoing activity nodes:\n");
                for (ActivityNeighborhood taskNode: outgoingActivities.values())
                    sb.append("\t").append(formatPadding(taskNode.toString())).append("\n\n");
            }
            if (!incomingGateways.isEmpty()) {
                sb.append("\nIncoming gateway nodes:\n");
                for (GatewayNeighborhood gatewayNode: incomingGateways.values())
                    if (gatewayNode != null)
                        sb.append("\t").append(formatPadding(gatewayNode.toString())).append("\n\n");
            }
            if (!outgoingGateways.isEmpty()) {
                sb.append("\nOutgoing gateway nodes:\n");
                for (GatewayNeighborhood gatewayNode: outgoingGateways.values())
                    if (gatewayNode != null)
                        sb.append("\t").append(formatPadding(gatewayNode.toString())).append("\n\n");
            }
            if (!incomingConditions.isEmpty()) {
                sb.append("\nConditions from incoming edges:\n");
                sb.append(formatPadding(getConditionsRepresentation(incomingConditions)));
            }
            if (!outgoingConditions.isEmpty()) {
                sb.append("\nConditions from outgoing edges:\n");
                sb.append(formatPadding(getConditionsRepresentation(outgoingConditions)));
            }
            if (!nullCountIncoming.isEmpty()) {
                sb.append("\nNumber of incoming edges with null conditions:\n");
                for (Entry<ActivityNode, Integer> nullEntry: nullCountIncoming.entrySet())
                    sb.append("\t").append(nullEntry.getKey().getHumanName()).append(": ").append(nullEntry.getValue()).append("\n");
            }
            if (!nullCountOutgoing.isEmpty()) {
                sb.append("\nNumber of outgoing edges with null conditions:\n");
                for (Entry<ActivityNode, Integer> nullEntry: nullCountOutgoing.entrySet())
                    sb.append("\t").append(nullEntry.getKey().getHumanName()).append(": ").append(nullEntry.getValue()).append("\n");
            }
            if (!partialRule.isEmpty())
                sb.append("\nPartial rule:\n").append(partialRule);
            return sb.toString();
        }
    }


    public BpmnSBVRExtractor(DiagramPresentationElement diagram, boolean strictOnly, boolean extractMMVoc) {
        super(diagram, strictOnly, extractMMVoc);
        setProfile();
        extractGatewayNeighborhoods();
    }

    public BpmnSBVRExtractor(Package model, boolean strictOnly, boolean extractMMVoc) {
        super(model, strictOnly, extractMMVoc);
        setProfile();
        extractGatewayNeighborhoods();
    }

    private void setProfile() {
        project = Application.getInstance().getProject();
        bpmnProfile = PluginUtilities.getBPMNProfile(project);
    }

    boolean hasAnyStereotype(Element el, String... stereotypes) {
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
        return el.getClassType().equals(CentralBufferNode.class) && hasAnyStereotype(el, "DataObject", "DataInput", "DataOutput");
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

    public boolean isGatewayElement(Element el) {
        return isGatewayOfType(el, "ExclusiveGateway") || isGatewayOfType(el, "InclusiveGateway") || isGatewayOfType(el, "ParallelGateway");
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
            if (isLaneElement(el) || isResourceElement(el))
                createGeneralConcept(el, extractElementText(el), true);
            else if (isMessageFlow(el)) {
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
            else if (isDataObject(el) || isDataStore(el)) {
                Collection<State> states = ((CentralBufferNode) el).getInState();
                if (!states.isEmpty())
                    for (State state : states) {
                        String stateText = extractElementText(state);
                        String elText = extractElementText(el);
                        if (stateText != null && elText != null)
                            createGeneralConcept(el, stateText + " " + elText, true);
                    }
                else
                    createGeneralConcept(el, extractElementText(el), true);
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
        for (GatewayNeighborhood nnode: gatewayNeighborhoods2.values())
            createPartialRules(nnode);
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            extractRuleT1(el);
            extractRuleT2(el);
            extractRuleT3(el);
            extractRuleT4(el);
            extractRuleT5(el);
            extractRuleT6(el);
            extractRuleT7(el);
            extractRuleT8(el);
            extractComplexRule(el);
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
            String subject = extractElementText(part.getRepresents() != null ? part.getRepresents() : part);
            if (subject == null)
                continue;
            names.put(part, subject);
        }
        return names;
    }

    private SBVRExpressionModel addActivity(SBVRExpressionModel model, ActivityNode task, String subject) {
        if (task == null)
            return model;
        String outTaskText = extractElementText(task);
        String verbText = subject + " " + outTaskText;
        if (isEventElement(task))
            verbText = outTaskText;
        SBVRExpressionModel binary2 = getVerbConcept(verbText);
        return binary2 != null ? model.addIdentifiedExpression(binary2) : model.addUnidentifiedText(verbText);
    }

    private SBVRExpressionModel addCondition(SBVRExpressionModel model, String condition) {
        if (condition == null)
            return model;
        condition = condition.replaceAll("\n", " ").replaceAll("_", " ").replaceAll("  ", " ").trim();
        SBVRExpressionModel binary2 = getVerbConcept(condition);
        return binary2 != null ? model.addIdentifiedExpression(binary2) : model.addUnidentifiedText(condition);
    }

    private SBVRExpressionModel addMultipleConditions(SBVRExpressionModel model, Map<ActivityEdge, String> conditions,
                                                      List<Object> objects, List<String> names) {
        boolean added_or_first = true;
        for (Entry<ActivityEdge, String> cond : conditions.entrySet()) {
            if (!added_or_first)
                model = model.addOrExpression();
            else
                added_or_first = false;
            String condition = cond.getValue();
            if (condition != null) {
                model = addCondition(model, cond.getValue());
                objects.add(cond.getKey());
                names.add(cond.getValue());
            }
        }
        return model;
    }

    private Conjunction getGatewayConjunction(ControlNode gateway) {
        if (hasAnyStereotype(gateway, "ExclusiveGateway"))
            return Conjunction.OR;
        else if (hasAnyStereotype(gateway, "InclusiveGateway"))
            return Conjunction.AND;
        return null;
    }

    private void extractRuleT4(Element el) {
        if (isGatewayOfType(el, "ParallelGateway")) {
            Map<ActivityEdge, ActivityNeighborhood> outgoingElements = new HashMap<>();
            for (ActivityEdge edge : ((ControlNode) el).getOutgoing()) {
                ActivityNeighborhood taskTuple = new ActivityNeighborhood(edge.getTarget());
                outgoingElements.put(edge, taskTuple);
            }
            for (Entry<ActivityEdge, ActivityNeighborhood> outTaskNode: outgoingElements.entrySet()) {
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
                    String incomingCondition = getCondition((ActivityEdge) el);
                    if (incomingCondition != null) {
                        candidate = candidate.addRuleConditional(Conditional.IF);
                        candidate = addCondition(candidate, incomingCondition);
                    }
                    SourceEntry source = new SourceEntry(Arrays.asList(part, after, part, before),
                            Arrays.asList(getProperName(part), getProperName(after), getProperName(part), getProperName(before)));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT2(Element el) {
        extractRuleWithGateways(el, "ExclusiveGateway", Conjunction.OR);
    }

    private void extractRuleT3(Element el) {
        extractRuleWithGateways(el, "InclusiveGateway", Conjunction.AND);
    }

    Object[] getRuleWithGateways(ControlNode el, Conjunction conjunction, ActivityNode excluded) {
        GatewayNeighborhood tuple = gatewayNeighborhoods2.get(el);
        if (tuple.incomingActivities.isEmpty() && tuple.outgoingActivities.isEmpty())
            return null;
        List<Object> objects = new ArrayList<>();
        List<String> representations = new ArrayList<>();
        SBVRExpressionModel candidate = new SBVRExpressionModel();
        if (!tuple.outgoingActivities.isEmpty())
            candidate = addTasksWithConditions(candidate, tuple.outgoingActivities, objects, representations, conjunction, excluded, el);
        // No incoming or outgoing sequences flows - bad practice, but must be checked
        //TODO: check incoming conditions from activities which are "incoming"
        if (!tuple.incomingActivities.isEmpty()) {
            // Skip conditional if only excluded task is in outgoing activities
            Set<ActivityNode> actOut = tuple.outgoingActivities.keySet();
            if (actOut.size() > 1 || (actOut.size() == 1 && actOut.stream().findFirst().get() != excluded))
                candidate = candidate.addUnidentifiedText(",").addRuleConditional(Conditional.AFTER);
            candidate = addTasksWithConditions(candidate, tuple.incomingActivities, objects, representations, conjunction, excluded, el);
        }
        Object[] results = new Object[3];
        results[0] = candidate;
        results[1] = objects;
        results[2] = representations;
        return results;
    }

    private void extractRuleWithGateways(Element el, String gatewayStereotype, Conjunction conjunction) {
        if (isGatewayOfType(el, gatewayStereotype)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel()
                    .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
            Object[] entry = getRuleWithGateways((ControlNode) el, conjunction, null);
            if (entry == null)
                return;
            candidate.addIdentifiedExpression((SBVRExpressionModel) entry[0]);
            SourceEntry source = new SourceEntry((List<Object>)entry[1], (List<String>)entry[2]);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private SBVRExpressionModel addTasksWithConditions(SBVRExpressionModel candidate, Map<ActivityNode, ActivityNeighborhood> tasksData,
                                                       List<Object> objects, List<String> representations, Conjunction conjunction,
                                                       ActivityNode excluded, ControlNode gate) {
        List<Object> tasksDefault = new ArrayList<>();
        boolean added_first = true;
        boolean rules_added = false;
        for (Entry<ActivityNode, ActivityNeighborhood> entryOut : tasksData.entrySet()) {
            Map<Element, String> subjectsOut = entryOut.getValue().taskSubjects;
            Map<ActivityNode, Map<ActivityEdge, String>> conditionsOut = entryOut.getValue().correctionsIncoming;
            Map<ActivityEdge, String> gateConditions = conditionsOut.get(gate);
            if (gateConditions != null) {
                for (Entry<ActivityEdge, String> cond : gateConditions.entrySet())
                    if (cond.getValue() == null)
                        gateConditions.remove(cond.getKey());
            }
            int nullTotal = entryOut.getValue().nullsTotalIncoming;
            for (Entry<Element, String> subjectOut : subjectsOut.entrySet()) {
                // Skip activity node it is excluded
                if (excluded != null && entryOut.getValue().taskNode == excluded)
                    continue;
                // Add verb concept from rule and subject (lane, resource, etc.)
                objects.add(subjectOut.getKey());
                objects.add(entryOut.getValue().taskNode);
                representations.add(subjectOut.getValue());
                representations.add(entryOut.getValue().taskText);
                if (gateConditions != null && !gateConditions.isEmpty()) {
                    if (!added_first)
                        candidate = candidate.addUnidentifiedText(",").addConjunction(conjunction);
                    else
                        added_first = false;
                    candidate = addActivity(candidate, entryOut.getValue().taskNode, subjectOut.getValue());
                    // Add verb concepts from conditions
                    candidate = candidate.addRuleConditional(Conditional.IF);
                    rules_added = true;
                    candidate = addMultipleConditions(candidate, gateConditions, objects, representations);
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
            extractTasksWithDataObjects(el, false, "is produced", "is provided to");
    }

    private void extractRuleT7(Element el) {
        if (isActivityElement(el))
            extractTasksWithDataObjects(el, true, "is available to", "is provided with data");
    }

    private void extractTasksWithDataObjects(Element el, boolean checkDataStore, String reservedVerb1, String reservedVerb2) {
        // Outgoing tasks with data objects
        List<Element> dataObjects = new ArrayList<>();
        Map<Element, String> taskSubjects = getSubjectNames(el);
        for (ActivityEdge outAssoc: ((ActivityNode)el).getOutgoing())
            if (isDataAssociation(outAssoc)) {
                ActivityNode dataObj = outAssoc.getTarget();
                if (checkDataStore ? isDataStore(dataObj) : isDataObject(dataObj))
                    dataObjects.add(dataObj);
            }
        if (!dataObjects.isEmpty()) {
            for (Entry<Element, String> taskSubject : taskSubjects.entrySet()) {
                SBVRExpressionModel subjectConcept = getGeneralConcept(taskSubject.getValue());
                SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.OBLIGATION);
                boolean added_first_obj = true;
                for (Element dataObj : dataObjects) {
                    Collection<State> states = ((CentralBufferNode) dataObj).getInState();
                    if (!added_first_obj)
                        candidate = candidate.addAndExpression();
                    else
                        added_first_obj = false;
                    String objText = extractElementText(dataObj);
                    if (states.isEmpty()) {
                        candidate = addGeneralConcept(candidate, dataObj);
                        candidate = candidate.addVerbConcept(reservedVerb1, true);
                        if (checkDataStore)
                            candidate = subjectConcept != null ?
                                    candidate.addIdentifiedExpression(subjectConcept) :
                                    candidate.addUnidentifiedText(taskSubject.getValue());
                    } else {
                        boolean added_first_state = true;
                        for (State state : states) {
                            if (!added_first_state)
                                candidate = candidate.addAndExpression();
                            else
                                added_first_state = false;
                            String stateText = extractElementText(state);
                            SBVRExpressionModel objConcept = getGeneralConcept(stateText + " " + objText);
                            candidate = objConcept != null ?
                                    candidate.addIdentifiedExpression(objConcept) :
                                    candidate.addUnidentifiedText(stateText + " " + objText);
                            candidate = candidate.addVerbConcept(reservedVerb1, true);
                            if (checkDataStore)
                                candidate = subjectConcept != null ?
                                        candidate.addIdentifiedExpression(subjectConcept) :
                                        candidate.addUnidentifiedText(taskSubject.getValue());
                        }
                    }
                }
                candidate = candidate.addRuleConditional(Conditional.WHEN);
                candidate = addActivity(candidate, (ActivityNode) el, taskSubject.getValue());
                List<Object> srcElements = new ArrayList<>(dataObjects);
                srcElements.add(taskSubject.getKey());
                srcElements.add(el);
                List<String> names = new ArrayList<>();
                for (Element dataObject : dataObjects)
                    names.add(getProperName(dataObject));
                names.add(getProperName(taskSubject.getKey()));
                names.add(getProperName(el));
                SourceEntry source = new SourceEntry(srcElements, names);
                br_candidates.add(source, candidate);
                br_candidates.setAutomaticExtraction(source);
            }
        }
        // Incoming tasks with data objects
        dataObjects.clear();
        for (ActivityEdge outAssoc: ((ActivityNode)el).getIncoming())
            if (isDataAssociation(outAssoc)) {
                ActivityNode dataObj = outAssoc.getSource();
                if (checkDataStore ? isDataStore(dataObj) : isDataObject(dataObj))
                    dataObjects.add(dataObj);
            }
        if (dataObjects.isEmpty())
            return;
        for (Entry<Element, String> taskSubject : taskSubjects.entrySet()) {
            SBVRExpressionModel subjectConcept = getGeneralConcept(taskSubject.getValue());
            SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PERMISSION);
            candidate = addActivity(candidate, (ActivityNode) el, taskSubject.getValue())
                    .addRuleConditional(Conditional.ONLY_IF);
            boolean added_first_obj = true;
            for (Element dataObj: dataObjects) {
                Collection<State> states = ((CentralBufferNode) dataObj).getInState();
                if (!added_first_obj)
                    candidate = candidate.addAndExpression();
                else
                    added_first_obj = false;
                String objText = extractElementText(dataObj);
                if (states.isEmpty()) {
                    candidate = addGeneralConcept(candidate, dataObj);
                    candidate = candidate.addVerbConcept(reservedVerb2, true);
                    if (!checkDataStore)
                        candidate = subjectConcept != null ?
                                candidate.addIdentifiedExpression(subjectConcept) :
                                candidate.addUnidentifiedText(taskSubject.getValue());
                } else {
                    boolean added_first_state = true;
                    for (State state: states) {
                        if (!added_first_state)
                            candidate = candidate.addAndExpression();
                        else
                            added_first_state = false;
                        String stateText = extractElementText(state);
                        SBVRExpressionModel objConcept = getGeneralConcept(stateText + " " + objText);
                        candidate = objConcept != null ?
                                candidate.addIdentifiedExpression(objConcept) :
                                candidate.addUnidentifiedText(stateText + " " + objText);
                        candidate = candidate.addVerbConcept(reservedVerb2, true);
                        if (!checkDataStore)
                            candidate = subjectConcept != null ?
                                    candidate.addIdentifiedExpression(subjectConcept) :
                                    candidate.addUnidentifiedText(taskSubject.getValue());
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

    private void extractGatewayNeighborhoods() {
        gatewayNeighborhoods = new HashMap<>();
        for (Element el: candidateElements)
            if (isGatewayElement(el))
                gatewayNeighborhoods.put((ControlNode) el, new GatewayNeighborhood((ControlNode) el, true));

        boundaryActivities = new HashSet<>();
        gatewayNeighborhoods2 = new HashMap<>();
        for (Element el: candidateElements)
            if (isGatewayElement(el)) {
                GatewayNeighborhood nnode = new GatewayNeighborhood((ControlNode) el, false);
                gatewayNeighborhoods2.put((ControlNode) el, nnode);
                getAllBoundaryActivities(nnode, boundaryActivities);
            }
    }

    private void getAllBoundaryActivities(GatewayNeighborhood nhood, Set<ActivityNode> nodes) {
        if (nhood.outgoingGateways.isEmpty())
            nodes.addAll(nhood.outgoingActivities.keySet());
        for (GatewayNeighborhood neighborNode: nhood.outgoingGateways.values())
            getAllBoundaryActivities(neighborNode, nodes);
    }

    private void createPartialRules(GatewayNeighborhood nhood) {
        ControlNode gateway = nhood.gatewayNode;
        // If gateway is a boundary gateway
        if (nhood.outgoingGateways.isEmpty()) {
            Conjunction conjunction = getGatewayConjunction(gateway);
            Object[] results = getRuleWithGateways(gateway, conjunction, null);
            nhood.partialRule = (SBVRExpressionModel) results[0];
            nhood.partialRuleSource = (List<Object>) results[1];
            nhood.partialRuleNames = (List<String>) results[2];
            return;
        }
        nhood.partialRule = new SBVRExpressionModel();
        nhood.partialRuleSource = new ArrayList<>();
        nhood.partialRuleNames = new ArrayList<>();
        Set<SBVRExpressionModel> defaultRules = new HashSet<>();
        Conjunction conjunction = getGatewayConjunction(gateway);
        boolean first_added = true;
        for (Entry<ControlNode, GatewayNeighborhood> gatewayOut: nhood.outgoingGateways.entrySet()) {
            createPartialRules(gatewayOut.getValue());
            // It is possible that multiple sequence flows are between the two gateways
            Map<ActivityEdge, String> conditions = nhood.outgoingConditions.get(gatewayOut.getKey());
            SBVRExpressionModel ruleModel = new SBVRExpressionModel();
            ruleModel = addMultipleConditions(ruleModel, conditions, nhood.partialRuleSource, nhood.partialRuleNames);
            SBVRExpressionModel partialOutgoingRule = gatewayOut.getValue().partialRule;
            if (partialOutgoingRule.isEmpty())
                continue;
            if (ruleModel.isEmpty())
                defaultRules.add(partialOutgoingRule);
            else {
                if (!first_added)
                    nhood.partialRule.addConjunction(conjunction);
                else
                    first_added = false;
                nhood.partialRule.addIdentifiedExpression(partialOutgoingRule)
                        .addRuleConditional(Conditional.IF)
                        .addIdentifiedExpression(ruleModel);
            }
            nhood.partialRuleSource.addAll(gatewayOut.getValue().partialRuleSource);
            nhood.partialRuleNames.addAll(gatewayOut.getValue().partialRuleNames);
        }
        for (Entry<ActivityNode, ActivityNeighborhood> activityOut: nhood.outgoingActivities.entrySet()) {
            // It is possible that multiple sequence flows are between the two tasks
            Map<ActivityEdge, String> conditions = nhood.outgoingConditions.get(activityOut.getKey());
            SBVRExpressionModel ruleModel = new SBVRExpressionModel();
            ruleModel = addMultipleConditions(ruleModel, conditions, nhood.partialRuleSource, nhood.partialRuleNames);
            Map<Element, String> subjects = getSubjectNames(activityOut.getKey());
            for (Entry<Element, String> subject : subjects.entrySet()) {
                SBVRExpressionModel partialOutgoingRule = addActivity(new SBVRExpressionModel(), activityOut.getKey(), subject.getValue());
                if (partialOutgoingRule.isEmpty())
                    continue;
                if (ruleModel.isEmpty())
                    defaultRules.add(partialOutgoingRule);
                else {
                    if (!first_added)
                        nhood.partialRule.addConjunction(conjunction);
                    else
                        first_added = false;
                    nhood.partialRule.addIdentifiedExpression(partialOutgoingRule)
                            .addRuleConditional(Conditional.IF)
                            .addIdentifiedExpression(ruleModel);
                }
                nhood.partialRuleSource.addAll(Arrays.asList(subject.getKey(), activityOut.getKey()));
                nhood.partialRuleNames.addAll(Arrays.asList(getProperName(subject.getKey()), getProperName(activityOut.getKey())));
            }
        }
        // Add default conditions
        if (!defaultRules.isEmpty()) {
            first_added = true;
            nhood.partialRule.addUnidentifiedText(",").addRuleConditional(Conditional.OTHERWISE);
            for (SBVRExpressionModel default_: defaultRules) {
                if (!first_added)
                    nhood.partialRule.addConjunction(Conjunction.OR);
                else
                    first_added = false;
                nhood.partialRule.addIdentifiedExpression(default_);
            }
        }
    }

    private void extractComplexRule(Element el) {
        if (!isActivityElement(el))
            return;
        Map<Element, String> subjects = getSubjectNames(el);
        Collection<ActivityEdge> incomingEdges = ((ActivityNode) el).getIncoming();
        if (incomingEdges.isEmpty())
            return;
        Set<ActivityEdge> incomingGateways = incomingEdges.stream().filter(n -> isGatewayElement(n.getSource())).collect(Collectors.toSet());
        // If task is connected only with other tasks, T2 rule will extract such rules
        if (incomingGateways.isEmpty())
            return;

        Set<ControlNode> gatewaysProcessed = new HashSet<>();
        if (boundaryActivities.contains(el)) {
            // Exclude incoming gateways which are connected with sequence flow themselves to avoid inner loops/connections between neighborhoods
            // In this case, the target will be included in source GatewayNeighborhood
            for (ActivityEdge incoming : incomingEdges)
                if (isGatewayElement(incoming.getSource()))
                    gatewaysProcessed.add((ControlNode) incoming.getSource());
            Iterator<ControlNode> iterSelected = gatewaysProcessed.iterator();
            while (iterSelected.hasNext()) {
                ControlNode c = iterSelected.next();
                for (ActivityEdge incGateway : c.getIncoming())
                    if (gatewaysProcessed.contains(incGateway.getSource())) {
                        iterSelected.remove();
                        break;
                    }
            }
        }
        for (Entry<Element, String> subject : subjects.entrySet()) {
            List<Object> sources = new ArrayList<>();
            List<String> names = new ArrayList<>();
            SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.OBLIGATION);
            // If starting activity is also a boundary element for some gateway
            if (boundaryActivities.contains(el)) {
                boolean first_or = true;
                for (ControlNode gatewayInc : gatewaysProcessed) {
                    if (!first_or)
                        candidate.addConjunction(Conjunction.OR);
                    else
                        first_or = false;
                    GatewayNeighborhood nnode = gatewayNeighborhoods2.get(gatewayInc);
                    candidate.addIdentifiedExpression(nnode.partialRule);
                    sources.addAll(nnode.partialRuleSource);
                    names.addAll(nnode.partialRuleNames);
                }
            } else {
                candidate = addActivity(candidate, (ActivityNode) el, subject.getValue());
                sources.addAll(Arrays.asList(subject.getKey(), el));
                names.addAll(Arrays.asList(getProperName(subject.getKey()), getProperName(el)));
                for (ActivityEdge incoming : incomingEdges) {
                    String incomingCondition = getCondition(incoming);
                    if (incomingCondition != null) {
                        candidate = candidate.addRuleConditional(Conditional.IF);
                        candidate = addCondition(candidate, incomingCondition);
                    }
                    ActivityNode node = incoming.getSource();
                    candidate = candidate.addUnidentifiedText(",").addRuleConditional(Conditional.AFTER);
                    // It should be a boundary activity for some GatewayNeighborhood in this case, but process if it is not
                    if (isGatewayElement(node)) {
                        GatewayNeighborhood nnode = gatewayNeighborhoods2.get(node);
                        if (nnode.partialRule != null && !nnode.partialRule.isEmpty())
                            candidate = candidate.addIdentifiedExpression(nnode.partialRule);
                        sources.addAll(nnode.partialRuleSource);
                        names.addAll(nnode.partialRuleNames);
                    } else if (isActivityElement(node)) {
                        candidate = addActivity(candidate, node, subject.getValue());
                        sources.addAll(Arrays.asList(subject.getKey(), node));
                        names.addAll(Arrays.asList(getProperName(subject.getKey()), getProperName(node)));
                    }
                }
            }
            SourceEntry src = new SourceEntry(sources, names);
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

    private String getConditionsRepresentation(Map<ActivityNode, Map<ActivityEdge, String>> structConditions){
        final StringBuilder sb = new StringBuilder();
        for (Entry<ActivityNode, Map<ActivityEdge, String>> entry: structConditions.entrySet()) {
            sb.append(entry.getKey().getHumanName()).append(": [");
            if (!entry.getValue().isEmpty()) {
                for (Entry<ActivityEdge, String> condition: entry.getValue().entrySet())
                    sb.append(condition.getKey().getHumanName()).append(" -> ").append(condition.getValue()).append(", ");
                sb.delete(sb.length() - 2, sb.length());
            }
            sb.append("]").append("\n");
        }
        return sb.toString();
    }

}
