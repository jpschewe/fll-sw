Release 12.2.5
==============

* Properly handle unspecified judging group and award group when adding teams to tournament specified by spreadsheet
* #605 - Don't clear head to head score when printing score sheet, rework display to handle deleted scores better


Release 12.2.4
==============

* #633 - Improve the schedules printed by time to make the breaks between times clearer
* #634 - fix head to head table assignment algorithm to ensure that only the desired tables are used


Release 12.2.3
==============

* change welcome screen layout to support more sponsor logos on low
    resolution projectors
* Auto-size images in the slide show


Release 12.2.2
==============

* #627 - If there is no description for a goal, make sure the scoresheet doesn't find a description in the rubric to use

Release 12.2.1
==============

* Fix error where visiting the edit tournaments page would delete all of the descriptions
* #616 - add logs and bug reports to the database dump
* #615 - Fix problem where named displays disappear from the remote control page after submitting

Release 12.2.0
==============

* #613 - Align goal titles to the right on the score entry page to match the printed score sheet
* #607 - don't let the score sheet print page submit with no check boxes checked
* #611 - allow the user to edit the scroll rate on the all teams display and the head to head brackets
* #608 - be able to edit teams with a null organization

Release 12.1.1
==============

* #606 - Fix bug where the wrong team is sent to the head to head display in a dynamic update 

Release 12.1.0
==============

* #480 - Add standard mean and sigma to the final scores legend
* Give reasonable error message when the user tries to run the table optimizer with errors in the schedule
* Use the Tika library to determine mime types to avoid issues with windows file associations
* #534 - Always warn about teams missing opponents.
* #603 - Allow one to remove brackets from the display
* #602 - Fix error with undefined showing up in the head to head display
* #601 - Use "award group" instead of "division" in the ranking report

Release 12.0.0
==============

* #582 - Truncate team names in the performance and subjective sheets instead of allowing them to wrap
* #583 - Add tournament name to subjective and performance score sheets
* #462 - Add the ability to output blank subjective sheets
* #578 - Make sure to catch exceptions in the event dispatch thread as well as those from other threads
* #559 - allow one to specify time constraints when checking a schedule
* #556 - add number of categories a team is being judged in to the finalist schedule
* #570 - Initial integration with FLL Tools to display scores (not very pretty, but functional)
* #540 - Handle narrow displays for score entry, also display errors in a floating window
* #553 - Make sure to update the last seen time for the remote control of displays
* #528 - Only display floating point scores when needed, otherwise display integers
* #533 - Handle disconnect and reconnect in the display pages
* #565 - Use web sockets to update the display pages
* #535 - Don't let the user try to initialize a bracket when there are
         no brackets to initialize
* #554 - Update public HTML finalist schedule to work on report and
         public index pages
* #566 - Use web sockets for the unverified runs update
* #552 - use web sockets to update the remote control brackets page 
* upgrade to selenium 3.3.0
* #536 - allow the admin bracketes to update automatically
* Upgrade to tomcat 8.5.11 - this includes new features for WebSockets

Release 11.7.2
============

* Ensure that shell scripts have unix line endings and batch files have dos line endings

Release 11.7
============

* #539 - Add bracket name and round number to the head to head match information displayed on the screen
* #543 - correct error in the head to head bracket dynamic updates where the wrong HTML id was used
* #547 - sort computed scores summary by category and then by station
* #544 - remove extra whitespace from the top of the head to head brackets

Release 11.6
============

* #538 - Correct final computed scores queries
* Fix the syncing of the table names on the score sheet generation page
* #454 - Add support for multiple playoff brackets on a single big screen display

Release 11.5
============

* #531 - Add support for the earliest start time for a performance round to the scheduler.
* Make sure that the all teams scoreboard page scrolls to the top before reloading  
* Add troubleshooting documentation to remote control page  
* Make sure that the scheduler UI has default parameters set at the start so that saving a new schedule description works
* #526 - Add support for generating schedules using alternating 7 and 8 minute performance intervals

Release 11.4
============

* Pause at the bottom of the all teams scores
* Make sure that the PDF and HTML score sheets have the enumerated goals sorted the same

Release 11.3
============

* Change location of slideshow images to be just slideshow, no images
    directory required.

Release 11.2
============

* Updated challenge description for scoring change


Release 11.1
============

* #450 - Refactor allteams.jsp
* Fix bug where the datasource doesn't get initialized before the setup page is loaded


Release 11.0
============

This is the first release for the 2016 season.

* #178 - Add the score page text to the welcome page
* #451 - Add support for uploading team information changes in batch from a spreadsheet
* #378 - Allow one to delete non-numeric categories and non-numeric nominees
* #513 - Add support for groups to the performance score sheet and performance score entry
* #516 - Add instructions to the dialog in the scheduler when choosing a challenge description
* #457, #499 - enhanced the training databases and made sure they are part of the distribution
* #510 - Add separator with category name to the subjective web application
* #476 - Sort finalist callback display by team number
* #495 - color background of subjective scores to make it clear which teams have been scored
* #479 - Add performance advancement percentage to the database and remove it from the generation of the final computed scores report
* #440 - Add more information to the final computed scores report to aide head judges

Release 10.9
============

This release focuses on updates to the documentation and making the
software easier to use for new users.

* #460 - Require the user to specify the tables used for a playoff bracket at initialization time
* #503 - Allow one to create new schedule description files
* #486 - Separate creating playoff brackets from initializing playoff brackets
* Use the term "Playoff Bracket" to mean the group of teams competing in a head to head competition (formally known as "Playoff Division")
* Use the term "Award Group" to mean the group of teams competing for a single set of awards (formally known as "Event Division")
* Allow the user to edit the judging groups
* Display team names in the scheduler user interface
* Add end times to the general schedule display in SchedulerUI
* Fix bug where schedule could not be replaced after playoff tables have been assigned
* Remove filtering of uploaded teams file. It's best to just filter outside of FLL-SW.

Release 10.8
============
* #466 - Add a windows executable for starting the main application
* #467 - Add a single application to launch the other applications
* #389 - Add a user interface to editing the scheduler input file
* #423 - Remove seasonal divisions, only event divisions are still used


Release 10.7
============
* #468 - Handle non-numeric categories without any teams in the finalist scheduling load
* #463 - Add judging station and event division to performance scores dump
* #464 - Allow one to edit a no show score, this converts it into a regular score 

Release 10.6
============
* #461 - use tournament information when looking up playoff brackets to ensure score sheets have correct bracket numbers
* #462 - make scoreboard display more responsive to different resolutions
* Take footer off of the slide show page

