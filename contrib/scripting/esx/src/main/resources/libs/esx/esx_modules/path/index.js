
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
/*
The path module provides utilities for working with file and directory paths. It can be accessed using:

const path = require('path');
@see https://nodejs.org/api/path.html

*/
// using posix

/**
 * @see https://nodejs.org/api/path.html#path_path_delimiter
 */
var delimiter = ":";

var sep = "/";

/**
 * @see https://nodejs.org/api/path.html#path_path_basename_path_ext
 */
function basename(path, ext) {
  var startPosition = path.lastIndexOf("/") + 1;
  var endPosition = (ext) ? (path.length-ext.length) : path.length;
  return path.substring(startPosition, endPosition);
}


/**
 * @see https://nodejs.org/api/path.html#path_path_dirname_path
 */
function dirname(path) {
  if(path) {
    var dirname = "";
    var parts = path.split("/");
    var endPosition = (parts[parts.length-1] == 0) ? parts.length-2 : parts.length-1;
    for(var i=0; i < endPosition; i++){
      var part = parts[i].trim();
      if(part.length > 0)
        dirname += "/" + part;
    }
    return dirname;
  }
  return ".";
}


/**
 * @see https://nodejs.org/api/path.html#path_path_extname_path
 */
function extname(path) {
  var lastPosition = path.lastIndexOf(".");
  var baseName = basename(path);
  if(lastPosition == -1 || baseName.startsWith("."))  {
    return "";
  }
  return baseName.substring(baseName.lastIndexOf("."), baseName.length);
}

/*
@see https://nodejs.org/api/path.html#path_path_format_pathobject
pathObject <Object>
dir <String>
root <String>
base <String>
name <String>
ext <String>
Returns: <String>
*/
function format(pathObject){
  var dir = pathObject.dir;
  var root = pathObject.root;
  var base = pathObject.base;
  var name = pathObject.name;
  var ext = pathObject.ext;
  var first = dir;
  var middle = sep;
  var end = base;

  if(dir && root && base) {
    first = dir;
    middle = sep;
    end = base;
  }
  if(!dir) {
      first = root;
  }

  if(!dir || dir == root) {
    middle = "";
  }

  if(!base) {
    middle = "";
    end = name + ext;
  }

  return  first.middle.end;// for es6: `${first}${middle}${end}`
}


/**
 * @see https://nodejs.org/api/path.html#path_path_isabsolute_path
 */
function isAbsolute(path) {
  if(path && path.length == 0) {
    return false;
  }

  return path.startsWith("/");
}

/**
 * @see https://nodejs.org/api/path.html#path_path_join_paths
 */
function join() {
  var path = "";
  var max = arguments.length - 1;
  for(key in arguments) {
    var value = arguments[key];

    if((value instanceof Object)) {
      throw "TypeError: Arguments to path.join must be strings"
    }
    if(value && value.length > 0) {
      path +=  value;
      if(key < max) {
        path += sep;
      }
    }
  }
  return (path.length == 0) ? "." : normalize(path);
}


/**
 * @see https://nodejs.org/api/path.html#path_path_normalize_path
 */
function normalize(path) {
  if(path.length == 0) {
    return ".";
  }

  var parts = path.split(sep);
  var normalizedPath = "";
  for(i in parts) {
    var dir = parts[i];
    var a = parseInt(i) + 1;


    if(parts[a] == ".." || dir == ".." || dir == ".") {
      i=i+2;
      continue;
    }

    if(dir.length > 0 && dir != sep) {
      normalizedPath += sep;
      normalizedPath += dir;
    }
  }
  return normalizedPath;
}


/*

path.parse(path)
path.posix
path.relative(from, to)
path.resolve([...paths])

*/

exports.basename = basename;
exports.delimiter = delimiter;
exports.dirname = dirname;
exports.extname = extname;
exports.format = format;
exports.sep = sep;
exports.isAbsolute = isAbsolute;
exports.join = join;
exports.normalize = normalize;

exports.posix = {};
exports.posix.basename = basename;
exports.posix.delimiter = delimiter;
exports.posix.dirname = dirname;
exports.posix.extname = extname;

exports.win32 = {};
// actually not implemented yet
exports.win32.basename = basename;
exports.win32.delimiter = delimiter;
exports.win32.dirname = dirname;
exports.win32.extname = extname;