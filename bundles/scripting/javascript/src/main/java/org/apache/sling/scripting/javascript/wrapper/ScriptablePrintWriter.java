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
package org.apache.sling.scripting.javascript.wrapper;

import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.javascript.SlingWrapper;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

public class ScriptablePrintWriter extends ScriptableObject implements SlingWrapper {

    public static final String CLASSNAME = "PrintWriter";
    private static final Class<?> [] WRAPPED_CLASSES = { PrintWriter.class };

    private PrintWriter writer;

    // the locale to use for printf
    private Locale locale;

    public ScriptablePrintWriter() {
    }

    public ScriptablePrintWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void jsConstructor(Object res) {
        this.writer = (PrintWriter) res;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    public Class<?> [] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }
    
    // print args to writer if any
    // this method supports write(Object)
    public static void jsFunction_write(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {
        print(thisObj, args);
    }

    // print args to writer if any
    // this method supports print(Object)
    public static void jsFunction_print(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {
        print(thisObj, args);
    }

    // print a formatted string to the writer. The first arg is used as the
    // formatter Locale if it is a Locale instance. The next argument is the
    // format string followed by format arguments.
    // This method supports printf(Locale, String, Object...) and
    // printf(String, Object...)
    public static void jsFunction_printf(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {

        if (args.length > 0) {

            // the index of the next argument to consider
            int nextIdx = 0;

            // the local for printf
            Locale locale = null;

            if (args[nextIdx] instanceof Locale) {

                // the Locale is the first argument, use it an increment idx
                locale = (Locale) args[nextIdx];
                nextIdx++;

            } else {

                // get the per-HTTP request local or the default locale
                locale = ((ScriptablePrintWriter) thisObj).getLocale();
            }

            // only continue, if there is at least an other argument
            // containing the format string
            if (args.length > nextIdx) {

                // the format string
                String format = ScriptRuntime.toString(args[nextIdx]);

                // arguments starting after that are formatting arguments
                nextIdx++;
                Object[] formatArgs = new Object[args.length - nextIdx];
                System.arraycopy(args, nextIdx, formatArgs, 0,
                    formatArgs.length);

                // now get the writer and call printf
                PrintWriter writer = ((ScriptablePrintWriter) thisObj).writer;
                writer.printf(locale, format, formatArgs);
            }
        }
    }

    // print args to the writer (if any) and append a line feed
    // this method supports println(Object)
    public static void jsFunction_println(Context cx, Scriptable thisObj,
            Object[] args, Function funObj) {
        print(thisObj, args).println();
    }

    // ---------- Wrapper interface --------------------------------------------

    // returns the wrapped print writer
    public Object unwrap() {
        return writer;
    }

    // ---------- internal helper ----------------------------------------------

    // print all arguments as strings to the writer
    private static PrintWriter print(Object thisObj, Object[] args) {
        PrintWriter writer = ((ScriptablePrintWriter) thisObj).writer;
        for (Object arg : args) {
            writer.print(ScriptRuntime.toString(arg));
        }
        return writer;
    }

    // helper method to return the locale to use for this instance:
    //    - if the global scope has a "request" object which is a
    //      HttpServletRequest, we call getLocale() on that object
    //    - Otherwise or if getLocale returns null, we use the platform default
    private Locale getLocale() {
        if (locale == null) {
            
            try {
                // check whether we have a request object which has the locale
                Object reqObj = ScriptRuntime.name(Context.getCurrentContext(),
                    this, SlingBindings.REQUEST);
                if (reqObj instanceof Wrapper) {
                    Object wrapped = ((Wrapper) reqObj).unwrap();
                    if (wrapped instanceof HttpServletRequest) {
                        locale = ((HttpServletRequest) wrapped).getLocale();
                    }
                }
            } catch (Exception e) {
                // ignore any exceptions resulting from this and use default
            }

            // default, if the no request locale or no request is available
            if (locale == null) {
                locale = Locale.getDefault();
            }

        }

        return locale;
    }
}
