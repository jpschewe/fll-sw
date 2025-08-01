* #1270 - Add page displaying all known challenge descriptions
* Allow the user to access database backups from the setup page
* Correct links to download database backups
* #1267 - On schedule upload filter columns that have data of the incorrect type
* #1266 - Add documentation of the schedule file format
* #1265 - Add more details to the exception message when a time in a schedule cannot be parsed
* #1244 - Make the crossed off teams more visible on the deliberations page
* #1243 - Display places for virtual subjective categories in the awards script
* #1268 - Add a judges summary page
* #1263 - Add report displaying raw scores from virtual subjective categories
* #1251 - automatically compute summarized scores when needed for a report
* #1264 - Put gear next to goals that double count in the subjective judging app
* #1262 - 2 column layout for macro buttons on edit awards script
* #1261 - prompt the user for award group sort after uploading a schedule
* Scale weighted rank by number of teams in group per category
* Display practice scores on the scoreboard by default
* #1245 - Sort the finalist schedule report in the same order as the deliberation category order
* #1220 - Add metadata to runs so that they can be named and the user can control what runs are displayed on the scoreboard
* #1259 - Fix bug switching between full scoreboard and all teams / most recent scores
* #1242 - Don't stomp awards script when saving presenters
* #1253 - fix error uploading delibration winners a second time
* Fix finalist schedule link missing
* #1232 - Allow the user to delete all advancing teams

Release 19.2.0
==============
* #1228 - Add synchronize button to the footer of the subjective judging app
* #1223 - Add button to allow downloading all common schedules at once
* #1227 - Add score to performance winners in the awards CSV file
* #1230 - Add option to display scoreboard with most recent and all teams scores
* #1229 - Support generating finalist schedule when not running head to head
* #1222 - Display all performance runs in the performance runs vs. schedule report
* Fix bug in subjective application where the score summary sometimes would not display
* #1211 - Add PDF version of the schedule
* #1224 - Allow judge names to be longer than 64 characters

Release 19.1.0
==============
* #1129 - Display scores for other judges in the subjective score summary
* shrink font size for judging group on pit signs
* #1215 - Allow the user to specify the order of the tables in performance schedules
* #1123 - Upload subjective scores in the background
* #1212 - Export and import images with database dumps
* #1218 - Make sure judges can see the goal they are commenting on
* Properly import virtual subjective category winners
* Allow one to specify how final computed scores is sorted
* Add regular match play vs. schedule to head judge links
* Remove server links from judge page
* Make judge comment dialog height dependent on screen size 
* #1217 - prefix rubric filenames with team number
* Remove server address links
* #1214 fix error merging databases when deleting teams
* More consistent handling of websocket errors
* Synchronize sending of messages to web sockets
* Clear redirect URL on logout
* Only store Judges final scores flag when final, don't allow it to be unset
* Implement output of schedule for single team
* Fix storing awards script text and presenters for virtual subjective categories
* Allow score page text to be cleared
* Properly import levels of tournaments
* #1210 - overwrite wave information on schedule upload
* #1210 - allow user to edit team wave information
* #1209 - Clean up team from schedule table when deleting

Release 19.0.0
==============
* #1206 - Allow one to specify the order that awards are determined
* #1207 - Add virtual subjective categories to finalist scheduling and deliberations
* #1204 - Allow sorting of virtual subjective categories for awards script
* #1205 - Add virtual subjective categories to the awards script
* #1203 - Add virtual subjective categories to the awards report
* #1199 - Prevent the user from opening the subjective scoring application twice
* Add CSV file of award winners
* Add subjective schedule by time to the head judge page
* Make HTML dialogs easier to see
* #1153 - Allow REPORT_GENERATOR to edit the awards presenters
* #1202 - Add gear icons to rubrics when referenced by a virtual category
* #1170 - Separate out finalist non-numeric nominees
* #1198 - Add support for virtual subjective categories
* #1200 - Remove normalization in favor of scaling
* Remove goal group from category scores by judging group report
* #1131 - Add schedule constraint for time between subjective events separate from the time between subjective and performance events
* #1190 - Fix support for subjective judging without a schedule
* #1176 - Expand ignored categories functionality to all places where non-numeric awards are handled
* #1178 - handle paragraph breaks in award descriptions in the Awards Script
* #1171 - quiet websocket EOF errors
* #1173 - Allow one to specify individual non-numeric categories as ranked or not ranked
* Don't display the award group on the awards report if there is only one award group
* #1174 - Add support for generating pit signs
* #1172 - Add support for judges stating that their scores are final
* Highlight place cells in the awards script
* #1143 - Sort award groups in all reports to match the awards script
* #1147 - Sort performance report award groups to match the awards script
* #1146 - Sort award groups on the edit awards page
* #1145 - Allow the web schedule upload to handle performance runs beyond the seeding rounds
* #1161 - Don't assign tables to BYE runs
* #1182 - Add CSV for award winners of the tournament
* #1181 - Allow team schedules to be output from the web
* #1166 - handle Bye scores everywhere that No Show scores are handled
* #1149 - handle changes to judging station in the schedule reports
* #1137 - add pages to facilitate deliberations
* #1157 - remove text about going to information desk for finalist schedule

