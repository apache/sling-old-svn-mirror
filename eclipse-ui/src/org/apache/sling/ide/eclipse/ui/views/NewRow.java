/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.views;

import javax.jcr.PropertyType;

class NewRow {

    private String name;
    private Integer type;
    private Object value;

    public NewRow() {
        this.name = "";
        this.type = PropertyType.STRING;
        this.value = "";
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }

    public void setType(Integer type) {
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }

    public Object getName() {
        return name;
    }
    
    public Integer getType() {
        return type;
    }

    public boolean isComplete() {
        return (name!=null && name.length()>0 && value!=null && String.valueOf(value).length()>0);
    }

}