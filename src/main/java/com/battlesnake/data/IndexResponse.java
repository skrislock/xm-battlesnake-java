/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlesnake.data;

import com.fasterxml.jackson.annotation.JsonProperty;


public class IndexResponse {
    private String apiversion = "1";
    private String author = "Shaun Krislock";
    private String color;
    private HeadType headType;
    private TailType tailType;


    public IndexResponse() {
    }

    public String getApiversion() {
        return this.apiversion;
    }

    public String getAuthor() {
        return this.author;
    }
    
    public String getColor() {
        return this.color;
    }

    @JsonProperty("head")
    public HeadType getHeadType() {
        return this.headType;
    }

    @JsonProperty("tail")
    public TailType getTailType() {
        return this.tailType;
    }

    // setters for method chaining
    public IndexResponse setApiversion(String apiversion) {
        this.apiversion = apiversion;
        return this;
    }
    
    public IndexResponse setColor(String color) {
        this.color = color;
        return this;
    }

    public IndexResponse setHeadType(HeadType headType) {
        this.headType = headType;
        return this;
    }

    public IndexResponse setTailType(TailType tailType) {
        this.tailType = tailType;
        return this;
    }
}