Release 10.5
============
* #449 - display playoff times in the finalist schedule
* #411 - add table names to the database when a schedule is uploaded
* #448 - modify finalist schedule times when the start time is modified
* #427 - run the scheduler and table optimizer in a background thread

Release 10.4
============
* #22 - allow one to override the auto-generated finalist schedule
* #421 - make the font larger on the subjective sheets and fix some of the wording
* #425 - use the same font across all of the big screen pages, should make everything readable
* #430 - display list of finalist teams on the big screen without the categories
* #431 - fix the size of the error column on the score entry page
* #433 - apply fix for login problems with tablets used back to back days
* #445 - properly delete tournaments from the edit tournaments page
* #434 - escape single quotes in team names and organizations on the edit team page
* 2015 challenge has performance weighted at 1 like the subjective
    categories
* #408 - Allows teams to be in multiple tournaments and no longer require a heirarchy of tournaments
* #420 - Correctly limit tables during playoff brackets when the limit tables checkboxes are used

Release 10.3
============
* #416 - Disable auto capitalize and auto correct for the username on the login page
* #418 - Add increment and decrement buttons to the score entry page for the max range
* #413 - Add team information to the header of the performance score report
* #412 - remove the overall ranking from the ranking report
* #414 - show more teams on the most recent scores scoreboard page
* #415 - Set background of scoreboard most recent scores title

Release 10.2
============
* Fix error in 2015 challenge description where subjective areas didn't all have the same spelling causing an extra table to appear on the judging sheet
* Fix crash in the subjective application when tabbing between elements
* #409 - sort subjective sheets by schedule time

Release 10.1
============
* #407 - open final computed scores in a new window
* #406 - allow one to download the subjective data file without all judges assigned

Release 10.0
============
* #394 - output subjective score sheets from the scheduler
* #403 - Allow judges to be added from the subjective datafile.
* #402 - Remove null subjective rows when uploading scores from the webapp 
* #398 - Add report to display each team's performance scores in detail
* #334 - Display the lowest scoring element first in the score sheets 
* #374 - Check all saved schedules with a unit test to make sure they all load
* #387 - Show quartiles rather than actual ranks in the ranking report
* #391 - Add shortDescription attribute to rubric range
* #392 - Properly support no schedule in the subjective web app
* #393 - Properly handle no judges in the subjective web app
* #383 - Add subjective webapp version to the sidebar
* #309 - support schedules without the division column, just use the judging group column instead
* #172 - make bracket sort be specified when initializing the brackets of a challenge descriptor attribute
* #381 - handle missing information in TeamScheduleInfo
* #380 - generate correct tanking report when there are multiple tournaments in the same database
* #377 - correctly identify Excel files by extension

Release 9.7
============
* 359 - table optimizer takes into account empty tables now as well
* Sort team schedules by team number
* 370 - Add organization to team schedule
* 362 - import tournament parameters table
* 363 - handle more than 6 logos on the welcome page
* 367 - Properly display summary for judge assigned to 2 judging groups

Release 9.6
============
* 361 - correct problem finding java on unix systems without JAVA_HOME set
* 358 - correct bug on the remote control page with named displays

Release 9.5
============
* 263 - Only allow connections from loopback interface to not require a password
* 351 - Detect when the summarized scores are out of date
* 114 - Add the ability to uninitialize a playoff division
* 14 - add the ability to print the schedules from the web 
* 339 - Highlight teams that are in the top X% of performance scores
* 12 - add the ability to delete named displays from the display remote control 
* 289 - add the ability to store non-numeric/"subjective" award winners in the database
* 230 - team upload now only adds teams without removing any existing information
* 337 - don't try to upload subjective scores from the web application if the tournament doesn't match
* 349 - Add note about performance times to team schedule and put multiple schedules on 1 page
* 338 - Add CSV dump of all seeding performance scores
* 348 - add a page break after each judging station in the category scores by judging station report
* 332 - pass parameters from limit tables servlet back to scoresheet generation page
* 346 - add link to non-scrolling playoffs from playoff index

Release 9.4
============
* 333 - redirect to authentication page from subjective app when not authenticated
* 331 - sort no shows last in scores by judging station report
* 336 - Modify writeup for what to do with ties
* 329 - Remove phone number from judge information
* 16 - put division first in playoff bracket page titles
* 17 - check for duplicate user before adding
* 35 - make the top 10 show as many scores as will fit 
* 15 - make wording clear when no teams are missing seeding rounds before playoffs
* 11 - remove extra jar files from application classpaths

Release 9.3
============
* 322 - Don't put No Show teams in the playoffs
* Reworked some pages to make it clear what is needed at a regional vs. state tournament
* 320 - make root tomcat application redirect to fll-sw
* 324 - display no shows in the subjective web application score summary
* 325 - make sure a judge is always specified in the subjective web application

Release 9.2
============
* 315 - fix playoff table assignment algorithm for initial data.
* 318 - use a double for the total score on a no show, allows sorting on the total score column
* 317 - format times with 2 digits in the subjective application

Release 9.1
============
* 305 - top page in subjective app now works offline
* Upgrade to jquery mobile 1.4.5
* 307 - Print warning instead of error if not all judges are assigned

Release 9.0
============
* Create specific exceptions for errors in challenge parsing
* 301 - allow computed goals to depend on computed goals
* 287 - schedule finalists by division rather than score group
* 284 - add finalist schedule for each team
* 97 - add room numbers to finalist schedules
* 26 - Generate database diagram for developers
* 298/299 - Switch from HttpUnit to HtmlUnit for better javascript support in tests
* 5 - Improvements to playoff score sheet generation pages
      * Keep table assignments in sync
      * Move scriplet into pure Java code for ease of maintenance
* 294 - Upgrade to FindBugs 3.0.0
* 209 - Put subjective application and webapp links on the admin page
* 176 - Handle parenthesis in column names for uploaded spreadsheets
* 236 - Add schedule information to the Java subjective application for sorting
* 36 - upgrade to Tomcat 8.0.9
* 293 - build workflow documentation as part of a release
* 28 - report to the user which step failed when loading data in the subjective web app
* 259 - upgrade jquery to 1.11.1 and related libraries
* 9 - allow one to click on the labels or the radio button when selecting a sheet within a spreadsheet
* 19 - Remind users to assign tables before printing playoff scoresheets
* 280 - Add mapping between schedule columns and category names
* 20 - Make sure the division is checked in the AJAX playoff bracket code
* 23 - pass 3rd/4th place parameter on when creating a new division 
* 33 - upgrade to poi 3.10 and include dom4j, this allows xlsx files to work
* 8 - always sort the challenge descriptions by title
* 7 - report errors to the user in the scheduler app and the subjective app
* SF.140 - change title on categorized score reports to include tournament name and title
* SF.144 - make it clear to use back to edit judges names 
* SF.169 - use CSS to trim team names

