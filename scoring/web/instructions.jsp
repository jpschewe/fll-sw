<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title><x:out select="$challengeDocument/fll/@title" />
(Instructions)</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" />
(Instructions)</h1>

<h2><a name='setup'>Setup of tournament</a></h2>

<p>Links in this section are also found on the <a
 href="admin/index.jsp">administration page</a>. Not all of these things
need to be done the day before, you can do them the morning of, but it
helps to prepare as much as you can the first few tournaments you do.</p>

<ol>

 <li>Go to the <a href="admin/index.jsp">administration page</a>
 and upload the team datafile. This takes in a tab delimited file and
 uses that information to determine what teams will exist in the
 database. Note the sizes and datatypes of columns. If the columns in
 your datafile are too large the text will be truncated. The most common
 problem here is with region names and not being able to tell regions
 apart.</li>

 <li>If the team data doesn't look quite right you can <a
  href="admin/select_team.jsp">edit it</a>.</li>

 <li>You will probably want to <a
  href="admin/index.jsp?addTournamentsForRegions=1">add tournaments
 for all regions</a>. This will create tournaments in the database that are
 named the same as the regions in the teams datafile.</li>

 <li>Then you should <a href="admin/tournamentInitialization.jsp">initialize
 tournaments by region</a>. This will automatically assign teams to a
 tournament based on the region that was specified in the teams
 datafile.</li>

 <li>If you'd like to change some of the information about the
 tournaments you can <a href="admin/tournaments.jsp">edit the
 tournament list</a>. </a></li>

 <li>Set the current tournament on the <a href="admin/index.jsp">administration
 page</a></li>

 <li>Now you'll need to <a href="admin/judges.jsp">assign
 judges</a> for each of the subjective categories</li>

 <li>Make sure to <a href="admin/tables.jsp">assign table
 labels</a> to allow the playoff brackets to do correct table assignments
 for you.</li>

 <li>Download the subjective score entry datafile from the <a
  href="admin/index.jsp">administration page</a> and copy it to all
 machines that will be used for entering subjective data.</li>

</ol>

<h2><a name='tournament_day'>Tournament day</a></h2>
<ul>

 <li>On the display computer bring up the <a href='display.jsp'>Big
 Screen Display</a> page. This will display the welcome page, the scoreboard
 and the playoff brackets as needed. This page is controlled by the <a
  href='admin/remoteControl.jsp'>Remote Control Page</a> found under
 administration. <b>Important note</b>: Do not close the background
 window that launched the fullscreen window. This background window
 actually controls the fullscreen window. Note that when the scorboard
 comes up the All Teams column will be blank until any scores are
 entered.</li>

 <li>Enter scores for the performance rounds on the <a
  href="scoreEntry/select_team.jsp">score entry</a> page. I suggest at
 least two people entering scores. One person enteres the scores and the
 second person goes back and checks the scores using the score edit
 feature. This has caught many data entry errors in previous
 tournaments.</li>

 <li>Use the <a href="subjective.zip">subjective score entry
 application</a> to enter subjective scores. Note that when the application
 comes up there will be a row for each judge per team in the same
 division, regardless of if that judge is definitely scoring that team.
 So this means there will be extra rows and you'll just have to watch
 closely to make sure all teams are scored properly. For instance if KH
 and KL are judging Programming for division 1, each will only judge
 roughly half of the teams in division 1, however for each team there
 will be two rows, on for KH and one for KL. It's up to you to make sure
 scores are in the correct rows. There is a summary button that will
 tell you how many scores have been entered for each team in a category.
 By looking at this you can quickly tell which scores haven't been
 entered or if a score has been entered for the wrong team.</li>

 <li>The scoreboard, welcome page and playoff brackets have a place
 at the top for text to be displayed, this can be changed from the <a
  href="index.jsp">main page</a>. It's labeled Score Page Text.</li>

 <li>If a team does not show up you should not enter any scores for
 that team in the subjective score application and enter all no show
 scores on the performance score entry page. Make sure you enter the no
 show scores on the performance score entry page, otherwise the playoff
 brackets won't work quite right. When you get to the brackets page this
 team will show up as last place and will still be placed on the
 brackets. You will need to enter a dummy score for the team that
 competes against the no show team and a no show for the team that isn't
 there for the brackets to properly advance the team.</li>

 <li>When it comes time to do the playoffs, make sure you intialize
 the brackets on the <a href="playoff/index.jsp">playoff page</a> before
 entering scores. If you attempt to enter playoff scores before
 initializing the brackets, you will get an error message. Don't worry,
 nothing is broken, you just need to initialize the brackets first.</li>

 <li>You can print out the brackets by going to the
 admin/printable bracket page, under the Playoffs link on the main
 index, for each division and printing that out.</li>

 <li>If there is a tie during playoffs the software will attempt to
 break it with the standard tie breaker defined in the software. However
 if this is not possible TIE will be shown as the winner of a round.
 This means that these two teams need to compete again and their scores
 edited, using the score entry page with the edit checkbox checked, to
 represent the second run's score. Then the brackets page will display
 the new scores as it updates.</li>

 <li>If you are not using the software to actually display the
 brackets then you'll have to do the tiebreakers by hand, below is the
 tiebreaker order:
 <ol>
  <x:forEach select="$challengeDocument/fll/Performance/tiebreaker/test">
   <li>Sum of
   <ul>
    <x:forEach select="./term">
     <li><x:out select="./@goal" /></li>
    </x:forEach>
   </ul>
   <x:out select="./@winner" /> score wins</li>
  </x:forEach>
 </ol>
 </li>


</ul>

<h2><a name='end'>End of tournament</a></h2>

<ul>

 <li>Make sure you upload all data from the subjective scoring
 computers via the <a href="admin/index.jsp">administration page.</a></li>

 <li>Follow the compute summarized scores link on the <a
  href="report/index.jsp">reporting page</a> to summarize the scores.
 Make sure the score groups shown are the ones you'd expect and that the
 number of teams seen by each score group are correct.</li>

 <li><a href="report/index.jsp">View the reports</a> and print them
 out through your web browser. You can visit these pages as many times
 as you like. Each time you visit them the report page will be
 recomputed. So if you find any missing scores, just enter them and goto
 the reporting pages again and make sure you compute summarized scores
 before going to the reports.</li>

</ul>

<h2><a name='troubleshooting'></a>Troubleshooting</h2>

<p>If something blows up, take a look at the message that was spit
out. It should be the first thing at the top of the page. Usually this
will be something descriptive like "Not enough teams to compute
standardized score" followed by a bunch of filenames and line numbers.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
