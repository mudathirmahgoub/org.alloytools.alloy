package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;


public class Type
{
    @JsonProperty("id")
    public int id;

    public Type() {}

    public Type(int id)
    {
        this.id = id;
    }
}
