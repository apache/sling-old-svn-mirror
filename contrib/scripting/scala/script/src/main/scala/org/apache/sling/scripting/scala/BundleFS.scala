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
package org.apache.sling.scripting.scala

import java.net.URL
import java.io.{File, IOException, InputStream}
import org.apache.sling.scripting.scala.Utils.{valueOrElse, nullOrElse}
import org.osgi.framework.Bundle
import scala.tools.nsc.io.AbstractFile

/**
 * Implementation of {@link AbstractFile} on top of a {@link org.osgi.framework.Bundle}
 */
object BundleFS {

  /**
   * Create a new {@link AbstractFile} instance representing an
   * {@link org.osgi.framework.Bundle}
   * @param bundle
   */
  def create(bundle: Bundle): AbstractFile = {
    require(bundle != null, "bundle must not be null")

    abstract class BundleEntry(url: URL, parent: DirEntry) extends AbstractFile {
      require(url != null, "url must not be null")
      lazy val (path: String, name: String) = getPathAndName(url)
      lazy val fullName: String = (path::name::Nil).filter(_.size != 0).mkString("/")

      /**
       * @return null
       */
      def file: File = null

      def absolute = this

      /**
       * @return last modification time or 0 if not known
       */
      def lastModified: Long =
        try { url.openConnection.getLastModified }
        catch { case _ => 0 }

      @throws(classOf[IOException])
      def container: AbstractFile =
        valueOrElse(parent) {
          throw new IOException("No container")
        }

      @throws(classOf[IOException])
      def input: InputStream = url.openStream()

      /**
       * Not supported. Always throws an IOException.
       * @throws IOException
       */
      @throws(classOf[IOException])
      def output = throw new IOException("not supported: output")

      def create { unsupported }
      def delete { unsupported }

      def lookupNameUnchecked(name: String, directory: Boolean) = {
        val file = lookupName(name, directory)
        if (file == null) NonExistingFile
        else file
      }
      
      private def getPathAndName(url: URL): (String, String) = {
        val u = url.getPath
        var k = u.length
        while( (k > 0) && (u(k - 1) == '/') )
          k = k - 1

        var j = k
        while( (j > 0) && (u(j - 1) != '/') )
          j = j - 1

        (u.substring(if (j > 0) 1 else 0, if (j > 1) j - 1 else j), u.substring(j, k))
      }

      override def toString = fullName
    }

    class DirEntry(url: URL, parent: DirEntry) extends BundleEntry(url, parent) {

      /**
       * @return true
       */
      def isDirectory: Boolean = true

      def iterator: Iterator[AbstractFile] = {
        new Iterator[AbstractFile]() {
          val dirs = bundle.getEntryPaths(fullName)
          
          var nextEntry = prefetch()
          
          def hasNext() = {
            if (nextEntry == null)
              nextEntry = prefetch()
           
            nextEntry != null
          }
          
          def next() = {
            if (hasNext()) {
                val entry = nextEntry
                nextEntry = null
                entry
            }
            else {
              throw new NoSuchElementException()
            }
          }
          
          private def prefetch() = {
            if (dirs != null && dirs.hasMoreElements) {
              val entry = dirs.nextElement.asInstanceOf[String]
              var entryUrl = bundle.getResource("/" + entry)

              // Bundle.getResource seems to be inconsistent with respect to requiring
              // a trailing slash
              if (entryUrl == null) 
                entryUrl = bundle.getResource("/" + removeTralingSlash(entry))
                
              // If still null OSGi wont let use load that resource for some reason
              if (entryUrl == null) {
                null
              }
              else {
                if (entry.endsWith(".class"))
                  new FileEntry(entryUrl, DirEntry.this)
                else
                  new DirEntry(entryUrl, DirEntry.this)
              }
            }
            else 
              null
          }
          
          private def removeTralingSlash(s: String): String = 
            if (s == null || s.length == 0)
              s
            else if (s.last == '/') 
              removeTralingSlash(s.substring(0, s.length - 1))
            else
              s
        }
      }

      def lookupName(name: String, directory: Boolean): AbstractFile = {
        val entry = bundle.getEntry(fullName + "/" + name)
        nullOrElse(entry) { entry =>
          if (directory)
            new DirEntry(entry, DirEntry.this)
          else
            new FileEntry(entry, DirEntry.this)
        }
      }

    }

    class FileEntry(url: URL, parent: DirEntry) extends BundleEntry(url, parent) {

      /**
       * @return false
       */
      def isDirectory: Boolean = false
      override def sizeOption: Option[Int] = Some(bundle.getEntry(fullName).openConnection().getContentLength())
      def iterator: Iterator[AbstractFile] = Iterator.empty
      def lookupName(name: String, directory: Boolean): AbstractFile = null
    }

    new DirEntry(bundle.getResource("/"), null)
  }

}
