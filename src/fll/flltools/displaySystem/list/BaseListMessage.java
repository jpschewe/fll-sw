/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.flltools.displaySystem.list;

import javax.annotation.Nonnull;

import fll.flltools.PublishCommand;

/**
 * Base class for messages sent to the list module.
 */
public abstract class BaseListMessage extends PublishCommand {

  public BaseListMessage(@Nonnull final String node,
                     @Nonnull final String action) {
    super("list:"
        + action, node);
  }

  @Override
  public abstract Object getData();

}
