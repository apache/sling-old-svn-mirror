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

package org.apache.sling.scripting.console.internal;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: chetanm
 * Date: 4/30/12
 * Time: 9:06 PM
 */
@Component
@Service
@Property(name = "felix.webconsole.label", value = ScriptConsolePlugin.NAME)
public class ScriptConsolePlugin extends SimpleWebConsolePlugin {
    private Logger log = LoggerFactory.getLogger(getClass());
    public static final String NAME = "scriptconsole";
    private static final String TITLE = "%script.title";
    private static final String[] CSS = {"/res/ui/codemirror/lib/codemirror.css","/res/ui/script-console.css"};
    private final String TEMPLATE;
    private BundleContext bundleContext;

    @Reference
    private ScriptEngineManager scriptEngineManager;

    public ScriptConsolePlugin() {
        super(NAME, TITLE, processFileNames(CSS));
        TEMPLATE = readTemplateFile("/templates/script-console.html");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void renderContent(HttpServletRequest request,
                                 HttpServletResponse response) throws ServletException, IOException {
        final PrintWriter pw = response.getWriter();
        DefaultVariableResolver varResolver = (DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request);
        varResolver.put("__scriptConfig__",getScriptConfig());
        pw.println(TEMPLATE);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String contentType = getContentType(req);
        resp.setContentType(contentType);
        if (contentType.startsWith("text/")) {
            resp.setCharacterEncoding("UTF-8");
        }
        final String script = getCodeValue(req);
        final SlingBindings bindings = new SlingBindings();
        final PrintWriter pw = resp.getWriter();
        //Populate bindings
        bindings.put(SlingBindings.REQUEST, req);
        bindings.put(SlingBindings.READER, new StringReader(script));
        bindings.put(SlingBindings.RESPONSE, resp);
        bindings.put(SlingBindings.OUT, pw);

        //Also expose the bundleContext to simplify scripts interaction with the
        //enclosing OSGi container
        bindings.put("bundleContext", bundleContext);



        final String lang = WebConsoleUtil.getParameter(req, "lang");
        final Resource resource = new RuntimeScriptResource(lang, script);
        final boolean webClient = "webconsole".equals(WebConsoleUtil.getParameter(req,"client"));

        SlingScript slingScript = resource.adaptTo(SlingScript.class);
        try {
            log.debug("Executing script {}",script);
            slingScript.eval(bindings);
        } catch (Throwable t){
            if(!webClient){
                resp.setStatus(500);
            }
            pw.println(exceptionToString(t));
            log.warn("Error in executing script",t);
        }
    }

    private String getCodeValue(HttpServletRequest req) throws IOException {
        String script = WebConsoleUtil.getParameter(req, "code");
        if(script == null){
            script = getContentFromFilePart(req, "code");
        }
        if(script == null){
            throw new IllegalArgumentException("'code' parameter not passed");
        }
        return script;
    }

    private String getContentType(HttpServletRequest req) {
        String passedContentType = WebConsoleUtil.getParameter(req,"responseContentType");
        if(passedContentType != null){
            return passedContentType;
        }
        return req.getPathInfo().endsWith(".json") ? "application/json" : "text/plain";
    }

    private String exceptionToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }


    private static String[] processFileNames(String[] cssFiles) {
        String[] css = new String[cssFiles.length];
        for(int i = 0; i < cssFiles.length; i++){
            css[i] =  '/' + NAME + CSS[i];
        }
        return css;
    }

    private String getScriptConfig() {
        try {
            return getScriptConfig0();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private String getScriptConfig0() throws JSONException {
        StringWriter sw = new StringWriter();
        JSONWriter jw = new JSONWriter(sw);
        jw.setTidy(true);
        jw.array();

        for(ScriptEngineFactory sef : scriptEngineManager.getEngineFactories()){
            jw.object();
            if(sef.getExtensions().isEmpty()){
                continue;
            }
            jw.key("langName").value(sef.getLanguageName());
            jw.key("langCode").value(sef.getExtensions().get(0));

            //Language mode as per CodeMirror names
            String mode = determineMode(sef.getExtensions());
            if(mode != null){
                jw.key("mode").value(mode);
            }

            jw.endObject();
        }

        jw.endArray();
        return sw.toString();
    }

    private String determineMode(List<String> extensions) {
        if(extensions.contains("groovy")){
            return "groovy";
        }else if (extensions.contains("esp")){
            return "javascript";
        }
        return null;
    }

    private String getContentFromFilePart(HttpServletRequest req, String paramName) throws IOException {
        String value = WebConsoleUtil.getParameter(req, paramName);
        if(value != null){
            return value;
        }
        final Map params = (Map) req.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null ){
            return null;
        }
        FileItem[] codeFile = getFileItems(params,paramName);
        if(codeFile.length == 0){
            return null;
        }
        InputStream is = null;
        try{
            is = codeFile[0].getInputStream();
            StringWriter sw = new StringWriter();
            IOUtils.copy(is,sw,"utf-8");
            return sw.toString();
        }finally{
            IOUtils.closeQuietly(is);
        }
    }

    private FileItem[] getFileItems(Map params, String name) {
        final List files = new ArrayList();
        FileItem[] items = (FileItem[]) params.get(name);
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (!items[i].isFormField() && items[i].getSize() > 0) {
                    files.add(items[i]);
                }
            }
        }
        return (FileItem[]) files.toArray(new FileItem[files.size()]);
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        super.activate(bundleContext);
        this.bundleContext = bundleContext;
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


}
