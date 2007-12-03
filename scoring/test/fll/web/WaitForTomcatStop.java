/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Wait for tomcat to shut down.
 *
 * @version $Revision$
 */
public final class WaitForTomcatStop {
  
  private WaitForTomcatStop() {
    // no instances
  }

  public static void main(final String[] args) {
    waitForTomcatStop();
  }

  /**
   * Wait for tomcat to shutdown by checking if we can create a listen socket
   * at port 9080.
   */
  public static void waitForTomcatStop() {
    boolean done = false;
    while(!done) {
      try {
        final ServerSocket socket = new ServerSocket(9080);
        socket.close();
        done = true;
      } catch(final IOException ioe) {
        done = false;
      }
    }
    
  }
  
}

