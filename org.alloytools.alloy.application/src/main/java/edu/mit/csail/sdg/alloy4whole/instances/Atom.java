package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "atom")
public class Atom
{
    @XmlAttribute(name = "label")
    @JsonProperty("label")
    public String label;

    public Atom()
    {
    }

    public Atom(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
