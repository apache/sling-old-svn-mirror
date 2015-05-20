/*
 * Emits for each document the direct parent path - allowing to fetch direct children by path.
 */
function(doc, meta) {
  
  // handle only sling resource documents with a valid path
  if (!(meta.id.indexOf("sling-resource:")==0 && doc.path && doc.data)) {
    return;
  }
  var pathParts = doc.path.split("/");
  if (pathParts.length < 3) {
    return;
  }
  
  // remove last element to get parent path
  pathParts.pop();
  var parentPath = pathParts.join("/");
  emit(parentPath, null);
}
