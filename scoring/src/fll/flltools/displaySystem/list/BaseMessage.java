/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

import javax.annotation.Nonnull;

/**
 * Base class for messages sent to the list module.
 */
/* package */ abstract class BaseMessage extends fll.flltools.BaseMessage {

  public BaseMessage(@Nonnull final String action) {
    super("list:" + action);
  }

  @Override
  public abstract Object getData();

}