Release 8.11
============
* 34 - support creating schedules without any performance rounds

Release 8.10
============
* Add challenge description for Anoka 2014

Release 8.9
============
* Add HTML version of the subjective application
* Add challenge description for REC 2014

Release 8.8
============
* 263 - Make it clear when specifying judging station vs. event division
* 264 - Use the correct run number to check if a playoff round is verified before sending JSON back

Release 8.7
============
* Fix table assignment during playoffs to ensure we don't trivally repeat tables
* Import playoff table limits

Release 8.6
============
* Properly sort no shows to the bottom of the list when computing rankings
* Fix bug causing scores to not be displayed in playoff brackets when data from another tournament exists
* Add back the ability to select which rounds to show on the scoresheet generation playoff brackets

Release 8.5
============
* Fix display remote control so that it shows which finalist schedule is up
* Allow the public to see the public finalist schedule without username and password
* 26 - put No Shows in the reports
* g3 - Allow one to store the subjective files uploaded if loglevel
       DEBUG is enabled for fll.web.admin.UploadSubjectiveData
* 238 - Make sure that the subjective application has a window so that
        it shows up in the task bar before a file is loaded.

Release 8.4
============
* 237 - Keep track of if allteams.jsp should scroll or not without using a session variable
* 203 - Allow the user to limit which tables are used for the playoff brackets
* 239 - add "label for" tag to enums, this allows one to click on the text next to an enum button
* 235 - make commit button more obvious on the judges verification page
* 234 - add some horizontal rules to the setup index
* 240 - make sure the division is present on all playoff scoresheets
* 241 - put the bracket number on the playoff scoresheets
* 236 - write out detailed schedules to inidividual files
* g1 - Find challenge descriptors inside of OneJar file for SchedulerUI distribution

Release 8.3
============
* Make sure minimum performance score is respected for entries into the database and displays
* Handle invalid scores properly in the subjective app
* When printing the morning score sheets start count at round 1 rather than round 0

Release 8.2
============
* 232 - Allow one to run all scheduling from the SchedulerUI
* 233 - cleanup temp files created by the table optimizer
* 230 - add the ability to print out the morning score sheets from the
        SchedulerUI

Release 8.1
============
* minor rewording to 2013 challenge description

Release 8.0
============
* 189 - Add a form to the error handler page to gather bug
        reporting information
* 20 - Allow one to modify the flip rate of the top 10 through a global
        parameter
* 221 - Allow one to change a user's password, create and remove users
* 227 - Upgrade to HSQL 2.3.0
* 228 - Put overall and performance at the top of the ranking report
* 222 - Added tournament name and optional tournament location to the
        subjective scores file.
* 197 - keep internal teams out of the team selection list for entering
        scores
* 206 - Use resposne.getWriter to write text and
        response.getOutputStream to write binary data
* 219 - Add more number of teams and judging group information to the
        ranking report
* 224 - handle '&' in column headers when uploading teams
* 159 - Allow one to choose a known challenge description
* 216 - Add judging station to non-numeric categories in the finalist
        scheduling app
* 212 - Add playoff divisions to the list of divisions one can schedule
        finalists for.
* 223 - Add the ability to easily choose teams for playoff divisions 
        by judging station.
* 220 - Always auto-select top 1 finalist in finalist scheduling app
* 220 - If two teams are tied (within 1) select them both
* 213 - Allow finalist categories to be marked public/private. Add PDF
        and HTML outputs of the finalist schedule (public & private).
* 211 - make sure to schedule finalists that are more constrained first, this allows the finalist schedule to be as short as possible
* 187 - Refresh the playoff brackets when the division changes
* 204 - Allow score entry to update or insert a score in a safe manner
    under heavy load
* Disallow setting up the database from a computer other than localhost.
    This solves a number of issues around init and security given that
    the database is part of the webapp. If the database is moved out of the
    app, then this might work again.
* Allow team information to be edited after the playoffs are
* 165 - Allow one to bypass security when connecting from localhost.
* 200 - Allow team information to be edited after the playoffs are
  initialized. Team divisions cannot be changed after the playoffs are
  initialized to avoid issues with divisions and the playoffs.

Release 7.7
============
* Allow schedules to be in 12 hour or 24 hour format. All schedules are
  output in 12 hour format.
* Added support for generating a schedule for an odd number of teams
  with an odd number of performance rounds

Release 7.6
============
* Fix bug where scoresheets wouldn't print due to incorrect error
     checking

Release 7.5
============
* 164 - Add report for winners of the playoff brackets
* 177 - Give a more reasonable error message when an XML document fails to parse
* 188 - Display a reasonable error message to the user when no score sheets are selected to print

Release 7.4
============
* 167 - Use AJAX push for changing display pages quickly

* 187 - Use AJAX polling for updating the display pages and handling network
  timeouts

* 6 - Ranking reports are sorted by division and then team number

* 193 - Make sure all exceptions are logged to fllweb.log.

* 191 - Show all playoff divisions, even those created for playoffs only

* 181 Update summary button in the subjective app for the new schema

* 184 - Make sure team names can be changed after the playoffs start

* 182 - Correctly handle previous playoff data from other tournaments in the
  database

* Warn users when they are marking a performance score as validated without
  the double check.

* More robust handling of data when printing scoresheets (ticket:179)

* 15, 16, 22 - Add the ability to edit all global parameters and parameter defaults as
  an advanced admin option

Release 7.3
============
* 173 - allow the schedule tables to have teams that aren't in the current
  tournament for cases when teams move tournaments and a new schedule isn't
  uploaded.

* 8 - color table labels in the playoff brackets by border rather than the
  whole cell to make them readable

* 172 - Handle deleted scores in the subjective score file on upload

Release 7.1
============
* 161 - Make sure that score page text can be set

* 162 - Handle no show's in the subjective upload with the new subjective
  score schema

* Ignore internal teams when initializing playoff data

* Allow the top 10 page to not error out when there is no data in the
  database

Release 7.0
============
* Debug importing a database into a current database and make sure that creating a tournament works.

* 27 - track the number of teams per judge by assigning teams to judging stations

* Upgrade opencsv

* Make sure team numbers are integers


Release 3.0
============
* Restrictions are now checked on the client side instead of by the server and sending users back to the score entry page on an error.

* Added support for computed goals.

* Terms of polynomial score elements may now specify the floating point handling.  The default handling is to truncate decimals to integers at each term.

* Raw value for an enum in the web page is now the enum value as a string, rather than the score.  This allows multiple enum values to have the same score (1583927)

