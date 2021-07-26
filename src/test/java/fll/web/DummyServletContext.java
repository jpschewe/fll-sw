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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

/**
 * Dummy ServletContext for testing that does nothing. Subclasses override the
 * methods they need.
 */
public class DummyServletContext implements ServletContext {

  /**
   * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final String arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
   *      jakarta.servlet.Filter)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final Filter arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
   *      java.lang.Class)
   */
  @Override
  public Dynamic addFilter(final String arg0,
                           final Class<? extends Filter> arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#addListener(java.lang.Class)
   */
  @Override
  public void addListener(final Class<? extends EventListener> arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#addListener(java.lang.String)
   */
  @Override
  public void addListener(final String arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#addListener(java.util.EventListener)
   */
  @Override
  public <T extends EventListener> void addListener(final T arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final String arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
   *      jakarta.servlet.Servlet)
   */
  @Override
  public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final Servlet arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
   *      java.lang.Class)
   */
  @Override
  public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String arg0,
                                                              final Class<? extends Servlet> arg1) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#createFilter(java.lang.Class)
   */
  @Override
  public <T extends Filter> T createFilter(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#createListener(java.lang.Class)
   */
  @Override
  public <T extends EventListener> T createListener(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#createServlet(java.lang.Class)
   */
  @Override
  public <T extends Servlet> T createServlet(final Class<T> arg0) throws ServletException {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#declareRoles(java.lang.String[])
   */
  @Override
  public void declareRoles(final String... arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#getAttribute(java.lang.String)
   */
  @Override
  public Object getAttribute(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getAttributeNames()
   */
  @Override
  public Enumeration<String> getAttributeNames() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getClassLoader()
   */
  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getContext(java.lang.String)
   */
  @Override
  public ServletContext getContext(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getContextPath()
   */
  @Override
  public String getContextPath() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getDefaultSessionTrackingModes()
   */
  @Override
  public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getEffectiveMajorVersion()
   */
  @Override
  public int getEffectiveMajorVersion() {
    return 0;
  }

  /**
   * @see jakarta.servlet.ServletContext#getEffectiveMinorVersion()
   */
  @Override
  public int getEffectiveMinorVersion() {
    return 0;
  }

  /**
   * @see jakarta.servlet.ServletContext#getEffectiveSessionTrackingModes()
   */
  @Override
  public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getFilterRegistration(java.lang.String)
   */
  @Override
  public FilterRegistration getFilterRegistration(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getFilterRegistrations()
   */
  @Override
  public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getInitParameter(java.lang.String)
   */
  @Override
  public String getInitParameter(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getInitParameterNames()
   */
  @Override
  public Enumeration<String> getInitParameterNames() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getJspConfigDescriptor()
   */
  @Override
  public JspConfigDescriptor getJspConfigDescriptor() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getMajorVersion()
   */
  @Override
  public int getMajorVersion() {
    return 0;
  }

  /**
   * @see jakarta.servlet.ServletContext#getMimeType(java.lang.String)
   */
  @Override
  public String getMimeType(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getMinorVersion()
   */
  @Override
  public int getMinorVersion() {
    return 0;
  }

  /**
   * @see jakarta.servlet.ServletContext#getNamedDispatcher(java.lang.String)
   */
  @Override
  public RequestDispatcher getNamedDispatcher(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getRealPath(java.lang.String)
   */
  @Override
  public String getRealPath(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getRequestDispatcher(java.lang.String)
   */
  @Override
  public RequestDispatcher getRequestDispatcher(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getResource(java.lang.String)
   */
  @Override
  public URL getResource(final String arg0) throws MalformedURLException {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getResourceAsStream(java.lang.String)
   */
  @Override
  public InputStream getResourceAsStream(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getResourcePaths(java.lang.String)
   */
  @Override
  public Set<String> getResourcePaths(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServerInfo()
   */
  @Override
  public String getServerInfo() {
    return null;
  }

  @SuppressWarnings("deprecation")
  @Override
  @Deprecated
  public Servlet getServlet(final String arg0) throws ServletException {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServletContextName()
   */
  @Override
  public String getServletContextName() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServletNames()
   */
  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public Enumeration<String> getServletNames() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServletRegistration(java.lang.String)
   */
  @Override
  public ServletRegistration getServletRegistration(final String arg0) {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServletRegistrations()
   */
  @Override
  public Map<String, ? extends ServletRegistration> getServletRegistrations() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getServlets()
   */
  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public Enumeration<Servlet> getServlets() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#getSessionCookieConfig()
   */
  @Override
  public SessionCookieConfig getSessionCookieConfig() {
    return null;
  }

  /**
   * @see jakarta.servlet.ServletContext#log(java.lang.String)
   */
  @Override
  public void log(final String arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#log(java.lang.Exception,
   *      java.lang.String)
   */
  @Override
  @Deprecated
  @SuppressWarnings("deprecation")
  public void log(final Exception arg0,
                  final String arg1) {
  }

  /**
   * @see jakarta.servlet.ServletContext#log(java.lang.String,
   *      java.lang.Throwable)
   */
  @Override
  public void log(final String arg0,
                  final Throwable arg1) {
  }

  /**
   * @see jakarta.servlet.ServletContext#removeAttribute(java.lang.String)
   */
  @Override
  public void removeAttribute(final String arg0) {
  }

  /**
   * @see jakarta.servlet.ServletContext#setAttribute(java.lang.String,
   *      java.lang.Object)
   */
  @Override
  public void setAttribute(final String arg0,
                           final Object arg1) {
  }

  /**
   * @see jakarta.servlet.ServletContext#setInitParameter(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public boolean setInitParameter(final String arg0,
                                  final String arg1) {
    return false;
  }

  /**
   * @see jakarta.servlet.ServletContext#setSessionTrackingModes(java.util.Set)
   */
  @Override
  public void setSessionTrackingModes(final Set<SessionTrackingMode> arg0)
      throws IllegalStateException, IllegalArgumentException {
  }

  /**
   * @see jakarta.servlet.ServletContext#getVirtualServerName()
   */
  @Override
  public String getVirtualServerName() {
    return "Dummy";
  }

  private String responseEncoding;

  @Override
  public void setResponseCharacterEncoding(final String encoding) {
    this.responseEncoding = encoding;
  }

  @Override
  public String getResponseCharacterEncoding() {
    return this.responseEncoding;
  }

  private String requestEncoding;

  @Override
  public void setRequestCharacterEncoding(final String encoding) {
    this.requestEncoding = encoding;
  }

  @Override
  public String getRequestCharacterEncoding() {
    return this.requestEncoding;
  }

  private int sessionTimeout;

  @Override
  public int getSessionTimeout() {
    return sessionTimeout;
  }

  @Override
  public void setSessionTimeout(final int v) {
    sessionTimeout = v;
  }

  @Override
  public ServletRegistration.Dynamic addJspFile(final String jspName,
                                                final String jspFile) {
    return null;
  }
}
