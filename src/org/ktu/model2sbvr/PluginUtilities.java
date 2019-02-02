package org.ktu.model2sbvr;

import com.nomagic.magicdraw.cbm.BPMNConstants;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.uml.ElementFinder;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import java.io.File;
import java.net.URI;
import java.util.Set;
import vepsem.PluginUtils;
import static vepsem.Workflows.getProfileByName;

public class PluginUtilities {

    public static final String UCD2SBVR_BV_STRICT_QVT = "UCD2SBVR BV strict.qvto";
    public static final String UCD2SBVR_BV_QVT = "UCD2SBVR BV.qvto";
    public static final String UCD2SBVR_MV_QVT = "UCD2SBVR MV.qvto";
    public static final String UCD2SBVR_BV_AND_MV_QVT = "UCD2SBVR BV+MV.qvto";
    public static final String UCD2SBVR_BV_AND_MV_STRICT_QVT = "UCD2SBVR BV+MV strict.qvto";
    
    public static final String GC_UNDERSCORE_STRING = "general_concept";
    public static final String VC_UNDERSCORE_STRING = "association";
    public static final String CONCEPT_TYPE_STRING = "Concept_type";
    public static final String SYNONYMOUS_FORM_STRING = "Synonymous_form";
    
    public static final String SBVR_MODELVOC_PACKAGE_NAME = "SBVR Model Vocabulary";

    public static final String getQvtDirPath() {
        String pluginDir = UML2SBVRPlugin.getInstance().getDescriptor().getPluginDirectory().getAbsolutePath();
        return pluginDir + "/" + "qvt" + "/";  // Do not use File.separator, otherwise Eclipse URI resolver will not resolve URI!
    }

    public static void addSBVRProfiles() {
        Project project = Application.getInstance().getProject();
        ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
        URI uriprof = new File("SBVR profile.mdxml").toURI();
        ProjectDescriptor projdesc = ProjectDescriptorsFactory.createProjectDescriptor(uriprof);
        if (projectsManager.findAttachedProject(project, projdesc) == null)
            projectsManager.useModule(project, projdesc);
        uriprof = new File("SBVR customizations.mdxml").toURI();
        projdesc = ProjectDescriptorsFactory.createProjectDescriptor(uriprof);
        if (projectsManager.findAttachedProject(project, projdesc) == null)
            projectsManager.useModule(project, projdesc);
        uriprof = new File("Integration profile.mdzip").toURI();
        projdesc = ProjectDescriptorsFactory.createProjectDescriptor(uriprof);
        if (projectsManager.findAttachedProject(project, projdesc) == null)
            projectsManager.useModule(project, projdesc);
        
    }

    public static Profile getCustomizationsProfile(Project project) {
        return StereotypesHelper.getProfileByURI(project, new File("SBVR customizations.mdxml").toURI().toString());
    }
    
    public static Profile getIntegrationProfile(Project project) {
        return StereotypesHelper.getProfileByURI(project, new File("Integration profile.mdzip").toURI().toString());
    }

    public static Profile getBPMNProfile(Project project) {
        return StereotypesHelper.getProfileByURI(project, new File(BPMNConstants.BPMN2_PROFILE_FILENAME).toURI().toString());
    }

    public static boolean isBPMNDiagram(DiagramPresentationElement diag) {
        Set<String> bpmnDiagrams = BPMNConstants.BPMN_DIAGRAMS;
        for (String dn: bpmnDiagrams)
            if (diag.getDiagramType().getType().compareToIgnoreCase(dn) == 0)
                return true;
        return false;
    }
    
    public static Package getConceptsRootPackage(Model owner) {
        Package sbvr_pack = (Package) ElementFinder.find(owner, Package.class, PluginUtils.SBVR_PACKAGE_NAME, true);
        if (sbvr_pack == null) {
            sbvr_pack = UMLFactory.eINSTANCE.createPackage();
            sbvr_pack.setOwningPackage(owner);
            sbvr_pack.setName(PluginUtils.SBVR_PACKAGE_NAME);
        }
        return sbvr_pack;
    }
    
    public static Package createResultPackage(Model owner, String packageName, boolean createModelVoc) {
        Stereotype vocabularyStereotype = null;
        Stereotype rulesStereotype = null;
        Project prj = Application.getInstance().getProject();
        Profile vdbvProfile = getProfileByName(prj, prj.getModel(), "Vocabulary for Describing Business Vocabularies");
        Profile sbvrExtensionProfile = getProfileByName(prj, prj.getModel(), "Extension for SBVR");

        if (vdbvProfile != null && sbvrExtensionProfile != null) {
            vocabularyStereotype = StereotypesHelper.getStereotype(prj, "vocabulary", vdbvProfile);
            rulesStereotype = StereotypesHelper.getStereotype(prj, "vocabulary of business rules", sbvrExtensionProfile);
        }
        Package sbvr_pack = PluginUtilities.getConceptsRootPackage(owner);
        String origName = packageName, packName = packageName;
        boolean found = false;
        int ind = 0;
        while (!found) {
            ind += 1;
            Element sbvr_2_pack = ElementFinder.find(sbvr_pack, Package.class, packName, false);
            if (sbvr_2_pack != null)
                packName = origName + "_" + ind;
            else
                found = true;
        }
        Package model_pack = UMLFactory.eINSTANCE.createPackage();
        model_pack.setOwningPackage(sbvr_pack);
        model_pack.setName(packName);
        Package sbvr_2_voc_pack = UMLFactory.eINSTANCE.createPackage();
        sbvr_2_voc_pack.setName(PluginUtils.SBVR_VOCAB_PACKAGE_NAME);
        sbvr_2_voc_pack.setOwningPackage(model_pack);
        if (vocabularyStereotype != null && !StereotypesHelper.hasStereotype(sbvr_2_voc_pack, vocabularyStereotype))
            StereotypesHelper.addStereotype(sbvr_2_voc_pack, vocabularyStereotype);
        Package sbvr_2_rules_pack = UMLFactory.eINSTANCE.createPackage();
        sbvr_2_rules_pack.setName(PluginUtils.SBVR_RULES_PACKAGE_NAME);
        sbvr_2_rules_pack.setOwningPackage(model_pack);
        if (vocabularyStereotype != null && !StereotypesHelper.hasStereotype(sbvr_2_rules_pack, rulesStereotype))
            StereotypesHelper.addStereotype(sbvr_2_rules_pack, rulesStereotype);
        if (createModelVoc) {
            Package sbvr_2_model_pack = UMLFactory.eINSTANCE.createPackage();
            sbvr_2_model_pack.setName(PluginUtilities.SBVR_MODELVOC_PACKAGE_NAME);
            sbvr_2_model_pack.setOwningPackage(model_pack);
            if (vocabularyStereotype != null && !StereotypesHelper.hasStereotype(sbvr_2_model_pack, vocabularyStereotype))
                StereotypesHelper.addStereotype(sbvr_2_model_pack, vocabularyStereotype);
        }
        return model_pack;
    }

}