Release 18.5.0
==============
* #1164 - modify database dump to explicitly handle NULL values
* #1163 - Handle no show and bye when gathering team results
* #1162 - Allow awards script text to be null in the database
* #1158 - Fix consistency issue with how Champion's award is stored

Release 18.4.1
==============
* Fix missing head to head in awards script report

Release 18.4.0
==============
* #1137 - Initial support for deliberations
* #1152 - Allow head judge to see regular match play vs. schedule report
* #1150 - Change background color of missing team cell on performance schedule to be compatible with black & white printing
* #1142 - Add presented by to awards script header
* #1141 - Handle missing scores when generating team results
* #1148 - Rename reports link on main index to match head judge page
* #1144 - Make duplicate teams in a schedule a hard error

Release 18.3.0
==============
* #1138 - Add timestamp of when report generated to final computed scores
* #1136 - Add role for report generators
* #1120 - Add support for changing the FLL logo on the subjective rubrics
* #1119 - Add support for changing the FLL logo on the welcome page
* #1121 - Add support for changing the partner logo on the welcome page
* #1128 - Refresh the performance score team selection page when scores are entered
* #1135 - Allow editing of the award group order on the awards script editor page
* #1126 - Reload scoreboard if a score is edited to be lower than it was previously
* #1127 - Add PDF reports for top performance scores
* #1122 - Add scheduled time to subjective score entry page
* #1125 - Make performance enter score button larger on tablet
* #1124 - Add a border to the top of the buttons on score entry
* #1130 - make it clear when a score needs to be verified on the score entry form
* Make database backups accessible via the web
* #1110 - Allow one to set change time to be 0 minutes when creating new schedules
* #1112 - Add save as for schedule description to the scheduler UI
* #161 - color teams in the finalist schedule

Release 18.2.0
==============
* #1116 - fix error creating database from internal challenge description
* #1115 - Store performance data locally in case there is an error talking to the server
* #1114 - Add table name to performance data
* 105 - Highlight ties in the finalist in the numeric awards

Release 18.1.0
==============
* #1108 - allow one to manipulate slideshow images from the web
* Copy slideshow and sponsor logs during migration from an older version of the software
* #1107 - allow one to manipulate sponsor logos from the web
* Fix bug in schedule UI where the default value of a time is a string
* #1106 - Ensure no duplicate rows in most recent scores scoreboard display
* #1105 - Rework dynamic display to properly work with stand alone scoreboard pages
* Better handling of unknown displays

Release 18.0.0
==============
* Automatically select short practice round columns when loading a schedule
* Allow one to save a modified schedule from the SchedulerUI
* #1096 - Allow one to optimize tables in groups
* #1102 - Send to web sockets in the background to speed up web clients
* automatically select common columns when loading a schedule into the scheduler UI, website and when uploading teams
* #1100 - Switch the scoreboard to web sockets to avoid issues with web servers
* #1091 - Use web sockets for the display updates
* #1101 - Ensure that enumerated values have distinct scores
* #1099 - Properly initialize StringValueEditor 
* #1097 - fix error generating PDF scoresheets
* #705 - make scoreboard dynamic so that scores update in real-tie rather than waiting for a page reload
* Remove flltools integration
* Scoreboard group filter is by award group rather than judging group
* #1094 - update subjective rubrics for new FLL design
* #1095 - Make blank subjective rubrics blank - no team information
* #1079 - Add before and after award text to the awards script
* #1076 - Add the ability to easily get all teams ranked by performance score
* #906 - Handle differences in the awards script fields when merging a database
* #1077 - Increase font size on finalist teams display
* #1083 - Make it clear which report to use for finalist check in
* #1080 - Add blank lines after paragraphs in the awards script
* #1085 - Add titles to scoresheet generation and table assignment brackets
* #1086 - Add playoff match number to team selection
* #1088 - Assign head to head table labels before sending update to display
* #1084 - Properly display head to head winners in the awards script
* #1089 - Allow everyone to see the list of finalist teams
* #1057 - Remove duplicate copy of performance goals in team performance report
* #1074 - Make ReplayTournament more robust for handling other databases, particularly head to head

