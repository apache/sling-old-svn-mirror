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
package org.apache.sling.scripting.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModel;

import javax.jcr.Node;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;

import org.apache.sling.scripting.freemarker.wrapper.NodeModel;

public class FreemarkerEngineHelper {

    private Configuration configuration;
    private Node node;

    public FreemarkerEngineHelper(Node node) {
        configuration = new Configuration();
        this.node = node;
    }

    public String evalToString(String templateContent) throws Exception {
        StringReader reader = new StringReader(templateContent);
        Template template = new Template("test", reader, configuration);
        
        Map<String, TemplateModel> data = new HashMap<String, TemplateModel>();
        data.put("node", new NodeModel(node));

        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

}
