package org.ktu.transformations.uml2sbvr.extract;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import org.ktu.transformations.uml2sbvr.models.AbstractCandidateConceptModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel;
import org.vetis.md.remote.RemoteService;
import org.vetis.md.ui.workflows.UiwfBindRemoteService;

public class VetisProducer {

    private final String projectPath, projectName;
    private AbstractCandidateConceptModel gcCandidates, vcCandidates, brCandidates;
    private final boolean includeModelVoc;

    public VetisProducer(String projectPath, String projectName) {
        super();
        this.projectPath = projectPath;
        this.projectName = projectName;
        this.includeModelVoc = false;
    }

    public VetisProducer(String projectPath, String projectName, boolean includeModelVoc) {
        this.projectPath = projectPath;
        this.projectName = projectName;
        this.includeModelVoc = includeModelVoc;
    }

    public void export() throws FileNotFoundException, NotDirectoryException {
        if (gcCandidates == null)
            return;
        File projDir = new File(projectPath);
        if (projDir.exists() && !projDir.isDirectory())
            throw new NotDirectoryException(projectPath);
        if (!projDir.exists() && !projDir.mkdir())
            throw new FileNotFoundException(String.format("Could not create directory %s", projDir));
        try {
            PrintWriter writer;
            String path = projectPath + File.separator + projectName;
            if (includeModelVoc)
                writer = new PrintWriter(path + " - Business Vocabulary.voc", "UTF-8");
            else
                writer = new PrintWriter(path + ".voc", "UTF-8");
            if (gcCandidates != null)
                exportCandidates(writer, gcCandidates, PluginUtilities.GC_UNDERSCORE_STRING, false);
            if (vcCandidates != null)
                exportCandidates(writer, vcCandidates, null, false);
            writer.close();
            if (includeModelVoc) {
                writer = new PrintWriter(path + " - Model Vocabulary.voc", "UTF-8");
                exportCandidates(writer, gcCandidates, PluginUtilities.GC_UNDERSCORE_STRING, true);
                exportCandidates(writer, vcCandidates, PluginUtilities.VC_UNDERSCORE_STRING, true);
                writer.close();
            } 
            writer = new PrintWriter(projectPath + File.separator + projectName + ".rules", "UTF-8");
            if (brCandidates != null)
                exportCandidates(writer, brCandidates, null, false);
            writer.close();
            writer = new PrintWriter(projectPath + File.separator + projectName + ".voc_head", "UTF-8");
            writer.println(projectName);
            writer.close();
            generateSettingsFile();

            // Start Vetis editor
            UiwfBindRemoteService uiwfBrs = new UiwfBindRemoteService();
            RemoteService rs = uiwfBrs.run();
            rs.focus();

            Project activePrj = Application.getInstance().getProjectsManager().getActiveProject();
            if (activePrj != null)
                rs.openRelatedProject(activePrj.getFileName());
        } catch (FileNotFoundException | UnsupportedEncodingException e1) {
            Logger.getLogger(VetisProducer.class.getName()).log(Level.SEVERE, null, e1);
        } catch (Exception e) {
            Logger.getLogger(VetisProducer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * @param conceptType A string representing "Concept_type" in VeTIS editor
     */
    private void exportCandidates(PrintWriter writer, AbstractCandidateConceptModel dataset, String conceptType, boolean modelVoc) {
        if (dataset == null || dataset.size() == 0)
            return;
        for (List<String> concepts : dataset.getDataset().keySet())
            for (SBVRExpressionModel obj : dataset.getDataset().get(concepts))
                if (obj.isModelVocabularyConcept() == modelVoc)
                    printCandidate(writer, dataset, obj, concepts, conceptType);
    }

    protected void printCandidate(PrintWriter writer, AbstractCandidateConceptModel dataset,
            SBVRExpressionModel obj, List<String> concepts, String conceptType) {
        writer.println(obj.toUnderscoreString());
        if (conceptType != null)
            writer.format("\t%s: %s\n", PluginUtilities.CONCEPT_TYPE_STRING, conceptType);
        if (!obj.getSynonymousForms().isEmpty())
            for (SBVRExpressionModel synonym : obj.getSynonymousForms())
                writer.format("\t%s: %s\n", PluginUtilities.SYNONYMOUS_FORM_STRING, synonym.toUnderscoreString());
    }

    private void generateSettingsFile() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(projectPath + File.separator + ".project", "UTF-8");
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<projectDescription>");
        writer.format("\t%s%s%s\n", "<name>", projectName, "</name>");
        writer.println("\t<comment></comment>");
        writer.println("\t<projects>");
        writer.println("\t</projects>");
        writer.println("\t<buildSpec>");
        writer.println("\t</buildSpec>");
        writer.println("\t<natures>");
        writer.println("\t\t<nature>org.vetis.app#project.natures.Vetis</nature>");
        writer.println("\t</natures>");
        writer.println("</projectDescription>");
        writer.close();
    }

    public void setGCCandidates(AbstractCandidateConceptModel gcCandidates) {
        this.gcCandidates = gcCandidates;
    }

    public void setVCCandidates(AbstractCandidateConceptModel vcCandidates) {
        this.vcCandidates = vcCandidates;
    }

    public void setBRCandidates(AbstractCandidateConceptModel brCandidates) {
        this.brCandidates = brCandidates;
    }

}
