package org.ktu.model2sbvr.util;

import com.nomagic.magicdraw.uml.DiagramTypeConstants;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.junit.Test;
import org.ktu.model2sbvr.extract.UseCaseSBVRExtractor;
import org.ktu.model2sbvr.tests.ExtractionTestCase;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;


public class TestGenerateTestFile extends ExtractionTestCase {

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "usecase", "atcs.mdzip");
    }

    private DiagramPresentationElement getUseCaseDiagram(String name) {
        Collection<DiagramPresentationElement> diagrams = project.getDiagrams(DiagramTypeConstants.UML_USECASE_DIAGRAM);
        for (DiagramPresentationElement diag: diagrams)
            if (diag.getDiagram().getName().equalsIgnoreCase(name))
                return diag;
        return null;
    }

    private void generateFile(String diagramName) {
        DiagramPresentationElement diagram = getUseCaseDiagram(diagramName);
        assertNotNull(diagram);
        UseCaseSBVRExtractor extractor = new UseCaseSBVRExtractor(diagram, false, false);
        extractor.setExtractedAuto(false);
        extractor.extractAll();
        String description = diagram.getDiagram().getName() + " diagram, taken from http://www.eso.org/pwp/tcsmgr/vlti/atcsdoc/Model/UseCases";
        boolean normalize = false;
        TestFileGenerator generator = new TestFileGenerator(extractor, description, normalize);
        generator.writeFile(diagramName.toLowerCase().replaceAll(" ", "_") + ".xml");
    }

    @Test
    public void testGenerateGuideStarTestFile() {
        generateFile("Guide Star");
    }

    @Test
    public void testGenerateRequiredTestFiles() {
        String[] models = {"Guide Star", "Active Optics", "Air Conditioning", "Autoguiding", "Enclosure", "Field Acquisition Sensor",
                "Field Stabilization", "M2 Control", "Calibrations", "Image Alignment", "Optical Adjustments",
                "Pupil Alignment", "Pointing Modelling", "Preset", "Services", };
        for (String model: models)
            generateFile(model);
    }
}
