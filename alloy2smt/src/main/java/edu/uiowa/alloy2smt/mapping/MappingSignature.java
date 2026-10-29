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

//ToDo: refactor this class with alloy Signature class
public class MappingSignature
{
    @JsonProperty("label")
    public String label;
    @JsonProperty("functionName")
    public String functionName; // function name in SMT model
    @JsonProperty("id")
    public int id;
    @JsonProperty("parents")
    public List<Integer> parents = new ArrayList<>();
    @JsonProperty("builtIn")
    public boolean builtIn;
    @JsonProperty("isAbstract")
    public boolean isAbstract;
    @JsonProperty("isOne")
    public boolean isOne;
    @JsonProperty("isLone")
    public boolean isLone;
    @JsonProperty("isSome")
    public boolean isSome;
    @JsonProperty("isPrivate")
    public boolean isPrivate;
    @JsonProperty("isMeta")
    public boolean isMeta;
    @JsonProperty("isExact")
    public boolean isExact;
    @JsonProperty("isEnum")
    public boolean isEnum;
    @JsonProperty("isSubset")
    public boolean isSubset;
}