Release 17.4.0
==============
* Add playoff bracket name to the team selection box and display the playoff round number rather than performance run number
* #1071 - allow the user to delete a playoff bracket
* #1071 - prevent the user from uninitializing a playoff bracket that will cause problems with run numbers
* #1072 - Fix bug creating team finalist schedule
* #1069 - support viewing a bye run
* sort teams by table during head to head
* Don't import awards script data when importing performance data
* #1068 - Store form parameters when redirecting to compute summarized scores
* Rework award summary sheet to be by judging group
* #1067 - Allow the user to generate the final computed scores report by award group or judging station

Release 17.3.0
==============
* #1066 - Add link to edit unverified runs
* Fix bug with finalist scheduling not automatically selecting tied top teams
* Fix bug with finalist scheduling and multiple award groups
* #1030 - Allow up to 2 schedules for a subjective category
* #982 - Alternate colors of goal groups in the subjective application
* #1061 - Warn the user when reloading the subjective application and not connected to the server
* #1018 - Allow the awards script order to handle non-numeric categories added after initial setup
* #1060 - Add comments to score summary page and allow them and the notes to be toggled
* #1063 - Add award summary sheet
* #1062 - Create automatic database backups before the database is overwritten or an import occurs
* Fix bug where an extra performance score can't be deleted when head to head is not used
* Add titles to scoreboard pages

Release 17.2.0
==============
* #523 - Allow one to search for teams on the team edit page
* #1056 - Don't display teams with all no shows in all teams 
* #1075 - Use distinct colors for ties and no shows on subjective score summary, also add legend
* Adjust all teams scroll parameters so that Chromebooks scroll
* Add performance schedule by table that has space for notes
* Add scoreboard display for teams and top scores only
* Skip no show and bye from last 8 scoreboard display 
* #1048 - Add the ability to get populated subjective rubrics for a judge 
* #1055 - Fix problem deleting teams
* #1054 - Fix bug moving team between tournaments

Release 17.1.0
==============
* #1053 - Make sure window is maximized when I want it maximized
* #1051 - Highlight goal group table in category scores by goal group
* #1052 - Fix adding team to multiple subjective overall awards
* #1047 - Limit the display of performance scores to the top 5 ranks
* #1049 - Make ties in the subjective web application readable (white on red)
* #1049 - Set header on subjective web application side panel to "Task menu"
* #1049 - Add link to judge index from subjective web application
* #1015 - add the ability to sort the performance team selection by various team properties
* #1046 - Add the ability to migrate a database from another installation
* Add links from the report of performance runs for a team or a round to be able to edit the score

Release 17.0.0
==============
* #1032 - Add page to change head to head bracket tables without printing score sheets
* #1042 - Fix bug where subjective comments were not displayed immediately
* #1042 - Use buttons instead of slider for performance entry when range is greater than 10
* #1041 - Collapse restrictions by default in the editor to make them more manageable 
* #1020 - Allow one to override sections of the awards text with an empty string
* #973 - Show a reasonable error message when a duplicate performance score is entered
* #971, #972 - make review mode button show as depressed when review mode is on
* #1024 - Allow judges to see comments on the score entry page
* #1031 - add support for assigning tables for playoffs without printing
* #1021 - Add link from scoreboard index page to scrolling version of all teams
* #1038 - Add label for restriction warning message in challenge editor
* #971 - Add review mode to performance score entry
* #972 - Add review mode to subjective application
* #1036 - display progress dialog while writing outputs from the scheduler UI
* #793 - Return to original page after editing team data
* #1029 - Fix missing team numbers on head to head brackets 
* #1028 - Ensure that all subjective categories are in the finalist schedule
* #1027 - Properly handle no assigned room in the final scheduling app
* #1037 - Ensure that no shows are treated as low scores in the finalist scheduling app
* #1025 - Fix finalist navigation via header links
* #1026 - invalidate finalist schedule when the user changes which non-numeric categories are to be scheduled
* #1022 - Notify the user to allow the application through the firewall when running on Windows
* #1017 - add revision comment field to challenge description
* #1016 - rewrite subjective application without jquery-mobile, remove jquery
* Rework scrolling to be compatible across Chrome and Firefox
* #1011 - Pause schedule checking while loading a new schedule file

