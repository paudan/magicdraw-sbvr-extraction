package org.ktu.transformations.uml2sbvr.transform;

import com.nomagic.magicdraw.copypaste.CopyPasting;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.options.GeneralOptionsGroup;
import com.nomagic.magicdraw.emfuml2xmi.BaseEmfUml2Helper;
import com.nomagic.magicdraw.emfuml2xmi.FinalizeActivity;
import com.nomagic.magicdraw.emfuml2xmi.v4.EmfUml2XmiPlugin;
import com.nomagic.magicdraw.emfuml2xmi.v4.imp0rt.convert.EmfUml2ImportFinalizeActivityManager;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.uml.ElementFinder;
import com.nomagic.magicdraw.uml.Visitor;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.jmi.reflect.VisitorContext;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageableElement;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import com.nomagic.utils.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import vepsem.CommentVisitor;
import vepsem.PluginUtils;
import vepsem.Workflows;

public class ExtractionWorkflows extends Workflows {

    private static Package pkgName = null;

    @SuppressWarnings("deprecation")
    public static Package importModelViaEmfUml2Xmi(String filePath, String lockFilePath, Package owner, final boolean importModelVoc) {
        //validate inputs
        assert (filePath != null) : "Argument 'filePath' is null."; //$NON-NLS-1$
        //lock lock file before importing, to enable external observers see when import starts and ends
        FileChannel lockFileChan = null;
        FileLock lockFileLock = null;

        if (lockFilePath != null) {
            try {
                lockFileChan = (new RandomAccessFile(new File(lockFilePath), "rw")).getChannel(); //$NON-NLS-1$
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
            try {
                lockFileLock = lockFileChan.lock();
            } catch (IOException e) {
                Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.SEVERE, null, e);
                return null;
            }
        }
        //import model from given file into active project
        final String finFilePath = filePath;
        final FileChannel finLockFileChan = lockFileChan;
        final FileLock finLockFileLock = lockFileLock;
        final Package finPackage = owner;
        Utilities.invokeAndWaitOnDispatcher(new Runnable() {
            @Override
            public void run() {
                //get the project containing target model
                Project dstPrj = Application.getInstance().getProject();
                if (dstPrj == null)
                    return;
                SessionManager sessionManager = SessionManager.getInstance();
                if (sessionManager.isSessionCreated())
                    sessionManager.closeSession();
                sessionManager.createSession(dstPrj, "Importing generated XMI...");
                // Custom parser of EAnnotations within the imported XMI file.
                EmfUml2ImportFinalizeActivityManager.getInstance().addActivity(new FinalizeActivity() {
                    @Override
                    public boolean finalize(Project project, Map elements, BaseEmfUml2Helper helper) {
                        Iterator it = elements.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pairs = (Map.Entry) it.next();
                            if (((org.eclipse.uml2_4_0_1.uml.Element) pairs.getKey()).getEAnnotation("trace") != null) {
                                System.out.println(pairs.getKey() + " = " + pairs.getValue() + "\n");
                                System.out.println("\nTrace: "
                                        + ((org.eclipse.uml2_4_0_1.uml.Element) pairs.getKey()).getEAnnotation("trace").getDetails().toString()
                                );
                                Element cand = (Element) pairs.getValue();
                                Comment com = UMLFactory.eINSTANCE.createComment();
                                Set<Entry<String, String>> entries = ((org.eclipse.uml2_4_0_1.uml.Element) pairs.getKey()).getEAnnotation("trace").getDetails().entrySet();
                                com.setBody("vepsem_trace:");
                                for (Entry<String, String> entry : entries)
                                    com.setBody(com.getBody().concat("{").concat(entry.getKey() + ":" + entry.getValue()).concat("}"));
                                cand.getOwnedComment().add(com);
                            }
                            it.remove();
                        }
                        return true;
                    }
                }
                );

                //import Eclipse UML2 model via EmfUml2Xmi plug-in, this will create a new project with imported model
                EmfUml2XmiPlugin.getInstance().imp0rt(finFilePath);
                System.out.println("import ok: ");
                //get the project containing source model
                Project srcPrj = Application.getInstance().getProject();
                if (srcPrj == null)
                    return;
                //copy data from the source model to the target model
                SessionManager.getInstance().createSession(srcPrj, "Copying");
                try {
                    Model dstModel = dstPrj.getModel();
                    pkgName = PluginUtilities.createResultPackage(dstModel, finPackage.getName(), importModelVoc);
                    CopyPasting.copyPasteElement(srcPrj.getModel(), dstModel);
                } finally {
                    SessionManager.getInstance().closeSession(srcPrj);
                }
                //close the temporary source project
                Application.getInstance().getProjectsManager().closeProjectNoSave();
                GeneralOptionsGroup options2 = Application.getInstance().getEnvironmentOptions().getGeneralOptions();
                List<String> recentFiles = options2.getRecentFiles();
                for (String file : recentFiles)
                    if (file.contains("projectName=vepsem_xmi_output"))
                        options2.removeRecentFile(file, true | false);
                //force the refreshing of containment tree of the target project
                Application.getInstance().getProjectsManager().setActiveProject(dstPrj);
                //unlock lock file to announce end of import
                try {
                    if (finLockFileLock != null)
                        finLockFileLock.release();
                    if (finLockFileChan != null)
                        finLockFileChan.close();
                } catch (IOException e) {
                    Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        });
        return pkgName;
    }

    public static void tidyImportedData(final String modelName, final String firstPackage, final String secondPackage, final String thirdPackage,
            final String importMode, final Boolean traceMode, final Boolean uuCDMode, final Collection<Visitor> visitors) {
        //validate inputs
        assert (modelName != null) : "Argument 'modelName' is null.";
        assert (pkgName != null) : "Argument 'pkgName' is null.";
        assert (firstPackage != null) : "Argument 'firstPackage' is null.";
        assert (importMode != null) : "Argument 'secondPackage' is null.";

        final Package mainModelPack = pkgName;
        Utilities.invokeAndWaitOnDispatcher(new Runnable() {
            @Override
            public void run() {
                //get active project
                final Project prj = Application.getInstance().getProject();
                if (prj == null)
                    return;
                Package modelChild = null;
                try {
                    Model modelRoot = prj.getModel();
                    //find imported model with given name in model root
                    Collection<PackageableElement> pkgElems = modelRoot.getPackagedElement();
                    for (PackageableElement pkgElem : pkgElems)
                        if (pkgElem instanceof Model && pkgElem.getName().compareTo(modelName) == 0) {
                            modelChild = (Model) pkgElem;
                            break;
                        }

                    Package importedFirstTargetPackage = null;
                    Package importedSecondTargetPackage = null;
                    Package importedThirdTargetPackage = null;
                    if (modelChild == null) {
                        Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.INFO,
                                "No target packages found in the child model. Finishing tidying procedure");
                        return;
                    }
                    //find package with given name in imported model							
                    pkgElems = modelChild.getPackagedElement();
                    for (PackageableElement pkgElem : pkgElems)
                        if (pkgElem instanceof Package) {
                            if (pkgElem.getName().compareTo(firstPackage) == 0)
                                importedFirstTargetPackage = (Package) pkgElem;
                            else if (pkgElem.getName().compareTo(secondPackage) == 0)
                                importedSecondTargetPackage = (Package) pkgElem;
                            else if (pkgElem.getName().compareTo(thirdPackage) == 0)
                                importedThirdTargetPackage = (Package) pkgElem;
                            if (importedFirstTargetPackage != null && importedSecondTargetPackage != null && importedThirdTargetPackage != null)
                                break;
                        }
                    Stereotype vocabularyStereotype = null;
                    Stereotype rulesStereotype = null;

                    Profile vdbvProfile = getProfileByName(prj, modelRoot, "Vocabulary for Describing Business Vocabularies");
                    Profile sbvrExtensionProfile = getProfileByName(prj, modelRoot, "Extension for SBVR");

                    if (vdbvProfile != null && sbvrExtensionProfile != null) {
                        vocabularyStereotype = StereotypesHelper.getStereotype(prj, "vocabulary", vdbvProfile);
                        rulesStereotype = StereotypesHelper.getStereotype(prj, "vocabulary of business rules", sbvrExtensionProfile);
                    }
                    // Visit main package
                    for (Visitor visitor : visitors)
                        visitor.visitPackage(mainModelPack, new VisitorContext());

                    //Blogai iesko ElementFinderis, tikslinti modelRoot (Tomas V.)
                    Package mainSBVRVocabPack = (Package) ElementFinder.find(mainModelPack, Package.class, PluginUtils.SBVR_VOCAB_PACKAGE_NAME, true);
                    Package mainSBVRRulesPack = (Package) ElementFinder.find(mainModelPack, Package.class, PluginUtils.SBVR_RULES_PACKAGE_NAME, true);

                    if (vocabularyStereotype != null && mainSBVRVocabPack != null && !StereotypesHelper.hasStereotype(mainSBVRVocabPack, vocabularyStereotype))
                        StereotypesHelper.addStereotype(mainSBVRVocabPack, vocabularyStereotype);
                    if (rulesStereotype != null && mainSBVRRulesPack != null && !StereotypesHelper.hasStereotype(mainSBVRRulesPack, rulesStereotype))
                        StereotypesHelper.addStereotype(mainSBVRRulesPack, rulesStereotype);
                    if (importedFirstTargetPackage != null && importedSecondTargetPackage != null)
                        if (modelName.equals(PluginUtils.SBVR_PACKAGE_NAME)) {

                            List<Element> firstPackageElements = new ArrayList<>(importedFirstTargetPackage.getOwnedElement());
                            List<Element> secondPackageElements = new ArrayList<>(importedSecondTargetPackage.getOwnedElement());

                            for (Element el : secondPackageElements)
                                System.out.println("Second pack element: " + el.getHumanType() + "\n getName: " + getName(el));

                            List<Element> firstPackageInnerElements = new ArrayList<>();
                            List<Element> secondPackageInnerElements = new ArrayList<>();
                            for (Element elem : firstPackageElements)
                                //if (elem instanceof Association)
                                firstPackageInnerElements.addAll(elem.getOwnedElement());
                            for (Element elem : secondPackageElements)
                                //if (elem instanceof Association)
                                secondPackageInnerElements.addAll(elem.getOwnedElement());

                            firstPackageElements.addAll(firstPackageInnerElements);
                            secondPackageElements.addAll(secondPackageInnerElements);

                            performStereotypePatch(prj, modelRoot, firstPackageElements);
                            performStereotypePatch(prj, modelRoot, secondPackageElements);
                            //Patching SBVR profile types
                            performSBVRTypePatch(modelRoot, firstPackageElements);

                            if (mainSBVRVocabPack != null && mainSBVRRulesPack != null) {
                                List<Element> firstPackElementsToCopy = new ArrayList<>(importedFirstTargetPackage.getOwnedElement());
                                List<Element> secondPackElementsToCopy = new ArrayList<>(importedSecondTargetPackage.getOwnedElement());
                                CopyPasting.copyPasteElements(firstPackElementsToCopy, mainSBVRVocabPack);
                                CopyPasting.copyPasteElements(secondPackElementsToCopy, mainSBVRRulesPack);

                                for (Visitor visitor : visitors) {
                                    visitor.visitPackage(mainSBVRVocabPack, new VisitorContext());
                                    visitor.visitPackage(mainSBVRRulesPack, new VisitorContext());
                                }

                                if (traceMode) {
                                    CommentVisitor commentVisitor = new CommentVisitor();
                                    visitChildren(mainSBVRVocabPack, commentVisitor);
                                    visitChildren(mainSBVRRulesPack, commentVisitor);
                                    createTraceLinks(prj, modelRoot, commentVisitor.getElementList());
                                } else {
                                    CommentVisitor commentVisitor = new CommentVisitor();
                                    visitChildren(mainSBVRVocabPack, commentVisitor);
                                    visitChildren(mainSBVRRulesPack, commentVisitor);

                                    for (Element element : commentVisitor.getCommentList()) {
                                        ModelElementsManager.getInstance().removeElement(element);
                                        System.out.println("Removing trace comment: " + element);
                                    }
                                }
                            }
                        }
                    if (importedThirdTargetPackage != null && modelName.equals(PluginUtils.SBVR_PACKAGE_NAME)) {
                        Package mainSBVRModelPack = (Package) ElementFinder.find(mainModelPack, Package.class, PluginUtilities.SBVR_MODELVOC_PACKAGE_NAME, true);
                        if (vocabularyStereotype != null && mainSBVRModelPack != null && !StereotypesHelper.hasStereotype(mainSBVRModelPack, vocabularyStereotype))
                            StereotypesHelper.addStereotype(mainSBVRModelPack, vocabularyStereotype);
                        List<Element> thirdPackageElements = new ArrayList<>(importedThirdTargetPackage.getOwnedElement());
                        List<Element> thirdPackageInnerElements = new ArrayList<>();
                        for (Element elem : thirdPackageElements)
                            thirdPackageInnerElements.addAll(elem.getOwnedElement());
                        thirdPackageElements.addAll(thirdPackageInnerElements);
                        performStereotypePatch(prj, modelRoot, thirdPackageElements);

                        if (mainSBVRModelPack != null) {
                            List<Element> thirdPackElementsToCopy = new ArrayList<>(importedThirdTargetPackage.getOwnedElement());
                            CopyPasting.copyPasteElements(thirdPackElementsToCopy, mainSBVRModelPack);
                            for (Visitor visitor : visitors)
                                visitor.visitPackage(mainSBVRModelPack, new VisitorContext());
                            CommentVisitor commentVisitor = new CommentVisitor();
                            visitChildren(mainSBVRModelPack, commentVisitor);
                            if (!traceMode)
                                for (Element element : commentVisitor.getCommentList()) {
                                    ModelElementsManager.getInstance().removeElement(element);
                                    System.out.println("Removing trace comment: " + element);
                                }
                        }

                    }
                    Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.INFO,
                            "Child model exists. Removing 1...");
                    ModelElementsManager.getInstance().removeElement(modelChild);

                } catch (ReadOnlyElementException e) {
                    Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.WARNING, null, e);
                } finally {
                    //Final removal in case of any failure.
                    if (modelChild != null) {
                        Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.INFO,
                                "Child model exists. Removing 2...");
                        try {
                            ModelElementsManager.getInstance().removeElement(modelChild);
                        } catch (ReadOnlyElementException e) {
                            Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.WARNING, null, e);
                        }
                    }
                    removeMarkingCommentElement();
                    SessionManager.getInstance().closeSession();
                }
            }
        });
    }

    //Remove UML Comment element which was used for QVT transformation
    public static void removeMarkingCommentElement() {
        Package model = Application.getInstance().getProject().getModel();
        Visitor visitor = new Visitor() {
            @Override
            public void visitPackage(Package element, VisitorContext context) {
                if (element.getOwnedComment() == null)
                    return;
                for (Comment comment : element.getOwnedComment())
                    if (comment.getBody() != null && comment.getBody().compareTo("_trans_owner_") == 0)
                        try {
                            ModelElementsManager.getInstance().removeElement(comment);
                        } catch (ReadOnlyElementException ex) {
                            Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.SEVERE, null, ex);
                        }
            }
        };
        Collection srcPackages = ElementFinder.getChildren(model, new Class[]{Package.class}, true, true);
        for (Object e : srcPackages)
            try {
                ((Package) e).accept(visitor);
            } catch (Exception ex) {
                Logger.getLogger(ExtractionWorkflows.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
}
