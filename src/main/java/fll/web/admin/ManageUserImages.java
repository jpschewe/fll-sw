/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.UserImages;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.Welcome;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import jakarta.servlet.jsp.PageContext;

/**
 * Backend logic for managing user images.
 */
@WebServlet("/admin/ManageUserImages")
@MultipartConfig()
public class ManageUserImages extends BaseFLLServlet {

  /**
   * @param page setup page variables
   */
  public static void populateContext(final PageContext page) {
    page.setAttribute("uuid", UUID.randomUUID().toString());
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), true)) {
      return;
    }

    final @Nullable Part partnerLogoDefault = request.getPart("partner_logo_default");
    final @Nullable Part partnerLogo = request.getPart("partner_logo");
    if (null != partnerLogoDefault) {
      UserImages.useDefaultPartnerLogo();
    } else if (null != partnerLogo) {
      partnerLogo.write(UserImages.getImagesPath().resolve(Welcome.PARTNER_LOGO_FILENAME).toAbsolutePath().toString());
    }

    final @Nullable Part fllLogoDefault = request.getPart("fll_logo_default");
    final @Nullable Part fllLogo = request.getPart("fll_logo");
    if (null != fllLogoDefault) {
      UserImages.useDefaultFllLogo();
    } else if (null != fllLogo
        && fllLogo.getSize() > 0) {
      fllLogo.write(UserImages.getImagesPath().resolve(Welcome.FLL_LOGO_FILENAME).toAbsolutePath().toString());
    }

    final String referrer = request.getHeader("Referer");
    response.sendRedirect(response.encodeRedirectURL(StringUtils.isEmpty(referrer) ? "index.jsp" : referrer));
  }

}
