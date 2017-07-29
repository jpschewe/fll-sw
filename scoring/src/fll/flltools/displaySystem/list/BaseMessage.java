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
/* package */ abstract class BaseMessage extends fll.flltools.PublishMessage {

  public BaseMessage(@Nonnull final String node,
                     final int seq,
                     @Nonnull final String action) {
    super("list:"
        + action, node, seq);
  }

  @Override
  public abstract Object getData();

}
