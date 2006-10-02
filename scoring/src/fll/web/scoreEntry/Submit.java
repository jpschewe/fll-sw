/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.scoreEntry;


import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Java code for submit.jsp.
 * 
 * @version $Revision$
 */
public final class Submit {
      
  private Submit() {
    
  }

  /**
   * Generate rows of a table that shows the values of all the parameters
   * expected.  
   */
  public static void generateParameterTableRows(final JspWriter out,
                                                final Document document,
                                                final HttpServletRequest request) throws IOException {
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    //check all goal min and max values
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");
      out.println("<tr>");
      out.println("  <td>" + name + "</td>");
      out.println("  <td>" + request.getParameter(name) + "</td>");
      out.println("</tr>");
    }
  }  
    
}
