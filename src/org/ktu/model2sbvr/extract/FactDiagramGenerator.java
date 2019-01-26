package org.ktu.model2sbvr.extract;

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
import com.nomagic.magicdraw.uml.RepresentationTextCreator.RepresentationTextProvider;
import com.nomagic.magicdraw.uml.RepresentationTextParams;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.CoreHelper;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.jmi.smartlistener.SmartListenerConfig;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Abstraction;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Enumeration;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ktu.model2sbvr.PluginUtilities;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.FilteredConceptModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.ktu.model2sbvr.models.SBVRExpressionModel.ExpressionType;
import org.ktu.model2sbvr.models.SourceEntry;
import vepsem.PluginUtils;

public class FactDiagramGenerator {

    private final String diagramName;
    private final Project project;
    private Package targetPackage, bvPackage, mvPackage, rulesPackage;
    private final ElementsFactory elementsFactory;
    private DiagramPresentationElement targetDiagram;
    private FilteredConceptModel gcCandidates, vcCandidates, brCandidates;
    private Profile profile;
    private Set<String> gcList = null;
    private Diagram table;
    private final boolean useModelVoc;
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/model2sbvr/messages");

    // Integration constants
    public static final String INTEGRATION_STEREOTYPE = "Integration";
    public static final String INTEGRATION_PROFILE = "Integration Profile";
    public static final String SOURCE_TAG = "sourceName";
    public static final String TARGET_TAG = "targetName";
    public static final String INTEGRATION_TYPE_TAG = "integrationType";

    /** Enumeration of integration type values */
    public static enum IntegrationType {

        /** No integration (explicit value) */
        NONE("none"),
        /** Default integration: {@linkplain #FULL}, if the names of the source and target element names are the same; {@linkplain #PARTIAL} otherwise */
        DEFAULT("default"),
        /** Partial integration */
        PARTIAL("partial"),
        /** Full integration */
        FULL("full"),
        /** Undefined integration */
        UNDEFINED("undefined");

        private final String name;

        public final String getName() {
            return name;
        }

        private IntegrationType(String name) {
            this.name = name;
        }
    }

    public Package getTargetPackage() {
        return targetPackage;
    }

    private class RepresentationTextProviderImpl implements RepresentationTextProvider {

        @Override
        public boolean accept(BaseElement element) {
            return element instanceof Association;
        }

        @Override
        public String getRepresentedText(BaseElement element, RepresentationTextParams textParams) {
            return RepresentationTextCreator.createId((Element) element, textParams.isAddColor())
                    + AbstractSBVRExtractor.extractElementText(element);
        }

        @Override
        public Map<Class<? extends Element>, SmartListenerConfig> createSmartListenerConfig() {
            Map<Class<? extends Element>, SmartListenerConfig> map = new HashMap<>();
            map.put(Element.class, SmartListenerConfig.STEREOTYPE_AND_TAGS_CONFIG);
            return map;
        }

        @Override
        public int getPriority() {
            return 0;
        }

    }

    public FactDiagramGenerator(String diagramName, boolean useModelVoc) {
        super();
        this.diagramName = diagramName;
        this.useModelVoc = useModelVoc;
        project = Application.getInstance().getProject();
        elementsFactory = project.getElementsFactory();
    }

    public void setGCCandidates(FilteredConceptModel gcCandidates) {
        this.gcCandidates = gcCandidates;
        gcList = gcCandidates.getCandidatesListText();
    }

    public void setVCCandidates(FilteredConceptModel vcCandidates) {
        this.vcCandidates = vcCandidates;
    }

    public void setBRCandidates(FilteredConceptModel brCandidates) {
        this.brCandidates = brCandidates;
    }

    private static EnumerationLiteral getIntegrationTypeElement(IntegrationType type, Enumeration enumeration) {
        if (enumeration == null)
            return null;
        for (EnumerationLiteral literal : enumeration.getOwnedLiteral())
            if (literal.getName().equals(type.getName()))
                return literal;
        return null;
    }

    protected void createTrace(NamedElement source, NamedElement target, boolean isModelConcept) throws TraceException {
        Abstraction abstraction = project.getElementsFactory().createAbstractionInstance();
        Profile prof = PluginUtilities.getIntegrationProfile(project);
        Stereotype st = StereotypesHelper.getStereotype(project, INTEGRATION_STEREOTYPE, prof);
        if (st == null)
            throw new TraceException(String.format(bundle.getString("FactDiagramGenerator.1"), INTEGRATION_STEREOTYPE));
        StereotypesHelper.addStereotype(abstraction, st);
        StereotypesHelper.createDefaultValues(abstraction, st, true);
        abstraction.setOwner(project.getModel());
        ModelHelper.setClientElement(abstraction, target);
        ModelHelper.setSupplierElement(abstraction, source);
        StereotypesHelper.setStereotypePropertyValue(abstraction, st, SOURCE_TAG, source.getName());
        StereotypesHelper.setStereotypePropertyValue(abstraction, st, TARGET_TAG, target.getName());
        Enumeration en = (Enumeration) ElementFinder.find(project.getModel(), Enumeration.class, "intType", true);
        IntegrationType type = isModelConcept || source.getName().compareTo(target.getName()) == 0 ? IntegrationType.FULL : IntegrationType.PARTIAL;
        EnumerationLiteral enLit = getIntegrationTypeElement(type, en);
        StereotypesHelper.setStereotypePropertyValue(abstraction, st, INTEGRATION_TYPE_TAG, enLit);
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
        Set<String> generated = new HashSet<>();
        Map<SourceEntry, ConceptExtractionEntry> model = gcCandidates.getDataset();
        for (SourceEntry obj : model.keySet()) {
            SBVRExpressionModel concept = model.get(obj).getCandidate(0);
            boolean createTrace = gcCandidates.isCreateTrace(obj, concept);
            String gcStr = concept.toString();
            if (!generated.contains(gcStr)) {
                generated.add(gcStr);
                createGeneralConcept(gcStr, concept, createTrace, obj);
            }
        }
    }

