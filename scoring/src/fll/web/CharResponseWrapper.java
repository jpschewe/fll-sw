/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Wrapper class for a response that stores everything in a local buffer to be
 * processed later.
 */
public class CharResponseWrapper extends HttpServletResponseWrapper {
  private final CharArrayWriter output;

  public String getString() {
    return output.toString();
  }

  public CharResponseWrapper(final HttpServletResponse response) {
    super(response);
    output = new CharArrayWriter();
  }

  public PrintWriter getWriter() {
    return new PrintWriter(output);
  }

}
