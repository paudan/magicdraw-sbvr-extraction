package org.ktu.model2sbvr;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import org.junit.Test;
import org.ktu.model2sbvr.extract.BpmnSBVRExtractor;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SourceEntry;
import org.ktu.model2sbvr.tests.ExtractionTestCase;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;


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
        printExtractorOutput(gcObjects);
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        printExtractorOutput(vcObjects);
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
    }

    @Test
    public void testRuleT2Extraction_Model2() {
        BpmnSBVRExtractor extractor = getExtractor("Model2");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        printExtractorOutput(gcObjects);
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        printExtractorOutput(vcObjects);
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
    }
}
