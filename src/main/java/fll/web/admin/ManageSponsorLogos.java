/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for managing sponsor logos.
 */
@WebServlet("/admin/ManageSponsorLogos")
@MultipartConfig()
public class ManageSponsorLogos extends ImageManagement {

  private static final String BASE_PATH = "sponsor_logos";

  /**
   * Setup management for sponsor logos.
   */
  public ManageSponsorLogos() {
    super(BASE_PATH);
  }

  /**
   * @param application application variables
   * @param page set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    ImageManagement.populateContext(BASE_PATH, application, page);
  }
}
