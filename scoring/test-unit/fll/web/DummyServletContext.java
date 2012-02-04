/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.descriptor.JspConfigDescriptor;

/**
 * Dummy ServletContext for testing that does nothing. Subclasses override the
 * methods they need.
 */
public class DummyServletContext implements ServletContext {

  /**
   * @see javax.servlet.ServletContext#addFilter(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final String arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#addFilter(java.lang.String,
   *      javax.servlet.Filter)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final Filter arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#addFilter(java.lang.String,
   *      java.lang.Class)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final Class<? extends Filter> arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#addListener(java.lang.Class)
   */
  @Override
  public void addListener(final Class<? extends EventListener> arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#addListener(java.lang.String)
   */
  @Override
  public void addListener(final String arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#addListener(java.util.EventListener)
   */
  @Override
  public <T extends EventListener> void addListener(final T arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#addServlet(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final String arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#addServlet(java.lang.String,
   *      javax.servlet.Servlet)
   */
  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final Servlet arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#addServlet(java.lang.String,
   *      java.lang.Class)
   */
  @Override
  public javax.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final Class<? extends Servlet> arg1) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#createFilter(java.lang.Class)
   */
  @Override
  public <T extends Filter> T createFilter(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#createListener(java.lang.Class)
   */
  @Override
  public <T extends EventListener> T createListener(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#createServlet(java.lang.Class)
   */
  @Override
  public <T extends Servlet> T createServlet(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#declareRoles(java.lang.String[])
   */
  @Override
  public void declareRoles(final String... arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
   */
  @Override
  public Object getAttribute(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getAttributeNames()
   */
  @Override
  public Enumeration<String> getAttributeNames() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getClassLoader()
   */
  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getContext(java.lang.String)
   */
  @Override
  public ServletContext getContext(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getContextPath()
   */
  @Override
  public String getContextPath() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getDefaultSessionTrackingModes()
   */
  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getEffectiveMajorVersion()
   */
  @Override
  public int getEffectiveMajorVersion() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletContext#getEffectiveMinorVersion()
   */
  @Override
  public int getEffectiveMinorVersion() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletContext#getEffectiveSessionTrackingModes()
   */
  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getFilterRegistration(java.lang.String)
   */
  @Override
  public FilterRegistration getFilterRegistration(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getFilterRegistrations()
   */
  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
   */
  @Override
  public String getInitParameter(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getInitParameterNames()
   */
  @Override
  public Enumeration<String> getInitParameterNames() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getJspConfigDescriptor()
   */
  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getMajorVersion()
   */
  @Override
  public int getMajorVersion() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
   */
  @Override
  public String getMimeType(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getMinorVersion()
   */
  @Override
  public int getMinorVersion() {
    return 0;
  }

  /**
   * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
   */
  @Override
  public RequestDispatcher getNamedDispatcher(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
   */
  @Override
  public String getRealPath(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
   */
  @Override
  public RequestDispatcher getRequestDispatcher(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getResource(java.lang.String)
   */
  @Override
  public URL getResource(final String arg0) throws MalformedURLException {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
   */
  @Override
  public InputStream getResourceAsStream(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
   */
  @Override
  public Set<String> getResourcePaths(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServerInfo()
   */
  @Override
  public String getServerInfo() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServlet(java.lang.String)
   */
  @Override
  @Deprecated
  public Servlet getServlet(final String arg0) throws ServletException {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServletContextName()
   */
  @Override
  public String getServletContextName() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServletNames()
   */
  @Override
  @Deprecated
  public Enumeration<String> getServletNames() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServletRegistration(java.lang.String)
   */
  @Override
  public ServletRegistration getServletRegistration(final String arg0) {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServletRegistrations()
   */
  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getServlets()
   */
  @Override
  @Deprecated
  public Enumeration<Servlet> getServlets() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#getSessionCookieConfig()
   */
  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    return null;
  }

  /**
   * @see javax.servlet.ServletContext#log(java.lang.String)
   */
  @Override
  public void log(final String arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#log(java.lang.Exception,
   *      java.lang.String)
   */
  @Override
  @Deprecated
  public void log(final Exception arg0,
                  final String arg1) {
  }

  /**
   * @see javax.servlet.ServletContext#log(java.lang.String,
   *      java.lang.Throwable)
   */
  @Override
  public void log(final String arg0,
                  final Throwable arg1) {
  }

  /**
   * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
   */
  @Override
  public void removeAttribute(final String arg0) {
  }

  /**
   * @see javax.servlet.ServletContext#setAttribute(java.lang.String,
   *      java.lang.Object)
   */
  @Override
  public void setAttribute(final String arg0,
                           final Object arg1) {
  }

  /**
   * @see javax.servlet.ServletContext#setInitParameter(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public boolean setInitParameter(final String arg0,
                                  final String arg1) {
    return false;
  }

  /**
   * @see javax.servlet.ServletContext#setSessionTrackingModes(java.util.Set)
   */
  @Override
  public void setSessionTrackingModes(final Set<SessionTrackingMode> arg0) throws IllegalStateException,
      IllegalArgumentException {
  }

}
