package javax.servlet.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;

public abstract class HttpServlet {
    protected abstract void doTrace(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException;

    protected abstract void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException;

    protected abstract void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException;

    protected abstract void doDelete(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException;

    protected abstract void doPut(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException;

    //added method for testing:
    public abstract ServletConfig getServletConfig();
}
