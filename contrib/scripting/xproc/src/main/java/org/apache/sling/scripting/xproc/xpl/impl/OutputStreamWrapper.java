/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.scripting.xproc.xpl.impl;

import java.io.IOException;
import java.io.OutputStream;

/** OutputStream that wraps another one using a method
 *  that's only called if the wrapped stream is actually
 *  used.
 */
abstract class OutputStreamWrapper extends OutputStream {

  protected abstract OutputStream getWrappedStream() throws IOException;
  
  @Override
  public void close() throws IOException {
    getWrappedStream().close();
  }

  @Override
  public void flush() throws IOException {
    getWrappedStream().flush();
  }

  @Override
  public void write(byte[] arg0, int arg1, int arg2) throws IOException {
    getWrappedStream().write(arg0, arg1, arg2);
  }

  @Override
  public void write(byte[] arg0) throws IOException {
    getWrappedStream().write(arg0);
  }

  @Override
  public void write(int arg0) throws IOException {
    getWrappedStream().write(arg0);
  }

}