* Added the ability for multipliers on goals to be decimals

* Finished support for multiplier in subjective goals (it was already started, but not finished)

* Added support for total scores to be decimals

* Allow coefficients of terms (restriction and tiebreaker) to be decimals

* Added the ability to import a database dump into a blank database.

* Upgraded to log4j 1.2.14

* Upgraded to iText 1.4.7

* removed jakarta string taglib and used the functions built into JSTL 1.1

* switched upload libraries from uptag to commons-fileupload

* upgraded to JSTL 1.1

* upgraded tomcat to 5.5.20

* Upgraded to junit 4.1

* Found bug in database setup form that causes some HTML parsers to choke (missing quote).  Firefox seems to handle it ok though.

Release 2.10
* added the ability to dump the database as a zip file containing CSV files and the challenge descriptor.

* fixed score entry check for initialized playoff brackets to use event division instead of team division.

* added ability to choose what rounds are displayed in the printable and scoresheet generation brackets

* fixed BracketData.getNumRows to return 0 if there is a NoSuchElementExeception when trying to look up data in the _bracketData map

* modified playoff menu page choices to use consistent language for all bracket page links; removed warnings on bracket page links, as it is no longer harmful to load any of the bracket pages before playoff initialization - the bracket pages will just come up with a title and nothing else

* improved output of the playoff/check.jsp page to explicitly state that no teams were found with fewer or more scores than seeding rounds

* remove experimental warning from PDF brackets (1605785)

* Set path in setenv.bat (1605786)

Release 2.9
============
* fixed compilation error in remoteControl.jsp and added unit test to watch for it

Release 2.8
============
* fixed categorized score report to show by division (1599044)

* Remote control for the scoreboard now uses all divisions to determine how many rounds are possible (1599039)

* PDF reports now show all raw scores, just like the HTML reports (1599786)

Release 2.7
============
* Added the ability for tiebreakers to have sums (and coefficients on the goals)

* added help text to the admin pages

* added underscores to the front of the goal titles in the PDF scoresheet to allow the refs to check them off

Release 2.6
============
* include Sun's JDK for Linux and Windows distributions so that Java doesn't need to be installed

* Switched databases from MySQL to HSQL to make installation easier.  Now the only thing that needs to be installed for the software to run is Java.

* included the Tomcat install as part of the software

* added PDF forms for the final scores document

* added PDF forms for the scoresheets

* added summary dialog for the subjective scoring application to allow one to see how many scores had been entered for each team in a category


2005-12-05  Jon Schewe  <jpschewe@mtu.net>

	* Release version 2.5
	
2005-01  Dan Churchill

	* web/scoreEntry/select_team.jsp: Added logic for Last Run when editing scores

2004-12-20  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/ScoreStandardization.java (ScoreStandardization): Fixed
	computation of final scores so that scores from other tournaments
	are not seen in the final report.

2004-12-18  Jon Schewe  <jpschewe@mtu.net>

	* prj.el: Updated the mysql database driver

	* web/WEB-INF/web-default.xml: Changed database host to be
	127.0.0.1, seems to work better than localhost with newer mysql installs.

2004-12-14  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/ChallengeParser.java (ChallengeParser): Removed
	requirement to put xerces in the common/lib directory on tomcat by
	referencing the xerces DOMParser directly.  Now can live up in the
	regular lib directory.

2004-12-13  Jon Schewe  <jpschewe@mtu.net>

	* build.xml: Cleaned up some variable naemes.  NOTE: changed
	catalina.home to dir.tomcat!  Please update your build.properties
	files.  Added building of the scripts to regular compile.

2004-12-09  Jon Schewe  <jpschewe@mtu.net>

	* build.xml: Do a better job of rewriting the library path when
	creating subjective.bat so that it works on more systems.  Also
	removed the requirement of JAVA_HOME.

2004-12-08  Dan Churchill <churchid@visi.com>

	* web/admin/select_team.jsp: Added Division to display of teams in the
	selection window, so it's possible to scan teams and verify divisions
	prior to tournament day.

2004-12-05  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/ChallengeParser.java (ChallengeParser): Use the
	context classloader to allow one to change the classloader without
	changing the code.

2004-11-23  Jon Schewe  <jpschewe@mtu.net>

	* web/WEB-INF/jspf/footer.jspf: Added sw version to the footer page.

	* web/instructions.jsp: Not about all teams being blank until
	scores are entered.

	* web/admin/remoteControl.jsp: Added note that it takes time for
	the big screen display to change.

2004-11-21  Jon Schewe  <jpschewe@mtu.net>

	* web/instructions.jsp: Remind users to not close the control
	window for the big screen display. 

2004-05-12  U-TOSHIBA-USER\jpschewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (computeTotalScore): Modified to catch
	SQLExceptions and to return Integer.MIN_VALUE in this case,
	signifing that the row is to be ignored (914631).

	* docs/index.html: Change link to point to the top level tomcat
	project and allow the users to find the actual builds (871671).

2004-05-11  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/GenerateDB.java (generateDB): factored out the sql
	datatype for tournaments to the top to make it easy to change the
	size of the column.

	* src/fll/web/report/FinalComputedScores.java (generateReport):
	Removed SummarizedScores table from the teamsRS query.  It was
	completly unused and would just slow down the join (836268). 

2004-04-07  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/ImportDB.java (importDatabase): Make sure that the
	division column is imported from the Judges table.

	* src/fll/model/SubjectiveTableModel.java: Modified
	SubjectiveTableModel to ignore case.  Although this shouldn't come
	up again much now that we don't have the language used column in
	Programming. (913700)

2004-04-05  Jon Schewe  <jpschewe@mtu.net>

	* build.xml: Added checkstyle to build.

2004-03-21  Jon Schewe  <jpschewe@mtu.net>

	* src/resources/challenge-region-2003.xml: Removed language used (914627).

	* src/fll/xml/GenerateDB.java (generateDB): Removed numMedals from
	the system (847499).

2004-03-15  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/admin/Judges.java (generatePage): Made all forms
	have POST methods.

2004-01-22  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreboard/allteams.jsp: Removed extraneous '> in allteams.jsp 

2004-01-17  Jon Schewe  <jpschewe@mtu.net>
	
	* Release version 2.4.
	
2004-01-10  Jon Schewe  <jpschewe@mtu.net>

	* web/display.jsp: Added ability to specify any URL relative to
	/fll-sw, for display on the main display.  This will allow us to
	display Fred's schedules and whatever else he comes up with.

2004-01-04  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/playoff/Playoff.java (displayPrintableBrackets):
	Changed admin brackets to show all playoff rounds and be nice for
	printing (866161).

