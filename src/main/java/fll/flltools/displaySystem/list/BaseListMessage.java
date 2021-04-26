/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.flltools.PublishCommand;

/**
 * Base class for messages sent to the list module.
 */
public abstract class BaseListMessage extends PublishCommand {

  /**
   * @param node {@link #getNode()}
   * @param action what list action to execute
   */
  public BaseListMessage(final String node,
                         final String action) {
    super("list:"
        + action, node);
  }

  @Override
  public abstract @Nullable Object getData();

}
