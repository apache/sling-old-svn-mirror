/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.discovery.impl.setup;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;

import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.WriterAppender;

public class WithholdingAppender extends WriterAppender {

    private final ByteArrayOutputStream baos;
    private final Writer writer;

    /**
     * Install the WithholdingAppender, essentially muting all logging 
     * and withholding it until release() is called
     * @return the WithholdingAppender that can be used to get the 
     * withheld log output
     */
    public static WithholdingAppender install() {
        LogManager.getRootLogger().removeAllAppenders();
        final WithholdingAppender withholdingAppender = new WithholdingAppender(
                new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} *%-5p* [%t] %c{1}: %m\n"));
        LogManager.getRootLogger().addAppender(withholdingAppender);
        return withholdingAppender;
    }
    
    /**
     * Release this WithholdingAppender and optionally dump what was
     * withheld (eg in case of an exception)
     * @param dumpToSysout
     */
    public void release(boolean dumpToSysout) {
        LogManager.resetConfiguration();
        URL log4jPropertiesFile = getClass().getResource("/log4j.properties");
        PropertyConfigurator.configure(log4jPropertiesFile);
        if (dumpToSysout) {
            String withheldLogoutput = getBuffer();
            System.out.println(withheldLogoutput);
        }
    }
    
    public WithholdingAppender(Layout layout) {
        this.layout = layout;
        this.baos = new ByteArrayOutputStream();
        this.writer = new BufferedWriter(new OutputStreamWriter(baos));
        this.setWriter(writer);
    }
    
    public String getBuffer() {
        try{
            writer.flush();
        } catch(IOException e) {
            // ignore
        }
        return baos.toString();
    }
}
