package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Alloy
{
    @JsonProperty("instances")
    public List<Instance> instances;

    @JsonProperty("buildDate")
    public String buildDate;

    @JsonProperty("alloyFiles")
    public List<AlloyFile> alloyFiles;

    public Map<String, String> getAlloyFiles()
    {
        Map<String, String> files = new HashMap<>();
        if(alloyFiles != null)
        {
            for (AlloyFile file: alloyFiles)
            {
                files.put(file.fileName, file.content);
            }
        }
        return files;
    }

    /**
     * Writes the instances in the XML format of Alloy instances (see A4SolutionReader).
     */
    public void writeToXml(String xmlFile) throws Exception
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element alloy = document.createElement("alloy");
        setAttribute(alloy, "builddate", buildDate);
        document.appendChild(alloy);
        for (Instance instance : instances)
        {
            Element instanceElement = document.createElement("instance");
            setAttribute(instanceElement, "bitwidth", Integer.toString(instance.bitWidth));
            setAttribute(instanceElement, "maxseq", Integer.toString(instance.maxSeq));
            // the instances of cvc5 are static: a trace of length 1 (see A4SolutionWriter)
            setAttribute(instanceElement, "mintrace", "1");
            setAttribute(instanceElement, "maxtrace", "1");
            setAttribute(instanceElement, "command", instance.command);
            setAttribute(instanceElement, "filename", instance.fileName);
            setAttribute(instanceElement, "tracelength", "1");
            setAttribute(instanceElement, "looplength", "1");
            alloy.appendChild(instanceElement);
            for (Signature signature : instance.signatures)
            {
                Element sig = document.createElement("sig");
                setAttribute(sig, "label", signature.label);
                setAttribute(sig, "ID", Integer.toString(signature.id));
                if (signature.parentId != 0)
                {
                    setAttribute(sig, "parentID", Integer.toString(signature.parentId));
                }
                setAttribute(sig, "builtin", signature.builtIn);
                setAttribute(sig, "abstract", signature.isAbstract);
                setAttribute(sig, "one", signature.isOne);
                setAttribute(sig, "lone", signature.isLone);
                setAttribute(sig, "some", signature.isSome);
                setAttribute(sig, "private", signature.isPrivate);
                setAttribute(sig, "meta", signature.isMeta);
                setAttribute(sig, "exact", signature.isExact);
                setAttribute(sig, "enum", signature.isEnum);
                if (signature.atoms != null)
                {
                    for (Atom atom : signature.atoms)
                    {
                        sig.appendChild(atomElement(document, atom));
                    }
                }
                for (Type type : signature.types)
                {
                    sig.appendChild(typeElement(document, type));
                }
                instanceElement.appendChild(sig);
            }
            for (Field field : instance.fields)
            {
                Element fieldElement = document.createElement("field");
                setAttribute(fieldElement, "label", field.label);
                setAttribute(fieldElement, "ID", Integer.toString(field.id));
                setAttribute(fieldElement, "parentID", Integer.toString(field.parentId));
                setAttribute(fieldElement, "private", field.isPrivate);
                setAttribute(fieldElement, "meta", field.isMeta);
                if (field.tuples != null)
                {
                    for (Tuple tuple : field.tuples)
                    {
                        Element tupleElement = document.createElement("tuple");
                        for (Atom atom : tuple.atoms)
                        {
                            tupleElement.appendChild(atomElement(document, atom));
                        }
                        fieldElement.appendChild(tupleElement);
                    }
                }
                for (Types types : field.types)
                {
                    Element typesElement = document.createElement("types");
                    for (Type type : types.types)
                    {
                        typesElement.appendChild(typeElement(document, type));
                    }
                    fieldElement.appendChild(typesElement);
                }
                instanceElement.appendChild(fieldElement);
            }
        }
        if (alloyFiles != null)
        {
            for (AlloyFile file : alloyFiles)
            {
                Element source = document.createElement("source");
                setAttribute(source, "filename", file.fileName);
                setAttribute(source, "content", file.content);
                alloy.appendChild(source);
            }
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(new File(xmlFile)));
    }

    private static void setAttribute(Element element, String name, String value)
    {
        if (value != null)
        {
            element.setAttribute(name, value);
        }
    }

    private static Element atomElement(Document document, Atom atom)
    {
        Element element = document.createElement("atom");
        element.setAttribute("label", atom.label);
        return element;
    }

    private static Element typeElement(Document document, Type type)
    {
        Element element = document.createElement("type");
        element.setAttribute("ID", Integer.toString(type.id));
        return element;
    }

    /**
     * Reads the commands, file names and sources of the instances. The signatures and fields are not read.
     */
    public static Alloy readFromXml(String xmlFile) throws Exception
    {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlFile));
        Element root = document.getDocumentElement();
        Alloy alloy = new Alloy();
        alloy.buildDate = root.getAttribute("builddate");
        alloy.instances = new ArrayList<>();
        alloy.alloyFiles = new ArrayList<>();
        NodeList instances = root.getElementsByTagName("instance");
        for (int i = 0; i < instances.getLength(); i++)
        {
            Element element = (Element) instances.item(i);
            Instance instance = new Instance();
            instance.command = element.getAttribute("command");
            instance.fileName = element.getAttribute("filename");
            if (!element.getAttribute("bitwidth").isEmpty())
            {
                instance.bitWidth = Integer.parseInt(element.getAttribute("bitwidth"));
            }
            if (!element.getAttribute("maxseq").isEmpty())
            {
                instance.maxSeq = Integer.parseInt(element.getAttribute("maxseq"));
            }
            alloy.instances.add(instance);
        }
        NodeList sources = root.getElementsByTagName("source");
        for (int i = 0; i < sources.getLength(); i++)
        {
            Element element = (Element) sources.item(i);
            alloy.alloyFiles.add(new AlloyFile(element.getAttribute("filename"), element.getAttribute("content")));
        }
        return alloy;
    }

    public void writeToJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), this);
    }

    public static Alloy readFromJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Alloy alloy = objectMapper.readValue(new File(jsonFile), Alloy.class);
        return alloy;
    }
}
