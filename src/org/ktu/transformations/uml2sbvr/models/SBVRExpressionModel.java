package org.ktu.transformations.uml2sbvr.models;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SBVRExpressionModel implements Cloneable {

    public final static String CGC_FORMAT = "<span style='text-decoration: underline; color: teal;'>%s</span>"; 
    public final static String CBLANK_FORMAT = "<span>%s</span>"; 
    public final static String CVC_FORMAT = "<span style='font-style: italic; color: blue;'>%s</span>"; 
    public final static String CRC_FORMAT = "<span style='color: orange;'>%s</span>"; 
    
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org/ktu/transformations/uml2sbvr/messages");

    public enum ExpressionType {
        GENERAL_CONCEPT, INDIVIDUAL_CONCEPT, VERB_CONCEPT, RULE_TYPE, RULE_IF, GENERIC
    }

    public enum RuleType {
        OBLIGATION("It is obligatory that"), 
        PERMISSION("It is permitted that"); 

        private final String expr;

        RuleType(String expr) {
            this.expr = expr;
        }

        public String toString() {
            return expr;
        }
    }

    // Original data
    private List<ExpressionType> types;
    private List<String> expressions;
    private boolean auto;
    private SBVRExpressionModel general_concept;
    private final List<SBVRExpressionModel> synonymous_forms;
    private boolean mmVocConcept;               // Is it a SBVR concept which belongs metamodel vocabulary?
    private List<Boolean> identified;   	// Identified by the user previously
    // Last user modifications
    private List<String> modifications;
    private List<ExpressionType> types_modified;
    private List<Boolean> identified_mod;

    public SBVRExpressionModel() {
        types = new ArrayList<>();
        expressions = new ArrayList<>();
        identified = new ArrayList<>();
        modifications = new ArrayList<>();
        types_modified = new ArrayList<>();
        identified_mod = new ArrayList<>();
        synonymous_forms = new ArrayList<>();
        general_concept = null;
        // By default, this SBVRExpressionModel does not represent a model vocabulary concept
        mmVocConcept = false;       
    }

    public SBVRExpressionModel addGeneralConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.GENERAL_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        modifications.add(expression);
        types_modified.add(ExpressionType.GENERAL_CONCEPT);
        identified_mod.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addIndividualConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.INDIVIDUAL_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        modifications.add(expression);
        types_modified.add(ExpressionType.INDIVIDUAL_CONCEPT);
        identified_mod.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addVerbConcept(String expression, Boolean isIdentified) {
        types.add(ExpressionType.VERB_CONCEPT);
        expressions.add(expression);
        identified.add(isIdentified);
        modifications.add(expression);
        types_modified.add(ExpressionType.VERB_CONCEPT);
        identified_mod.add(isIdentified);
        return this;
    }

    public SBVRExpressionModel addRuleExpression(RuleType type) {
        types.add(ExpressionType.RULE_TYPE);
        expressions.add(type.toString());
        identified.add(true);
        modifications.add(type.toString());
        types_modified.add(ExpressionType.RULE_TYPE);
        identified_mod.add(true);
        return this;
    }

    public SBVRExpressionModel addIfExpression() {
        types.add(ExpressionType.RULE_IF);
        expressions.add("if"); 
        identified.add(true);
        modifications.add("if"); 
        types_modified.add(ExpressionType.RULE_IF);
        identified_mod.add(true);
        return this;
    }

    public SBVRExpressionModel addUnidentifiedText(String expression) {
        types.add(ExpressionType.GENERIC);
        expressions.add(expression);
        identified.add(false);
        modifications.add(expression);
        types_modified.add(ExpressionType.GENERIC);
        identified_mod.add(false);
        return this;
    }

    public SBVRExpressionModel addIdentifiedExpression(SBVRExpressionModel model) {
        types.addAll(model.types);
        expressions.addAll(model.expressions);
        identified.addAll(model.identified);
        modifications.addAll(model.modifications);
        types_modified.addAll(model.types_modified);
        identified_mod.addAll(model.identified_mod);
        return this;
    }

    public void modify(SBVRExpressionModel modification) {
        modifications.clear();
        types_modified.clear();
        identified_mod.clear();
        modifications.addAll(modification.modifications);
        types_modified.addAll(modification.types_modified);
        identified_mod.addAll(modification.identified_mod);
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public void setIdentified(boolean value) {
        for (int i = 0; i < identified.size(); i++)
            identified.set(i, value);
        for (int i = 0; i < identified_mod.size(); i++)
            identified_mod.set(i, value);
    }

    public boolean originalEqualsTo(SBVRExpressionModel model) {
        return expressions.equals(model.expressions) && types.equals(model.types);
    }

    public boolean equalsTo(String string) {
        return string.compareTo(toString()) == 0;
    }

    public String getExpressionElement(int index) {
        if (modifications.size() <= index)
            return null;
        return modifications.get(index);
    }

    public ExpressionType getExpressionType(int index) {
        if (types_modified.size() <= index)
            return null;
        return types_modified.get(index);
    }

    public int length() {
        return modifications.size();
    }

    private String getString(List<String> expressions) {
        if (expressions.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        for (String expression : expressions)
            res.append(expression).append(" "); 
        return res.toString().trim();
    }

    public String toOriginalString() {
        return getString(expressions);
    }

    public String toString() {
        return getString(modifications);
    }

    public String toUnderscoreString() {
        if (modifications.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < modifications.size(); i++)
            res.append(types_modified.get(i) == ExpressionType.RULE_TYPE ? modifications.get(i)
                    : modifications.get(i).replaceAll(" ", "_")).append(" ");
        // Remove underscore before "'" in model vocabulary concepts
        return res.toString().trim().replace("_'", " '");
    }

    public String toHTMLString(boolean addHtml, Boolean identified) {
        if (modifications.isEmpty())
            return null;
        StringBuilder res = new StringBuilder();
        if (addHtml)
            res.append("<html>"); 
        for (int i = 0; i < modifications.size(); i++)
            res.append(String.format(getFormat(types_modified.get(i),
                    identified != null ? identified : identified_mod.get(i)), modifications.get(i))).append(" "); 
        if (addHtml)
            res.append("</html>"); 
        return res.toString().trim();
    }

    private String getFormat(ExpressionType type, boolean identified) {
        if ((type == ExpressionType.GENERAL_CONCEPT || type == ExpressionType.VERB_CONCEPT) && !identified)
            return CBLANK_FORMAT;
        if (type == ExpressionType.GENERAL_CONCEPT && identified)
            return CGC_FORMAT;
        if (type == ExpressionType.VERB_CONCEPT && identified)
            return CVC_FORMAT;
        if (type == ExpressionType.RULE_TYPE || type == ExpressionType.RULE_IF)
            return CRC_FORMAT;
        return CBLANK_FORMAT;
    }

    public SBVRExpressionModel clone() {
        SBVRExpressionModel copy = new SBVRExpressionModel();
        copy.types = new ArrayList<>();
        for (ExpressionType type : types)
            copy.types.add(type);
        copy.expressions = new ArrayList<>();
        for (String str : expressions)
            copy.expressions.add(str);
        copy.identified = new ArrayList<>();
        for (Boolean ident : identified)
            copy.identified.add(ident);
        copy.modifications = new ArrayList<>();
        for (String ident : modifications)
            copy.modifications.add(ident);
        copy.types_modified = new ArrayList<>();
        for (ExpressionType type : types_modified)
            copy.types_modified.add(type);
        copy.identified_mod = new ArrayList<>();
        for (Boolean ident : identified_mod)
            copy.identified_mod.add(ident);
        copy.auto = auto;
        return copy;
    }

    public List<ExpressionType> getTypes() {
        return types;
    }

    public List<String> getExpressions() {
        return expressions;
    }
    
    public SBVRExpressionModel getGeneralConcept() {
        return general_concept;
    }

    public void setGeneralConcept(SBVRExpressionModel general_concept) throws SBVRModelException {
        if (general_concept == null)
            return;
        if (general_concept.getTypes().size() == 1 && 
                general_concept.getTypes().get(0) == ExpressionType.GENERAL_CONCEPT)
            this.general_concept = general_concept;
        else
            throw new SBVRModelException(bundle.getString("SBVRExpressionModel.1"));
    }

    public boolean isModelVocabularyConcept() {
        return mmVocConcept;
    }

    public void setModelVocabularyConcept(boolean mmVocConcept) {
        this.mmVocConcept = mmVocConcept;
    }

    public List<SBVRExpressionModel> getSynonymousForms() {
        return synonymous_forms;
    }

    public void addSynonymousForm(SBVRExpressionModel model) {
        if (model == null)
            return;
        synonymous_forms.add(model);
    }
    
    
}
