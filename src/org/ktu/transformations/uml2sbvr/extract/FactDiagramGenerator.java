package org.ktu.transformations.uml2sbvr.extract;

import com.nomagic.diagramtable.columns.NumberColumn;
import com.nomagic.generictable.GenericTableManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.PresentationElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.properties.BooleanProperty;
import com.nomagic.magicdraw.properties.PropertyID;
import com.nomagic.magicdraw.properties.PropertyManager;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.magicdraw.uml.ElementFinder;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.magicdraw.uml.RepresentationTextCreator.RepresentationTextProvider2;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ktu.transformations.uml2sbvr.PluginUtilities;
import org.ktu.transformations.uml2sbvr.models.FilteredCandidateConceptModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel;
import org.ktu.transformations.uml2sbvr.models.SBVRExpressionModel.ExpressionType;
import vepsem.PluginUtils;

public class FactDiagramGenerator {

    private final String diagramName;
    private final Project project;
    private Package targetPackage, bvPackage, mvPackage, rulesPackage;
    private final ElementsFactory elementsFactory;
    private DiagramPresentationElement targetDiagram;
    private FilteredCandidateConceptModel gcCandidates, vcCandidates, brCandidates;
    private Profile profile;
    private Set<String> gcList = null;
    private Diagram table;
    private boolean useModelVoc;
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/messages");

    public Package getTargetPackage() {
        return targetPackage;
    }

    private class RepresentationTextProviderImpl implements RepresentationTextProvider2 {

        @Override
        public boolean accept(BaseElement element) {
            return element instanceof Association;
        }

        @Override
        public SmartListenerConfig createSmartListenerConfig(Element arg0, boolean arg1) {
            return SmartListenerConfig.STEREOTYPE_AND_TAGS_CONFIG;
        }

        @Override
        public String getRepresentedText(BaseElement element, boolean addColor, boolean fullSignature, boolean addId) {
            return (addId ? RepresentationTextCreator.createId((Element) element, addColor) : "")
                    + AbstractSBVRExtractor.extractElementText(element);
        }

    }

    public FactDiagramGenerator(String diagramName, boolean useModelVoc) {
        super();
        this.diagramName = diagramName;
        this.useModelVoc = useModelVoc;
        project = Application.getInstance().getProject();
        elementsFactory = project.getElementsFactory();
    }

    public void setGCCandidates(FilteredCandidateConceptModel gcCandidates) {
        this.gcCandidates = gcCandidates;
        gcList = gcCandidates.getCandidatesListText();
    }

    public void setVCCandidates(FilteredCandidateConceptModel vcCandidates) {
        this.vcCandidates = vcCandidates;
    }

    public void setBRCandidates(FilteredCandidateConceptModel brCandidates) {
        this.brCandidates = brCandidates;
    }

