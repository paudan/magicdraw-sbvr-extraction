package org.ktu.transformations.uml2sbvr.transform;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.emfuml2xmi.v4.EmfUml2XmiPlugin;
import com.nomagic.magicdraw.ui.MagicDrawProgressStatusRunner;
import com.nomagic.magicdraw.uml.Visitor;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;
import com.nomagic.ui.ProgressStatusRunner;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import org.ktu.transformations.uml2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.transformations.uml2sbvr.ui.ExtractionWizardDialog;
import vepsem.PluginUtils;

/**
 *
 * @author Paulius
 */
public class ExtractionRunnable {

    private static ExtractionRunnable INSTANCE = null;
    private Collection<Visitor> visitors = new HashSet<>();

    private ExtractionRunnable() {
    }

    public static ExtractionRunnable getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ExtractionRunnable();
        return INSTANCE;
    }

    public void setVisitors(Collection<Visitor> visitors) {
        this.visitors = visitors;
    }

    /**
     * MagicDraw UI is not locked during execution - single change affects UI.
     */
    public String nonLockedExecution(Project project, Package owner, String direction,
            String importMode, Boolean traceMode, Boolean uucdMode, Boolean importModelVoc,
            boolean showWizard, AbstractSBVRExtractor extractor) {
        ExtractionWithProgress task = createRunnable(project, owner, direction,
                importMode, traceMode, uucdMode, importModelVoc, showWizard, extractor);
        task.setVisitors(visitors);
        ProgressStatusRunner.runWithProgressStatus(task, "SBVR BV&BR extraction", true, 0);
        return task.getResultCondition();
    }

    /**
     * MagicDraw UI is locked during execution - UI is updated only when task
     * finishes (single changes does not affect UI).
     */
    public void lockedExecution(Project project, Package owner, String direction, String importMode,
            Boolean traceMode, Boolean uucdMode, Boolean importModelVoc, boolean showWizard, AbstractSBVRExtractor extractor) {
        ExtractionWithProgress task = createRunnable(project, owner,
                direction, importMode, traceMode, uucdMode, importModelVoc, showWizard, extractor);
        task.setVisitors(visitors);
        MagicDrawProgressStatusRunner.runWithProgressStatus(task, "SBVR BV&BR extraction", true, 0);
    }

    private ExtractionWithProgress createRunnable(Project project, Package owner,
            String direction, String importMode, Boolean traceMode, Boolean uucdMode,
            boolean importModelVoc, boolean showWizard, AbstractSBVRExtractor extractor) {
        return new ExtractionWithProgress(project, owner, direction, importMode,
                traceMode, uucdMode, 500, importModelVoc, showWizard, extractor);
    }

    private static class ExtractionWithProgress implements RunnableWithProgress {

        private final Project mProject;
        private final long mStepPause;
        private final String mDirection;
        private final String mImportMode;
        private final Boolean mTraceMode;
        private final Boolean mUUCDMode;
        private final Package mPackage;
        private String resultCondition;
        private final boolean importModelVoc;
        private Package createdPackage;
        private final boolean showWizard;
        private final AbstractSBVRExtractor extractor;
        private Collection<Visitor> visitors = new HashSet<>();

        public ExtractionWithProgress(Project project, Package owner, String direction,
                String importMode, Boolean traceMode, Boolean uucdMode, final long stepPause,
                boolean importModelVoc, boolean showWizard, AbstractSBVRExtractor extractor) {
            mProject = project;
            mPackage = owner;
            mStepPause = stepPause;
            mDirection = direction;
            mImportMode = importMode;
            mTraceMode = traceMode;
            resultCondition = "";
            mUUCDMode = uucdMode;
            this.importModelVoc = importModelVoc;
            createdPackage = null;
            this.showWizard = showWizard;
            this.extractor = extractor;
        }

        public void setVisitors(Collection<Visitor> visitors) {
            this.visitors = visitors;
        }

        public String getResultCondition() {
            return this.resultCondition;
        }

        @Override
        public void run(final ProgressStatus progressStatus) {
            progressStatus.setCurrent(0);
            progressStatus.setMax(0);
            progressStatus.setMax(5);
            File lockFile = null;

            step("Starting transformation...", progressStatus);

            try {
                lockFile = File.createTempFile("vepsem_export_", ".lock");
            } catch (IOException ex) {
                Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            final String lockFilestr = TransformationEngine.createFileURI_c(lockFile.getAbsolutePath()).toFileString();
            if (mProject == null)
                return;

            final String prj_name = mProject.getName().replace(" ", "_");
            if (!progressStatus.isCancel())
                step("Exporting active project model...", progressStatus);
            else {
                step("Cancelling...", progressStatus);
                resultCondition = "Cancelled";
            }
            // Mark package as current
            Comment comment = UMLFactory.eINSTANCE.createComment();
            comment.setOwningElement(mPackage);
            comment.setBody("_trans_owner_");
            try {
                EmfUml2XmiPlugin.getInstance().exportXMI(mProject,
                        TransformationEngine.createFileURI_c("file:///" + PluginUtils.getInstance().getConfigDirPath() + "export/").toFileString());
            } catch (Exception ex) {
                ExtractionWorkflows.removeMarkingCommentElement();
                Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
            step("Executing QVTo transformation...", progressStatus);
            Callable<Boolean> transformationTask = new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    while (!Thread.currentThread().isInterrupted()) {
                        System.out.println("mDirection: " + mDirection);
                        Boolean transformationResult = CustomClassLoader.executeTransformation(prj_name + ".uml", mDirection);
                        return transformationResult;
                    }
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    return null;
                }
            };

            final ExecutorService service = Executors.newSingleThreadExecutor();
            Future<Boolean> future = service.submit(transformationTask);
            boolean listen = true;
            Boolean transformationResult = null;
            while (listen) {
                if (future.isDone())
                    try {
                        transformationResult = future.get();
                        listen = false;
                    } catch (InterruptedException | ExecutionException e) {
                        ExtractionWorkflows.removeMarkingCommentElement();
                        Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, e);
                    } finally {
                        service.shutdown();
                    }
                else if (progressStatus.isCancel()) {
                    resultCondition = "Cancelled";
                    ExtractionWorkflows.removeMarkingCommentElement();
                    future.cancel(true);
                    service.shutdown();
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            String filePath = TransformationEngine.createFileURI_c("file:///"
                    + PluginUtils.getInstance().getConfigDirPath() + PluginUtils.OUTPUT_FILE_NAME).toFileString();

            if (transformationResult != null && transformationResult) {
                step("Importing transformed model...", progressStatus);
                if (mDirection.compareTo(PluginUtilities.UCD2SBVR_BV_STRICT_QVT) == 0
                        || mDirection.compareTo(PluginUtilities.UCD2SBVR_BV_QVT) == 0
                        || mDirection.compareTo(PluginUtilities.UCD2SBVR_MV_QVT) == 0
                        || mDirection.compareTo(PluginUtilities.UCD2SBVR_BV_AND_MV_QVT) == 0
                        || mDirection.compareTo(PluginUtilities.UCD2SBVR_BV_AND_MV_STRICT_QVT) == 0) {
                    ExtractionWorkflows.importModelViaEmfUml2Xmi(filePath, lockFilestr, mPackage, importModelVoc);
                    ExtractionWorkflows.tidyImportedData(PluginUtils.SBVR_PACKAGE_NAME, PluginUtils.SBVR_VOCAB_PACKAGE_NAME,
                            PluginUtils.SBVR_RULES_PACKAGE_NAME, PluginUtilities.SBVR_MODELVOC_PACKAGE_NAME,
                            mImportMode, mTraceMode, mUUCDMode, visitors);
                }
            } else {
                step("Error in transformation code!...", progressStatus);
                resultCondition = "Failed";
                Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, "Error during transformation");
            }
            step("Finishing...", progressStatus);
            resultCondition = "Finished";
            ExtractionWorkflows.removeMarkingCommentElement();
            if (progressStatus.isCompleted() && showWizard && extractor != null) {
                final JFrame mainframe = Application.getInstance().getMainFrame();
                final ExtractionWizardDialog dlg = new ExtractionWizardDialog(mainframe, null, true, null, extractor);
                dlg.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        dlg.setVisible(false);
                    }
                });
                dlg.setVisible(true);
            }
        }

        private void step(final String description, final ProgressStatus progressStatus) {
            progressStatus.increase();
            progressStatus.setDescription(description);
            try {
                // pause to see changes
                Thread.sleep(mStepPause);
            } catch (InterruptedException ex) {
                Logger.getLogger(ExtractionRunnable.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public Package getCreatedPackage() {
            return createdPackage;
        }

    }

}
