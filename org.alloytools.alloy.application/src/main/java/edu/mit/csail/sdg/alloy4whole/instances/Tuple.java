package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Tuple
{
    @JsonProperty("atoms")
    public List<Atom> atoms;
}
