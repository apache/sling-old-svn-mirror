// recursive dump of a Resource in server-side javascript

function dumpResource(r, level) {
	out.print(level + " " + r + '\n');
	
	// TODO for now, "children" returns a Java
	// iterator, need a javascript wrapper
	var iterator = r.children;
	while(iterator.hasNext()) {
		dumpResource(iterator.next(), level + 1);
	}
}

dumpResource(resource, 1);
