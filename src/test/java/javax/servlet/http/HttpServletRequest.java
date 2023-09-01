package javax.servlet.http;

import javax.servlet.EnumerationImpl;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HttpServletRequest implements ServletRequest {

    private final Map<String, String> mParameters = new HashMap<>();
    private final Map<String, String> mHeaders = new HashMap<>();

    public String getParameter(String s) {
        return "secret";
    }

    public Map<String, String> getParameterMap() {
        return mParameters;
    }

    public String[]  getParameterValues(String fieldName) {
        return mParameters.values().toArray(new String[0]);
    }

    public javax.servlet.http.HttpSession getSession() {
        return new HttpSession();
    }

    public Enumeration getHeaderNames() {
        return new EnumerationImpl<>(mHeaders.keySet().toArray(new String[0]));
    }

    public String getProtocol() {
        return "protocol";
    }

    public String getScheme() {
        return "scheme";
    }

    public String getAuthType() {
        return "authType";
    }

    public String getQueryString() {
        return "queryString";
    }

    public String getRemoteUser() {
        return "remoteUser";
    }

    public StringBuffer getRequestURL() {
        return new StringBuffer();
    }

    public Enumeration getParameterNames() {
        return new EnumerationImpl<>(mParameters.keySet().toArray(new String[0]));
    }

    public javax.servlet.http.Cookie[] getCookies() {
        return new Cookie[0];
    }

    public String getHeader(String s) {
        return s;
    }

    public Enumeration getHeaders(String s) {
        return new EnumerationImpl(mHeaders.values().toArray(new String[0]));
    }

    public javax.servlet.ServletInputStream getInputStream() {
        return new ServletInputStream();
    }
}
