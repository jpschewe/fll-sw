/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.Formatter;
import java.util.List;

import fll.UserImages;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;

/**
 * Helpers for welcome.jsp.
 */
public final class Welcome {

  private Welcome() {
  }

  /**
   * Name of file in {@link UserImages#getImagesPath()} with the logo for the
   * partner.
   */
  public static final String PARTNER_LOGO_FILENAME = "partner_logo.jpg";

  /**
   * Name of file in {@link UserImages#getImagesPath()} with the logo for FLL.
   */
  public static final String FLL_LOGO_FILENAME = "fll_logo.jpg";

  /**
   * @param page used to set variables for the page
   */
  public static void populateContext(final PageContext page) {
    page.setAttribute("partner_logo",
                      String.format("%s/%s", UserImages.getImagesPath().getFileName(), PARTNER_LOGO_FILENAME));
    page.setAttribute("fll_logo", String.format("%s/%s", UserImages.getImagesPath().getFileName(), FLL_LOGO_FILENAME));
  }

  // private static final int MAX_NUM_LOGOS_PER_COLUMN = 6;

  /**
   * @param application used to read application variables
   * @param out where to write the logo HTML code
   */
  public static void outputLogos(final ServletContext application,
                                 final JspWriter out) {
    final List<String> logoFiles = WebUtils.getSponsorLogos(application);

    // final int numColumns = (int) Math.ceil((double) logoFiles.size()
    // / (double) MAX_NUM_LOGOS_PER_COLUMN);

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
            % numColumns == numColumns
                - 1) {
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
