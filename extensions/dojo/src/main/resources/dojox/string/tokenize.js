if(!dojo._hasResource["dojox.string.tokenize"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.string.tokenize"] = true;
dojo.provide("dojox.string.tokenize");

dojox.string.tokenize = function(/*String*/ str, /*RegExp*/ re, /*Function?*/ parseDelim, /*Object?*/ instance){
	// summary:
	//		Split a string by a regular expression with the ability to capture the delimeters
	// parseDelim:
	//		Each group (excluding the 0 group) is passed as a parameter. If the function returns
	//		a value, it's added to the list of tokens.
	// instance:
	//		Used as the "this" instance when calling parseDelim
	var tokens = [];
	var match, content, lastIndex = 0;
	while(match = re.exec(str)){
		content = str.substring(lastIndex, re.lastIndex - match[0].length);
		if(content.length){
			tokens.push(content);
		}
		if(parseDelim){
			var parsed = parseDelim.apply(instance, match.slice(1));
			if(typeof parsed != "undefined"){
				tokens.push(parsed);
			}
		}
		lastIndex = re.lastIndex;
	}
	content = str.substr(lastIndex);
	if(content.length){
		tokens.push(content);
	}
	return tokens;
}

}
