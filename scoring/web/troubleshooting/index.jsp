<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/fll-sw.css'/>" />
    <title>Troubleshooting</title>
  </head>

  <body>
    <h1>Troubleshooting</h1>

    <p>Here's where you'll find various errors you might get and what to do
    about them.  First to remember is that just because you get a nasty error
    page with lots of long stuff about Java programming, it's not that bad.
    There will be one or more exceptions listed, separated by a blank line or
    something like root cause.  The first line of each exception is the
    message of the exception.  If it's one that I cause intentionaly it'll
    have a, hopefully, understandable message.  Either way, look through the
    things in the following list to see if the messages match anything listed
    here.  If all else fails, email me (Jon Schewe): jpschewe@mtu.net.
    </p>

    <ul>
      <li>First make sure that you've got the latest version of Java.  Java version -&amp; <%=System.getProperty("java.vm.version")%></li>

      <li>When you tell the system to compute summarized scores from the
      reporting page you get an exception that a particular score group
      doesn't have enough scores.  Score groups are something the system
      creates when it's putting all of the scores together to determine scaled
      scores.  The name of the score group is created by appending the ids of
      all of the judges that scored a particular team in a particular category
      with '_' between the names.  The category that had a problem will be
      listed in the error message.  Once you fix these errors, try computing
      scores again and you may get another error.  This is because it fails on
      the first error, possibly masking other data entry problems.
        <ul>
            
          <li>One possibility is that only one score, or zero scores, for a
          judge got entered.  The judges id will be show in the message of the
          error.  This is the result of the score being entered in the wrong
          row in the score entry app.  To fix this, download the data file for
          subjective scoring and start up the subjective scoring app looking
          at this file and correct the score.  You'll have to check against
          the sheets and the judges schedule to make sure that all is
          correct.</li>

          <li>The other possibility, which is more likely (since it happened
          at New Hope this year), is that you have two or more judges juding
          the same teams (which is commonly done in Teamwork) and a score was
          not entered.  In this case look at the score group and see if it's
          what you'd expect, given the explanation above on how this name is
          created.  For instance if both KH and KL were judging team 1234 in
          Teamwork the score group will be KH_KL.  If a score is missing for
          KL, then the error would point to the category Teamwork and say that
          score group KH has too few scores.  Go back and use the solution in
          the previous possibility to correct the problem.</li>
        </ul>
      </li>

    </ul>
      

  </body>
</html>
