package edu.mit.csail.sdg.alloy4whole.instances;

import com.fasterxml.jackson.annotation.JsonProperty;


public class AlloyFile
{
    @JsonProperty("fileName")
    public String fileName;

    @JsonProperty("content")
    public String content;

    public AlloyFile()
    {
    }

    public AlloyFile(String fileName, String content)
    {
        this.fileName = fileName;
        this.content  = content;
    }
}
