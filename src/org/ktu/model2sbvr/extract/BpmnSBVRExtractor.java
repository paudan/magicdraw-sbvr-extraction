package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdcompleteactions.AcceptEventAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdintermediateactions.SendObjectAction;
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
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Relationship;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conditional;
import org.ktu.model2sbvr.models.SBVRExpressionModel.Conjunction;
import org.ktu.model2sbvr.models.SBVRExpressionModel.RuleType;

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
    private static String[] intermediaryStereotypes = {"NoneIntermediateEvent", "MessageCatchIntermediateEvent", "TimerCatchIntermediateEvent",
            "ConditionalCatchIntermediateEvent", "LinkCatchIntermediateEvent", "SignalCatchIntermediateEvent", "MultipleCatchIntermediateEvent",
            "ParallelMultipleCatchIntermediateEvent",
            "MessageThrowIntermediateEvent", "EscalationThrowIntermediateEvent", "CompensationThrowIntermediateEvent", "LinkThrowIntermediateEvent",
            "SignalThrowIntermediateEvent", "MultipleThrowIntermediateEvent"};

    Map<ControlNode, GatewayNeighborhood> gatewayNeighborhoods, gatewayNeighborhoods2;


    class ActivityNodeNeighborhood {
        ActivityNode activityNode;
        String activityText;
        Map<Element, String> activitySubjects;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        Map<ActivityNode, Map<ActivityEdge, String>> correctionsIncoming, correctionsOutgoing;
        private Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;
        int nullsTotalIncoming, nullsTotalOutgoing;

        private ActivityNodeNeighborhood(ActivityNode node) {
            this.activityNode = node;
            this.activityText = extractElementText(node);
            activitySubjects = getSubjectNames(node);
            incomingConditions = new HashMap<>();
            nullCountIncoming = new HashMap<>();
            for (ActivityEdge edge : node.getIncoming())
                addCondition(incomingConditions, nullCountIncoming, edge, edge.getSource());
            outgoingConditions = new HashMap<>();
            nullCountOutgoing = new HashMap<>();
            for (ActivityEdge edge : node.getOutgoing())
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
            sb.append("Activity element:").append(activityNode.getHumanName()).append("\n")
                    .append("Extracted text: ").append(activityText).append("\n")
                    .append("Subjects (executing elements): ")
                    .append(String.join(",", activitySubjects.keySet().stream().map(Element::getHumanName).collect(Collectors.toList())));
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
        Map<ActivityNode, ActivityNodeNeighborhood> incomingActivities, outgoingActivities;
        Map<ActivityNode, Map<ActivityEdge, String>> incomingConditions, outgoingConditions;
        private Map<ActivityNode, Integer> nullCountIncoming, nullCountOutgoing;
        Map<ControlNode, GatewayNeighborhood> incomingGateways, outgoingGateways;
        int nullsTotalIncoming, nullsTotalOutgoing;
        SBVRExpressionModel partialRule;
        List<Object> partialRuleSource;

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
            for (ActivityEdge edge : gatewayNode.getIncoming()) {
                ActivityNode srcElement = edge.getSource();
                if (srcElement == null)
                    continue;
                addIncomingCondition(edge, srcElement);
                if (isActivityElement(srcElement) || isEventElement(srcElement)) {
                    ActivityNodeNeighborhood taskTuple = new ActivityNodeNeighborhood(srcElement);
                    incomingActivities.put(taskTuple.activityNode, taskTuple);
                } else if (isGatewayElement(srcElement)) {
                    ControlNode node = (ControlNode) srcElement;
                    GatewayNeighborhood nnode = null;
                    if (fillIncoming)
                        nnode = new GatewayNeighborhood(node, fillIncoming);
                    incomingGateways.put(node, nnode);
                }
            }
            outgoingActivities = new HashMap<>();
            for (ActivityEdge edge : gatewayNode.getOutgoing()) {
                ActivityNode targetElement = edge.getTarget();
                if (targetElement == null)
                    continue;
                addOutgoingCondition(edge, targetElement);
                if (isActivityElement(targetElement) || isEventElement(targetElement)) {
                    ActivityNodeNeighborhood taskTuple = new ActivityNodeNeighborhood(targetElement);
                    outgoingActivities.put(taskTuple.activityNode, taskTuple);
                } else if (isGatewayElement(targetElement)) {
                    ControlNode node = (ControlNode) targetElement;
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
                for (ActivityNodeNeighborhood taskNode: incomingActivities.values())
                    sb.append("\t").append(formatPadding(taskNode.toString())).append("\n\n");
            }
            if (!outgoingActivities.isEmpty()) {
                sb.append("\nOutgoing activity nodes:\n");
                for (ActivityNodeNeighborhood taskNode: outgoingActivities.values())
                    sb.append("\t").append(formatPadding(taskNode.toString())).append("\n\n");
            }
            if (!incomingGateways.isEmpty()) {
                sb.append("\nIncoming gateway nodes:\n");
                for (Entry<ControlNode, GatewayNeighborhood> gatewayNode: incomingGateways.entrySet()) {
                    sb.append("\t").append("Element: ").append(gatewayNode.getKey().getHumanName()).append("\n");
                    if (gatewayNode.getValue() != null)
                        sb.append("\t").append(formatPadding(gatewayNode.getValue().toString())).append("\n\n");
                }
            }
            if (!outgoingGateways.isEmpty()) {
                sb.append("\nOutgoing gateway nodes:\n");
                for (Entry<ControlNode, GatewayNeighborhood> gatewayNode: outgoingGateways.entrySet()) {
                    sb.append("\t").append("Element: ").append(gatewayNode.getKey().getHumanName()).append("\n");
                    if (gatewayNode.getValue() != null)
                        sb.append("\t").append(formatPadding(gatewayNode.getValue().toString())).append("\n\n");
                }
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
        return Arrays.asList(AcceptEventAction.class, SendObjectAction.class).contains(el.getClassType()) &&
                hasAnyStereotype(el, boundaryStereotypes);
    }

    private boolean isIntermediaryGateway(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(AcceptEventAction.class) && hasAnyStereotype(el, intermediaryStereotypes);
    }

    private boolean isEndEventElement(Element el) {
        if (el == null)
            return false;
        return el.getClassType().equals(ActivityFinalNode.class)
                && hasAnyStereotype(el, "EndEvent", "MessageEndEvent", "ErrorEndEvent", "EscalationEndEvent",
                "CompensationEndEvent", "SignalEndEvent", "MultipleEndEvent", "TerminateEndEvent");
    }

    private boolean isEventElement(Element el) {
        return isStartEventElement(el) || isBoundaryEvent(el) || isIntermediaryGateway(el) || isEndEventElement(el);
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

    boolean isGatewayElement(Element el) {
        return isGatewayOfType(el, "ExclusiveGateway") || isGatewayOfType(el, "InclusiveGateway") ||
               isGatewayOfType(el, "ParallelGateway") || isGatewayOfType(el, "EventBasedGateway");
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
                    gc_candidates.setManualExtraction(new MagicDrawSourceEntry(Collections.singletonList(el)));
            } else if (isEventElement(el) && extractElementText(el) != null)
                gc_candidates.setManualExtraction(new MagicDrawSourceEntry(Collections.singletonList(el)));
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
                    gc_candidates.setManualExtraction(new MagicDrawSourceEntry(Collections.singletonList(el)));
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
            } else if (isEventElement(el) && !strictOnly) {
                createVerbConceptFromCondition(el, extractElementText(el));
                vc_candidates.setManualExtraction(new MagicDrawSourceEntry(Collections.singletonList(el)));
            } else if (isSequenceFlow(el) && !strictOnly)
                createVerbConceptFromCondition(el, getCondition((ActivityEdge) el));
            else if (isDataObject(el) || isDataStore(el))
                for (State state : ((CentralBufferNode) el).getInState())
                    createCharacteristic(el, state);
            else if (el.getClassType().equals(Comment.class) && extractElementText(el) != null && !strictOnly)
                vc_candidates.setManualExtraction(new MagicDrawSourceEntry(Collections.singletonList(el)));
        }
    }

    @Override
    protected void extractBusinessRuleCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            extractRuleT1(el);
            extractRuleT2(el);
            extractRuleT3(el);
            extractRuleT4(el);
            extractRuleT5(el);
            extractRuleT5(el);
            extractRuleT6(el);
            extractRuleT7(el);
            extractRuleT8(el);
            extractRuleT9(el);
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

    private SBVRExpressionModel addActivity(SBVRExpressionModel model, ActivityNode activity, String subject) {
        if (activity == null)
            return model;
        String outTaskText = extractElementText(activity);
        String verbText = outTaskText;
        if (!isEventElement(activity))
            verbText = subject + " " + outTaskText;
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

    private SBVRExpressionModel createMultipleConditions(Map<ActivityEdge, String> conditions, List<Object> objects) {
        SBVRExpressionModel model = new SBVRExpressionModel();
        if (conditions == null || conditions.isEmpty())
            return model;
        boolean added_or_first = true;
        Set<String> strConditions = conditions.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (strConditions.isEmpty())
            return model;
        for (Entry<ActivityEdge, String> cond : conditions.entrySet()) {
            if (!added_or_first)
                model.addConjunction(Conjunction.OR);
            else
                added_or_first = false;
            String condition = cond.getValue();
            if (condition != null) {
                model = addCondition(model, cond.getValue());
                if (objects != null)
                    objects.add(cond.getKey());
            }
        }
        return model;
    }

    private Conjunction getGatewayConjunction(ControlNode gateway) {
        if (hasAnyStereotype(gateway, "ExclusiveGateway", "EventBasedGateway"))
            return Conjunction.OR;
        else if (hasAnyStereotype(gateway, "InclusiveGateway"))
            return Conjunction.AND;
        return null;
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
                    MagicDrawSourceEntry source = new MagicDrawSourceEntry(Arrays.asList(part, after, part, before));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT2(Element el) {
        extractRulesWithGatewaysSimplified(el, "ExclusiveGateway", Conjunction.OR);
        //extractRuleWithGateways(el, "ExclusiveGateway", Conjunction.OR);
    }

    private void extractRuleT3(Element el) {
        extractRulesWithGatewaysSimplified(el, "InclusiveGateway", Conjunction.AND);
        //extractRuleWithGateways(el, "InclusiveGateway", Conjunction.AND);
    }

    private void extractRulesWithGatewaysSimplified(Element el, String gatewayStereotype, Conjunction conjunction) {
        if (!isGatewayOfType(el, gatewayStereotype))
            return;
        GatewayNeighborhood tuple = gatewayNeighborhoods.get(el);
        if (tuple.incomingActivities.isEmpty() && tuple.outgoingActivities.isEmpty())
            return;
        if (!tuple.outgoingActivities.isEmpty()) {
            for (Entry<ActivityNode, ActivityNodeNeighborhood> actOut: tuple.outgoingActivities.entrySet()) {
                Map<Element, String> subjectsOut = actOut.getValue().activitySubjects;
                for (Entry<Element, String> subjectOut: subjectsOut.entrySet()) {
                    SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.OBLIGATION);
                    List<Object> objects = new ArrayList<>(Arrays.asList(actOut.getKey(), subjectOut.getKey()));
                    candidate = addActivity(candidate, actOut.getKey(), subjectOut.getValue());
                    candidate.addRuleConditional(Conditional.AFTER);
                    if (!tuple.incomingActivities.isEmpty()) {
                        boolean first_added = true;
                        for (Entry<ActivityNode, ActivityNodeNeighborhood> incNode: tuple.incomingActivities.entrySet()) {
                            Map<Element, String> subjectsIn = incNode.getValue().activitySubjects;
                            for (Entry<Element, String> subjectIn: subjectsIn.entrySet()) {
                                if (!first_added)
                                    candidate.addConjunction(conjunction);
                                else
                                    first_added = false;
                                candidate = addActivity(candidate, incNode.getKey(), subjectIn.getValue());
                                objects.add(subjectIn.getKey());
                                SBVRExpressionModel conditionModel = createMultipleConditions(incNode.getValue().outgoingConditions.get(el), objects);
                                if (!conditionModel.isEmpty())
                                    candidate.addRuleConditional(Conditional.IF).addIdentifiedExpression(conditionModel);
                            }
                        }
                    }
                    SBVRExpressionModel conditionModel = createMultipleConditions(actOut.getValue().incomingConditions.get(el), objects);
                    if (!conditionModel.isEmpty())
                        candidate.addConjunction(Conjunction.AND).addRuleConditional(Conditional.IF)
                                .addIdentifiedExpression(conditionModel);
                    MagicDrawSourceEntry source = new MagicDrawSourceEntry(objects);
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    Object[] getRuleWithGateways(ControlNode el, Conjunction conjunction) {
        GatewayNeighborhood tuple = gatewayNeighborhoods.get(el);
        List<Object> objects = new ArrayList<>();
        List<String> representations = new ArrayList<>();
        SBVRExpressionModel candidate = new SBVRExpressionModel();
        Object[] results = new Object[3];
        results[0] = candidate;
        results[1] = objects;
        results[2] = representations;
        if (tuple.incomingActivities.isEmpty() && tuple.outgoingActivities.isEmpty())
            return results;
        if (!tuple.outgoingActivities.isEmpty())
            candidate = addTasksWithConditions(candidate, tuple.outgoingActivities, objects, conjunction, null, el);
        // No incoming or outgoing sequences flows - bad practice, but must be checked
        //TODO: check incoming conditions from activities which are "incoming"
        if (!tuple.incomingActivities.isEmpty()) {
            // Skip conditional if only excluded task is in outgoing activities
            Set<ActivityNode> actOut = tuple.outgoingActivities.keySet();
            if (actOut.size() >= 1)
                candidate = candidate.addUnidentifiedText(",").addRuleConditional(Conditional.AFTER);
            candidate = addTasksWithConditions(candidate, tuple.incomingActivities, objects, conjunction, null, el);
        }
        results[0] = candidate;
        return results;
    }

    private void extractRuleWithGateways(Element el, String gatewayStereotype, Conjunction conjunction) {
        if (isGatewayOfType(el, gatewayStereotype)) {
            SBVRExpressionModel candidate = new SBVRExpressionModel()
                    .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION);
            Object[] entry = getRuleWithGateways((ControlNode) el, conjunction);
            if (entry == null)
                return;
            candidate.addIdentifiedExpression((SBVRExpressionModel) entry[0]);
            MagicDrawSourceEntry source = new MagicDrawSourceEntry((List<Object>)entry[1]);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private SBVRExpressionModel addTasksWithConditions(SBVRExpressionModel candidate, Map<ActivityNode, ActivityNodeNeighborhood> tasksData,
                                                       List<Object> objects, Conjunction conjunction, ActivityNode excluded, ControlNode gate) {
        List<Object> tasksDefault = new ArrayList<>();
        boolean added_first = true;
        boolean rules_added = false;
        for (Entry<ActivityNode, ActivityNodeNeighborhood> entryOut : tasksData.entrySet()) {
            Map<Element, String> subjectsOut = entryOut.getValue().activitySubjects;
            Map<ActivityNode, Map<ActivityEdge, String>> conditionsOut = entryOut.getValue().incomingConditions;
            Map<ActivityEdge, String> gateConditions = conditionsOut.get(gate);
            if (gateConditions != null) {
                for (Entry<ActivityEdge, String> cond : gateConditions.entrySet())
                    if (cond.getValue() == null)
                        gateConditions.remove(cond.getKey());
            }
            for (Entry<Element, String> subjectOut : subjectsOut.entrySet()) {
                // Skip activity node it is excluded
                if (excluded != null && entryOut.getValue().activityNode == excluded)
                    continue;
                // Add verb concept from rule and subject (lane, resource, etc.)
                objects.add(subjectOut.getKey());
                objects.add(entryOut.getValue().activityNode);
                if (gateConditions != null && !gateConditions.isEmpty()) {
                    if (!added_first)
                        candidate.addUnidentifiedText(",").addConjunction(conjunction);
                    else
                        added_first = false;
                    candidate = addActivity(candidate, entryOut.getValue().activityNode, subjectOut.getValue());
                    // Add verb concepts from conditions
                    SBVRExpressionModel conditionModel = createMultipleConditions(gateConditions, objects);
                    if (!conditionModel.isEmpty())
                        candidate.addRuleConditional(Conditional.IF).addIdentifiedExpression(conditionModel);
                    rules_added = true;
                } else {
                    // No conditions are present, process as default
                    String outTaskText = extractElementText(entryOut.getValue().activityNode);
                    SBVRExpressionModel taskModel = getVerbConcept(subjectOut.getValue() + " " + outTaskText);
                    tasksDefault.add(taskModel != null ? taskModel : subjectOut.getValue() + " " + outTaskText);
                }
            }
        }
        if (!tasksDefault.isEmpty()) {
            if (rules_added)
                candidate.addUnidentifiedText(",").addRuleConditional(Conditional.OTHERWISE);
            added_first = true;
            for (Object model : tasksDefault) {
                if (!added_first)
                    candidate.addConjunction(Conjunction.OR);
                else
                    added_first = false;
                if (model instanceof SBVRExpressionModel)
                    candidate.addIdentifiedExpression((SBVRExpressionModel) model);
                else
                    candidate.addUnidentifiedText(model.toString());
            }
        }
        return candidate;
    }


    private void extractRuleT4(Element el) {
        if (!isGatewayOfType(el, "EventBasedGateway"))
            return;
        ControlNode node = (ControlNode) el;
        for (ActivityEdge edgeInc: node.getIncoming()) {
            if (isActivityElement(edgeInc.getSource())) {
                Map<Element, String> subjectsInc = getSubjectNames(edgeInc.getSource());
                for (Entry<Element, String> subjectInc: subjectsInc.entrySet()) {
                    for (ActivityEdge edgeOut: node.getOutgoing()) {
                        if (isIntermediaryGateway(edgeOut.getTarget())) {
                            SBVRExpressionModel model = new SBVRExpressionModel().addRuleExpression(RuleType.PERMISSION);
                            model = addActivity(model, edgeOut.getTarget(), null);
                            model.addRuleConditional(Conditional.AFTER);
                            model = addActivity(model, edgeInc.getSource(), subjectInc.getValue());
                            String incomingCondition = getCondition(edgeInc);
                            if (incomingCondition != null) {
                                model.addRuleConditional(Conditional.IF);
                                model = addCondition(model, incomingCondition);
                            }
                            String outgoingCondition = getCondition(edgeOut);
                            if (outgoingCondition != null) {
                                model.addConjunction(Conjunction.AND).addRuleConditional(Conditional.IF);
                                model = addCondition(model, outgoingCondition);
                            }
                            MagicDrawSourceEntry source = new MagicDrawSourceEntry(Arrays.asList(edgeOut.getTarget(), edgeInc.getSource(), subjectInc.getKey()));
                            br_candidates.add(source, model);
                            br_candidates.setAutomaticExtraction(source);
                        }
                    }
                }
            }
        }
    }

    private void extractRuleT5(Element el) {
        if (isGatewayOfType(el, "ParallelGateway")) {
            Map<ActivityEdge, ActivityNodeNeighborhood> outgoingElements = new HashMap<>();
            for (ActivityEdge edge : ((ControlNode) el).getOutgoing()) {
                ActivityNodeNeighborhood taskTuple = new ActivityNodeNeighborhood(edge.getTarget());
                outgoingElements.put(edge, taskTuple);
            }
            for (Entry<ActivityEdge, ActivityNodeNeighborhood> outTaskNode: outgoingElements.entrySet()) {
                ActivityNode outgoingTask = outTaskNode.getValue().activityNode;
                if (!(isTaskElement(outgoingTask) || isEndEventElement(outgoingTask)))
                    continue;
                String taskText = outTaskNode.getValue().activityText;
                if (taskText == null)
                    continue;
                for (Entry<Element, String> entry: outTaskNode.getValue().activitySubjects.entrySet()) {
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
                    candidate = addActivity(candidate, outTaskNode.getValue().activityNode, subject);
                    candidate.setAuto(true);
                    List<Element> srcObj = new ArrayList<>(Arrays.asList(part, outTaskNode.getKey(), el));
                    for (ActivityNode outTask : incomingTasks) {
                        srcObj.add(part);
                        srcObj.add(outTask);
                    }
                    MagicDrawSourceEntry source = new MagicDrawSourceEntry(new ArrayList<>(srcObj));
                    br_candidates.add(source, candidate);
                    br_candidates.setAutomaticExtraction(source);
                }
            }
        }
    }

    private void extractRuleT6(Element el) {
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
                    MagicDrawSourceEntry source = new MagicDrawSourceEntry(Arrays.asList(subject.getKey(), el, boundary));
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
                            source = new MagicDrawSourceEntry(Arrays.asList(subject.getKey(), el, boundary, outTask));
                            br_candidates.add(source, candidate);
                            br_candidates.setAutomaticExtraction(source);
                        }
                    }

                    if (!isInterrupting) {
                        candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PROHIBITION);
                        candidate = addActivity(candidate, (ActivityNode) el, subject.getValue())
                                .addRuleConditional(Conditional.AFTER);
                        candidate = addCondition(candidate, getProperName(boundary));
                        source = new MagicDrawSourceEntry(Arrays.asList(subject.getKey(), el, boundary));
                        br_candidates.add(source, candidate);
                        br_candidates.setAutomaticExtraction(source);
                    }
                }
            }
        }
    }

    private void extractRuleT7(Element el) {
        if (isActivityElement(el) || isEventElement(el))
            extractTasksWithDataObjects(el, false, "is produced", "is provided to");
        else if (isSequenceFlow(el))
            extractTasksWithAssociationsAndDataObjects(el, false, "is produced", "is provided to");
    }

    private void extractRuleT8(Element el) {
        if (isActivityElement(el))
            extractTasksWithDataObjects(el, true, "is available to", "is provided with data");
        else if (isSequenceFlow(el))
            extractTasksWithAssociationsAndDataObjects(el, true, "is available to", "is provided with data");
    }

    private void extractTasksWithAssociationsAndDataObjects(Element el, boolean checkDataStore, String reservedVerb1, String reservedVerb2) {
        ActivityEdge flow = (ActivityEdge) el;
        List<Element> dataObjects = new ArrayList<>();
        for (Relationship assoc: flow.get_relationshipOfRelatedElement())
            if (assoc instanceof Dependency) {
                Collection<NamedElement> clients = ((Dependency) assoc).getClient();
                for (NamedElement client: clients)
                    if (checkDataStore ? isDataStore(client) : isDataObject(client))
                        dataObjects.add(client);
            }
        processOutgoingConnectionsWithDataObjects(dataObjects, flow.getSource(), checkDataStore, reservedVerb1);
        processIncomingConnectionsWithDataObjects(dataObjects, flow.getTarget(), checkDataStore, reservedVerb2);
    }

    private void extractTasksWithDataObjects(Element el, boolean checkDataStore, String reservedVerb1, String reservedVerb2) {
        // Outgoing tasks with data objects
        List<Element> dataObjects = new ArrayList<>();
        for (ActivityEdge outAssoc: ((ActivityNode)el).getOutgoing())
            if (isDataAssociation(outAssoc)) {
                ActivityNode dataObj = outAssoc.getTarget();
                if (checkDataStore ? isDataStore(dataObj) : isDataObject(dataObj))
                    dataObjects.add(dataObj);
            }
        processOutgoingConnectionsWithDataObjects(dataObjects, el, checkDataStore, reservedVerb1);
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
        processIncomingConnectionsWithDataObjects(dataObjects, el, checkDataStore, reservedVerb2);
    }

    private void processOutgoingConnectionsWithDataObjects(List<Element> dataObjects, Element taskElement, boolean checkDataStore, String reservedVerb1) {
        if (dataObjects.isEmpty())
            return;
        Map<Element, String> taskSubjects = getSubjectNames(taskElement);
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
                candidate = addActivity(candidate, (ActivityNode) taskElement, taskSubject.getValue());
                List<Object> srcElements = new ArrayList<>(dataObjects);
                srcElements.add(taskSubject.getKey());
                srcElements.add(taskElement);
                MagicDrawSourceEntry source = new MagicDrawSourceEntry(srcElements);
                br_candidates.add(source, candidate);
                br_candidates.setAutomaticExtraction(source);
            }
        }
    }

    private void processIncomingConnectionsWithDataObjects(List<Element> dataObjects, Element taskElement, boolean checkDataStore, String reservedVerb2) {
        if (dataObjects.isEmpty())
            return;
        Map<Element, String> taskSubjects = getSubjectNames(taskElement);
        for (Entry<Element, String> taskSubject : taskSubjects.entrySet()) {
            SBVRExpressionModel subjectConcept = getGeneralConcept(taskSubject.getValue());
            SBVRExpressionModel candidate = new SBVRExpressionModel().addRuleExpression(RuleType.PERMISSION);
            candidate = addActivity(candidate, (ActivityNode) taskElement, taskSubject.getValue())
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
            srcElements.add(taskElement);
            srcElements.add(taskSubject.getKey());
            srcElements.addAll(dataObjects);
            MagicDrawSourceEntry source = new MagicDrawSourceEntry(srcElements);
            br_candidates.add(source, candidate);
            br_candidates.setAutomaticExtraction(source);
        }
    }

    private void extractRuleT9(Element el) {
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
        if (task1 != null)
            source.add(getProperName(task1));
        if (task2 != null)
            source.add(getProperName(task2));
        MagicDrawSourceEntry src = new MagicDrawSourceEntry(source);
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
                MagicDrawSourceEntry src = new MagicDrawSourceEntry(Arrays.asList(el, subject, source, target));
                br_candidates.add(src, candidate);
                br_candidates.setAutomaticExtraction(src);
            }
    }

    private void extractGatewayNeighborhoods() {
        gatewayNeighborhoods = new HashMap<>();
        for (Element el: candidateElements)
            if (isGatewayElement(el))
                gatewayNeighborhoods.put((ControlNode) el, new GatewayNeighborhood((ControlNode) el, true));

        gatewayNeighborhoods2 = new HashMap<>();
        for (Element el: candidateElements)
            if (isGatewayElement(el))
                gatewayNeighborhoods2.put((ControlNode) el, new GatewayNeighborhood((ControlNode) el, false));
    }

    private void getAllBoundaryGateways(GatewayNeighborhood nhood, Set<ControlNode> nodes) {
        if (nhood.incomingGateways.isEmpty())
            nodes.add(nhood.gatewayNode);
        for (GatewayNeighborhood neighborNode: nhood.incomingGateways.values())
            getAllBoundaryGateways(neighborNode, nodes);
    }

    private void createPartialRules(GatewayNeighborhood nhood, ControlNode excluded, Element startedElement) {
        ControlNode gateway = nhood.gatewayNode;
        if (gateway.equals(excluded))
            return;
        // If gateway is a boundary gateway, return atomic partial rule
        if (nhood.outgoingGateways.isEmpty()) {
            Conjunction conjunction = getGatewayConjunction(gateway);
            Object[] results = getRuleWithGateways(gateway, conjunction);
            nhood.partialRule = (SBVRExpressionModel) results[0];
            nhood.partialRuleSource = (List<Object>) results[1];
            return;
        }
        nhood.partialRule = new SBVRExpressionModel();
        nhood.partialRuleSource = new ArrayList<>();
        Set<SBVRExpressionModel> defaultRules = new HashSet<>();
        Set<SBVRExpressionModel> conditionedRules = new HashSet<>();
        Conjunction conjunction = getGatewayConjunction(gateway);
        boolean first_added = true;
        SBVRExpressionModel modelPart = new SBVRExpressionModel();
        // Recursively process outgoing gateways
        for (Entry<ControlNode, GatewayNeighborhood> gatewayOut: nhood.outgoingGateways.entrySet()) {
            createPartialRules(gatewayOut.getValue(), excluded, startedElement);
            // It is possible that multiple sequence flows are between the two gateways
            Map<ActivityEdge, String> conditions = nhood.outgoingConditions.get(gatewayOut.getKey());
            SBVRExpressionModel ruleModel = createMultipleConditions(conditions, nhood.partialRuleSource);
            SBVRExpressionModel partialOutgoingRule = gatewayOut.getValue().partialRule;
            if (partialOutgoingRule.isEmpty())
                continue;
            if (ruleModel.isEmpty())
                defaultRules.add(partialOutgoingRule);
            else {
                if (!first_added)
                    modelPart.addConjunction(conjunction);
                else
                    first_added = false;
                modelPart.addIdentifiedExpression(partialOutgoingRule)
                        .addRuleConditional(Conditional.IF)
                        .addIdentifiedExpression(ruleModel);
                conditionedRules.add(partialOutgoingRule);
            }
            nhood.partialRuleSource.addAll(gatewayOut.getValue().partialRuleSource);
        }
        // Process outgoing activities
        for (Entry<ActivityNode, ActivityNodeNeighborhood> activityOut: nhood.outgoingActivities.entrySet()) {
            // Exclude element which was used as the starting point
            if (activityOut.getKey().equals(startedElement))
                continue;
            // It is possible that multiple sequence flows are between the two tasks
            Map<ActivityEdge, String> conditions = activityOut.getValue().incomingConditions.get(activityOut.getKey());
            SBVRExpressionModel ruleModel = createMultipleConditions(conditions, nhood.partialRuleSource);
            Map<Element, String> subjects = getSubjectNames(activityOut.getKey());
            for (Entry<Element, String> subject : subjects.entrySet()) {
                SBVRExpressionModel partialOutgoingRule = addActivity(new SBVRExpressionModel(), activityOut.getKey(), subject.getValue());
                if (partialOutgoingRule.isEmpty())
                    continue;
                if (ruleModel.isEmpty())
                    defaultRules.add(partialOutgoingRule);
                else {
                    if (!first_added)
                        modelPart.addConjunction(conjunction);
                    else
                        first_added = false;
                    modelPart.addIdentifiedExpression(partialOutgoingRule)
                            .addRuleConditional(Conditional.IF)
                            .addIdentifiedExpression(ruleModel);
                    conditionedRules.add(partialOutgoingRule);
                }
                nhood.partialRuleSource.addAll(Arrays.asList(subject.getKey(), activityOut.getKey()));
            }
        }
        // Add default conditions
        if (!defaultRules.isEmpty()) {
            first_added = true;
            if (!conditionedRules.isEmpty())
                modelPart.addUnidentifiedText(",").addRuleConditional(Conditional.OTHERWISE);
            for (SBVRExpressionModel default_: defaultRules) {
                if (!first_added)
                    modelPart.addConjunction(Conjunction.OR);
                else
                    first_added = false;
                modelPart.addIdentifiedExpression(default_);
            }
        }
        if (!modelPart.isEmpty())
            nhood.partialRule.addRuleConditional(Conditional.IF_NOT)
                    .addUnidentifiedText("(").addIdentifiedExpression(modelPart).addUnidentifiedText(")");
    }


    private void extractComplexRule(Element el) {
        if (!isActivityElement(el) && !isEventElement(el))
            return;
        Map<Element, String> subjects = getSubjectNames(el);
        Collection<ActivityEdge> incomingEdges = ((ActivityNode) el).getIncoming();
        if (incomingEdges.isEmpty())
            return;
        Set<ActivityEdge> incomingGateways = incomingEdges.stream().filter(n -> isGatewayElement(n.getSource())).collect(Collectors.toSet());
        // If task is connected only with other tasks, T1 rule will extract such rules
        if (incomingGateways.isEmpty())
            return;
        for (Entry<Element, String> subject : subjects.entrySet()) {
            for (ActivityEdge edge: incomingGateways) {
                ControlNode gateway = (ControlNode) edge.getSource();
                GatewayNeighborhood nhood = gatewayNeighborhoods.get(gateway);
                Set<ControlNode> boundaryGateways = new HashSet<>();
                getAllBoundaryGateways(nhood, boundaryGateways);
                for (ControlNode boundaryGate: boundaryGateways) {
                    SBVRExpressionModel rule = new SBVRExpressionModel()
                            .addRuleExpression(RuleType.OBLIGATION);
                    List<Object> sources = new ArrayList<>(Arrays.asList(el, subject.getKey()));
                    rule = addActivity(rule, (ActivityNode)el, subject.getValue());
                    ActivityNodeNeighborhood activityNhood = nhood.outgoingActivities.get(el);
                    if (activityNhood == null)
                        continue;
                    Map<ActivityEdge, String> conditionsOut = activityNhood.incomingConditions.get(gateway);
                    rule.addIdentifiedExpression(createMultipleConditions(conditionsOut, sources));
                    GatewayNeighborhood boundaryNhood = gatewayNeighborhoods2.get(boundaryGate);
                    if (boundaryGate.equals(gateway))
                        continue;   // Apply Rule T3
                    createPartialRules(boundaryNhood, gateway, el);
                    rule.addIdentifiedExpression(boundaryNhood.partialRule);
                    sources.addAll(boundaryNhood.partialRuleSource);
                    for (ActivityNode incTask: boundaryNhood.incomingActivities.keySet()) {
                        SBVRExpressionModel ruleCopy = rule.clone();
                        List<Object> sourcesCopy = new ArrayList<>(sources);
                        ruleCopy.addUnidentifiedText(",").addRuleConditional(Conditional.AFTER);
                        ruleCopy = addActivity(ruleCopy, incTask, subject.getValue());
                        sourcesCopy.addAll(Arrays.asList(incTask, subject.getKey()));
                        MagicDrawSourceEntry src = new MagicDrawSourceEntry(sourcesCopy);
                        br_candidates.add(src, ruleCopy);
                        br_candidates.setAutomaticExtraction(src);
                    }
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
