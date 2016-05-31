package org.ktu.transformations.uml2sbvr;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.tests.MagicDrawTestCase;
import com.nomagic.magicdraw.uml.Visitor;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.ktu.transformations.uml2sbvr.extract.UseCaseSBVRExtractor;
import org.ktu.transformations.uml2sbvr.transform.ExtractionRunnable;
import org.ktu.transformations.uml2sbvr.transform.ExtractionWorkflows;

/**
 *
 * @author Paulius
 */
public class AutoExtractionTest extends MagicDrawTestCase {

    protected static Project project = null;
    protected String filename;
    protected SessionManager sessionManager = SessionManager.getInstance();
    protected Map<String, Map<String, Map<String, Integer[]>>> stats;

    public AutoExtractionTest() {
        super();
        filename = "tests\\resources\\UseCase test diagrams.mdzip";
        stats = new HashMap<>();
        // Remove any existing log and statistics files
        try {
            Path path = FileSystems.getDefault().getPath("business_vocabulary_auto.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("model_vocabulary_auto.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_rules_auto.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_vocabulary_auto_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("model_vocabulary_auto_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("business_rules_auto_strict.txt");
            Files.delete(path);
            path = FileSystems.getDefault().getPath("stats.csv");
            Files.delete(path);
        } catch (NoSuchFileException | DirectoryNotEmptyException x) {
        } catch (IOException x) {
            System.err.println(x);
        }
        try (PrintWriter statswriter = new PrintWriter(new BufferedWriter(new FileWriter("stats.csv", false)))) {
            statswriter.println("Model name;BV general concepts;BV verb concepts;BV unary concepts;MV general concepts;"
                    + "MV verb concepts;MV unary concepts;Obligation rules;Permission rules;Structural rules;Strict transformation;");
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void setUpTest() throws Exception {
        super.setUpTest();
        setSkipMemoryTest(true);
        setMemoryTestReady(false);
        if (filename == null)
            throw new IOException("Filename of testing project is not specified!");
        if (filename != null && (project == null || !project.isLoaded()))
            project = openProject(Paths.get(filename).normalize().toUri().getPath());
        if (project == null || !project.isLoaded())
            throw new IOException("File " + filename + " was not opened or could not be found!");
        if (sessionManager.isSessionCreated())
            sessionManager.closeSession();
        sessionManager.createSession("Perform tests");
    }

    protected void endTest() {
        if (sessionManager.isSessionCreated())
            sessionManager.cancelSession();
        // Try to remove temporary comment element once more to ensure it is absent
        sessionManager.createSession("Remove temporary element");
        ExtractionWorkflows.removeMarkingCommentElement();
        if (sessionManager.isSessionCreated())
            sessionManager.closeSession();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Logger.getLogger(DiagramExtractionTest.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    protected void runExtraction(String packageName, Boolean strict) throws IOException {
        boolean extractModel = true;
        if (project == null)
            project = Application.getInstance().getProject();
        assertNotNull(project);
        Package model = (Package) ModelHelper.findInParent(project.getModel(), packageName, Package.class, true);
        assertNotNull(model);
        PluginUtilities.addSBVRProfiles();
        Visitor pkgvisitor = new StatsCollectionVisitor(strict, stats, packageName, "auto");
        ExtractionRunnable runnable = ExtractionRunnable.getInstance();
        runnable.setVisitors(Arrays.asList(pkgvisitor));
        String transform = strict ? PluginUtilities.UCD2SBVR_BV_AND_MV_STRICT_QVT : PluginUtilities.UCD2SBVR_BV_AND_MV_QVT;
        runnable.lockedExecution(project, model, transform, "overwrite", true, true, extractModel,
                false, new UseCaseSBVRExtractor(model, strict, extractModel));
        try (PrintWriter statswriter = new PrintWriter(new BufferedWriter(new FileWriter("stats.csv", true)))) {
            Map<String, Map<String, Integer[]>> modelEntry = stats.get(packageName);
            if (modelEntry == null)
                return;
            Map<String, Integer[]> dEntry = modelEntry.get(null);
            if (dEntry == null)
                return;
            statswriter.print(packageName + ";");
            Integer[] bvStats = dEntry.get("SBVR Business Vocabulary");
            for (int i = 0; i < 3; i++)
                statswriter.print((bvStats != null ? bvStats[i] : "") + ";");
            Integer[] mvStats = dEntry.get("SBVR Model Vocabulary");
            for (int i = 0; i < 3; i++)
                statswriter.print((mvStats != null ? mvStats[i] : "") + ";");
            Integer[] brStats = dEntry.get("SBVR Business Rules");
            for (int i = 0; i < 3; i++)
                statswriter.print((brStats != null ? brStats[i] : "") + ";");
            statswriter.print(strict.toString() + ";");
            statswriter.println();
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        stats = ((StatsCollectionVisitor)pkgvisitor).getStats();
        endTest();
    }

    @Test
    public void testCase1() {
        try {
            runExtraction("Case 1", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase2() {
        try {
            runExtraction("Case 2", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase3() {
        try {
            runExtraction("Case 3", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase4() {
        try {
            runExtraction("Case 4", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase5() {
        try {
            runExtraction("Case 5", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase6() {
        try {
            runExtraction("Case 6", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase7() {
        try {
            runExtraction("Case 7", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase8() {
        try {
            runExtraction("Case 8", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase9() {
        try {
            runExtraction("Case 9", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase10() {
        try {
            runExtraction("Case 10", false);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase1Strict() {
        try {
            runExtraction("Case 1", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase2Strict() {
        try {
            runExtraction("Case 2", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase3Strict() {
        try {
            runExtraction("Case 3", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase4Strict() {
        try {
            runExtraction("Case 4", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase5Strict() {
        try {
            runExtraction("Case 5", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase6Strict() {
        try {
            runExtraction("Case 6", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase7Strict() {
        try {
            runExtraction("Case 7", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase8Strict() {
        try {
            runExtraction("Case 8", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase9Strict() {
        try {
            runExtraction("Case 9", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    @Test
    public void testCase10Strict() {
        try {
            runExtraction("Case 10", true);
        } catch (IOException ex) {
            Logger.getLogger(AutoExtractionTest.class.getName()).log(Level.WARNING, null, ex);
        }
    }
}