    private PresentationElement createGeneralConcept(String name, SBVRExpressionModel concept, boolean createTrace, SourceEntry srcEntry) {
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "general concept", profile);
        com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class classel = elementsFactory.createClassInstance();
        boolean mvConcept = concept.isModelVocabularyConcept();
        classel.setName(name);
        if (StereotypesHelper.canApplyStereotype(classel, stereotype))
            StereotypesHelper.addStereotype(classel, stereotype);
        if (mvConcept && useModelVoc)
            classel.setOwner(mvPackage);
        else
            classel.setOwner(bvPackage);
        List<Object> source = srcEntry.getSourceObjects();
        if (createTrace)
            for (Object src : source)
                try {
                    createTrace((NamedElement) src, classel, mvConcept);
                } catch (TraceException ex) {
                    Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, ex.getMessage());
                }
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
        for (SourceEntry sourceEntry : brCandidates.getDataset().keySet()) {
            List<SBVRExpressionModel> sbvrList = brCandidates.getDataset().get(sourceEntry).getCandidates();
            for (SBVRExpressionModel concept : sbvrList)
                if (brCandidates.isSelected(sourceEntry, concept)) {
                    Constraint constraint = elementsFactory.createConstraintInstance();
                    Expression expression = elementsFactory.createExpressionInstance();
                    expression.setSymbol(concept.toString());
                    constraint.setSpecification(expression);
                    if (StereotypesHelper.canApplyStereotype(constraint, stereotype))
                        StereotypesHelper.addStereotype(constraint, stereotype);
                    constraint.setOwner(rulesPackage);
                    constraint.setName(concept.toString());
                    // Bug with representing attached constraints - fixed currently
                    attachConstraint(concept, constraint);
                    boolean createTrace = brCandidates.isCreateTrace(sourceEntry, concept);
                    List<Object> source = sourceEntry.getSourceObjects();
                    if (createTrace)
                        for (Object src : source)
                            try {
                                createTrace((NamedElement) src, constraint, concept.isModelVocabularyConcept());
                            } catch (TraceException ex) {
                                Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, ex.getMessage());
                            }
                    GenericTableManager.addRowElement(table, constraint);
                }
        }
    }

    private void generateVerbConcepts() {
        if (vcCandidates == null || vcCandidates.size() == 0)
            return;
        Stereotype stereotype = StereotypesHelper.getStereotype(project, "verb concept", profile);
        for (SourceEntry sourceEntry : vcCandidates.getDataset().keySet()) {
            List<SBVRExpressionModel> sbvrList = vcCandidates.getDataset().get(sourceEntry).getCandidates();
            for (SBVRExpressionModel concept : sbvrList)
                if (vcCandidates.isSelected(sourceEntry, concept)) {
                    String concept1 = concept.getExpressionElement(0);
                    String verb = concept.getExpressionElement(1);

                    // Ensure that necessary general concepts exist; if not, then they must be created
                    PresentationElement el1, el2 = null;
                    boolean createTrace = vcCandidates.isCreateTrace(sourceEntry, concept);
                    if (!gcList.contains(concept1))
                        el1 = createGeneralConcept(concept1, concept, createTrace, sourceEntry);
                    else
                        el1 = getElementWithName(targetDiagram, concept1);
                    String concept2 = concept.getExpressionElement(2);
                    if (concept2 != null && concept2.trim().length() > 0)
                        if (!gcList.contains(concept2))
                            el2 = createGeneralConcept(concept2, concept, createTrace, sourceEntry);
                        else
                            el2 = getElementWithName(targetDiagram, concept2);
                    if (el2 != null) {
                        Association association = elementsFactory.createAssociationInstance();
                        association.setName(verb);
                        Package pkg = concept.isModelVocabularyConcept() ? mvPackage : bvPackage;
                        association.setOwner(pkg);
                        ModelHelper.setClientElement(association, el1.getElement());
                        ModelHelper.setSupplierElement(association, el2.getElement());
                        ModelHelper.setNavigable(CoreHelper.getFirstMemberEnd(association), true);
                        ModelHelper.setNavigable(CoreHelper.getSecondMemberEnd(association), true);
                        if (StereotypesHelper.canApplyStereotype(association, stereotype))
                            StereotypesHelper.addStereotype(association, stereotype);
                        List<Object> source = sourceEntry.getSourceObjects();
                        if (createTrace)
                            for (Object src : source)
                                try {
                                    createTrace((NamedElement) src, association, concept.isModelVocabularyConcept());
                                } catch (TraceException ex) {
                                    Logger.getLogger(FactDiagramGenerator.class.getName()).log(Level.SEVERE, null, ex.getMessage());
                                }
                        try {
                            ModelElementsManager.getInstance().addElement(association, pkg);
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
                        Stereotype charactSt = StereotypesHelper.getStereotype(project, "characteristic", profile);
                        if (StereotypesHelper.canApplyStereotype(attr, charactSt))
                            StereotypesHelper.addStereotype(attr, charactSt);
                        attr.setName(verb.replace(" ", "_"));
                        attr.setOwner(el1.getElement());
                    }
                }
        }
    }

    private PresentationElement getElementWithName(DiagramPresentationElement targetDiagram, String name) {
        List<PresentationElement> elements = targetDiagram.getPresentationElements();
        for (PresentationElement element : elements)
            if (element.getElement() instanceof NamedElement &&  
                    ((NamedElement)element.getElement()).getName().compareTo(name) == 0)
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
