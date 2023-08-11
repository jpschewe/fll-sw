/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.IOException;
import java.io.PrintWriter;

import fll.web.BaseFLLServlet;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Subscribe to server send events for score updates.
 */
@WebServlet(urlPatterns = "/scoreboard/SubscribeScoreUpdate", asyncSupported = true)
public class SubscribeScoreUpdate extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    response.setContentType("text/event-stream");
    response.setCharacterEncoding("UTF-8");

    final AsyncContext asyncContext = request.startAsync();
    asyncContext.setTimeout(0); // never time out
    
    ScoreboardUpdates.addClient(asyncContext);
//
//    // Start a new thread to send events to the client
//    new Thread(() -> {
//      try (PrintWriter writer = asyncContext.getResponse().getWriter()) {
//        for (int i = 0; i < 10; i++) {
//          writer.write("event: score_update\n");
//          writer.write("data: This is event "
//              + i
//              + "\n\n");
//          writer.flush();
//          Thread.sleep(1000);
//        }
//        asyncContext.complete();
//      } catch (IOException | InterruptedException e) {
//        e.printStackTrace();
//      }
//    }).start();
  }

}
