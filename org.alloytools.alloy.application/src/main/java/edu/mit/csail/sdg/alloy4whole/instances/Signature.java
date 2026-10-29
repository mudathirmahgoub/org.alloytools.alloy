package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class Signature
{
    @JsonProperty("atoms")
    public List<Atom> atoms;

    @JsonProperty("types")
    public List<Type> types = new ArrayList<>();

    @JsonProperty("label")
    public String label;

    @JsonProperty("id")
    public int id;

    @JsonProperty("parents")
    public int parentId;

    @JsonProperty("builtIn")
    public String builtIn; // yes/no

    public String isAbstract; // yes/no

    @JsonProperty("isOne")
    public String isOne; // yes/no

    @JsonProperty("isLone")
    public String isLone; // yes/no

    @JsonProperty("isSome")
    public String isSome; // yes/no

    public String isPrivate; // yes/no

    @JsonProperty("isMeta")
    public String isMeta; // yes/no

    @JsonProperty("isExact")
    public String isExact; // yes/no

    @JsonProperty("isEnum")
    public String isEnum; // yes/no
}
