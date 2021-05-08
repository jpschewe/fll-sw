/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import fll.util.FLLRuntimeException;

/**
 * Thrown when a required web parameter is missing.
 */
public class MissingRequiredParameterException extends FLLRuntimeException {

  private final String parameterName;

  /**
   * @param parameterName {@link #getParameterName()}
   */
  public MissingRequiredParameterException(final String parameterName) {
    super("The required parameter '"
        + parameterName
        + "' was not found");
    this.parameterName = parameterName;
  }

  /**
   * @return the name of the parameter that is required
   */
  public String getParameterName() {
    return parameterName;
  }
}
