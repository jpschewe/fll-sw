/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Take care of initializing some variables in the servlet context.
 */
public class FLLContextListener implements ServletContextListener {

  /**
   * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
   */
  public void contextDestroyed(final ServletContextEvent event) {
    // do nothing for now
  }

  /**
   * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
   */
  public void contextInitialized(final ServletContextEvent event) {
    final ServletContext application = event.getServletContext();
    application.setAttribute("testing", "it worked");
    
    application.setAttribute(ApplicationAttributes.DATABASE, application.getRealPath("/WEB-INF/flldb"));
    
    // set some default text
    application.setAttribute(ApplicationAttributes.SCORE_PAGE_TEXT, "FLL");

  }

}
