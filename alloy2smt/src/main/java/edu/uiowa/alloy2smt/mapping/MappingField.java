/*
 * This file is part of alloy2smt.
 * Copyright (C) 2018-2019  The University of Iowa
 *
 * @author Mudathir Mohamed, Paul Meng
 *
 */

package edu.uiowa.alloy2smt.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
public class MappingField
{
    @JsonProperty("label")
    public String label;
    @JsonProperty("functionName")
    public String functionName; // function name in SMT model
    @JsonProperty("id")
    public int id;
    @JsonProperty("parentId")
    public int parentId;
    @JsonProperty("isPrivate")
    public boolean isPrivate;
    @JsonProperty("isMeta")
    public boolean isMeta;
    @JsonProperty("types")
    public List<List<MappingType>> types = new ArrayList<>();
}
