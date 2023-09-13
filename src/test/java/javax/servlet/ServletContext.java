package javax.servlet;

import java.util.Enumeration;


public interface ServletContext {

    String getInitParameter(String name);

    Enumeration getInitParameterNames();
}



