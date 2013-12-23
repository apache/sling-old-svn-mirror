function toHtmlString (str) {
	var newstr = str.replace(/\ufffd/g, "").replace(/\&/g, "&amp;").replace(/\</g, "&lt;").replace(/\>/g, "&gt;").replace(/\"/g, "&quot;");
	return newstr;
}

// author Rafael
// taken from http://stackoverflow.com/questions/831030/how-to-get-get-request-parameters-in-javascript
function getRequestParam (name) {
   if (name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search))
      return decodeURIComponent(name[1]);
}
