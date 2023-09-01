package javax.servlet;

import java.util.Enumeration;

public interface ServletConfig {

    String getInitParameter(String arg0);

    Enumeration getInitParameterNames();

    ServletContext getServletContext();

    String getServletName();
}
