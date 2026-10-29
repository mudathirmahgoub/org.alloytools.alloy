package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;


import java.util.ArrayList;
import java.util.List;

public class Field
{
    @JsonProperty("label")
    public String label;

    @JsonProperty("id")
    public int id;

    @JsonProperty("parentId")
    public int parentId;

    @JsonProperty("isPrivate")
    public String isPrivate;

    @JsonProperty("isMeta")
    public String isMeta;

    @JsonProperty("tuples")
    public List<Tuple> tuples;

    @JsonProperty("types")
    public List<Types> types = new ArrayList<>();
}
