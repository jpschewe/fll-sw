/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.debug;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * Debug class for HttpSession.  Mostly useful for storing and reading
 * attributes.
 *
 * @version $Revision$
 */
public class DebugHttpSession implements HttpSession {
   
  public DebugHttpSession() {
    _attributes = new HashMap();
    _creationTime = System.currentTimeMillis();
    setMaxInactiveInterval(60 * 3); //3 minutes
  }

  public Object getAttribute(final String name) {
    return _attributes.get(name);
  }

  public Enumeration getAttributeNames() {
    return Collections.enumeration(_attributes.keySet());
  }

  public long getCreationTime() {
    return _creationTime;
  }

  public String getId() {
    return "debug";
  }

  public long getLastAccessedTime() {
    return _creationTime;
  }

  public int getMaxInactiveInterval() {
    return _maxInactiveInterval;
  }

  /**
   * @return null
   */
  public ServletContext getServletContext() {
    return null;
  }

  /**
   * @return null
   */
  public HttpSessionContext getSessionContext() {
    return null;
  }

  public Object getValue(final String name) {
    return getAttribute(name);
  }

  public String[] getValueNames() {
    return (String[])_attributes.keySet().toArray(new String[_attributes.size()]);
  }

  public void invalidate() {
    _attributes.clear();
  }

  public boolean isNew() {
    return false;
  }

  public void putValue(final String name, final Object value) {
    setAttribute(name, value);
  }

  public void removeAttribute(final String name) {
    _attributes.remove(name);
  }

  public void removeValue(final String name) {
    removeAttribute(name);
  }

  public void setAttribute(final String name, final Object value) {
    _attributes.put(name, value);
  }

  public void setMaxInactiveInterval(final int interval) {
    _maxInactiveInterval = interval;
  }

  private int _maxInactiveInterval;
  private final Map _attributes;
  private final long _creationTime;
}
