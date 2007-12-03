/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Process uploads from JSP pages.
 */
public class UploadProcessor {

  /**
   * Processes <code>request</code> as a file upload and puts the results back
   * in the <code>request</code> object. Each parameter that is a file upload
   * has a value of type {@link FileItem}. Other parameters have values of type
   * {@link String}.
   * 
   * @param request
   */
  public static void processUpload(final HttpServletRequest request) throws FileUploadException {
    // Parse the request
    final List<?> items = UPLOAD.parseRequest(request);
    final Iterator<?> iter = items.iterator();
    while (iter.hasNext()) {
      final FileItem item = (FileItem) iter.next();
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
