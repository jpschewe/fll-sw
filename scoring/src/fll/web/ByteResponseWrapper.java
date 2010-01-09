/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wrapper class for a response that stores everything in a local buffer to be
 * processed later.
 * 
 * @see HttpServletResponse
 */
public class ByteResponseWrapper extends HttpServletResponseWrapper {
  private final ByteArrayOutputStream binary;

  private final StringWriter string;

  private final PrintWriter writer;

  private final ServletOutputStream stream;

  private boolean binaryUsed = false;

  /**
   * Was {@link #getOutputStream()} used for output. 
   */
  public boolean isBinaryUsed() {
    return binaryUsed;
  }

  private boolean stringUsed = false;

  /**
   * Was {@link #getWriter()} used for output.
   */
  public boolean isStringUsed() {
    return stringUsed;
  }

  public byte[] getBinary() {
    return binary.toByteArray();
  }

  public String getString() {
    return string.getBuffer().toString();
  }

  public ByteResponseWrapper(final HttpServletResponse response) {
    super(response);
    string = new StringWriter();
    writer = new PrintWriter(string);
    binary = new ByteArrayOutputStream();
    stream = new WrapperServletOutputStream(binary);
  }

  @Override
  public PrintWriter getWriter() {
    if (binaryUsed) {
      throw new IllegalStateException("Cannot call getWriter after calling getOutputStream");
    }
    stringUsed = true;
    return writer;
  }

  @Override
  public ServletOutputStream getOutputStream() {
    if (stringUsed) {
      throw new IllegalStateException("Cannot call getOutputStream after calling getWriter");
    }
    binaryUsed = true;
    return stream;
  }

  /**
   * Wrapper for a {@link ServletOutputStream} that delegates to another output
   * stream.
   */
  private static class WrapperServletOutputStream extends ServletOutputStream {
    private final OutputStream os;

    public WrapperServletOutputStream(final OutputStream os) {
      this.os = os;
    }

    @Override
    public void close() throws IOException {
      os.close();
    }

    @Override
    public void flush() throws IOException {
      os.flush();
    }

    @Override
    public void write(final byte[] b) throws IOException {
      os.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
      os.write(b, off, len);
    }

    @Override
    public void write(final int i) throws IOException {
      os.write(i);
    }

  }
}