    public void generate() {
        SessionManager sessionManager = SessionManager.getInstance();
        if (sessionManager.isSessionCreated())
            sessionManager.closeSession();
        sessionManager.createSession(bundle.getString("FactDiagramGenerator_0"));
        PluginUtilities.addSBVRProfiles();
        Package sbvrPackage = PluginUtilities.createResultPackage(project.getModel(), diagramName, true);
        targetPackage = (Package) ElementFinder.find(sbvrPackage, Package.class, diagramName, true);
        bvPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtils.SBVR_VOCAB_PACKAGE_NAME, true);
        if (useModelVoc)
            mvPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtilities.SBVR_MODELVOC_PACKAGE_NAME, true);
        rulesPackage = (Package) ElementFinder.find(targetPackage, Package.class, PluginUtils.SBVR_RULES_PACKAGE_NAME, true);
        /*String targetName = diagramName + " BV&BR";
        Model model = project.getModel();
        int ind = 1;
        while (packageExists(model, targetName))
            targetName = String.format("%s %s(%s %d)", diagramName, "BV&BR", "Copy", ind++);
        targetPackage = elementsFactory.createPackageInstance();
        targetPackage.setOwner(model);
        targetPackage.setName(targetName);*/
        profile = PluginUtilities.getCustomizationsProfile(project);
        RepresentationTextCreator.addProvider(new RepresentationTextProviderImpl());
        try {
            Diagram diagram = ModelElementsManager.getInstance().createDiagram("Business Vocabulary", targetPackage);
            diagram.setName(diagramName);
            targetDiagram = project.getDiagram(diagram);
            initGenericTable();
            generateGeneralConcepts();
            generateVerbConcepts();
            generateBusinessRules();
        } catch (ReadOnlyElementException ex) {
            Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        sessionManager.closeSession();
        targetDiagram.open();
        targetDiagram.layout(true);
    }

    private void generateGeneralConcepts() {
        if (gcCandidates == null || gcCandidates.size() == 0 || gcList == null)
            return;
        for (String obj : gcCandidates.getListMap().keySet()) {
            SBVRExpressionModel concept = gcCandidates.getListMap().get(obj);
            createGeneralConcept(concept.toString(), concept.isModelVocabularyConcept());
        }   
    }

    private PresentationElement createGeneralConcept(String concept, boolean mvConcept) {
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "general concept", profile);
        com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class classel = elementsFactory.createClassInstance();
        classel.setName(concept);
        if (StereotypesHelper.canApplyStereotype(classel, stereotype))
            StereotypesHelper.addStereotype(classel, stereotype);
        if (mvConcept && useModelVoc)
            classel.setOwner(mvPackage);
        else
            classel.setOwner(bvPackage);
        try {
            PresentationElementsManager manager = PresentationElementsManager.getInstance();
            PresentationElement gcelement = manager.createShapeElement(classel, targetDiagram);
            PropertyManager properties = new PropertyManager();
            properties.addProperty(new BooleanProperty(PropertyID.SHOW_CONSTRAINTS, Boolean.FALSE));
            manager.setPresentationElementProperties(gcelement, properties);
            return gcelement;
        } catch (ReadOnlyElementException e) {
            Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    private void generateBusinessRules() {
        if (brCandidates == null || brCandidates.size() == 0)
            return;
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "operative business rule", profile);
        for (List<String> concepts : brCandidates.getDataset().keySet())
            for (SBVRExpressionModel concept : brCandidates.getDataset().get(concepts))
                if (brCandidates.isSelected(concepts, concept)) {
                    Constraint constraint = elementsFactory.createConstraintInstance();
                    Expression expression = elementsFactory.createExpressionInstance();
                    expression.setSymbol(concept.toString());
                    constraint.setSpecification(expression);
                    if (StereotypesHelper.canApplyStereotype(constraint, stereotype))
                        StereotypesHelper.addStereotype(constraint, stereotype);
                    constraint.setOwner(rulesPackage);
                    constraint.setName(concept.toString());
                    // Bug with representing attached constraints - turned off currently
                    attachConstraint(concept, constraint);
                    GenericTableManager.addRowElement(table, constraint);
                }
    }

    private void generateVerbConcepts() {
        if (vcCandidates == null || vcCandidates.size() == 0)
            return;
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "verb concept", profile);
        for (List<String> concepts : vcCandidates.getDataset().keySet())
            for (SBVRExpressionModel concept : vcCandidates.getDataset().get(concepts))
                if (vcCandidates.isSelected(concepts, concept)) {
                    String concept1 = concept.getExpressionElement(0);
                    String verb = concept.getExpressionElement(1);

                    // Ensure that necessary general concepts exist; if not, then they must be created
                    PresentationElement el1, el2 = null;
                    if (!gcList.contains(concept1))
                        el1 = createGeneralConcept(concept1, concept.isModelVocabularyConcept());
                    else
                        el1 = getElementWithName(targetDiagram, concept1);
                    String concept2 = concept.getExpressionElement(2);
                    if (concept2 != null && concept2.trim().length() > 0)
                        if (!gcList.contains(concept2))
                            el2 = createGeneralConcept(concept2, concept.isModelVocabularyConcept());
                        else
                            el2 = getElementWithName(targetDiagram, concept2);
                    if (el2 != null) {
                        Association association = elementsFactory.createAssociationInstance();
                        association.setName(verb);
                        Package pkg = concept.isModelVocabularyConcept() ? mvPackage : bvPackage;
                        association.setOwner(pkg);
                        ModelHelper.setClientElement(association, el1.getElement());
                        ModelHelper.setSupplierElement(association, el2.getElement());
                        ModelHelper.setNavigable(ModelHelper.getFirstMemberEnd(association), true);
                        ModelHelper.setNavigable(ModelHelper.getSecondMemberEnd(association), true);
                        try {
                            ModelElementsManager.getInstance().addElement(association, pkg);
                            if (StereotypesHelper.canApplyStereotype(association, stereotype))
                                StereotypesHelper.addStereotype(association, stereotype);
                            PresentationElementsManager manager = PresentationElementsManager.getInstance();
                            PresentationElement assocel = manager.createPathElement(association, el1, el2);
                            PropertyManager properties = new PropertyManager();
                            properties.addProperty(new BooleanProperty(PropertyID.SHOW_CONSTRAINTS, Boolean.FALSE));
                            manager.setPresentationElementProperties(assocel, properties);
                        } catch (ReadOnlyElementException e) {
                            Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, e);
                        }
                    } else {
                        // Create unary verb concept
                        Property attr = elementsFactory.createPropertyInstance();
                        attr.setName(verb.replace(" ", "_"));
                        attr.setOwner(el1.getElement());
                    }
                }
    }

    private PresentationElement getElementWithName(DiagramPresentationElement targetDiagram, String name) {
        List<PresentationElement> elements = targetDiagram.getPresentationElements();
        for (PresentationElement element : elements)
            if (element.getElement().getHumanName().substring("general concept".length() + 1).compareTo(name) == 0)
                return element;
        return null;
    }

    private void attachConstraint(SBVRExpressionModel concept, Constraint constraint) {
        for (int i = 0; i < concept.length(); i++)
            if (concept.getExpressionType(i) == ExpressionType.GENERAL_CONCEPT) {
                PresentationElement gcelement = getElementWithName(targetDiagram, concept.getExpressionElement(i));
                if (gcelement != null)
                    constraint.getConstrainedElement().add(gcelement.getElement());
            }
    }

    private void initGenericTable() throws ReadOnlyElementException {
        table = GenericTableManager.createGenericTable(project, "Business rules");
        table.setOwner(targetPackage);
        List<Object> tableElementTypes = new ArrayList<>();
        tableElementTypes.add(StereotypesHelper.getStereotype(project, "operative business rule", profile));
        GenericTableManager.setTableElementTypes(table, tableElementTypes);
        List<String> columnIds = new ArrayList<>();
        columnIds.add(NumberColumn.DEFAULT_NUMBER_COLUMN_ID);
        columnIds.add("QPROP:Element:name");
        columnIds.add("QPROP:Element:constrainedElement");
        GenericTableManager.addColumnsById(table, columnIds);
    }
    
}
