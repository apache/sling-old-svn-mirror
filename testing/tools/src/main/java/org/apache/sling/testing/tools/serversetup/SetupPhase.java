/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.tools.serversetup;

/** A single phase of the test server setup */
public interface SetupPhase {
    /** Run this phase in the context of supplied ServerSetup */
    public void run(ServerSetup owner) throws Exception;
    
    /** Is this a startup or shutdown phase? */
    public boolean isStartupPhase();
    
    /** Get the phase ID string, a list of those
     *  is used by {@link ServerSetup} to decide
     *  which phases to run
     */
    public String getId();
}
