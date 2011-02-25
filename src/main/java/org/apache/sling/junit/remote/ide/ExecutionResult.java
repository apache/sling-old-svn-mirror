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
package org.apache.sling.junit.remote.ide;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class ExecutionResult implements Serializable {
   private static final long serialVersionUID = 7935484811381524530L;
   private final Throwable throwable;
   
   public ExecutionResult(Result result) {
       if (result.getFailureCount() > 0) {
           final List<Throwable> failures = new ArrayList<Throwable>(result.getFailureCount());
           for (Failure f : result.getFailures()) {
               failures.add(f.getException());
           }
           
           // TODO MultipleFailureException is an internal JUnit class - 
           // we don't have it when running server-side in Sling
           // throwable = new MultipleFailureException(failures);
           throwable = failures.get(0);
       } else {
           throwable = null;
       }
   }
   
   public Throwable getException() {
       return throwable;
   }
   
   public boolean isFailure() {
       return throwable != null;
   }
}