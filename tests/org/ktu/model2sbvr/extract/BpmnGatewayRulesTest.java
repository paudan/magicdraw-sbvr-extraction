package org.ktu.model2sbvr.extract;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SourceEntry;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class BpmnGatewayRulesTest extends BpmnExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "bpmn", "gateway_rules.mdzip");
    }

    @Test
    public void testTestModel1Extraction() {
        BpmnSBVRExtractor extractor = getExtractor("TestModel1");
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
        List<String> outputs = getOutputsAsStrings(brObjects);
        String[] expected = {
                "It is obligatory that provider apply VIP discount if customer is VIP customer, after provider register order",
                "It is obligatory that provider process order, after provider apply VIP discount if customer is VIP customer, after provider register order",
                "It is obligatory that provider package order, after provider order additional packaging if additional packaging is required, otherwise provider process order",
                "It is obligatory that provider apply VIP discount if customer is VIP customer",
                "It is obligatory that provider order additional packaging if additional packaging is required, otherwise provider process order",
                "It is obligatory that provider package order, after provider order additional packaging if additional packaging is required, otherwise provider process order",
                "It is obligatory that provider apply VIP discount if customer is VIP customer, after provider register order",
                "It is obligatory that provider ship order after provider package order",
                "It is obligatory that provider order additional packaging if additional packaging is required, after provider apply VIP discount if customer is VIP customer, after provider register order"
        };
        MatcherAssert.assertThat(outputs, Matchers.containsInAnyOrder(expected));
    }

    @Test
    public void testTestModel2Extraction() {
        BpmnSBVRExtractor extractor = getExtractor("TestModel2");
        Map<SourceEntry, ConceptExtractionEntry> brObjects = extractor.getBRCandidateModel().getDataset();
        printExtractorOutput(brObjects);
    }

}
