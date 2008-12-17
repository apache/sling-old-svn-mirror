print "<html><body>"

print "Python"
print "<p>" + currentNode.getProperty('text').getString() + "</p>"

print "<!-- test access to sling java classes -->"
from java.util import LinkedList
list = LinkedList()
list.add("LinkedListTest")
print "<p>Test" + list.get(0) + "</p>"

print "</body></html>"