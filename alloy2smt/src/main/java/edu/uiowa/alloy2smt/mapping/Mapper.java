/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Mapper
{
    @JsonProperty("signatures")
    public List<MappingSignature> signatures = new ArrayList<>();

    @JsonProperty("fields")
    public List<MappingField> fields = new ArrayList<>();

    public void writeToJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFile), this);
    }

    public static Mapper readFromJson(String jsonFile) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        Mapper mapper = objectMapper.readValue(new File(jsonFile), Mapper.class);
        return mapper;
    }
}