2004-01-02  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/adminbrackets.jsp: Removed unneeded <br> from
	Bracket number display on all playoff brackets.

2003-12-28  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreEntry/scoreEntry.jsp: Now one can edit scores that are
	non-consecutive, for those times when someone goes to the playoff
	brackets too soon (860145).

	* src/fll/Queries.java (isBye): Added support to score entry/edit
	page to handle editing bye runs (866851).

2003-12-27  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/remoteControlBrackets.jsp: Don't show final scores
	in playoffs (847952).

2003-12-18  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/ImportDB.java: Added check for differences before
	importing team data.

	* src/fll/web/report/FinalComputedScores.java (generateReport):
	Added tournament name to the final scores report.

2003-12-17  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/advanceTeams.jsp: Added page to do batch advancement
	of teams (853008).

2003-12-14  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/commitTeam.jsp: Ensure that scores don't get deleted
	when team data is edited (859692).

2003-12-07  Jon Schewe  <jpschewe@mtu.net>
	
	* Release version 2.2.

	* src/fll/Queries.java: Updated some documentation as well as
	minimum score values (subjective was using performance).

2003-12-06  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/index.jsp: Removed old links to brackets, only use
	the remote control and admin ones now.

	* src/fll/web/report/CategoryScores.java (generateCategoryTable):
	Added quotes around division to handle when it's a string.  This
	was a problem at the Crosswinds tournament today where the
	divisions were 2E and 2W. 

2003-11-30  Jon Schewe  <jpschewe@mtu.net>

	* Release version 2.2.
	
2003-11-29  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/index.jsp: Updated to handle the situation where
	this page is visited and no teams are in the database.

2003-11-22  Jon Schewe  <jpschewe@mtu.net>

	* web/display.jsp: Fixed big screen display to only refresh the
	window when what is to be shown has changed.  

	* web/scoreboard/allteams.jsp: Add some padding at the end of the
	scrolling of all teams to ensure that the teams can see the last
	team when running in 800x600 mode.

	* web/display.jsp: Ensure that welcome page doesn't come up everytime.

	* web/instructions.jsp: Tiebreaker info is automatically pulled
	out of the current document.

	* web/developer/index.jsp: Fixed change database page to properly
	switch databases.

2003-11-21  Jon Schewe  <jpschewe@mtu.net>

	* Release version 2.1.  Found some bugs, so this is the release
	used at Sandburg.
	
2003-11-19  Jon Schewe  <jpschewe@mtu.net>

	* src/resources/challenge-region-2003.xml: Added new tiebreaker
	and removed interference goal.

	* src/fll/web/playoff/Playoff.java (pickWinner): Fixed pickWinner
	to handle enumerated goals (845319).

2003-11-12  Jon Schewe  <jpschewe@mtu.net>

	* Release version 2.0.  This is the version of the software I plan
	to use for this season.
	
2003-11-09  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/verifyTournamentInitialization.jsp: Fix Oops when
	setting tournaments.

	* src/fll/xml/GenerateDB.java (generateDB): Handle long region names.

	* web/admin/teamColumnSelection.jsp: Added column sizes to column
	selection page.

	* src/fll/Queries.java (insertTournamentsForRegions): Make sure we
	don't try to create duplicate tournaments.

2003-11-08  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/scoreEntry/ScoreEntry.java
	(generateInitForScoreEdit): Include the tournament when looking
	for initial values for score edit (836264). 

	* web/scoreboard/title.jsp: Change scoreboard text to be white so
	that it's easier to read (836265).

	* web/report/teamPerformanceReport.jsp: Display no show for no show
	performance scores (836266).

	* web/report/performanceRunReport.jsp: Display no show for no show
	performance scores (836266).

	* build.xml: Changed slashes in subjective.bat to work with win9x
	as well as NT.

2003-10-27  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (updateScoreTotals): Handle rows with all
	NULLs for scores.  This is a bug that Chuck Davis found as well.

2003-10-25  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (updateScoreTotals): need different update
	to include Judge column (830276)

