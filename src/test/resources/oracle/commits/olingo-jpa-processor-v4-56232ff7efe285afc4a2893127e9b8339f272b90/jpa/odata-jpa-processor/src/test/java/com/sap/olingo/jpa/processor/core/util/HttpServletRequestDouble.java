package com.sap.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.olingo.commons.api.http.HttpMethod;

public class HttpServletRequestDouble implements HttpServletRequest {
  private final HttpRequestHeaderDouble reqHeader;
  private final String queryString;
  private final StringBuffer url;
  private final StringBuffer input;

  public HttpServletRequestDouble(final String uri) throws IOException {
    this(uri, null);
  }

  public HttpServletRequestDouble(final String uri, final StringBuffer body) throws IOException {
    this(uri, body, null);
  }

  public HttpServletRequestDouble(final String uri, final StringBuffer body,
      final Map<String, List<String>> headers) throws IOException {

    super();
    this.reqHeader = new HttpRequestHeaderDouble();
    String[] uriParts = uri.split("\\?");
    this.url = new StringBuffer(uriParts[0]);
    queryString = (uriParts.length == 2) ? uriParts[1] : null;
    this.input = body;
    if (uri.contains("$batch")) {
      reqHeader.setBatchRequest();
    }
    this.reqHeader.setHeaders(headers);
  }

  @Override
  public Object getAttribute(final String name) {
    if (!"requestMapping".equals(name))
      fail();
    return null;
  }

  @Override
  public Enumeration<?> getAttributeNames() {
    fail();
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    fail();
    return null;
  }

  @Override
  public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
    fail();

  }

  @Override
  public int getContentLength() {
    fail();
    return 0;
  }

  @Override
  public String getContentType() {
    fail();
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new ServletInputStreamDouble(input);
  }

  @Override
  public String getParameter(final String name) {
    fail();
    return null;
  }

  @Override
  public Enumeration<?> getParameterNames() {
    fail();
    return null;
  }

  @Override
  public String[] getParameterValues(final String name) {
    fail();
    return null;
  }

  @Override
  public Map<?, ?> getParameterMap() {
    fail();
    return null;
  }

  @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @Override
  public String getScheme() {
    fail();
    return null;
  }

  @Override
  public String getServerName() {
    fail();
    return null;
  }

  @Override
  public int getServerPort() {
    fail();
    return 0;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    fail();
    return null;
  }

  @Override
  public String getRemoteAddr() {
    fail();
    return null;
  }

  @Override
  public String getRemoteHost() {
    fail();
    return null;
  }

  @Override
  public void setAttribute(final String name, final Object o) {
    fail();

  }

  @Override
  public void removeAttribute(final String name) {
    fail();

  }

  @Override
  public Locale getLocale() {
    fail();
    return null;
  }

  @Override
  public Enumeration<?> getLocales() {
    fail();
    return null;
  }

  @Override
  public boolean isSecure() {
    fail();
    return false;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(final String path) {
    fail();
    return null;
  }

  @Override
  public String getRealPath(final String path) {
    fail();
    return null;
  }

  @Override
  public int getRemotePort() {
    fail();
    return 0;
  }

  @Override
  public String getLocalName() {
    fail();
    return null;
  }

  @Override
  public String getLocalAddr() {
    fail();
    return null;
  }

  @Override
  public int getLocalPort() {
    fail();
    return 0;
  }

  @Override
  public String getAuthType() {
    fail();
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    fail();
    return null;
  }

  @Override
  public long getDateHeader(final String name) {
    fail();
    return 0;
  }

  @Override
  public String getHeader(final String name) {
    return null;
  }

  @Override
  public Enumeration<?> getHeaders(final String name) {

    return reqHeader.get(name);
  }

  @Override
  public Enumeration<?> getHeaderNames() {
    return reqHeader.getEnumerator();
  }

  @Override
  public int getIntHeader(final String name) {
    fail();
    return 0;
  }

  @Override
  public String getMethod() {
    if (url.toString().contains("$batch"))
      return HttpMethod.POST.toString();
    else
      return HttpMethod.GET.toString();
  }

  @Override
  public String getPathInfo() {
    fail();
    return null;
  }

  @Override
  public String getPathTranslated() {
    fail();
    return null;
  }

  @Override
  public String getContextPath() {
    fail();
    return null;
  }

  @Override
  public String getQueryString() {
    // $orderby=Roles/$count%20desc,Address/Region%20asc&$select=ID,Name1
    return queryString;
  }

  @Override
  public String getRemoteUser() {
    fail();
    return null;
  }

  @Override
  public boolean isUserInRole(final String role) {
    fail();
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    fail();
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    fail();
    return null;
  }

  @Override
  public String getRequestURI() {
    fail();
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    // http://localhost:8090/BuPa/BuPa.svc/AdministrativeDivisions(DivisionCode='BE252',CodeID='3',CodePublisher='NUTS')/Parent/Parent
    // http://localhost:8090/BuPa/BuPa.svc/Organizations
    return url;
  }

  @Override
  public String getServletPath() {
    return "/Olingo.svc";
  }

  @Override
  public HttpSession getSession(final boolean create) {
    return null;
  }

  @Override
  public HttpSession getSession() {
    fail();
    return null;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    fail();
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    fail();
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    fail();
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    fail();
    return false;
  }

}
