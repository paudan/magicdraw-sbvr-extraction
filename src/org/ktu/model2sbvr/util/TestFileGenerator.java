package org.ktu.model2sbvr.util;

import org.ktu.model2sbvr.extract.AbstractSBVRExtractor;
import org.ktu.model2sbvr.models.AbstractConceptModel;
import org.ktu.model2sbvr.models.ConceptExtractionEntry;
import org.ktu.model2sbvr.models.SBVRExpressionModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class TestFileGenerator {

    private AbstractSBVRExtractor extractor;
    private String description;
    boolean normalize = true;

    public TestFileGenerator(AbstractSBVRExtractor extractor, String description, boolean normalize) {
        this.extractor = extractor;
        this.description = description;
        this.normalize = normalize;
    }

    public void writeFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            writer.write(asXML(true));
        } catch (IOException e) {
            Logger.getLogger(TestFileGenerator.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private Set<SBVRExpressionModel> getExtractedGeneralCandidatesAsSet(AbstractConceptModel conceptModel) {
        return conceptModel.getDataset().values().stream()
                .map(ConceptExtractionEntry::getTestCaseSelectedCandidates)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private Set<SBVRExpressionModel> getExtractedCandidatesAsSet(AbstractConceptModel conceptModel) {
        return conceptModel.getDataset().values().stream()
                .map(ConceptExtractionEntry::getCandidates)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
    }

    private void createSourceRumblingsNodes(Document doc, Element sourceNode, Set<SBVRExpressionModel> gcCandidates, String tag) {
        Set<String> items = gcCandidates.stream().map(SBVRExpressionModel::toString).collect(Collectors.toSet());
        for (String model: items) {
            Element srcEntry = doc.createElement("rumbling");
            srcEntry.setTextContent(model);
            srcEntry.setAttribute("expected", tag);
            sourceNode.appendChild(srcEntry);
        }
    }

    public String asXML(boolean indented) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("experiment_case");
            doc.appendChild(rootElement);
            if (description != null) {
                Element entry = doc.createElement("case_name");
                entry.setTextContent(description);
                rootElement.appendChild(entry);
            }
            Element normalize = doc.createElement("normalize");
            normalize.setTextContent(Boolean.toString(this.normalize));
            rootElement.appendChild(normalize);
            Element sourceNode = doc.createElement("source");
            extractor.getGCCandidateModel().getDataset().values();
            Set<SBVRExpressionModel> gcEntries = getExtractedGeneralCandidatesAsSet(extractor.getGCCandidateModel());
            createSourceRumblingsNodes(doc, sourceNode, gcEntries, "gc");
            Set<SBVRExpressionModel> vcEntries = getExtractedCandidatesAsSet(extractor.getVCCandidateModel());
            createSourceRumblingsNodes(doc, sourceNode, vcEntries, "vc");
            Set<SBVRExpressionModel> brEntries = getExtractedCandidatesAsSet(extractor.getBRCandidateModel());
            createSourceRumblingsNodes(doc, sourceNode, brEntries, "br");
            rootElement.appendChild(sourceNode);
            Element targetNode = doc.createElement("target");
            Set<String> gcReps = gcEntries.stream().map(SBVRExpressionModel::toString).collect(Collectors.toSet());
            for (String model: gcReps) {
                Element entryNode = doc.createElement("entry");
                Element rumblingNode = doc.createElement("rumbling");
                rumblingNode.setTextContent(model);
                entryNode.appendChild(rumblingNode);
                Element conceptsNode = doc.createElement("concepts");
                Element conceptNode = doc.createElement("concept");
                conceptNode.setTextContent(model);
                conceptNode.setAttribute("type", "gc");
                conceptsNode.appendChild(conceptNode);
                entryNode.appendChild(conceptsNode);
                targetNode.appendChild(entryNode);
            }
            gcEntries = getExtractedCandidatesAsSet(extractor.getGCCandidateModel());
            gcReps = gcEntries.stream().map(SBVRExpressionModel::toString).collect(Collectors.toSet());
            Set<String> vcReps = vcEntries.stream().map(SBVRExpressionModel::toString).collect(Collectors.toSet());
            for (String model: vcReps) {
                Element entryNode = doc.createElement("entry");
                Element rumblingNode = doc.createElement("rumbling");
                rumblingNode.setTextContent(model);
                entryNode.appendChild(rumblingNode);
                Element conceptsNode = doc.createElement("concepts");
                for (String gcModel: gcReps) {
                    if (model.contains(gcModel)) {
                        Element conceptNode = doc.createElement("concept");
                        conceptNode.setTextContent(gcModel);
                        conceptNode.setAttribute("type", "gc");
                        conceptsNode.appendChild(conceptNode);
                    }
                }
                Element conceptNode = doc.createElement("concept");
                conceptNode.setTextContent(model);
                conceptNode.setAttribute("type", "vc");
                conceptsNode.appendChild(conceptNode);
                entryNode.appendChild(conceptsNode);
                targetNode.appendChild(entryNode);
            }
            Set<String> brReps = brEntries.stream().map(SBVRExpressionModel::toString).collect(Collectors.toSet());
            for (String model: brReps) {
                Element entryNode = doc.createElement("entry");
                Element rumblingNode = doc.createElement("rumbling");
                rumblingNode.setTextContent(model);
                entryNode.appendChild(rumblingNode);
                Element conceptsNode = doc.createElement("concepts");
                for (String vcModel: vcReps) {
                    if (model.contains(vcModel)) {
                        Element conceptNode = doc.createElement("concept");
                        conceptNode.setTextContent(vcModel);
                        conceptNode.setAttribute("type", "vc");
                        conceptsNode.appendChild(conceptNode);
                    }
                }
                Element conceptNode = doc.createElement("concept");
                conceptNode.setTextContent(model);
                conceptNode.setAttribute("type", "br");
                conceptsNode.appendChild(conceptNode);
                entryNode.appendChild(conceptsNode);
                targetNode.appendChild(entryNode);
            }
            rootElement.appendChild(targetNode);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            if (indented) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            }
            DOMSource source = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.getBuffer().toString();
        } catch (ParserConfigurationException | TransformerException ex) {
            Logger.getLogger(TestFileGenerator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
