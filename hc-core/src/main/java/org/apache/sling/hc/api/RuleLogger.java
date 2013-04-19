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
package org.apache.sling.hc.api;

import java.util.List;

import org.slf4j.Logger;

/** slf4j Logger that captures log output and provides
 *  the level of the highest message that was logged.
 *  Used when evaluating {@link Rule} objects, to find
 *  out whether anything was logged that needs to be reported.
 *  
 *  The convention is that messages above the DEBUG level are
 *  always reported when evaluating rules, all messages can
 *  optionally be made available in the rule evaluation results.   
 */
public interface RuleLogger extends Logger {

    /** Return the list of messages logged during rule execution */
    List<EvaluationResult.LogMessage> getMessages();
    
    /** Return the highest log level used during execution */
    EvaluationResult.LogLevel getMaxLevel();
}
