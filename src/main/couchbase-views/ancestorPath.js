/*
 * Emits for each document the all parent paths - allowing to fetch children and their decendants by path.
 * Includes the path of the item itself.
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
  
  while (pathParts.length >= 2) {
    // remove last element to get parent path
    var parentPath = pathParts.join("/");
    emit(parentPath, null);
    pathParts.pop();
  }
}
