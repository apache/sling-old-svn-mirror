package org.apache.sling.ide.eclipse.ui.views;

class NewRow {

    private String name;
    private Object value;

    public NewRow() {
        this.name = "";
        this.value = "";
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public Object getName() {
        return name;
    }

    public boolean isComplete() {
        return (name!=null && name.length()>0 && value!=null && String.valueOf(value).length()>0);
    }

}