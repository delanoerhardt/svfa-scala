/**
   $Id: BasicTestCase.java,v 1.5 2006/04/21 17:14:24 livshits Exp $
*/
package securibench.micro;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import securibench.supportClasses.DummyServletConfig;

public abstract class BasicTestCase extends HttpServlet {
    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        // do nothing
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        // do nothing
    }

    @Override
    protected void doPost(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        // do nothing     
    }

    @Override
    protected void doDelete(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        // do nothing        
    }

    @Override
    protected void doPut(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
        // do nothing        
    }
    
    //added method for testing:
    @Override
	public ServletConfig getServletConfig(){
		return new DummyServletConfig();
    	
    }
}