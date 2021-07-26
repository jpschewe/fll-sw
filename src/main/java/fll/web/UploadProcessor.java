/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Process uploads from JSP pages.
 */
public final class UploadProcessor {

  private UploadProcessor() {
    // no instances
  }

  /**
   * Processes <code>request</code> as a file upload and puts the results back
   * in the <code>request</code> object. Each parameter that is a file upload
   * has a value of type {@link FileItem}. Other parameters have values of type
   * {@link String}.
   *
   * @param request the web request
   * @throws FileUploadException see
   *           {@link ServletFileUpload#parseRequest(org.apache.tomcat.util.http.fileupload.RequestContext)}
   */
  public static void processUpload(final HttpServletRequest request) throws FileUploadException {
    // Parse the request
    final List<FileItem> items = UPLOAD.parseRequest(new ServletRequestContext(request));
    for (final FileItem item : items) {
      if (item.isFormField()) {
        request.setAttribute(item.getFieldName(), item.getString());
      } else {
        request.setAttribute(item.getFieldName(), item);
      }
    }
  }

  /**
   * Create a factory for disk-based file items.
   */
  private static final FileItemFactory FACTORY = new DiskFileItemFactory();

  /**
   * Create a new file upload handler.
   */
  private static final ServletFileUpload UPLOAD = new ServletFileUpload(FACTORY);

}
