<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Instructions)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Instructions)</h1>
        
      <h2><a name='setup'>Setup of tournament</a></h2>

      <p>Links in this section are also found on the <a href="admin/index.jsp">administration page</a></p>
      
      <ol>
        
        <li>Go to the <a
        href="admin/index.jsp">administration page</a> and upload the team
        datafile.  This takes in a tab delimited file and uses that
        information to determine what teams will exist in the database.
        Note the sizes and datatypes of columns.  If the columns in your
        datafile are too large the text will be truncated.  The most common
        problem here is with region names and not being able to tell
        regions apart</li>

        <li>If the team data doesn't look quite right you can <a
        href="admin/select_team.jsp">edit it</a></li>
          
        <li>If you're going to run more than one tournament, <a href="admin/tournaments.jsp">edit the tournament list</a>.  Otherwise
        teams will just be in the DUMMY tournament</a></li>

        <li>Set the current tournament on the <a href="admin/index.jsp">administration page</a></li>
        
        <li>Now you'll need to <a href="admin/judges.jsp">assign judges</a>
        for each of the subjective categories</li>

        <li>Download the subjective score entry datafile from the <a href="admin/index.jsp">administration page</a> and copy it to all
        machines that will be used for entering subjective data.</li>
          
      </ol>

      <h2><a name='tournament_day'>Tournament day</a></h2>
      <ul>

        <li>On the display computer bring up the <a href='display.jsp'>Big
        Screen Display</a> page.  This will display the welcome page, the
        scoreboard and the playoff brackets as needed.  This page is
        controlled by the <a href='admin/remoteControl.jsp'>Remote Control
        Page</a> found under administration. <b>Important note</b>: Do not
        close the background window that launched the fullscreen window.
        This background window actually controls the fullscreen window.
        Note that when the scorboard comes up the All Teams column will be
        blank until any scores are entered.</li>

        <li>Enter scores for the performance rounds on the <a
        href="scoreEntry/select_team.jsp">score entry</a> page.  I suggest
        at least two people entering scores.  One person enteres the scores
        and the second person goes back and checks the scores using the
        score edit feature.  This has caught many data entry errors in
        previous tournaments.</li>

        <li>Use the subjective score entry application to enter subjective
        scores.  Note that when the application comes up there will be a
        row for each judge per team in a same division, regardless of if
        that judge is definitely scoring that team.  So this means there
        will be extra rows and you'll just have to watch closely to make
        sure all teams are scored properly.  For instance if KH and KL are
        judging Programming for division 1, each will only judge roughly
        half of the teams in division 1, however for each team there will
        be two rows, on for KH and one for KL.  It's up to you to make sure
        scores are in the correct rows.</li>

        <li>The scoreboard, welcome page and playoff brackets have a place
        at the top for text to be displayed, this can be changed from the
        <a href="index.jsp">main page</a>.  It's labeled Score Page
        Text.</li>

        <li>If a team does not show up you should not enter any scores for
        that team in the subjective score application and enter all no show
        scores on the performance score entry page.  Make sure you enter the
        no show scores on the performance score entry page, otherwise the
        playoff brackets won't work quite right.  When you get to the brackets
        page this team will show up as last place and will still be placed on
        the brackets.  You will need to enter a dummy score for the team that
        competes against the no show team and a no show for the team that
        isn't there for the brackets to properly advance the team.</li>

        <li>When it comes time to do the playoffs, make sure you intialize
        the brackets on the <a href="playoff/index.jsp">playoff page</a> before entering scores.  If you
        attempt to enter playoff scores before initializing the brackets,
        you will get an error message.  Don't worry, nothing is broken, you
        just need to initialize the brackets first.</li>
          
        <li>It is suggested that you by hand keep track of the brackets, in
        addition to showing them on the computer.  This makes it easier for
        the announcer, especially when switching between divisions.  The
        way to do this is to goto the admin playoff page (under the
        Playoffs link on the main page) for each division once all of the
        seeding rounds are done and fill in a blank bracket on paper with
        the bracket order that it comes up with.  Be careful to write down
        the order correctly, otherwise you'll end up matching the wrong
        teams against each other.  This seeding hasn't shown any problems,
        so this should work just fine.</li>

        <li>You can also print out the brackets by going to the
        admin/printable bracket page, under the Playoffs link on the main
        index, for each division and printing that out.</li>

        <li>If there is a tie during playoffs the software will attempt to
        break it with the standard tie breaker defined in the software.
        However if this is not possible TIE will be shown as the winner of
        a round.  This means that these two teams need to compete again and
        their scores edited, using the score entry page with the edit
        checkbox checked, to represent the second run's score.  Then the
        brackets page will display the new scores as it updates.</li>
        
        <li>If you are not using the software to actually display the brackets
        then you'll have to do the tiebreakers by hand, below is the
        tiebreaker order:
          <ol>
            <x:forEach select="$challengeDocument/fll/Performance/tiebreaker/test">
              <li><x:out select="./@goal" /> - <x:out select="./@winner"/> score wins</li>
            </x:forEach>
          </ol>
        </li>
        
          
      </ul>
      
      <h2><a name='end'>End of tournament</a></h2>

      <ul>

        <li>Make sure you upload all data from the subjective scoring
        computers via the <a href="admin/index.jsp">administration
        page.</a></li>

        <li>Follow the compute summarized scores link on the <a
        href="report/index.jsp">reporting page</a> to summarize the scores.
        Make sure the score groups shown are the ones you'd expect and that
        the number of teams seen by each score group are correct.</li>
          
        <li><a href="report/index.jsp">View the reports</a> and print them out
        through your web browser.  You can visit these pages as many times as
        you like.  Each time you visit them the report page will be
        recomputed.  So if you find any missing scores, just enter them and
        goto the reporting pages again and make sure you compute summarized
        scores before going to the reports.</li>

      </ul>

      <h2><a name='troubleshooting'></a></h2>

      <p>If something blows up, take a look at the message that was spit out.
      It should be the first thing at the top of the page.  Usually this will
      be something descriptive like "Not enough teams to compute standardized
      score" followed by a bunch of filenames and line numbers.</p>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
