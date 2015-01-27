use(function () {
    var test = {};

    test.text = properties.get('text') ||  resource.getPath();
    test.tag = properties.get('tag') || null;
    if (test.tag != null) {
        test.startTag = '<' + test.tag + '>';
        test.endTag = '</' + test.tag + '>';
    }
    test.includeChildren = properties.get('includeChildren') || false;
    if (test.includeChildren) {
        test.children = sightly.resource.getChildren();
    }

    return test;
});