Release 16.5.0
==============
* #964 - Rework SubjectiveByJudge to be broken down by Judging Group rather than by Award Group
* #1013 - Repalce jquery.scrollto
* #1014 - Remove dependency on jquery-ui
* Upgrade to JDK 17
* #966 - Add report of regular match play runs vs. the schedule for the refs
* #955 - Use redirect instead of forward for login redirection
* #1000 - Add report of regular match play runs for the refs
* #1012 - Remove need for jQuery validation library
* #958 - Replace jQuery datetime picker and time picker with standard input types
* #998 - Scale the main scoreboard based on screen size
* #1006 - Use the tournament name as the base name when writing out schedules
* #1009 - Add report that shows scaled and raw scores for all teams in an award group
* #1002 - Display next round and schedule information when selecting teams for performance runs
* #34 - Allow the user to specify which row in a spreadsheet is the header row and allow the user to specify all column mappings
* Add number of practice rounds as a tournament parameter
* #1008 - Report performance ties in the awards report
* #1005 - Move merge database link to admin index
* #1004 - Update title on the awards script

Release 16.4.0
==============
* #994 - Add the ability to get subjective score sheets by category and award group
* #999 - Add performance schedules to ref page
* #997 - Add timestamp to performance reports used by refs
* #993 - Add award group to the edit award winners page for overall optional awards
* #1001 - Change button text when selecting team for performance
* Add page for the refs
* #886 - Clear redirect session variable once used
* #992 - fix storing of finalist schedule
* #991 - handle undefined score in finalist scheduling application

Release 16.3.0
==============
* #975 - Create offline download in subjective application for use when synchronize doesn't work 
* #949 - Notify the user of duplicate advancement group names
* #947 - Notify the user when the same team is listed as advancing twice
* #984 - Add tournament name to the footer of the breakout schedules
* #985 - Add nominated categories to the score summary page
* #938 - Require award group and judging group when a tournament is specified for imported teams
* #983 - Limit award groups displayed in the awards script to "real" award groups
* #979 - add basic check to make sure subjective scores aren't overwritten when importing finalist data
* #989 - Rework subjective authentication on synchronization
* #988 - rename Championship to Champion's to match FIRST language
* #987 - Add performance schedule for each table
* Fix saving of subjective data to local storage 
* Keep paragraph breaks entered into awards script entry
* Allow tournament levels to be renamed
* fix error editing team data 
* #974 - resolve implicit build dependency issues with spotbugs HTML reports and the distribution task

Release 16.2.0
==============
* #980 - Add performance reports to the main page for refs only
* #976 - output "last" place first in the awards script
* #981 - output description about team before team number in the awards script
* fix missing output of Championship award in awards script
* #978 - fix deleting of performance scores

Release 16.1.1
==============
* 2021 Minnesota challenge description 


Release 16.1.0
==============
* Import awards script information into a new database
* #966 - Remove edit score when entering performance scores for a single table 
* #964 - Add report of subjective scores by judge
* #942 - improve the visibility of the left and right portions of a condition
* #940 - make if/then/else obvious in the challenge description editor
* #962 - Reword team acceptance of performance entries

Release 16.0.1
==============
* 2021 Minnesota challenge description

