package com.sap.olingo.jpa.processor.core.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpServletResponseDouble implements HttpServletResponse {

  private int setStatus;
  private ServletOutputStream outputStream = new OutPutStream();

  @Override
  public String getCharacterEncoding() {
    fail();
    return null;
  }

  @Override
  public String getContentType() {
    fail();
    return null;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return this.outputStream;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    fail();
    return null;
  }

  @Override
  public void setCharacterEncoding(String charset) {
    fail();

  }

  @Override
  public void setContentLength(int len) {
    fail();

  }

  @Override
  public void setContentType(String type) {
    fail();

  }

  @Override
  public void setBufferSize(int size) {
    fail();

  }

  @Override
  public int getBufferSize() {
    return ((OutPutStream) this.outputStream).getSize();
  }

  @Override
  public void flushBuffer() throws IOException {
    fail();

  }

  @Override
  public void resetBuffer() {
    fail();

  }

  @Override
  public boolean isCommitted() {
    fail();
    return false;
  }

  @Override
  public void reset() {
    fail();

  }

  @Override
  public void setLocale(Locale loc) {
    fail();

  }

  @Override
  public Locale getLocale() {
    fail();
    return null;
  }

  @Override
  public void addCookie(Cookie cookie) {
    fail();

  }

  @Override
  public boolean containsHeader(String name) {
    fail();
    return false;
  }

  @Override
  public String encodeURL(String url) {
    fail();
    return null;
  }

  @Override
  public String encodeRedirectURL(String url) {
    fail();
    return null;
  }

  @Override
  public String encodeUrl(String url) {
    fail();
    return null;
  }

  @Override
  public String encodeRedirectUrl(String url) {
    fail();
    return null;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    fail();

  }

  @Override
  public void sendError(int sc) throws IOException {
    fail();

  }

  @Override
  public void sendRedirect(String location) throws IOException {
    fail();

  }

  @Override
  public void setDateHeader(String name, long date) {
    fail();

  }

  @Override
  public void addDateHeader(String name, long date) {
    fail();

  }

  @Override
  public void setHeader(String name, String value) {
    fail();

  }

  @Override
  public void addHeader(String name, String value) {
    // TODO
  }

  @Override
  public void setIntHeader(String name, int value) {
    fail();

  }

  @Override
  public void addIntHeader(String name, int value) {
    fail();

  }

  @Override
  public void setStatus(int sc) {
    this.setStatus = sc;
  }

  @Override
  public void setStatus(int sc, String sm) {
    fail();

  }

  public int getStatus() {
    return setStatus;
  }

  class OutPutStream extends ServletOutputStream {
    List<Integer> buffer = new ArrayList<>();

    @Override
    public void write(int b) throws IOException {
      buffer.add(new Integer(b));
    }

    public Iterator<Integer> getBuffer() {
      return buffer.iterator();
    }

    public int getSize() {
      return buffer.size();
    }
  }

  //
  class ResultStream extends InputStream {
    private final Iterator<Integer> bufferExcess;

    public ResultStream(OutPutStream buffer) {
      super();
      this.bufferExcess = buffer.getBuffer();
    }

    @Override
    public int read() throws IOException {
      if (bufferExcess.hasNext())
        return bufferExcess.next().intValue();
      return -1;
    }

  }

  public InputStream getInputStream() {

    return new ResultStream((OutPutStream) this.outputStream);
  }
}
