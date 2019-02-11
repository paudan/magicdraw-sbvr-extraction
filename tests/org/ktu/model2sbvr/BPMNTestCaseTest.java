package org.ktu.model2sbvr;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.Finder;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.OpaqueAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdcompleteactions.AcceptEventAction;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import org.junit.Test;
import org.ktu.model2sbvr.extract.BpmnSBVRExtractor;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SourceEntry;
import org.ktu.model2sbvr.tests.ExtractionTestCase;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class BPMNTestCaseTest extends ExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "bpmn", "test_cases.mdzip");
    }

    private Package getRootPackage() {
        if (project == null)
            project = Application.getInstance().getProject();
        assertNotNull(project);
        return project.getPrimaryModel();
    }

    private BpmnSBVRExtractor getExtractor(String modelName) {
        Package model = getRootPackage();
        assertNotNull(model);
        Collection<DiagramPresentationElement> diagrams = BpmnSBVRExtractor.getBPMNDiagrams(model);
        Optional<DiagramPresentationElement> diagramOpt = diagrams.stream()
                .filter(n -> n.getName() != null && n.getName().compareToIgnoreCase(modelName) == 0).findFirst();
        assertTrue(diagramOpt.isPresent());
        DiagramPresentationElement diagram = diagramOpt.get();
        BpmnSBVRExtractor extractor = new BpmnSBVRExtractor(diagram, false, false);
        extractor.extractAll();
        return extractor;
    }

    @Test
    public void testRuleT1Extraction() {
        BpmnSBVRExtractor extractor = getExtractor("Model1");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        printExtractorOutput(gcObjects);
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        printExtractorOutput(vcObjects);
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT2Extraction_PaperExample() {
        BpmnSBVRExtractor extractor = getExtractor("PaperExampleT2");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(5, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(4, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(2, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT2Extraction_Model2() {
        BpmnSBVRExtractor extractor = getExtractor("Model2");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(6, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(5, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(2, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT3Extraction_Model3() {
        BpmnSBVRExtractor extractor = getExtractor("Model3");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(6, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(5, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(2, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT5Extraction_Model5() {
        BpmnSBVRExtractor extractor = getExtractor("Model5");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(2, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(2, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(8, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testGetBoundaryEvents_Model5() {
        Project project = Application.getInstance().getProject();
        assertNotNull(project);
        Profile profile = PluginUtilities.getBPMNProfile(project);
        Stereotype taskStereotype = StereotypesHelper.getStereotype(project, "Task", profile);
        Package root = getRootPackage();
        Element pkg = Finder.byName().find(root, Activity.class, "Model5");
        assertNotNull(pkg);
        Element task = Finder.byNameRecursively().find(root, OpaqueAction.class, "perform task");
        assertNotNull(task);
        List<Element> boundaryElements = BPMNHelper.getBoundaryEventRefs(task, taskStereotype);
        assertEquals(3, boundaryElements.size());
    }

    @Test
    public void testGetStereotypeProperties() {
        Project project = Application.getInstance().getProject();
        assertNotNull(project);
        Profile profile = PluginUtilities.getBPMNProfile(project);
        Package root = getRootPackage();
        Element boundary = Finder.byNameRecursively().find(root, AcceptEventAction.class, "e-mail is sent");
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "MessageBoundaryEvent", profile);
        assertNotNull(stereotype);
        List<Property> stereotypeProperties = StereotypesHelper.getPropertiesWithDerivedOrdered(stereotype);
        assertNotNull(stereotypeProperties);
        for (Property prop: stereotypeProperties) {
            List<Object> result = StereotypesHelper.getStereotypePropertyValue(boundary, stereotype, prop);
            List<String> repr = result.stream().map(Object::toString).collect(Collectors.toList());
            System.out.println(prop.getName() + ", value: " + String.join(",", repr));
        }
        List<Object> cancelActivity = StereotypesHelper.getStereotypePropertyValue(boundary, stereotype, "cancelActivity");
        assertEquals(1, cancelActivity.size());
        Object value = cancelActivity.get(0);
        assertTrue(value instanceof EnumerationLiteral);
        EnumerationLiteral valueCasted = (EnumerationLiteral) value;
        System.out.println(valueCasted.getName());
        System.out.println(valueCasted.getOwnedElement());
    }
}
