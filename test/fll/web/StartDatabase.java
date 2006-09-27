/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import fll.Utilities;

import java.sql.Connection;

import org.apache.log4j.Logger;

/**
 * Add class comment here!
 *
 * @version $Revision$
 */
public class StartDatabase {
  
  private static final Logger LOG = Logger.getLogger(StartDatabase.class);
  
  public static void main(final String[] args) throws InterruptedException {
    System.setProperty("inside.test", "true");
    final Connection connection = Utilities.createDBConnection("fll", "fll", "fll");
    final Object foo = new Object();
    synchronized(foo) {
      foo.wait();
    }
  }
}