Release 16.0.0
==============
* #211 - Generate the script for the awards ceremony
* #963 - Provide friendly feedback when a team number is not selected for verification
* #943 - Use words instead of symbols for inequalities in restrictions
* #936 - highlight goals involved in restriction errors
* #957 - Allow one to specify non-numeric categories that are not awarded for a particular tournament level
* #933 - Make buttons on subjective score entry larger
* #950 - Make Tournament Level a first class object, this will allow parts of the award script to be attached to a tournament level, all tournaments now have a level
* #953 - Add version of score entry form for practice rounds
* #951 - Allow users to be carried over through a database initialization
* #954 - Increase the font size on the login page
* #928 - modify layout of links on index pages to be more friendly
* #937 - Add head judge role and make the judges room page easier to navigate for the head judge
* #932 - Add top navigation bar
* #923 - Rework editing of award winners to allow multiple editors at the same time and to support multiple winners for each award
* #945 - hide the score column when using the tablets to enter performance scores
* #935 - allow a ref to specify which table they are scoring for and sort the teams based on the schedule for that table
* #934 - make team selection boxes before score entry larger
* #927 - UI changes to make performance score entry more friendly on tablet devices
* Fix initial display of conditions in challenge description editor
* #865 - Remove use of jquery-json in favor of standard JSON object included in current browsers
* #926 - Add judge initials to the subjective rubrics
* #922 - If the server is offline when synchronizing subjective scores, give the user a reasonable error message
* #907 - Add weighted rank column to final computed scores
* #925 - Move breadcrumbs to the top of the subjective application
* #920 - When a team is a No Show for subjective judging, make it clear on the PDF that is output
* #484 - Improve usability of the numeric category finalist scheduling page
* Added the forwarded for address to the access logs
* #915 - Fix bug moving subjective and non-numeric categories in the challenge editor
* #447, #908 - Allow the finalist schedule to be loaded from the database
* #909 - Use time picker widget when choosing start and end times in the finalist scheduling workflow
* Fix bug where awards report would fail to generate when multiple winners were selected for numeric categories
* #887 - security requirements are  handled on each page now rather than in the central initialize method

Release 15.9.1
==============
* #911, #918 - Allow the awards report to be output when there are no performance scores

Release 15.9.0
==============
* #912 - Use the order of subjective categories in the challenge description for the awards report
* #914 - Fix parsing of LocalTime in javascript by using the JSJoda time package
* #905 - Make tab to skip over read-only cells on non-numeric categories page
* #904 - Add FLL-SW version information to bug reports
* #902 - Fix loading of non-numeric award winners into the edit award winners page
* #903 - Fix importing of finalist schedule information when loading a database
* #903 - properly handle a tournament without a date

Release 15.8.0
==============
* #900 - Focus on the text field after opening the comment dialog in the subjective application
* #899 - Make sure that all properties exist in the subjective judging application all of the time
* Fix bug where the SQL query page doesn't work

Release 15.7.0
==============
* #897 - Fix handling of repeating question marks in comments
* #892 - Color comment buttons in the subjective application when comments have been entered
* Make subjective judging app reload smarter. If there are no modified scores, just load from the server.
* #891 - Fix typo in where non-numeric categories are stored in the subjective application
* #890 - Remove footer from login page

Release 15.6.0
==============
* #888 - Handle null team organization when generating the final computed scores report

Release 15.5.0
==============
* #680 - pass form parameters through the login redirect

Release 15.4.0
==============
* Make sure non-numeric nominees reload properly when the local data needs to be cleared
* #882 - Properly handle storing non-numeric nominees that don't have a judge associated
* #885 - Don't error on too few scores in summarization, instead depend on the UI to alert the user to this situation

Release 15.3.0
==============
* #880 - Add non-numeric nominees report
* #881 - Fix display of non-numeric categories on edit award winners page
* #878 - Use "wss:" for websockets when the page was loaded via https 
* #877 - Lock user accounts after too many failed password attempts
* #452 - Use jquery.scrollTo for all automatic scrolling
* #340 - Move setting of score page text to the remote control page
* #870 - Add security roles (admin, judge, ref)
* #869 - Keep carriage returns in subjective comments when creating PDF for teams
* #874 - Add challenge revision to the bottom of the score entry page
* #872 - Display non-numeric award description in subjective app
* #871 - Fix bug where judges were not saved from the subjective application
* Various fixes to the challenge description editor

Release 15.1.0
==============
* #857 - Clear non-numeric nominees before load
* #856 - Support non-numeric categories when displaying the challenge description
* #860 - Upgrade javascript libraries
* Fix storing of restriction message in the Challenge Editor
* require restrictions to have messages in the Challenge Editor

