package org.ktu.model2sbvr;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.DiagramTypeConstants;
import com.nomagic.magicdraw.uml.ElementFinder;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.reflect.VisitorContext;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.ktu.model2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.model2sbvr.extract.FactDiagramGenerator;
import org.ktu.model2sbvr.extract.UseCaseSBVRExtractor;
import org.ktu.model2sbvr.models.FilteredConceptModel;
import org.ktu.model2sbvr.tests.ExtractionTestCase;
import vepsem.PluginUtils;

public class DiagramExtractionTest extends ExtractionTestCase {

    protected Map<String, Map<String, Map<String, Integer[]>>> stats = new HashMap<>();

    public DiagramExtractionTest() {
        super();
        // Remove any existing log and statistics files
        try {
            Path path = FileSystems.getDefault().getPath("business_vocabulary_diagram.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("model_vocabulary_diagram.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_rules_diagram.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_vocabulary_diagram_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("model_vocabulary_diagram_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_rules_diagram_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("stats_diagram.csv");
            Files.delete(path);

        } catch (NoSuchFileException | DirectoryNotEmptyException x) {
        } catch (IOException x) {
            System.err.println(x);
        }
        try (PrintWriter statswriter = new PrintWriter(new BufferedWriter(new FileWriter("stats_diagram.csv")))) {
            statswriter.println("Model name;Diagram name;BV general concepts;BV verb concepts;BV unary concepts;MV general concepts;"
                    + "MV verb concepts;MV unary concepts;Obligation rules;Permission rules;Structural rules;Strict transformation;");
        } catch (IOException ex) {
            Logger.getLogger(DiagramExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    protected Path getFilename() {
        return Paths.get("tests", "resources", "UseCase test diagrams.mdzip");
    }

    protected void runExtraction(final String packageName, Boolean strict) throws IOException {
        boolean extractModel = true;
        if (project == null)
            project = Application.getInstance().getProject();
        assertNotNull(project);
        Package model = (Package) ModelHelper.findInParent(project.getModel(), packageName, Package.class, true);
        assertNotNull(model);
        PluginUtilities.addSBVRProfiles();
        Collection<DiagramPresentationElement> diagrams = getUseCaseDiagrams(model);
        for (DiagramPresentationElement diagram : diagrams) {
            if (diagram.getDiagram() == null)
                continue;
            String diagramName = diagram.getDiagram().getName();
            StatsCollectionVisitor pkgvisitor = new StatsCollectionVisitor(strict, stats, packageName, "diagram");
            pkgvisitor.setDiagramName(diagramName);
            AbstractSBVRExtractor extractor = new UseCaseSBVRExtractor(diagram, strict, extractModel);
            extractor.setGCCandidateModel(new FilteredConceptModel());
            extractor.setVCCandidateModel(new FilteredConceptModel());
            extractor.setBRCandidateModel(new FilteredConceptModel());
            extractor.extractAll();
            FilteredConceptModel gcCandidates;
            FilteredConceptModel vcCandidates, brCandidates;
            gcCandidates = (FilteredConceptModel) extractor.getGCCandidateModel();
            vcCandidates = (FilteredConceptModel) extractor.getVCCandidateModel();
            brCandidates = (FilteredConceptModel) extractor.getBRCandidateModel();
            FactDiagramGenerator generator = new FactDiagramGenerator(diagramName, extractModel);
            generator.setGCCandidates(gcCandidates);
            generator.setVCCandidates(vcCandidates);
            generator.setBRCandidates(brCandidates);
            generator.generate();
            Package targetPackage = generator.getTargetPackage();
            Package bvPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtils.SBVR_VOCAB_PACKAGE_NAME, true);
            Package mvPackage = null;
            if (extractModel)
                mvPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtilities.SBVR_MODELVOC_PACKAGE_NAME, true);
            Package rulesPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtils.SBVR_RULES_PACKAGE_NAME, true);
            if (bvPackage != null)
                pkgvisitor.visitPackage(bvPackage, new VisitorContext());
            if (mvPackage != null)
                pkgvisitor.visitPackage(mvPackage, new VisitorContext());
            if (rulesPackage != null)
                pkgvisitor.visitPackage(rulesPackage, new VisitorContext());
            try (PrintWriter statswriter = new PrintWriter(new BufferedWriter(new FileWriter("stats_diagram.csv", true)))) {
                statswriter.print(packageName + ";");
                statswriter.print(diagramName + ";");
                Map<String, Map<String, Integer[]>> modelEntry = stats.get(packageName);
                if (modelEntry == null)
                    return;
                Map<String, Integer[]> dEntry = modelEntry.get(diagramName);
                if (dEntry == null)
                    continue;
                Integer[] bvStats = dEntry.get("SBVR Business Vocabulary");
                for (int i = 0; i < 3; i++)
                    statswriter.print(bvStats != null ? bvStats[i] : "" + ";");
                Integer[] mvStats = dEntry.get("SBVR Model Vocabulary");
                for (int i = 0; i < 3; i++)
                    statswriter.print(mvStats != null ? mvStats[i] : "" + ";");
                Integer[] brStats = dEntry.get("SBVR Business Rules");
                for (int i = 0; i < 3; i++)
                    statswriter.print(brStats != null ? brStats[i] : "" + ";");
                statswriter.print(strict.toString() + ";");
                statswriter.println();
            } catch (IOException ex) {
                Logger.getLogger(DiagramExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (sessionManager.isSessionCreated())
                sessionManager.cancelSession();
        }
        endTest();
    }

    private Collection<DiagramPresentationElement> getUseCaseDiagrams(Package model) {
        Collection<DiagramPresentationElement> diagrams = new HashSet<>();
        final Project proj = Application.getInstance().getProject();
        if (proj == null)
            return diagrams;
        for (Diagram diag : model.getOwnedDiagram()) {
            DiagramPresentationElement pres = proj.getDiagram(diag);
            if (pres != null && pres.getDiagramType().getType().compareToIgnoreCase(DiagramTypeConstants.UML_USECASE_DIAGRAM) == 0)
                diagrams.add(pres);
        }
        Collection<Package> packages = model.getNestedPackage();
        while (!packages.isEmpty()) {
            Set<Package> newPackages = new HashSet<>();
            for (Package pkg : packages) {
                newPackages.addAll(pkg.getNestedPackage());
                for (Diagram diag : pkg.getOwnedDiagram()) {
                    DiagramPresentationElement pres = proj.getDiagram(diag);
                    if (pres != null && pres.getDiagramType().getType().compareToIgnoreCase(DiagramTypeConstants.UML_USECASE_DIAGRAM) == 0)
                        diagrams.add(pres);
                }
            }
            packages = newPackages;
        }
        return diagrams;
    }

    @Test
    public void testCase1() {
        try {
            runExtraction("Case 1", false);
        } catch (IOException ex) {
            Logger.getLogger(DiagramExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase2() {
        try {
            runExtraction("Case 2", false);
        } catch (IOException ex) {
            Logger.getLogger(DiagramExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

}
