package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mit.csail.sdg.alloy4.Util;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@XmlRootElement(name = "alloy")
public class AlloySolution
{
    @XmlElement(name = "instance")
    @JsonProperty("instances")
    public List<Instance> instances;

    @XmlAttribute(name = "builddate")
    @JsonProperty("buildDate")
    public String buildDate;

    @XmlElement(name = "source")
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

    public void writeToXml(String xmlFile) throws IOException {
        // JAXBContext context = JAXBContext.newInstance(AlloySolution.class);
        // Marshaller marshaller = context.createMarshaller();
        // marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        // marshaller.marshal(this, new File(xmlFile));

        PrintWriter out = new PrintWriter(xmlFile);
        out.println("<alloy>");
        for (Instance i : instances) {
            out.print("<instance bitwidth=\"");
            out.print(i.bitWidth);
            out.print("\" maxseq=\"");
            out.print(i.maxSeq);
            out.print("\" mintrace=\"");
            out.print(i.minTrace);
            out.print("\" maxtrace=\"");
            out.print(i.minTrace);
            out.print("\" command=\"");
            Util.encodeXML(out, i.command);
            out.print("\" filename=\"");
            Util.encodeXML(out, i.fileName);
            out.print("\" tracelength=\"");
            out.print(i.traceLength);
            out.print("\" backloop=\"");
            out.print(i.backLoop);
            out.print("\">\n");
            for (Signature s : i.signatures) {
                // if the sig is the univ
                if (s.parentId == -1) {
                    out.println("<sig label=\"univ\" ID=\"" + s.id + "\" builtin=\"yes\">\n" +
                            "</sig>\n");
                    continue;
                }
                out.print("<sig label=\"");
                out.print(s.label);

                out.print("\" ID=\"");
                out.print(s.id);

                out.print("\" parentID=\"");
                out.print(s.parentId);

                out.print("\" builtIn=\"");
                out.print(s.builtIn);

                out.print("\" abstract=\"");
                out.print(s.isAbstract);

                out.print("\" one=\"");
                out.print(s.isOne);

                out.print("\" lone=\"");
                out.print(s.isOne);

                out.print("\" some=\"");
                out.print(s.isSome);

                out.print("\" private=\"");
                out.print(s.isPrivate);

                out.print("\" meta=\"");
                out.print(s.isMeta);

                out.print("\" exact=\"");
                out.print(s.isExact);

                out.print("\" enum=\"");
                out.print(s.isEnum);

                out.println("\">");

                if (s.atoms != null) {
                    for (Atom a : s.atoms) {
                        out.print("<atom label=\"");
                        out.print(a.label);
                        out.print("\"/>\n");
                    }
                }
                out.println("</sig>");
            }
            for (Field f : i.fields) {
                out.print("<field label=\"");
                out.print(f.label);
                out.print("\" ID=\"");
                out.print(f.id);
                out.print("\" parentID=\"");
                out.print(f.parentId);

                out.print("\" private=\"");
                out.print(f.isPrivate);

                out.print("\" meta=\"");
                out.print(f.isMeta);

                out.print("\">\n");
                for (Tuple t : f.tuples) {
                    out.print("<tuple>");
                    for (Atom a : t.atoms) {
                        out.print("<atom label=\"");
                        out.print(a.label);
                        out.print("\"/> ");
                    }
                    out.print("</tuple>\n");
                }
                for (Types t : f.types) {
                    out.print("<types>");
                    for (Type type : t.types) {
                        out.print("<type ID=\"");
                        out.print(type.id);
                        out.print("\"/> ");
                    }
                    out.print("</types>\n");
                }
                out.println("</field>");
            }
            out.print("\n</instance>\n");
        }
        out.println("</alloy>");
        out.close();
    }

    public static AlloySolution readFromXml(String xmlFile) // throws JAXBException
    {
//        JAXBContext context = JAXBContext.newInstance(AlloySolution.class);
//        Unmarshaller unmarshaller = context.createUnmarshaller();
//        AlloySolution alloySolution = (AlloySolution) unmarshaller.unmarshal(new File(xmlFile));
//        return alloySolution;
        return null;
    }

    public void writeToJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), this);
    }

    public static AlloySolution readFromJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        AlloySolution alloySolution = objectMapper.readValue(new File(jsonFile), AlloySolution.class);
        return alloySolution;
    }
}
