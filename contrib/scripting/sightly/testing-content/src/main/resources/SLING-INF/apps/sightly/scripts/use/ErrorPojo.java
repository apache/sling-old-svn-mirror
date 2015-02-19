package apps.sightly.scripts.use;

import javax.script.Bindings;

import org.apache.sling.scripting.sightly.pojo.Use;

public class ErrorPojo implements Use {

    @Override
    public void init(Bindings bindings) {
        Object o = null;
        // throw a silly NPE
        o.toString();
    }

    public String sayHello() {
        return "hello";
    }

}