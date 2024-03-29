/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Servlet request that supports an additional map of parameters that override
 * or add to the existing parameters.
 */
public class AdditionalParameterServletRequest extends HttpServletRequestWrapper {

  private final Map<String, String[]> params = new HashMap<>();

  /**
   * @param params additional form parameters
   * @param request the request to wrap
   */
  public AdditionalParameterServletRequest(final HttpServletRequest request,
                                           final Map<String, String[]> params) {
    super(request);
    this.params.putAll(request.getParameterMap());
    this.params.putAll(params);
  }

  @Override
  public @Nullable String getParameter(final String name) {
    final String[] result = this.params.get(name);
    if (null == result
        || result.length < 1) {
      return null;
    } else {
      return result[0];
    }
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.unmodifiableMap(this.params);
  }

  @Override
  public Enumeration<String> getParameterNames() {
    // cast away KeyFor to comply with the interface specification
    // https://github.com/typetools/checker-framework/issues/1653
    return (Enumeration<String>) Collections.enumeration(this.params.keySet());
  }

  @Override
  public String @Nullable [] getParameterValues(final String name) {
    return params.get(name);
  }

}
