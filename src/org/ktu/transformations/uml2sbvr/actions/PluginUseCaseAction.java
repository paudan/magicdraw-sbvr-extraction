package org.ktu.transformations.uml2sbvr.actions;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import org.ktu.transformations.uml2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.transformations.uml2sbvr.extract.UseCaseSBVRExtractor;
import org.ktu.transformations.uml2sbvr.transform.ExtractionRunnable;
import org.ktu.transformations.uml2sbvr.ui.ExtractionWizardDialog;
import org.ktu.transformations.uml2sbvr.ui.OptionsDialog;
import org.ktu.transformations.uml2sbvr.ui.VetisExporter;

/**
 *
 * @author Paulius
 */
public class PluginUseCaseAction {

    private DiagramPresentationElement diagram;
    private Package pkg;

    public PluginUseCaseAction(DiagramPresentationElement diagram) {
        this.diagram = diagram;
    }

    public PluginUseCaseAction(Package pkg) {
        this.pkg = pkg;
    }

    public void performTransformAction(final boolean showWizard) {
        final JFrame mainframe = Application.getInstance().getMainFrame();
        final OptionsDialog otDlg = new OptionsDialog(mainframe, true);
        if (pkg == null && (diagram == null || diagram.getDiagram() == null))
            return;
        otDlg.tbpOptions.remove(1);
        otDlg.getTransformButton().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                otDlg.setVisible(false);
                Project pkgProj = Application.getInstance().getProjectsManager().getActiveProject();
                Package diagramPkg = pkg != null ? pkg : (Package) diagram.getDiagram().getOwner();
                if (diagramPkg == null)
                    return;
                if (otDlg.isM2MSelected()) {
                    PluginUtilities.addSBVRProfiles();
                    String transform = null;
                    if (otDlg.isM2MSelected() && otDlg.isVocabularySelected() && !otDlg.isMMVocabularySelected())
                        transform = otDlg.isStrictSelected() ? PluginUtilities.UCD2SBVR_BV_STRICT_QVT : PluginUtilities.UCD2SBVR_BV_QVT;
                    else if (otDlg.isM2MSelected() && otDlg.isMMVocabularySelected() && !otDlg.isVocabularySelected())
                        transform = PluginUtilities.UCD2SBVR_MV_QVT;
                    else if (otDlg.isM2MSelected() && otDlg.isMMVocabularySelected() && otDlg.isVocabularySelected())
                        transform = otDlg.isStrictSelected() ? PluginUtilities.UCD2SBVR_BV_AND_MV_STRICT_QVT : PluginUtilities.UCD2SBVR_BV_AND_MV_QVT;
                    if (transform == null)
                        return;
                    AbstractSBVRExtractor extractor = null;
                    if (showWizard) {
                        if (pkg != null)
                            extractor = new UseCaseSBVRExtractor(pkg, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected());
                        else
                            extractor = new UseCaseSBVRExtractor(diagram, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected());
                        extractor.setExtractedAuto(true);
                    }
                    String result = ExtractionRunnable.getInstance().nonLockedExecution(pkgProj, diagramPkg, transform, "overwrite",
                            true, true, otDlg.isMMVocabularySelected(), showWizard, extractor);
                    JOptionPane.showMessageDialog(MDDialogParentProvider.getProvider().getDialogParent(),
                            "Transformation result: " + result, null, JOptionPane.INFORMATION_MESSAGE, null);
                } else if (otDlg.isM2TSelected()) {
                    AbstractSBVRExtractor extractor = pkg != null ? 
                            new UseCaseSBVRExtractor(pkg, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected()) :
                            new UseCaseSBVRExtractor(diagram, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected());
                    extractor.extractAll();
                    new VetisExporter(pkg != null ? pkg.getName() : diagram.getDiagram().getName()).exportProject(mainframe, extractor);
                }
                otDlg.setVisible(false);
            }
        });
        otDlg.setVisible(true);
    }

    public void performWizardAction() {
        final JFrame mainframe = Application.getInstance().getMainFrame();
        final OptionsDialog otDlg = new OptionsDialog(mainframe, true);
        otDlg.setM2MEnabled(false);
        otDlg.setM2TEnabled(false);
        otDlg.setM2TSelected(true);
        otDlg.getTransformButton().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractSBVRExtractor extractor;
                if (pkg != null)
                    extractor = new UseCaseSBVRExtractor(pkg, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected());
                else
                    extractor = new UseCaseSBVRExtractor(diagram, otDlg.isStrictSelected(), otDlg.isMMVocabularySelected());
                final ExtractionWizardDialog dlg = new ExtractionWizardDialog(mainframe, otDlg, true, pkg, extractor);
                dlg.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        dlg.setVisible(false);
                        otDlg.setVisible(false);
                    }
                });
                dlg.setVisible(true);
                otDlg.setVisible(false);
            }

        });
        otDlg.setVisible(true);
    }

}