Release 15.0.0
==============
* #855 - Remove Subjective Java Application in favor of the web application
* #817 - Move database import to the top index
* #850 - Add support for delaying the display of performance scores
* #821 - Reformat the finalist team schedule to be similar to the grid used for scheduling
* #737 - Validate that all goals of a subjective category have the same rubric range titles
* #739 - Validate that all goals of a subjective category have the same rubric range min and max values
* #824 - Add next and previous buttons to finalist scheduling
* #831 - Add buttons to make fixing mistakes in finalist scheduling easier
* #816 - Add a report showing teams advancing to another tournament and their scores
* #820 - Filter all teams on scoreboard by judging group
* #819 - Add report of performance scores by judging station
* #849 - If all rubric range short descriptions are the same in a category, display it at the top of the subjective PDF
* #846 - Add support for nominating teams for non-numeric categories during subjective judging
* #810 - Add support for non-numeric categories in the challenge description.
* #844 - Output GoalGroup description on the subjective PDF files
* #511 - Add GoalGroup as a first class object. This allows it to have a description and cleans up the XML document.
* #845 - Add the ability to specify comments per goal in the subjective web app.
* #843 - Add the ability to generate PDF files with the subjective category results and comments
* #842 - Add the ability to enter comments in the subjective web application.
* #840 - Fix use of user provided constraint change times
* #837 - Fix errors around editing restrictions in the challenge description editor
* #839 - Allow the application to run headless
* #838 - Allow one to specify the port for the web server
* #799 - Convert subjective sheets to Apache FOP
* #802 - Convert miscellaneous reports to Apache FOP
* #800 - Convert detailed schedules to Apache FOP
* #801 - Convert final computed scores to Apache FOP
* #798 - Convert score sheet PDF to use Apache FOP instead of iText
* #778 - Add the ability to write blank score sheets from the challenge editor
* #826 - Remove application cache from finalist scheduling workflow
* #828 - Remove the concept of public finalist categories
* #818 - Fix setting of custom judging and award groups

Release 14.4.0
==============
* #813 - Allow the user to specify the order that the awards groups appear in the awards report
* #806 - Short term fix to add categories from non-numeric nominees to the award winners entry page. Long term fix is #810
* #811 - Add head to head winners to the awards report
* add horizontal lines separating the goal groups on the score entry page to match the lines on the score sheets
* link to awards report from judges page

Release 14.3.0
==============
* #795 - add report for awards
* #400 - turn on strict mode in all javascript files
* #804 - Remove decimal points from displays where the scores are clearly integers
* #722 - Fix splitting of times across pages in the performance schedule
* #797 - add subjective schedule sorted only by time across all judging stations
* #790 - add date and tournament name to display on main pages
* #789 - add to documentation to make it clear that reports can be generated with partial data
* #788 - right-align radio buttons on the score entry page to put them near the labels
* #749 - Remove error about jar file not found when running under windows
* #787 - Fix launching fll-sw.html from Launcher when run from Eclipse
* #786 - Configure tomcat to write access logs to log4j
* #781 - ensure that goals in a goal group are contiguous in the challenge description

Release 14.2.0
==============
* #782 - Make "practice" text lighter on score sheet
* #784 - Put "Ref" at the top of performance score sheets rather than "Judge"
* #551 - use waits instead of sleeps in the web tests to decrease test time
* #780 - Fix problem entering runs after regular match play when head to head is not used

Release 14.1.1
==============
* #779 - correct bug in upload schedule workflow that prevented Excel workbooks from being used 

Release 14.1.0
==============
* #775 - Allow teams to be added to the database during schedule upload
* #770 - Rename all HTML fields with names or IDs equal to 'submit'
* #767 - Allow one to skip score summarization to generate a report
* #767 - Always redirect back to the index page when computing summarized scores from the index
* Add server addresses to the top of the scoring coordinator and judges room pages
* Send users back to the page that they came from after setting the current tournament