2003-10-24  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/commitTeam.jsp: Report an error on attempting to enter
	a duplicate team number.

	* src/fll/Queries.java (updateScoreTotals): Only check for Bye
	column on performance scores (bug from Chuck, didn't make it to SF).
	(updateScoreTotals): Fixed another bug from Chuck where all
	subjective scores were 0.

2003-10-22  Jon Schewe  <jpschewe@mtu.net>
	
	* labeled fll_2003_test_20031022 for testing

	* web/scoreEntry/scoreEntry.jsp: Made background yellow instead of
	grey on edit.  Also changed changed submit prompt for score edit
	(827928).

	* src/resources/challenge-region-2003.xml: Consistency in
	capitalization (827924).

	* web/WEB-INF/web-default.xml: Changed instances of 2002 to be
	2003 (827935).

	* src/fll/web/admin/Tournaments.java (generatePage): Make sure
	that when rows are added keep the key field as new for rows not
	committed to the database (827926).

2003-10-22  Jon Schewe  <jpschewe@mtu.net>

	* docs/index.html: Added information for database permissions for
	Linux (824072).

2003-10-21  Jon Schewe  <jpschewe@mtu.net>

	* docs/README.developer: Removed tournamentTeams application
	attribute to avoid caching problems (827929).

	* src/fll/Queries.java (computeTotalScore): Fixed so that Bye runs
	are ignored when computing total score (827932).

	* build.xml: Make subjective.sh and subjective.bat use
	application.classpath with pathconvert rather than hardcoding it
	to avoid mistakes (824074).

2003-10-15  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/admin/UploadTeams.java (parseFile): Handle empty
	columns as nulls.

2003-10-07  Jon Schewe  <jpschewe@mtu.net>
	
	* labeled fll_2003_test_20031007
	
2003-10-04  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (getColorForDivisionIndex): Support
	coloring more than just 2 divisions (766208).

	* docs/index.html: Added instructions for using jar to uncompress
	zip archives.  Perhaps we should just write a little Java tool
	that has a nice GUI (707892).

	* web/scoreEntry/select_team.jsp: Moved edit widgets around a bit
	to make it more obvious that the checkbox and dropdown are tied
	together (707865).

	* src/fll/web/playoff/Playoff.java (getDisplayString): Check the
	other team for a bye and don't show a score in that case (707872).

2003-10-03  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/model/SubjectiveTableModel.java (SubjectiveTableModel):
	Added total column on the subjective app (707863).

	* src/fll/web/admin/Tournaments.java (verifyData): Check for
	circularities in tournaments (766563).

2003-10-02  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/scoreEntry/Submit.java: Added support for
	enumerations and checking restrictions on the server side (707877).

2003-10-01  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (updateScoreTotals): Added support for
	totaling of scores with enumerated goals (707877).

2003-09-29  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/scoreEntry/ScoreEntry.java: Added support for
	enumerated goals (707877).  Just need to fix the score computation
	now.

	* src/resources/fll.xsd: Removed namespace declaration from schema
	since JSTL 1.0 doesn't know how to properly handle namespaces.
	This also caused the web pages to be updated to properly reference
	the challenge title, I was just getting lucky that it was the
	first title attribute in the document.

2003-09-20  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/report/FinalComputedScores.java (generateReport):
	Don't show scores from subjective categories that have 0 weight
	(809035).

	* src/fll/web/admin/UploadSubjectiveData.java
	(saveSubjectiveData): Now handle carriage returns in score.xml
	properly.

2003-09-17  Jon Schewe  <jpschewe@mtu.net>

	* docs/index.html: Updated with information about computers
	required for a tournament (707857).

2003-09-16  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/GenerateDB.java (generateDB): Make sure no team name
	is blank by defaulting it to '<No Name>' in the database (707849).

2003-09-08  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/filterTeams.jsp: Remvoed javascript from submit button
	in favor of using standard form parameters and server side
	checking (707895).

2003-09-07  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreboard/top10.jsp: Now the top10 page uses session
	variables instead of form parameters to keep track of which
	division is being displayed.  This should keep the browsers
	history from filling up with urls (707896).

	* web/index.jsp: Figured out how to use the JSTL XML tags to get
	information out of challengeDocument to remove some more
	scriptlets.

2003-09-06  Jon Schewe  <jpschewe@mtu.net>

	* web/report/teamPerformanceReport.jsp: Added a report that
	contains the performance scores for a team by run number (707861).

2003-08-27  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreboard/allteams.jsp: Got scrolling working in Mozilla and
	IE (707853).

	* web/playoff/remoteControlBrackets.jsp: Got scrolling working in Mozilla and
	IE (707853).

	* web/playoff/brackets.jsp: Got scrolling working in Mozilla and
	IE (707853).

2003-08-25  Jon Schewe  <jpschewe@mtu.net>

	* web/style/style.jsp: Started on using stylesheets everywhere.
	Use a jsp file as a stylesheet so that parameters such as images
	can be done via JSTL.  In this case make sure that all files use
	the appropriate background image and color without having to put
	it in all of the body tags (707854).

	* src/fll/gui/SubjectiveFrame.java: Display title attribute rather
	than name attribute from subjective categories, also changed in
	the report generation classes (707873).

	* src/resources/fll.xsd: Added title attribute to subjective
	category element (707873).

2003-08-16  Jon Schewe  <jpschewe@mtu.net>

	* Multiple files: Added display page the is used for the display
	computer.  This page opens up a new window in fullscreen mode
	(Mozilla users may need to press F11) and then displays the
	welcome page, the scoreboard or the remotely controlled brackets.
	This page is controlled via a remote control page accessible from
	the admin page.  Also the scoreboard pages now automatically
	detect the screen size and redirect the web browser to the
	appropriate page. (707891, 707856).
	
	* web/WEB-INF/jspf/initializeApplicationVars.jspf: Added cache
	control headers.  This should keep all pages from being cached in
	browser and proxy caches.  (707883)

2003-08-14  Jon Schewe  <jpschewe@mtu.net>

	* web/welcome.jsp: Welcome page for Minnesota tournaments.
	(707859)

2003-08-11  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/model/SubjectiveTableModel.java (SubjectiveTableModel):
	Changed default sort to be by team number (707851)

2003-08-10  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/report/FinalComputedScores.java (generateReport):
	Put "No Score" in bold red on final score report for missing
	scores.  (707890)

	* docs/README.developer (MISC): Removed the application variable
	currentTournament, just use Queries.getCurrentTournament() all of
	the time now.

	* web/admin/index.jsp: Make sure we don't grab the current
	tournament until after it's actually been changed, otherwise the
	combobox reports the previous tournament after changes.

	* src/fll/web/GetFile.java (getFile): Ensure that
	judges are assigned before downloading subjective datafile (707847).

	* src/fll/Queries.java (isJudgesProperlyAssigned): Ensure that
	judges are assigned before downloading subjective datafile (707847).

2003-08-09  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/model/SubjectiveTableModel.java (sort): Use
	fireTableDataChanged to signify that a sort has occurred instead
	of fireTableStructureChanged.  This keeps the table columns from
	getting recreated and loosing the editor. (707840)  

2003-07-27  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/GetFile.java (getFile): Make score.xml human
	readable (707855).

2003-07-25  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/gui/SubjectiveFrame.java (SubjectiveFrame): Gave each
	category it's own table and put them in a tabbed pane.  This makes
	sure that a table doesn't loose it's size and sort settings when
	switching categories. (707885)

2003-07-21  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/playoff/Playoff.java (insertBye): 
	Always use the tournament from the database. (707887)

	* src/fll/ScoreStandardization.java (setSubjectiveScoreGroups):
	Always use the tournament from the database. (707887)

2003-07-20  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Queries.java (getNextRunNumber): Always use the
	tournament from the database. (707887)

	* web/scoreEntry/scoreEntry.jsp: Always use the tournament from
	the database. (707887)

	* web/admin/index.jsp: Added link to add tournaments for each
	region in the teams table. (766898)

	* src/fll/Queries.java (insertTournamentsForRegions): Added
	ability to add tournaments for each region in the teams table. (766898) 

	* web/playoff/remoteControlBrackets.jsp: Reworked to always
	recompute the teams and don't use session variables.  This makes
	sure we don't have caching problems. (707839)

	* web/playoff/adminbrackets.jsp: Reworked to always recompute the
	teams and allow the run number to advance to to be passed in as a
	parameter.  This should fix the problems with refresh and
	advancement of rounds. (707838)

2003-07-19  Jon Schewe  <jpschewe@mtu.net>

	* web/report/summarizePhase1.jsp: Modified query to group by
	ScoreGroup and Category.  (707841)

2003-07-16  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/GenerateDB.java (generateDB): varchar fields are
	limited to 255 characters.

2003-07-06  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/GenerateDB.java (generateDB): Make division a string
	rather than a number (707893)

	* build.xml: the compile.jsp task is broken again, so I've taken
	it out of the build.  Gonna have to fix it again later.

	* web/admin/editTeam.jsp: Added support for moving teams from one
	tournament to the next (707876).  
	Continued cleanup by adding JSTL tags.

	* scripts/convertTournaments.sql:  I think I've got all of the
	table changes in now for hierarchial tournaments (707876), along
	with some cleanups to table fields.

	* src/fll/xml/GenerateDB.java (generateDB): More work on
	hierarchial tournaments (707876).  Cleaned up the tables by
	removing extra columns that aren't used.

2003-07-02  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/model/SubjectiveTableModel.java (SubjectiveTableModel):
	cleaned up javadoc

	* src/fll/web/report/FinalComputedScores.java
	(FinalComputedScores): Cleaned up javadoc

2003-06-11  Jon Schewe  <jpschewe@mtu.net>

	* build.xml: Upgraded to Tomcat 4.1.x (4.1.18)

2003-04-27  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/editTeam.jsp: Convert null values to "" for editing.

2003-04-24  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/gui/SubjectiveFrame.java (SubjectiveFrame): When
	switching score categories, make sure to tell the table to stop
	editing a cell, if it is.  This keeps one from losing data when
	switching categories without first tabing to another cell.
	(707852)

2003-04-22  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/admin/Tournaments.java (verifyData): Allow one to
	specify the next tournament and do validation on the data input
	before committing it to the database.

	* build.xml: Force overwrite of web.xml when copying to ensure the
	correct one is always deployed.  Cleaned up javadoc target.

2003-04-20  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/admin/UploadTeams.java (verifyTeams): make sure that
	upload page defaults Teams.CurrentTournament =
	Teams.EntryTournament when uploading team information (707876)

2003-04-19  Jon Schewe  <jpschewe@mtu.net>

	* scripts/convertTournaments.sql: SQL script to convert databases
	from last season to add support for heirarchial tournaments and to
	be consistent about region/tournament naming.  This should allow
	us to use last season's databases for test data.  (707874, 707876)

	* web/admin/editTeam.jsp: Allow the current tournament as well as
	the entry tournament to be edited to support heirarchial
	tournaments.  (707876)

	* src/fll/web/admin/Tournaments.java (commitData): Don't delete
	all of the tournament data when updating information.  Just update
	the rows.  (707869)

	* src/log4j.properties: Created a logfile for all output in
	addition to the one going to stdout.

	* build.xml: Upgraded log4j from 1.2.6 to 1.2.8.

	* src/fll/xml/GenerateDB.java (generateDB): Removed fll_admin as a
	database user.  Just use fll as hte only database user with full
	privileges.  At this point I can't come up with a good reason for
	having two different database users. (707870)

2003-04-04  Jon Schewe  <jpschewe@mtu.net>

	* docs/README.developer: Added information about JSTL

	* web/scoreboard_800/main.jsp: Merged common pieces of code
	between the 800x600 scoreboard and the 1024x768 scoreboard.  

	* build.xml: (707867) Added a direct Java call to JspC using the
	-webapp option.  This handles directories correctly, however
	doesn't keep track of timestamps, so all jsp files get recompiled
	each time.

2003-04-02  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreboard/allteams.jsp: Started working on using JSTL in
	the pages (707868)

	* web/scoreboard/top10.jsp: merged in changes from the
	state tournament (707844)

2003-03-30  Jon Schewe  <jpschewe@mtu.net>

	* Release version 1.0.  This is just the software we used last
	year, but now it's released on SourceForge.

2003-03-21  Jon Schewe  <jpschewe@mtu.net>
        * Moved to SourceForge.net as the project fll-sw/scoring
 
2003-01-29  Jon Schewe  <jpschewe@mtu.net>

	* web/report/performanceRunReport.jsp: Queries.getDivisions()
	requires a connection to be passed in.

2003-01-25  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/report/ScoreGroupScores.java
	(generateCategoryTable): Fixed query to only show teams from the
	score group being displayed in the table as well as only showing
	score groups for the appropriate division.

2003-01-23  Jon Schewe  <jpschewe@mtu.net>

	* web/report/finalComputedScores.jsp: Shrink point size to try and
	make sure everything prints on one page landscape.

2003-01-19  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/adminbrackets.jsp: Split out the admin version of
	the brackets into a separate page to make things easier to manage,
	even though some code is duplicated

	* web/playoff/remoteControlBrackets.jsp: Added another set of
	brackets that are completly controlled through application
	variables, so we don't have to keep hitting the next round button
	on a scrolling display.

2003-01-18  Jon Schewe  <jpschewe@mtu.net>

	* web/scoreboard/top10.jsp: Make sure that the top 10 ranks
	are correct in the case of a tie

	* web/scoreboard_800/top10.jsp: Make sure that the top 10 ranks
	are correct in the case of a tie

	* web/scoreEntry/scoreEntry.jsp: Changed confirm prompt when
	editing scores to let the user know that only the changes will be
	lost.

	* build.xml: Fixed spelling error in name of directory that
	subjective zip uses.

	* src/fll/web/report/ScoreGroupScores.java
	(generateCategoryTable): Added report that shows scores by
	category and score group for finalist judging.

2003-01-09  Jon Schewe  <jpschewe@mtu.net>

	* Labeled fll_01_09_2003 and published

2003-01-07  Jon Schewe  <jpschewe@mtu.net>

	* src/resources/challenge-highschool.xml: Added restriction to
	ensure that rocks returned to base is never greater than rocks
	removed from soccer field

2002-12-28  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/Utilities.java (Utilities): Added Common NumberFormat
	instance for other classes to use to try and lower the overhead of
	getting an instance.

	* web/scoreboard_800/top10.jsp: Added support for divisions other
	than 1 and 2.

	* web/scoreboard/top10.jsp: Added support for divisions other than
	1 and 2.

	* src/fll/xml/XMLUtils.java: Added division to the judges table.
	Now a judge may judge a division or all divisions and then the xml
	file sent to the subjective scoring app will only contain entries
	for the appropriate judge based on this information.

2002-12-27  Jon Schewe  <jpschewe@mtu.net>

	* web/setup/index.jsp: Now allow the database to be generated from
	the web for the standard setup, rather than using a separate Java
	application.  Also allows one to upload the challenge.xml
	document.  Now challenge.xml document is stored in the database.

2002-12-24  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/index.jsp: Allow one to change the number of seeding
	rounds from the web.

	* web/scoreEntry/select_team.jsp: Only allow one to select run
	numbers to edit that have been completed by some team. 

	* web/scoreEntry/scoreEntry.jsp: Make sure that only the most
	recently entered run can be deleted

	* web/instructions.jsp: Added instructions on what to do if a team
	doesn't show up.

	* web/admin/verifyTeams.jsp: Fixed bug where tournament teams
	application variables are not initialized after team upload.

	* src/fll/web/admin/UploadTeams.java: Removed some debugging and
	fixed error where the last column in the datafile was not
	recognized.

	* src/resources/challenge.xml: Removed reference to fll.css
	because it cause me to not be able to look at the document
	directly in IE at a region tournament.

	* src/resources/fll.xsd: Added annotation elements so that XML
	tools can properly display information about how the schema is to
	be used.

	* web/initializeApplicationVars.jsp: Put all code in it's own
	scope so that this file may be included multiple times in the same
	document (required for the developer page)

	* web/footer.jsp: Added warning message when using a database
	other than "fll"

	* web/developer/index.jsp: Added page to do change databases and
	to reinitialize the database from the XML document.  This is just
	to make it easier to do development. 

2002-12-18  Jon Schewe  <jpschewe@mtu.net>

	* web/admin/index.jsp: Sort regions by name of region in drop down
	list

	* src/fll/web/admin/UploadSubjectiveData.java
	(saveSubjectiveData): Changed delete/insert into a replace
	statement to minimize chance of transaction problems

2002-12-17  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/report/FinalComputedScores.java: Sort by team number
	in the case of a tie, just so we get the same report every time

	* web/scoreboard/allteams.jsp (rs): Scroll the scoreboard on fewer
	records

	* src/fll/xml/ImportDB.java (importDatabase): Class for importing
	score data from another database

	* src/fll/web/admin/Judges.java (commitData): Added tournament
	column to the Judges table, thus allowing me to merge all of the
	tournament databases and still be able to compute scores for each
	one.

	* src/fll/web/admin/Tournaments.java: Fixed bug where tournaments
	get deleted from the edit page (at least I think I got it)

2002-12-15  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/GenerateDB.java (generateDB): Fixed typo in
	populating TournamentParameters

2002-12-14  Jon Schewe  <jpschewe@mtu.net>

	* Labeled fll_12_14_2002 and published

	* web/scoreboard_800/allteams.jsp: Start scrolling the all teams
	list after 2 scores.

	* src/fll/web/playoff/Playoff.java (getDisplayString): Display "No
	Show" in the playoff brackets for no show teams.  

	* web/playoff/brackets.jsp: Make sure that admin brackets don't
	scroll and the regular brackets do

	* src/fll/web/admin/UploadSubjectiveData.java
	(saveSubjectiveData): remove debugging output

	* src/fll/xml/GenerateDB.java (generateDB): Populate Regions and
	TournamentParameters with default values

2002-12-13  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/ChallengeParser.java: Handle case where reference to
	xsd file does not end in a slash.  

	* published to website with fix for score entry and yes/no
	
2002-12-12  Jon Schewe  <jpschewe@mtu.net>
	* published to website
	
2002-12-10  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/brackets.jsp: Added ability to show brackets without
	title and scrolling so one can print the brackets out.

	* src/fll/web/scoreEntry/ScoreEntry.java: Show YES or NO in the
	count column for yes/no goals.

	* web/report/summarizePhase1.jsp: Added a column to show how many
	teams were scored by each score group.  This should help catch
	errors in data entry before one gets to the actual reports.

2002-12-09  Jon Schewe  <jpschewe@mtu.net>

	* build.xml: Added commands to create generatedb.zip, which is
	used to generate a blank database off of the XML document.

	* src/resources/challenge.xml: Fixed bug where one could enter
	some number of penalty loops less than 4 and 0 loops in our market.

	* src/fll/Queries.java: Pay attention to minimumScore in the
	performance element

	* src/fll/xml/GenerateDB.java (generateGoalColumnDefinition):
	Allow all goal columns to be null.  This will allow one to delete
	subjective scores.

	* scoreboard_800: added scoreboard that works on an 800x600 screen
	
2002-12-08  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/xml/XMLWriter.java (write): Added indentation and
	carriage returns to the output XML files. 

	* build.xml: Add ALL source to src.zip when making a distribution

	* src/resources/challenge.xml: Fixed naming of teamwork subcategories

	* web/scoreEntry/select_team.jsp: Selecting run numbers greater
	than 7 always returned the value 6.  This is now fixed.

2002-12-05  Jon Schewe  <jpschewe@mtu.net>
	* published to website
	
2002-11-30  Jon Schewe  <jpschewe@mtu.net>

	* web/playoff/brackets.jsp: Set the session variable currentRound
	when switching rounds.  This should prevent byes being given in
	later rounds like what happened at the New Hope tournament. 

2002-11-26  Jon Schewe  <jpschewe@mtu.net>

	* web/report/summarizePhase1.jsp: split computing summarized
	scores into two phases so that one can check the score groups
	before it just blows up

2002-11-25  Jon Schewe  <jpschewe@mtu.net>

	* web/footer.jsp: Setup so that can be installed in any context
	and will break out of a frameset

	* src/fll/web/report/FinalComputedScores.java (generateReport):
	Fixed query for report that didn't check FinalScores.Tournament

	* src/fll/Utilities.java (createDBConnection): Added ability to
	create a connection to a database other than fll

2002-11-24  Jon Schewe  <jpschewe@mtu.net>

	* web/errorHandler.jsp: provide a nicer page for errors

	* web/scoreEntry/scoreEntry.jsp: Allow one to delete performance
	scores from the web

	* web/credits/credits.jsp: Added licenses for MySQL, Tomcat and Java

	* web/troubleshooting/index.jsp: Added page for troubleshooting
	errors

	* web/admin/index.jsp: Make sure that TournamentTeams gets
	reinitialized when the current tournament is changed via the web

	* web/admin/commitTeam.jsp: Make sure TournamentTeams gets
	reinitialized when a team is edited or added from the web

	* web/admin/uploadTeams.jsp: Make sure TournamentTeams gets
	reinitialized when teams are uploaded

	* src/fll/web/admin/Tournaments.java: Make sure TournamentTeams
	get reinitialized when a Tournament is edited

	* web/scoreboard: modified scoreboard pages to use the scores from
	current tournament, rather than just state.

2002-11-23  Jon Schewe  <jpschewe@mtu.net>

	* src/fll/web/report/CategoryScores.java: When generating the
	tables for category scores, pay attention to the catgory.  Oops.

	* src/fll/model/SubjectiveTableModel.java: Allow users to delete
	scores in the subjective scoring app.

	* web/scoreboard/allteams.jsp: Make sure to order by TeamNumber
	after ordering by Organization so that all scores for a team are
	grouped together.

	* web/scoreboard/alldata.jsp: Make sure to order by TeamNumber
	after ordering by Organization so that all scores for a team are
	grouped together.

	* web/scoreboard/allteams.jsp: Add some space to the top of
	allteams.jsp so the first team doesn't scroll by so fast.
	Decrease the number of entries required to determine when
	scrolling of the scoreboard can start



