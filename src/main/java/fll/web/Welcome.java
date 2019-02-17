/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.util.Formatter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspWriter;

import fll.Utilities;

/**
 * Helpers for welcome.jsp.
 */
public class Welcome {

  public static final int MAX_NUM_LOGOS_PER_COLUMN = 6;

  public static void outputLogos(final ServletContext application,
                                 final JspWriter out) {
    // All logos shall be located under sponsor_logos in the fll web folder.
    final String imagePath = application.getRealPath("/sponsor_logos");

    final List<String> logoFiles = Utilities.getGraphicFiles(new File(imagePath));

//    final int numColumns = (int) Math.ceil((double) logoFiles.size()
//        / (double) MAX_NUM_LOGOS_PER_COLUMN);

    final int numColumns = 3;
    
    final Formatter formatter = new Formatter(out);

    if (!logoFiles.isEmpty()) {
      formatter.format("<td align='center' width='50%%'>%n");
      formatter.format("<table width='100%%'>%n");

      for (int index = 0; index < logoFiles.size(); ++index) {
        final String file = logoFiles.get(index);

        if (index
            % numColumns == 0) {
          // even
          formatter.format("<tr>%n");
        }

        formatter.format("<td align='center'><img src='%s' /></td>%n", file);

        if (index
            % numColumns == numColumns - 1) {
          // odd
          formatter.format("</tr>%n");
          formatter.format("<tr><td colspan='%d'>&nbsp;</td></tr>%n", numColumns);
        }
      } // foreach file
      
      final int remainder = logoFiles.size()
          % numColumns;
      for (int i = remainder; i > 0; --i) {
        formatter.format("<td>&nbsp;</td>%n");
      }
      if (remainder != 0) {
        formatter.format("</tr>%n");
      }
      
      formatter.format("</table>%n");
      formatter.format("</td>");
    } // if logo files
    
  }
  
}
