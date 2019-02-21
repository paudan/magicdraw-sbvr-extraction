package org.ktu.model2sbvr.extract;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ControlNode;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import org.junit.Test;
import org.ktu.model2sbvr.extract.BpmnSBVRExtractor.GatewayNeighborhood;
import org.ktu.model2sbvr.tests.ExtractionTestCase;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

public class BpmnExtractorInternalsTest extends ExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "bpmn", "gateway_rules.mdzip");
    }

    private Package getRootPackage() {
        if (project == null)
            project = Application.getInstance().getProject();
        assertNotNull(project);
        return project.getPrimaryModel();
    }

    private BpmnSBVRExtractor getBpmnExtractor(String name) {
        Package model = getRootPackage();
        assertNotNull(model);
        Collection<DiagramPresentationElement> diagrams = BpmnSBVRExtractor.getBPMNDiagrams(model);
        Optional<DiagramPresentationElement> diagramOpt = diagrams.stream()
                .filter(n -> n.getName() != null && n.getName().compareToIgnoreCase(name) == 0).findFirst();
        assertTrue(diagramOpt.isPresent());
        BpmnSBVRExtractor extractor = new BpmnSBVRExtractor(diagramOpt.get(), false, false);
        extractor.extractAll();
        return extractor;
    }

    @Test
    public void testInternalStructures() {
        BpmnSBVRExtractor extractor = getBpmnExtractor("TestModel1");
        Collection<Element> elements = extractor.getExtractedDiagramElements();
        for (Element el: elements)
            if (extractor.isGatewayElement(el)){
                System.out.println();
                GatewayNeighborhood tuple = extractor.new GatewayNeighborhood((ControlNode) el);
                System.out.println(tuple);
            }

    }
}
