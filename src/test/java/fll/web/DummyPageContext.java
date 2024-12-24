/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.el.ELContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;

/**
 * Dummy PageContext for testing that does nothing. Subclasses override the
 * methods they need.
 */
public class DummyPageContext extends PageContext {

  @Override
  public void initialize(final Servlet servlet,
                         final ServletRequest request,
                         final ServletResponse response,
                         final String errorPageURL,
                         final boolean needsSession,
                         final int bufferSize,
                         final boolean autoFlush)
      throws IOException, IllegalStateException, IllegalArgumentException {

  }

  @Override
  public void release() {

  }

  @Override
  public HttpSession getSession() {
    return null;
  }

  @Override
  public Object getPage() {
    return null;
  }

  @Override
  public ServletRequest getRequest() {
    return null;
  }

  @Override
  public ServletResponse getResponse() {
    return null;
  }

  @Override
  public Exception getException() {
    return null;
  }

  @Override
  public ServletConfig getServletConfig() {
    return null;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public void forward(final String relativeUrlPath) throws ServletException, IOException {

  }

  @Override
  public void include(final String relativeUrlPath) throws ServletException, IOException {

  }

  @Override
  public void include(final String relativeUrlPath,
                      final boolean flush)
      throws ServletException, IOException {

  }

  @Override
  public void handlePageException(final Exception e) throws ServletException, IOException {

  }

  @Override
  public void handlePageException(final Throwable t) throws ServletException, IOException {
  }

  @Override
  public void setAttribute(final String name,
                           final Object value) {
  }

  @Override
  public void setAttribute(final String name,
                           final Object value,
                           final int scope) {
  }

  @Override
  public Object getAttribute(final String name) {
    return null;
  }

  @Override
  public Object getAttribute(final String name,
                             final int scope) {
    return null;
  }

  @Override
  public Object findAttribute(final String name) {
    return null;
  }

  @Override
  public void removeAttribute(final String name) {
  }

  @Override
  public void removeAttribute(final String name,
                              final int scope) {
  }

  @Override
  public int getAttributesScope(final String name) {
    return 0;
  }

  @Override
  public Enumeration<String> getAttributeNamesInScope(final int scope) {
    return null;
  }

  @Override
  public JspWriter getOut() {
    return null;
  }

  @Override
  public ELContext getELContext() {
    return null;
  }

}
