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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import fll.util.FLLRuntimeException;

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
   *
   * @return if {@link #getOutputStream()} has been called
   */
  public boolean isBinaryUsed() {
    return binaryUsed;
  }

  private boolean stringUsed = false;

  /**
   * @return if {@link #getWriter()} used for output.
   */
  public boolean isStringUsed() {
    return stringUsed;
  }

  /**
   * @return th ebinary data that was written
   */
  public byte[] getBinary() {
    return binary.toByteArray();
  }

  /**
   * @return the string that was written
   */
  public String getString() {
    return string.getBuffer().toString();
  }

  /**
   * @param response passed to the parent class
   */
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

    WrapperServletOutputStream(final OutputStream os) {
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
    public void write(final byte[] b,
                      final int off,
                      final int len)
        throws IOException {
      os.write(b, off, len);
    }

    @Override
    public void write(final int i) throws IOException {
      os.write(i);
    }

    @Override
    public boolean isReady() {
      return true;
    }

    /**
     * Not supported.
     */
    @Override
    public void setWriteListener(final WriteListener arg0) {
      throw new FLLRuntimeException("Async I/O not supported by the wrapper");
    }

  }
}
