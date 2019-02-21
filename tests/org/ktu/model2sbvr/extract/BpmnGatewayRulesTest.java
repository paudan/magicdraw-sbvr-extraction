package org.ktu.model2sbvr.extract;

import org.junit.Test;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SourceEntry;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class BpmnGatewayRulesTest extends BpmnExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "bpmn", "gateway_rules.mdzip");
    }

    @Test
    public void testTestModel1Extraction() {
        BpmnSBVRExtractor extractor = getExtractor("TestModel1");
        Map<SourceEntry, ConceptExtractionEntry> gcObjects = extractor.getGCCandidateModel().getDataset();
        printExtractorOutput(gcObjects);
        Map<SourceEntry, ConceptExtractionEntry> vcObjects = extractor.getVCCandidateModel().getDataset();
        printExtractorOutput(vcObjects);
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
    }

}
