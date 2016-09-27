/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ktu.transformations.uml2sbvr.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.ktu.transformations.uml2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.transformations.uml2sbvr.extract.VetisProducer;
import org.ktu.transformations.uml2sbvr.models.AbstractConceptModel;

/**
 *
 * @author Paulius
 */
public class VetisExporter {
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/ui/Bundle");
    
    private final String projectName;

    public VetisExporter(String projectName) {
        this.projectName = projectName;
    }
    
    public void exportProject(JFrame frame, AbstractSBVRExtractor extractor) {
        exportProject(frame, extractor.getGCCandidateModel(), extractor.getVCCandidateModel(), extractor.getBRCandidateModel(),
                extractor.isExtractModelVocabulary());
    }
    
    public void exportProject(JFrame frame, AbstractConceptModel gcCandidates, 
            AbstractConceptModel vcCandidates, AbstractConceptModel brCandidates, boolean includeModelVoc) {
        String projectPath = null;
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(bundle.getString("ExtractionWizardDialog_40"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            projectPath = chooser.getSelectedFile().getAbsolutePath();
            File projDir = new File(projectPath);
            if (projDir.isDirectory() && projDir.list().length > 0)
                if (JOptionPane.showConfirmDialog(frame, projectPath + bundle.getString("ExtractionWizardDialog_39"),
                        bundle.getString("ExtractionWizardDialog_42"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    if (JOptionPane.showConfirmDialog(frame, bundle.getString("ExtractionWizardDialog_45"),
                            bundle.getString("ExtractionWizardDialog_46"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                        projDir.mkdir();
                }
            JOptionPane.showMessageDialog(frame, bundle.getString("ExtractionWizardDialog_41")
                    + projectPath + "." + bundle.getString("ExtractionWizardDialog_43"));
        }
        if (projectPath == null)
            return;
        VetisProducer exportObj = new VetisProducer(projectPath, projectName, includeModelVoc);
        exportObj.setGCCandidates(gcCandidates);
        exportObj.setVCCandidates(vcCandidates);
        exportObj.setBRCandidates(brCandidates);
        try {
            exportObj.export();
        } catch (FileNotFoundException | NotDirectoryException ex) {
            Logger.getLogger(ExtractionWizardDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
