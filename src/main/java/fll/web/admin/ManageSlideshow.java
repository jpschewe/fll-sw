/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for managing slideshow images.
 */
@WebServlet("/admin/ManageSlideshow")
@MultipartConfig()
public class ManageSlideshow extends ImageManagement {

  /**
   * Setup management for slideshow images.
   */
  public ManageSlideshow() {
    super(WebUtils.SLIDESHOW_PATH);
  }

  /**
   * @param application application variables
   * @param page set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    ImageManagement.populateContext(WebUtils.SLIDESHOW_PATH, application, page);
  }
}
