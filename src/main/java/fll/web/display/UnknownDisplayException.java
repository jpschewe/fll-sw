/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import fll.util.FLLException;

/**
 * 
 */
public class UnknownDisplayException extends FLLException {

  /**
   * @param uuid the UUID of the display that was not found
   */
  public UnknownDisplayException(final String uuid) {
    super(String.format("Display with the ID '%s' is not known", uuid));
  }

}
