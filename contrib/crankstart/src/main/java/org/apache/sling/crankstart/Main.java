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
package org.apache.sling.crankstart;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

/** Execute a crankstart file */
public class Main {
    public static void main(String [] args) throws Exception {
        String crankFile = "default.crank.txt";
        if(args.length < 1) {
            System.err.println("Using default crank file " + crankFile);
            System.err.println("To use a different one, provide its name as a jar file argument");
        } else {
            crankFile = args[0];
        }
        final Reader r = new FileReader(new File(crankFile));
        try {
            final CrankstartFileProcessor p = new CrankstartFileProcessor();
            p.process(r);
            p.waitForExit();
        } finally {
            r.close();
        }
    }
}