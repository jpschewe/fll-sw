/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.debug;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import java.io.Writer;


/**
 * Implementation of JspWriter for debugging.
 *
 * @version $Revision$
 */
public class DebugJspWriter extends JspWriter {

  /**
   * Calls super(64, true)
   * 
   * @param writer where to output
   */
  public DebugJspWriter(final Writer writer) {
    super(64, true);
    _writer = writer;
  }

  public void clear() throws IOException {
    println("==== CLEAR ====");
  }

  public void clearBuffer() throws IOException {
    println("==== CLEAR BUFFER ====");
  }

  public void close() throws IOException {
    _writer.close();
  }

  public void flush() throws IOException {
    _writer.flush();
  }

  public void write(final char[] cbuf, final int off, final int len) throws IOException {
    _writer.write(cbuf, off, len);
  }

  /**
   * @return 0
   */
  public int getRemaining() {
    return 0;
  }

  public void newLine() throws IOException {
    print(System.getProperty("line.separator"));
  }

  public void print(final boolean b) throws IOException {
    _writer.write(String.valueOf(b));
  }

  public void print(final char c) throws IOException {
    _writer.write(String.valueOf(c));
  }

  public void print(final char[] s) throws IOException {
    _writer.write(s);
  }

  public void print(final double d) throws IOException {
    _writer.write(String.valueOf(d));
  }

  public void print(final float f) throws IOException {
    _writer.write(String.valueOf(f));
  }

  public void print(final int i) throws IOException {
    _writer.write(String.valueOf(i));
  }

  public void print(final long l) throws IOException {
    _writer.write(String.valueOf(l));
  }

  public void print(final Object obj) throws IOException {
    _writer.write(String.valueOf(obj));
  }

  public void print(final String s) throws IOException {
    _writer.write(s);
  }

  public void println() throws IOException {
    newLine();
  }

  public void println(final boolean b) throws IOException {
    print(b);
    newLine();
  }

  public void println(final char x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final char[] x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final double x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final float x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final int x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final long x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final Object x) throws IOException {
    print(x);
    newLine();
  }

  public void println(final String x) throws IOException {
    print(x);
    newLine();
  }
  
  private final Writer _writer;
}
