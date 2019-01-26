package org.ktu.model2sbvr.transform;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.qvt.oml.BasicModelExtent;
import org.eclipse.m2m.qvt.oml.ExecutionContextImpl;
import org.eclipse.m2m.qvt.oml.ExecutionDiagnostic;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.m2m.qvt.oml.util.WriterLog;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.ktu.model2sbvr.PluginUtilities;
import vepsem.CustomURIConverter;
import vepsem.PluginUtils;

public class TransformationEngine {

    public static boolean transform(String inputName, String transformationName) {
        /*Working directories*/
        final String input_dir = "file://" + PluginUtils.getInstance().getConfigDirPath() + "export/" + inputName;
        final String transform_dir = "file://" + PluginUtilities.getQvtDirPath() + transformationName;
        final String output_dir = "file://" + PluginUtils.getInstance().getConfigDirPath() + PluginUtils.OUTPUT_FILE_NAME;

        /*Profile directories*/
        final String sbvr_profile_dir = "file://" + PluginUtils.getInstance().getProfileDirPath() + PluginUtils.SBVR_PROFILE_FILE_NAME;
        final String sbvr_profile_rules_dir = "file://" + PluginUtils.getInstance().getProfileDirPath() + PluginUtils.SBVR_RULES_PROFILE_FILE_NAME;
        final String sbvr_vocab_profile_dir = "file://" + PluginUtils.getInstance().getProfileDirPath() + PluginUtils.SBVR_VOCAB_PROFILE;
        final String sbvr_extension_profile_dir = "file://" + PluginUtils.getInstance().getProfileDirPath() + PluginUtils.SBVR_EXTENSION_PROFILE;
        final String main_sbvr_profile_dir = "file://" + PluginUtils.getInstance().getProfileDirPath() + PluginUtils.SBVR_PROFILE;

        System.out.println("input url: " + input_dir);
        System.out.println("profile url: " + sbvr_profile_dir);
        System.out.println("hi");
        if (sbvr_vocab_profile_dir != null)
            System.out.println("vocab profile url: " + sbvr_vocab_profile_dir);

        /*Preparing EMF for standalone execution*/
        UMLPackage.eINSTANCE.getName();
        Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
        Map<String, Object> m = reg.getExtensionToFactoryMap();
        m.put("uml", new XMIResourceFactoryImpl());

        String umlResourcePath = org.eclipse.uml2.uml.resources.util.UMLResourcesUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            umlResourcePath = URLDecoder.decode(umlResourcePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger(TransformationEngine.class.getName()).log(Level.SEVERE, null, e);
        }
        System.out.println("pathas: " + umlResourcePath);
        EPackage.Registry.INSTANCE.put("http://www.eclipse.org/uml2/4.0.0/UML", UMLPackage.eINSTANCE);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

        EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(UMLResource.PROFILE_FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        URI baseUri = URI.createURI("jar:file:" + umlResourcePath + "!/");
        URIConverter.URI_MAP.put(URI.createURI(UMLResource.LIBRARIES_PATHMAP),
                baseUri.appendSegment("libraries").appendSegment(""));
        URIConverter.URI_MAP.put(URI.createURI(UMLResource.METAMODELS_PATHMAP
        ), baseUri.appendSegment("metamodels").appendSegment(""));
        URIConverter.URI_MAP.put(URI.createURI(UMLResource.PROFILES_PATHMAP),
                baseUri.appendSegment("profiles").appendSegment(""));
        /*Finished EMF preparation for standalone execution*/

        ResourceSet rs = new ResourceSetImpl();
        rs.setURIConverter(new CustomURIConverter());

        //URIs for possible input parameters
        URI qvtFileURI = createFileURI_c(transform_dir);
        URI sbvrProfileURI = createFileURI_c(sbvr_profile_dir);
        URI inputModelURI = createFileURI_c(input_dir);
        URI sbvrRulesProfileURI = createFileURI_c(sbvr_profile_rules_dir);
        URI sbvrVocabProfileURI = createFileURI_c(sbvr_vocab_profile_dir);
        URI sbvrExtensionProfileURI = createFileURI_c(sbvr_extension_profile_dir);
        URI mainSBVRPRofileURI = createFileURI_c(main_sbvr_profile_dir);
        TransformationExecutor executor = new TransformationExecutor(qvtFileURI);
        // setup the execution environment details -> 
        // configuration properties, logger, monitor object etc.
        ExecutionContextImpl context1 = new ExecutionContextImpl();
        context1.setConfigProperty("keepModeling", true);
        context1.setLog(new WriterLog(new OutputStreamWriter(System.out)));
        ExecutionDiagnostic result = null;
        ModelExtent outputModel = null;
        try {
            Resource inputModelResource = rs.getResource(inputModelURI, true);
            Resource inputSBVRProfileResource = rs.getResource(sbvrProfileURI, true);
            Resource inputSBVRRulesProfileResource = rs.getResource(sbvrRulesProfileURI, true);
            Resource inputSBVRVocabProfileResource = rs.getResource(sbvrVocabProfileURI, true);
            Resource inputSBVRMainProfileResource = rs.getResource(mainSBVRPRofileURI, true);
            Resource inputSBVRExtProfileResource = rs.getResource(sbvrExtensionProfileURI, true);

            EList<EObject> inputModelList = new BasicEList<>(Arrays.asList(inputModelResource.getContents().get(0)));
            EList<EObject> inputSBVRProfileList = new BasicEList<>(Arrays.asList(inputSBVRProfileResource.getContents().get(0)));
            EList<EObject> inputSBVRRulesProfileList = new BasicEList<>(Arrays.asList(inputSBVRRulesProfileResource.getContents().get(0)));
            EList<EObject> inputSBVRVocabProfileList = new BasicEList<>(Arrays.asList(inputSBVRVocabProfileResource.getContents().get(0)));
            EList<EObject> inputSBVRMainProfileList = new BasicEList<>(Arrays.asList(inputSBVRMainProfileResource.getContents().get(0)));
            EList<EObject> inputSBVRExtProfileModelList = new BasicEList<>(Arrays.asList(inputSBVRExtProfileResource.getContents().get(0)));

            ModelExtent inputModel = new BasicModelExtent(inputModelList);
            ModelExtent inputSBVRProfile = new BasicModelExtent(inputSBVRProfileList);
            ModelExtent inputSBVRRulesProfile = new BasicModelExtent(inputSBVRRulesProfileList);
            ModelExtent inputSBVRVocabProfile = new BasicModelExtent(inputSBVRVocabProfileList);
            ModelExtent inputSBVRMainProfile = new BasicModelExtent(inputSBVRMainProfileList);
            ModelExtent inputSBVRExtProfile = new BasicModelExtent(inputSBVRExtProfileModelList);
            outputModel = new BasicModelExtent();
            result = executor.execute(context1, inputModel, inputSBVRProfile, inputSBVRRulesProfile, inputSBVRVocabProfile,
                    inputSBVRExtProfile, inputSBVRMainProfile, outputModel);

        } catch (Exception e) {
            System.out.println("Error while preparing QVTo model resources");
            Logger.getLogger(TransformationEngine.class.getName()).log(Level.SEVERE, null, e);
        }

        // check the result for success
        if (result != null && result.getSeverity() == Diagnostic.OK) {
            // the output objects got captured in the output extent
            List<EObject> outObjects = outputModel.getContents();
            // let's persist them using a resource 
            ResourceSet resourceSet2 = new ResourceSetImpl();
            //Resource outResource = resourceSet2.getResource(createFileURI_c(output_dir), true);
            Resource outResource = resourceSet2.createResource(createFileURI_c(output_dir));
            outResource.getContents().addAll(outObjects);
            try {
                outResource.save(Collections.emptyMap());
            } catch (IOException ex) {
                Logger.getLogger(TransformationEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // turn the result diagnostic into status and send it to error log			
            IStatus status = BasicDiagnostic.toIStatus(result);
            for (IStatus error : status.getChildren())
                Logger.getLogger(TransformationEngine.class.getName()).log(Level.SEVERE, null, 
                        String.format("Error %d: %s", error.getCode(), error.getMessage()));
            System.out.println("Transformation failed");
            return false;

            //Activator.getDefault().getLog().log(status);
        }
        return true;
    }

    public static URI createFileURI_c(String relativePath) {
        return URI.createURI(relativePath).resolve(URI.createFileURI(System.getProperty("user.dir") + "/"));
    }

}
