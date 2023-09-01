package javax.servlet.http;

import javax.servlet.ServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

public class HttpServletResponse implements ServletResponse {

    public PrintWriter getWriter() {
        return new PrintWriter(new StringWriter());
    }

    public void setContentType(String contentType) {}

    public void sendRedirect(String s) {}
}
