package javax.servlet.http;

import javax.servlet.EnumerationImpl;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpSession {
    private final Map<String, String> mAttributes = new HashMap<>();

    public void setAttribute(String key, String val) {
        mAttributes.put(key, val);
    }

    public Enumeration<String> getAttributeNames() {
        return new EnumerationImpl<>(mAttributes.keySet().toArray(new String[0]));
    }

    public Object getAttribute(String attrName) {
        return this.mAttributes.get(attrName);
    }
}
