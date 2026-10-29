package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;


public class Atom
{
    @JsonProperty("label")
    public String label;

    public Atom()
    {
    }

    public Atom(String label)
    {
        this.label = label;
    }
}
