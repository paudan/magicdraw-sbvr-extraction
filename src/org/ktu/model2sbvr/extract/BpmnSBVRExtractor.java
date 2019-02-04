package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityFinalNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.CentralBufferNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.statemachines.mdbehaviorstatemachines.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
        for (String st : stereotypes)
            if (StereotypesHelper.hasStereotype(el, StereotypesHelper.getStereotype(project, st, bpmnProfile)))
                return true;
        return false;
    }

    private boolean isTaskElement(Element el) {
        return hasAnyStereotype(el, "Task", "ServiceTask", "SendTask", "ReceiveTask", "UserTask", "ManualTask", "BusinessRuleTask", "ScriptTask");
    }

    private boolean isStartEventElement(Element el) {
        return el.getClassType().equals(InitialNode.class)
                && hasAnyStereotype(el, "StartEvent", "MessageStartEvent", "TimerStartEvent", "ErrorStartEvent", "EscalationStartEvent",
                        "CompensationStartEvent", "ConditionalStartEvent", "SignalStartEvent", "MultipleStartEvent", "ParallelMultipleStartEvent");
    }

    private boolean isEndEventElement(Element el) {
        return el.getClassType().equals(ActivityFinalNode.class)
                && hasAnyStereotype(el, "EndEvent", "MessageEndEvent", "ErrorEndEvent", "EscalationEndEvent",
                        "CompensationEndEvent", "SignalEndEvent", "MultipleEndEvent", "TerminateEndEvent");
    }

    private boolean isDataObject(Element el) {
        return el.getClassType().equals(CentralBufferNode.class) && hasAnyStereotype(el, "DataObject", "DataStore", "DataInput", "DataOutput");
    }

    private boolean isResourceElement(Element el) {
        return hasAnyStereotype(el, "Resource", "PartnerRole");
    }

    private boolean isGatewayOfType(Element el, String stereotype) {
        return el.getClassType().equals(ForkNode.class) && BPMNHelper.getGatewayStereotype(el).getName().compareToIgnoreCase(stereotype) == 0;
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
                gc_candidates.setManualExtraction(new SourceEntry(Arrays.asList((Object) el), Arrays.asList(getProperName(el))));
            else if (el.getClassType().equals(Comment.class))
                if (extractElementText(el) != null)
                    gc_candidates.setManualExtraction(new SourceEntry(Arrays.asList((Object) el), Arrays.asList(getProperName(el))));
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
                vc_candidates.setManualExtraction(new SourceEntry(Arrays.asList((Object) el), Arrays.asList(getProperName(el))));
        }
    }

    @Override
    protected void extractBusinessRuleCandidates() {
        Iterator<Element> iterator = candidateElements.iterator();
        while (iterator.hasNext()) {
            Element el = iterator.next();
            // Rule T4
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
                    // It's a task
                    if (((ActivityNode) outgoingTask).hasInPartition())
                        for (ActivityPartition part : ((ActivityNode) outgoingTask).getInPartition()) {
                            String subject = extractElementText(part);
                            if (subject == null)
                                continue;
                            SBVRExpressionModel candidate = new SBVRExpressionModel()
                                    .addRuleExpression(SBVRExpressionModel.RuleType.OBLIGATION)
                                    .addUnidentifiedText(",")
                                    .addAfterExpression();
                            boolean added_first = true;
                            for (ActivityNode outTask : incomingTasks) {
                                String outTaskText = extractElementText(outTask);
                                SBVRExpressionModel binary2 = getVerbConcept(subject + " " + outTaskText);
                                if (!added_first)
                                    candidate = candidate.addAndExpression();
                                else
                                    added_first = false;
                                candidate = binary2 != null ? candidate.addIdentifiedExpression(binary2) : candidate.addUnidentifiedText(subject + " " + outTaskText);
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

}
