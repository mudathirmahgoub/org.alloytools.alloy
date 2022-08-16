package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "type")
public class Type
{
    @XmlAttribute(name = "ID")
    @JsonProperty("id")
    public int id;

    public Type() {}

    public Type(int id)
    {
        this.id = id;
    }
}
