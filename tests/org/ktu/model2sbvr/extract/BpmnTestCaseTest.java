package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.cbm.BPMNHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.Finder;
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
import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SourceEntry;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class BpmnTestCaseTest extends BpmnExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "bpmn", "test_cases.mdzip");
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
    public void testRuleT4Extraction_Model4() {
        BpmnSBVRExtractor extractor = getExtractor("Model4");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(2, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(1, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(3, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT5Extraction_Model5() {
        BpmnSBVRExtractor extractor = getExtractor("Model5");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(3, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(2, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(8, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT7Extraction_Model6a() {
        BpmnSBVRExtractor extractor = getExtractor("Model6a");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(6, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(2, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(2, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT7Extraction_Model6b() {
        BpmnSBVRExtractor extractor = getExtractor("Model6b");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(6, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(2, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(3, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT8Extraction_Model7a() {
        BpmnSBVRExtractor extractor = getExtractor("Model7a");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(6, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(2, vcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        assertEquals(2, brObjects.size());
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT9Extraction_Model8() {
        BpmnSBVRExtractor extractor = getExtractor("Model8");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        assertEquals(10, gcObjects.size());
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        assertEquals(4, vcObjects.size());
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
