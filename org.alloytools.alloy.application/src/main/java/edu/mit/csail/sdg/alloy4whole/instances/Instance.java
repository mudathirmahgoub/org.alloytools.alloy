package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Instance
{
    @JsonProperty("signatures")
    public List<Signature> signatures;

    @JsonProperty("fields")
    public List<Field> fields;

    @JsonProperty("bitWidth")
    public int bitWidth = 4;

    @JsonProperty("maxSeq")
    public int maxSeq = 4;

    @JsonProperty("command")
    public String command;

    @JsonProperty("fileName")
    public String fileName;
}
