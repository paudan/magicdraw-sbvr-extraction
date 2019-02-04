package org.ktu.model2sbvr;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.tests.MagicDrawTestCase;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.OpaqueAction;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ActivityPartition;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.ktu.model2sbvr.extract.BpmnSBVRExtractor;
import org.ktu.model2sbvr.models.AbstractConceptModel;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SourceEntry;

/**
 * @author Paulius Danenas, 2019
 */
public class BPMNExtractionTest extends MagicDrawTestCase {

    protected static Project project = null;
    protected Path filename = Paths.get("tests", "resources", "bpmn", "vepsem_md19.mdzip");
    protected SessionManager sessionManager = SessionManager.getInstance();

    @Override
    protected void setUpTest() throws Exception {
        super.setUpTest();
        setSkipMemoryTest(true);
        setMemoryTestReady(false);
        if (filename != null && (project == null || !project.isLoaded()))
            project = openProject(filename.normalize().toUri().getPath());
        if (project == null || !project.isLoaded())
            throw new IOException("File " + filename + " was not opened or could not be found!");
        if (sessionManager.isSessionCreated())
            sessionManager.closeSession();
        sessionManager.createSession("Perform tests");
    }

    protected void endTest() {
        if (sessionManager.isSessionCreated())
            sessionManager.cancelSession();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Logger.getLogger(BPMNExtractionTest.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @Test
    public void testVepsemModel() {
        Package model = getVepsemPackage();
        assertNotNull(model);
        Collection<DiagramPresentationElement> diagrams = new HashSet<>();
        diagrams = this.getBPMNDiagrams(model, diagrams);
        assertEquals(1, diagrams.size());
        ActivityPartition branch = getOrganizationProcessModel(model);
        assertNotNull(branch);
    }
    
    @Test
    public void testSBVRExtraction() {
        Package model = getVepsemPackage();
        assertNotNull(model);
        Collection<DiagramPresentationElement> diagrams = new HashSet<>();
        diagrams = this.getBPMNDiagrams(model, diagrams);
        assertEquals(1, diagrams.size());  // There is only one BPMN process diagram
        DiagramPresentationElement diagram = diagrams.stream().findFirst().get();
        BpmnSBVRExtractor extractor = new BpmnSBVRExtractor(diagram, false, false);
        extractor.createGeneralConceptCandidates();
        extractor.createVerbConceptCandidates();
        AbstractConceptModel gcModel = extractor.getGCCandidateModel();
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = gcModel.getDataset();
        //printExtractorOutput(gcObjects);
        assertEquals(13, gcObjects.size());
        AbstractConceptModel vcModel = extractor.getVCCandidateModel();
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = vcModel.getDataset();
        assertEquals(11, vcObjects.size());
        //printExtractorOutput(vcObjects);
        extractor.createBusinessRuleCandidates();
        AbstractConceptModel brModel = extractor.getBRCandidateModel();
        Map<SourceEntry, ConceptExtractionEntry> brObjects = brModel.getDataset();
        assertEquals(3, brObjects.size());
        //printExtractorOutput(brObjects);
    }

    private void printExtractorOutput(Map<SourceEntry, ConceptExtractionEntry> objects) {
        for(Entry<SourceEntry, ConceptExtractionEntry> item: objects.entrySet()) {
            List<String> outputs = new ArrayList<>();
            for (SBVRExpressionModel sbvr: item.getValue().getCandidates())
                outputs.add(sbvr.toString());
            System.out.println("Source: " + String.join(",", item.getKey().getSourceNames()) + " -> output: " + String.join(",", outputs));
        }
    }

    @Test
    public void testRuleExtractionT4() {
        Package model = getVepsemPackage();
        assertNotNull(model);
        ActivityPartition branch = getOrganizationProcessModel(model);
        assertNotNull(branch);
        InitialNode node = null;
        for (ActivityNode el : branch.getNode())
            if (el instanceof InitialNode && el.getName().compareToIgnoreCase("car booking request approval is_started") == 0)
                node = (InitialNode) el;
        assertNotNull(node);
        ActivityEdge seqFlow = node.getOutgoing().stream().findFirst().orElse(null);
        assertNotNull(seqFlow);
        ActivityNode parNode = seqFlow.getTarget();
        assertNotNull(parNode);
        assertTrue(parNode instanceof ForkNode);
        assertEquals(0, BPMNHelper.getGatewayStereotype(parNode).getName().compareToIgnoreCase("ParallelGateway"));
        List<ActivityNode> tasks = new ArrayList<>();
        List<String> vcTasks = new ArrayList<>();
        for (ActivityEdge edge : parNode.getOutgoing()) {
            ActivityNode task = edge.getTarget();
            assertTrue(task instanceof OpaqueAction);
            tasks.add(task);
            vcTasks.add(branch.getName() + " " + task.getName());
        }
        Collections.sort(vcTasks);  // for reproducibility
        String rule1 = String.format("It is obligatory that, after %s, %s", node.getName(), String.join(" and ", vcTasks));
        assertEquals("It is obligatory that, after car booking request approval is_started, branch check renter credit card and branch check renter driving license", rule1);
        // Check if tasks have sequence flows to parallel joins, and form corresponding rules
        Map<ForkNode, List<String>> vcJoined = new HashMap<>();
        for (ActivityNode task : tasks) {
            Collection<ActivityEdge> edges = task.getOutgoing();
            assertTrue(edges.size() > 0);
            for (ActivityEdge edge : edges) {
                ActivityNode gateNode = edge.getTarget();
                if (gateNode instanceof ForkNode && BPMNHelper.getGatewayStereotype(gateNode).getName().compareToIgnoreCase("ParallelGateway") == 0) {
                    ForkNode joinEl = (ForkNode) gateNode;
                    List<String> vcOutgoingTasks = vcJoined.get(joinEl);
                    if (vcOutgoingTasks == null) {
                        vcOutgoingTasks = new ArrayList<>();
                        vcJoined.put(joinEl, vcOutgoingTasks);
                    }  
                    vcOutgoingTasks.add(branch.getName() + " " + task.getName());
                }
            }
        }
        assertEquals(1, vcJoined.size());     // Should be single ForkNode here
        Set<String> rules = new TreeSet<>();
        for (Entry<ForkNode, List<String>> join : vcJoined.entrySet()) {
            Collection<ActivityEdge> edges = join.getKey().getOutgoing();
            assertTrue(edges.size() > 0);
            for (ActivityEdge edge : edges)
                if (edge.getTarget() instanceof OpaqueAction) {
                    rules.add(String.format("It is obligatory that %s, after %s", 
                            branch.getName() + " " + edge.getTarget().getName(), 
                            String.join(" and ", join.getValue())));
                }
        }
        assertEquals(1, rules.size());
        assertEquals("It is obligatory that branch validate car booking request, after branch check renter driving license and branch check renter credit card", 
                rules.stream().findFirst().get());
        for (String rule: rules)
            System.out.println(rule);
    }

    private Package getVepsemPackage() {
        if (project == null)
            project = Application.getInstance().getProject();
        assertNotNull(project);
        return (Package) ModelHelper.findInParent(project.getModel(), "VEPSEM", Package.class, true);
    }

    private ActivityPartition getOrganizationProcessModel(Package model) {
        Collection<Activity> processes = BPMNHelper.getBPMNProcesses(model);
        assertEquals(1, processes.size());
        Activity process = processes.stream().findFirst().get();
        Collection<? extends BaseElement> elements = BPMNHelper.getBpmnElements(process);
        ActivityPartition branch = null;
        for (BaseElement el : elements)
            if (el instanceof ActivityPartition && ((ActivityPartition) el).getName().compareToIgnoreCase("branch") == 0)
                branch = (ActivityPartition) el;
        return branch;
    }

    private Collection<DiagramPresentationElement> getBPMNDiagrams(Package model, Collection<DiagramPresentationElement> diagrams) {
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

}