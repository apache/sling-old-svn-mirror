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
package org.apache.sling.scripting.esx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.scripting.esx.plugins.ConsoleLog;
import org.apache.sling.scripting.esx.plugins.SimpleResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Module extends SimpleBindings implements Require {

    public static final String CONTEXT_FIELD_SLING = "sling";
    public static final String CONTEXT_FIELD_PARENT = "parent";
    public static final String CONTEXT_FIELD_ID = "id";
    public static final String CONTEXT_FIELD_MAIN = "main";
    public static final String CONTEXT_FIELD_IS_LOADED = "isLoaded";
    public static final String CONTEXT_FIELD_FILENAME = "filename";
    public static final String CONTEXT_FIELD_RESOURCE = "resource";
    public static final String CONTEXT_FIELD_CHILDREN = "children";
    public static final String CONTEXT_FIELD_MODULE_RESOURCE = "moduleResource";
    public static final String CONTEXT_FIELD_EXPORTS = "exports";
    public static final String CONTEXT_FIELD_CONSOLE = "console";
    public static final String LOADER_TEXT = "text!";
    public static final String LOADER_RESOURCE = "resource!";
    public static final String LOADER_JS = "js!";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private SlingScriptHelper scriptHelper;
    private final EsxScriptEngineFactory factory;
    private ModuleScript moduleScript;
    private boolean isLoaded = false;
    private final List<Module> children = new ArrayList();
    private String loader;

    /**
     *
     * @param factory
     * @param resource
     * @param moduleScript
     * @param id
     * @param parent
     * @param scriptHelper
     * @throws ScriptException
     */
    public Module(EsxScriptEngineFactory factory, Resource resource,
            ModuleScript moduleScript, String id, Module parent,
            SlingScriptHelper scriptHelper,
            String loader) throws ScriptException {
        this.factory = factory;
        this.scriptHelper = scriptHelper;
        this.moduleScript = moduleScript;
        this.loader = loader;

        put(CONTEXT_FIELD_PARENT, parent);
        put(CONTEXT_FIELD_SLING, scriptHelper);

        put(CONTEXT_FIELD_ID, id);
        put(CONTEXT_FIELD_MAIN, (parent == null) ? this : (Module) parent.get(CONTEXT_FIELD_PARENT));
        put(CONTEXT_FIELD_IS_LOADED, isLoaded);
        put(CONTEXT_FIELD_FILENAME, moduleScript.getResource().getPath());
        put(CONTEXT_FIELD_RESOURCE, resource);
        put(CONTEXT_FIELD_CHILDREN, children);
        put(CONTEXT_FIELD_MODULE_RESOURCE, moduleScript.getResource());

        put(CONTEXT_FIELD_EXPORTS, ((JSObject) factory.getNashornEngine().eval("Object")).newObject());

        log.debug("this is the main script: " + (get(CONTEXT_FIELD_MAIN) == this));

        put(CONTEXT_FIELD_CONSOLE, new ConsoleLog((String) get(CONTEXT_FIELD_FILENAME)));
    }

    /**
     *
     * @param source
     * @return
     * @throws ScriptException
     */
    private ScriptObjectMirror decoreateScript(String source)
            throws ScriptException {

        String polyfill = "";

        if ("polyfill".equals(get(CONTEXT_FIELD_ID))) {
            //polyfill = "require('/libs/esx/esx_modules/polyfills/index.js')(this).bind(this)";
        }
        source = "//@sourceURL=" + (String) get("filename") + "\n"
                + "(function (exports, require, module, __filename,"
                + " __dirname, currentNode, console, properties, sling, simpleResource) { "
                + "var window = this;"
                + "var global = this;"
                + source
                + "})";

        // use load + filenane for older JDK versions, @sourceURL is working for latest JDK version
        source = "load( { name : \"" + get("filename") + "\","
                + " script: \""
                + StringEscapeUtils.escapeEcmaScript(source)
                + "\" } )";

        ScriptObjectMirror function = null;
        try {
            function = (ScriptObjectMirror) factory.getNashornEngine().eval(
                    source
            );
            if (function == null) {
                log.error("Function is null !");
            }
        } catch (ScriptException ex) {
            // todo: better handling in future
            throw ex;
        }
        return function;
    }

    /**
     *
     * @return @throws ScriptException
     */
    public Object runScript() throws ScriptException {
        log.debug("run script with id {}", get("id"));
        ScriptObjectMirror function = factory.getModuleCache().get((String) get(CONTEXT_FIELD_FILENAME));

        Resource moduleResource = (Resource) get(CONTEXT_FIELD_MODULE_RESOURCE);
        Resource resource = (Resource) get(CONTEXT_FIELD_RESOURCE);

        if (function == null) {
            if (moduleScript.isJsFile()) {
                function = decoreateScript(
                        //readScript(moduleResource)//  
                        readScript(moduleResource)
                );
            }

            if (moduleScript.isJsonFile()) {
                String jsonfile = readScript(moduleResource);
                function = decoreateScript(
                        "module.exports = " + jsonfile
                );
            }

            if (moduleScript.isResourceFile()) {
                Iterator<Resource> resChildren = moduleResource.listChildren();
                ArrayList<Resource> children = new ArrayList<Resource>();
                resChildren.forEachRemaining(children::add);

                put("children", children);

                ValueMap map = moduleResource.adaptTo(ValueMap.class);

                JSONObject values = new JSONObject(map);

                String jsonprop = values.toString();

                SimpleResource simpleResource = moduleResource.adaptTo(SimpleResource.class);
                put("simpleResource", simpleResource);

                String source = "exports.properties =  " + jsonprop + ";"
                        + "exports.path = currentNode.resource.path;"
                        + "exports.simpleResource = this.simpleResource;"
                        + "exports.children = this.children;";

                function = decoreateScript(source);
            }
            if (!moduleScript.isResourceFile()) {
                factory.getModuleCache().put(moduleScript.getResource().getPath(), function);
            }
        } else {
            log.debug("module " + get(CONTEXT_FIELD_ID) + " received from cache");
        }

        if (moduleScript.isTextFile()) {
            log.debug("is textfile loaidng file");
            String source = StringEscapeUtils.escapeEcmaScript(readScript(moduleResource));
            log.debug("sourcE: ");
            source = "module.exports = \"" + source + "\";";
            log.debug(source);
            function = decoreateScript(source);
            factory.getModuleCache().put(moduleScript.getResource().getPath(), function);
        }
        JSObject process = (JSObject) factory.getNashornEngine().eval("Object");
        process.setMember("domain", log);

        if (function != null) {
            SimpleBindings currentNode = new SimpleBindings();
            if (resource != null) {
                currentNode.put("resource", resource);
                currentNode.put("properties", resource.adaptTo(ValueMap.class));
            } else {
                log.debug("module id {} resource is null", get(CONTEXT_FIELD_ID));
            }

            function.call(this, get(CONTEXT_FIELD_EXPORTS), (Require) this::require, this, get(CONTEXT_FIELD_FILENAME),
                    ((Resource) get(CONTEXT_FIELD_MODULE_RESOURCE)).getParent().getPath(), currentNode,
                    (ConsoleLog) get(CONTEXT_FIELD_CONSOLE),
                    null,
                    (SlingScriptHelper) get(CONTEXT_FIELD_SLING),
                    resource.adaptTo(SimpleResource.class)
            );

        } else {
            log.warn("function not called because it is null");
        }

        put(CONTEXT_FIELD_EXPORTS, get(CONTEXT_FIELD_EXPORTS));

        return get(CONTEXT_FIELD_EXPORTS);
    }

    /**
     *
     * @param script
     * @return
     */
    public String readScript(Resource script) throws ScriptException {
        InputStream is = script.getChild("jcr:content").adaptTo(InputStream.class);
        BufferedReader esxScript = new BufferedReader(new InputStreamReader(is));
        StringBuilder buffer = new StringBuilder();
        String temp;
        try {
            while ((temp = esxScript.readLine()) != null) {
                buffer.append(temp).append("\r\n");
            }
            return buffer.toString();
        } catch (IOException ioex) {
            throw new ScriptException(ioex);
        }
    }

    /**
     *
     * @param path
     * @return
     */
    private boolean isLocalModule(String path) {
        return (path.startsWith("./") == true
                || path.startsWith("/") == true
                || path.startsWith("../") == true);
    }

    /**
     *
     * @param path
     * @param basePath
     * @return
     */
    private String normalizePath(String path, String basePath) {
        path = StringUtils.removeStart(cleanModulePathFromLoaders(path), basePath);
        return ResourceUtil.normalize(basePath + "/" + path);
    }

    /**
     * not implemented yet
     *
     * @param path
     * @return
     */
    private boolean isGlobalModule(String path) {
        return false;
    }

    /**
     *
     * @param file
     * @param type
     * @return
     * @throws ScriptException
     */
    private ModuleScript createModuleScript(Resource file, int type) throws ScriptException {
        log.debug("module created. " + file.getPath());
        Node currentNode = file.adaptTo(Node.class);
        if (currentNode != null) {
            log.debug("currentNode !) null = " + (currentNode != null));
            try {
                boolean isFile = currentNode.isNodeType(NodeType.NT_FILE);
                log.debug("isFile: " + isFile);
                if (isFile) {
                    return new ModuleScript(type, file);
                }
                log.debug("not a file " + currentNode.getMixinNodeTypes().toString());
            } catch (RepositoryException ex) {
                throw new ScriptException("cannot load file " + file.getPath());
            }
        }
        return null;

    }

    /**
     *
     * @param module
     * @param path
     * @param currentResource
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadAsFile(String module, String path,
            Resource currentResource, String loader) throws ScriptException {
        int type = ModuleScript.JS_FILE;

        // this is need to be refactored, it is this way because I followed the
        // node.js extension handling at first but switched over to requirejs
        // loader notation        
        Resource file = currentResource.getResourceResolver().getResource(path);

        // require.extensions is deprecated, however to implement this might 
        // be a good way to handle .resource loading or to implemend loader 
        // like in requirejs e.g. similar to https://github.com/requirejs/text
        // "text!some/module.html" 
        // or require("resource!/content/homepage/jcr:content")
        if (LOADER_RESOURCE.equals(loader) && file != null) {
            return new ModuleScript(ModuleScript.RESOURCE_FILE, file);
        }

        if (LOADER_TEXT.equals(loader) && file != null) {
            return new ModuleScript(ModuleScript.TEXT_FILE, file);
        }

        //special handling for json file require
        if (path.endsWith(".json") && file != null) {
            return new ModuleScript(ModuleScript.JSON_FILE, file);
        }

        if (path.endsWith(".bin") && file != null) {
            log.warn(".bin loder are currently not supported (file  requested: " + path);
        }

        try {
            if (file == null || !file.adaptTo(Node.class).isNodeType(NodeType.NT_FILE)) {
                file = currentResource.getResourceResolver().getResource(path + ".js");
                if (file == null) {
                    file = currentResource.getResourceResolver().getResource(path + ".json");
                    if (file == null) {
                        return null;
                    }
                    type = ModuleScript.JSON_FILE;
                } else {
                    type = ModuleScript.JS_FILE;
                }
            }
        } catch (RepositoryException ex) {
            log.error(module + "", ex);
        }

        return createModuleScript(file, type);
    }

    private String cleanModulePathFromLoaders(String path) {
        if (path.startsWith("resource!")) {
            return path.substring("resource!".length(), path.length());
        }

        if (path.startsWith("text!")) {
            return path.substring("text!".length(), path.length());
        }

        return path;
    }

    /**
     *
     * @param module
     * @param path
     * @param currentResource
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadAsDirectory(String module, String path, Resource currentResource, String loader) throws ScriptException {

        ResourceResolver resolver = currentResource.getResourceResolver();
        Resource packageJson = resolver.getResource(path + "/package.json");

        if (packageJson != null) {
            Node jsonFile = packageJson.adaptTo(Node.class);

            try {
                boolean isFile = (jsonFile.isNodeType(NodeType.NT_FILE) || jsonFile.isNodeType(NodeType.NT_RESOURCE));

                if (isFile) {

                    InputStream is = packageJson.getChild("jcr:content").adaptTo(InputStream.class);
                    try {
                        String jsonData = IOUtils.toString(is);

                        JSONObject json;
                        try {
                            json = new JSONObject(jsonData);
                        } catch (JSONException ex) {
                            throw new ScriptException(ex);
                        }

                        if (json.has("main")) {
                            String packageModule;
                            try {
                                packageModule = json.getString("main");
                            } catch (JSONException ex) {
                                throw new ScriptException(ex);
                            }

                            String mainpath = normalizePath(packageModule,
                                    path);

                            return loadAsFile(packageModule, mainpath, currentResource, loader);
                        }

                    } catch (IOException ex) {
                        throw new ScriptException(ex);
                    }
                }

            } catch (RepositoryException ex) {
                throw new ScriptException(ex);
            }

        }

        Resource indexjs = resolver.getResource(path + "/index.js");

        if (indexjs != null) {
            return createModuleScript(indexjs, ModuleScript.JS_FILE);
        }

        Resource indexjson = resolver.getResource(path + "/index.json");

        if (indexjson != null) {
            return createModuleScript(indexjson, ModuleScript.JSON_FILE);
        }

        Resource indexnode = resolver.getResource(path + "/index.node");
        if (indexnode != null) {
            throw new ScriptException("Node module .node (binary) loading is currently not supported");
        }

        return null;
    }

    /**
     *
     * @param module
     * @param currentResource
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadAsModule(String module, Resource currentResource, String loader) throws ScriptException {
        return loadAsModule(module, currentResource, true, loader);
    }

    /**
     *
     * @param paths
     * @return
     */
    private String[] loadModulePaths(String paths) {
        String[] parts = paths.split("/");
        List<String> dirs = new ArrayList<String>();

        for (int i = (parts.length - 1); i > 0;) {
            log.debug(parts[i]);
            if (parts[i] == "node_modules" || parts[i] == "esx_modules") {
                continue;
            }
            String part = StringUtils.join(parts, "/", 0, i);
            String dir = part + "/node_modules";
            log.debug("load dir: " + dir);
            dirs.add(part + "/esx_modules");
            dirs.add(dir);
            i = i - 1;
        }

        // if the regular module resoultion is not finding anything, try and check the
        // global paths. needs to be optimized.
        dirs.add("/apps/esx/esx_modules");
        dirs.add("/apps/esx/node_modules");
        dirs.add("/libs/esx/esx_modules");
        dirs.add("/libs/esx/node_modules");

        return dirs.stream().toArray(String[]::new);
    }

    /**
     *
     * @param module
     * @param currentResource
     * @param isFileResource
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadAsModule(String module, Resource currentResource, boolean isFileResource, String loader) throws ScriptException {
        ModuleScript script = null;

        String[] dirs = loadModulePaths(currentResource.getPath());
        log.debug("loading modules from dir path");
        for (String dir : dirs) {
            log.debug("trying to resolve. " + dir);
            Resource searchPath = currentResource.getResourceResolver().resolve(dir);
            log.debug("searchpath  = " + searchPath.getPath());
            if (searchPath != null && !ResourceUtil.isNonExistingResource(searchPath)) {
                log.debug("searchpath loadasmodule: " + searchPath.getPath());
                script = loadLocalModule(module, searchPath, false, loader);
                if (script != null) {
                    return script;
                }
            } else {
                log.debug("dir is null = " + dir);
            }
        }
        log.debug("finsihed loading dirs, script is loaded?!");
        return script;
    }

    /**
     *
     * @param module
     * @param currentResource
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadLocalModule(String module, Resource currentResource, String loader) throws ScriptException {
        return loadLocalModule(module, currentResource, false, loader);
    }

    /**
     *
     * @param resource
     * @return
     */
    private boolean resourceIsfile(Resource resource) {
        try {
            return resource.adaptTo(Node.class).isNodeType(NodeType.NT_FILE);
        } catch (RepositoryException ex) {
            log.error("resourceIsfile", ex);
        }
        return false;
    }

    /**
     *
     * @param module
     * @param currentResource
     * @param isFile
     * @return
     * @throws ScriptException
     */
    public ModuleScript loadLocalModule(String module, Resource currentResource, boolean isFile, String loader) throws ScriptException {
        String basePath = (resourceIsfile(currentResource))
                ? currentResource.getParent().getPath() : currentResource.getPath();
        String path = normalizePath(module, basePath);

        if (module.startsWith("/")) {
            path = module;
        }
        ModuleScript script = loadAsFile(module, path, currentResource, loader);

        if (script != null) {
            return script;
        }

        return loadAsDirectory(module, path, currentResource, loader); // load as directory                              
    }

    /**
     * p´
     *
     * @param module
     * @param currentResource
     * @return
     * @throws javax.script.ScriptException
     */
    public ModuleScript resolve(String module, Resource currentResource, String loader) throws ScriptException {
        // if x is core module / library return directly the one
        log.debug("resolving module: " + module);
        ModuleScript script;

        if (isGlobalModule(module)) {
            // ignore for now
        }

        if (isLocalModule(module)) {
            script = loadLocalModule(module, currentResource, loader);
            if (script != null) {
                return script;
            }
        }

        // load as module (first split path, then load)
        script = loadAsModule(module, currentResource, loader);
        if (script != null) {
            return script;
        }

        throw new ScriptException("module not found " + module);
    }

    @Override
    public Object require(String id) throws ScriptException, IOException {
        log.debug("Trying to require Module with Id: " + id);
        // different behavior if we are the main module, require directly
        // run runScript directly on this module
        if (get("id").equals(id)
                && get("main") == this) {
            return runScript();
        }

        String loader = LOADER_JS;
        if (id.startsWith(LOADER_TEXT)) {
            loader = LOADER_TEXT;
        }

        if (id.startsWith(LOADER_RESOURCE)) {
            loader = LOADER_RESOURCE;
        }
        ModuleScript subModuleScript = resolve(cleanModulePathFromLoaders(id), (Resource) get(CONTEXT_FIELD_MODULE_RESOURCE), loader);

        Module subModule = new Module(factory, (Resource) get(CONTEXT_FIELD_RESOURCE),
                subModuleScript, id, this, (SlingScriptHelper) get(CONTEXT_FIELD_SLING), loader);
        Object result = subModule.runScript();
        children.add(subModule);
        subModule.put(CONTEXT_FIELD_IS_LOADED, true);
        return result;
    }
}