Release 14.0.2
==============
* #313 - Use JQuery UI dialog on the score entry page so that the button text can be customized 
* #771 - updates to the 2019 performance score sheet and add extra space between team info and goals
* #768 - Confirm zero scores in the subjective web application are not No Shows
* #769 - Add light grey background to top scores on final scores report to make them easier to see
* #757 - Add an asterisk next to scores in the final report where a team scored zero on a required goal
* #399 - Add distributions that include a bundled JDK so that Java doesn't need to be installed
* #764 - Add labels to fields in the subjective web application
* #755 - Put tick marks on the score entry sliders in the subjective web application
* #753 - Add score sheet instructions to the challenge description file
* #758 - Allow rubric ranges to not have titles - this is needed for "ND"
* #752 - update subjective sheet writer code to handle "ND" being part of the challenge description rubric
* #751 - remove duplicated headers in subjective web application
* #706 - Allow one to choose which award groups are on each scoreboard display
* #721 - Remove pause from the bottom of the head to head bracket scrolling
* #716 - Add command line option to start webserver immediately
* #720 - Add performance score report for finalist judging
* #717 - Confirm closing the launcher when the web server is running
* #730 - Add table for each goal group within a subjective category to the subjective score report
* #746 - lock rubric range titles in the subjective web application header
* #728 - Document regular match play terminology
* #748 - remove ".." from paths sent to desktop applications. This allows the HTML page to properly open on Windows.
* #745 - make slider handle more obvious in the subjective web application
* #743 - fix finding of web root when running from launch4j executable
* #708 - prompt user to confirm when reloading the subjective application
* #734 - respect initial value in the subjective scoring web application
* #729 - sort the regular match play performance sheets by table then time 
* #727 - Add support for practice rounds
* #719 - Add judges room and performance area to the footer
* #725 - rework subjective sheets to have more comment space
* #726 - improve readability of subjective score summary
* #711 - handle double quotes in team name on the select team page for score entry
* #600 - migrate from FindBugs to SpotBugs
* #724 - update GatherBugReport to find logs in the new location after switching to embedded Tomcat
* #569 - upgrade to log4j2
* #352 - switch to mainstream opencsv
* #696 - build with gradle instead of ant
* #713 - use embedded Tomcat for the webserver

Release 13.5.0
==============
* #714 - fix bug displaying finalist schedule on the big screen display
* #712 - save the database as a dump file on an integration test failure
* #701 - add parameter to enable/disable head to head. This makes it much easier to run a tournament with extra rounds instead of head to head. 
* #702 - allow easy editing of parameters for the current tournament

Release 13.4.0
==============

* Work around for displaying all performance scores when not r
* #703 - be clear that head to head runs should be on single table pairs when running in parallel
* #704 - note that the rank is in parenthesis in the final computed scores report
* #700 - remove SSL code, this isn't needed now that the application cache isn't used

Release 13.3.0
==============

* #695 - compute performance and subjective standardized scores independently
* #509 - add date to tournament to help with sorting
* #675 - repeat category names over the count & score columns on the score entry page 
* #682 - add some more schedule outputs to the admin page (CSV schedule, subjective sheets, performance sheets)
* #173 - add tool to edit challenge descriptions
* #688 - allow the user to specify the filename suffix on subjective score sheets
* #670 - streamline the export/import process for the 2 server model
* #690 - remove the ranking report
* #689 - Support longer team names on the subjective sheets
* #681 - Remove yes/no from subjective clear data page. The yes/no can be confusing.
* #657 - Allow the import database process to do partial imports
* #679 - when loading from a database, select the tournament that was in the database
* #676 - keep tomcat logs from testing out of the distribution
* #673 - Pass full URL, including query string, when redirected to the login form

Release 13.2.0
==============

* #494 - add application icon to get rid of the default Java icon
* #669 - Keep the launcher from running twice
* #671 - Ensure that the link to the SSL web page uses the host name that the user visited 

Release 13.1.1
==============

* Add tie breakers to the 2018 challenge description
* #668 - support adding a team that's not in the schedule and still being able to edit their subjective scores

Release 13.1.0
==============

* #663 - add the ability to overwrite all subjective scores on upload
* #667 - provide error message, without stack trace, when there are too few scores for a judging group
* #666 - do DNS lookups in the background so that the loading of pages isn't slowed down by looking for host names
* #665 - keep database files out of the release

Release 13.0.0
==============

* #661 - Update FIRST Lego League logo for the subjective scoring sheets
* #645 - Add SSL option for the subjective judging web application to handle new security in web browsers
* #659 - add report of the top seeding round scores for all award groups
* #644 - allow one to automatically finish a playoff bracket
* #629 - Make selecting tournaments easier (better sorting, clearer language)
* #643 - Keep rows together with the same award group in the subjective by judging station report
* #618 - improvements to score summarization workflow
* #129 - validate form parameters in the finalist scheduling application
* #642 - explicitly update or insert performance scores and report proper errors
* #632 - add instructions for using the relative URL feature
* #631 - add launcher buttons for directories used
* #637 - Correct PDF footers showing the total number of pages
* #641 - Fix computed score report format when there is no schedule

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



