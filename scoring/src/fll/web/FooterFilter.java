/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Formatter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Ensure that all HTML pages get the same footer.
 */
public class FooterFilter implements Filter {

  private static final Logger LOGGER = Logger.getLogger(FooterFilter.class);

  /**
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
    filterConfig = null;
  }

  /**
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
    if (response instanceof HttpServletResponse) {
      final PrintWriter out = response.getWriter();
      final CharResponseWrapper wrapper = new CharResponseWrapper((HttpServletResponse) response);
      chain.doFilter(request, wrapper);
      final String origData = wrapper.getString();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(new Formatter().format("Got content type: %s html ###%s###", wrapper.getContentType(), origData));
      }
      if ("text/html".equals(wrapper.getContentType())) {
        final CharArrayWriter caw = new CharArrayWriter();
        final int bodyIndex = origData.indexOf("</body>");
        if (-1 != bodyIndex) {
          caw.write(origData.substring(0, bodyIndex - 1));
          caw.write("\n<p>My custom footer</p>");
          caw.write("\n</body></html>");
          response.setContentLength(caw.toString().length());
          out.write(caw.toString());
        } else {
          out.write(origData);
        }
      } else {
        out.write(origData);
      }
      out.close();
    } else {
      chain.doFilter(request, response);
    }
  }

  /**
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(final FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
  }

  private FilterConfig filterConfig;
}
