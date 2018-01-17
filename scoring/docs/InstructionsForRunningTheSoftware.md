# FLL-SW Instructions

It is best to go through this documentation the first time from the server so that you can follow the links.
When you click on a link you should right click on it and open it in a new tab or window. 
This way you won't loose the this document.

See [FLLTools Integration](flltools-integration.md) for integration with the FLLTools project from FIRST.

See the [Minnesota Notes page](MinnesotaNotes.md) for details specific to running a tournament in Minnesota.

See the [training presentation](training/computer-training.odp) for an overview presentation of running a tournament.

## Loading from a saved database

If you, or someone else, setup the database previously you can then load that database onto the server. To do that visit http://localhost:9080/fll-sw/setup on the server and then use the middle section of the page to specify a database to load. If you connect from the server you don't need to know the username and password.

## Setup of tournament without a saved database

Links in this section are also found on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp). Not all of these things need to be done the day before, you can do them the morning of, but it helps to prepare as much as you can the first few tournaments you do. You can skip this section if you were provided a database dump to load.

  1. You will need to provide a challenge description, either one from the software itself or a custom one or from a saved database ([instructions below](#loading-from-a-saved-database))
  1. Once the database is initialized, go to the [admin index](http://localhost:9080/fll-sw/admin/index.jsp) and upload the team datafile. This takes in a tab delimited file and uses that information to determine what teams will exist in the database. Note the sizes and datatypes of columns. If the columns in your datafile are too large the text will be truncated.
    * Files may be tab delimited, comma delimited or Excel spreadsheets
    * Any tournament that is listed as the initial tournament for a team during upload is automatically created 
  1. If the team data doesn't look quite right you can edit it by following the link to [Edit Team](http://localhost:9080/fll-sw/admin/select_team.jsp) on the admin index.
  1. If you'd like to change some of the information about the tournaments you can [edit the tournament list](http://localhost:9080/fll-sw/admin/tournaments.jsp).
  1. Set the current tournament on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp)
  1. At this point you can just use this computer, or you can download the database file from the admin page and import that into another computer at the tournament from the setup page. In Minnesota we typically have one person create the database that contains the data for all regional tournaments and then that database is distributed to all of the regional tournaments.  


## Things to bring to the tournament

  * Computers for score entry, display and the server
  * Extension cables and power strips. Many times these are provided by the site or other setup people.
  * Network switch and/or router
  * Network cables, even if using wireless, just in case
  * Make sure there is a VGA cable to connect the display computer to the projector. If your display computer doesn't have a VGA connector, make sure you have the appropriate adapter.
  * Bring or already have a couple of volunteers to help enter scores depending on the size of your tournament
  * Make sure you have something to write with for each performance score entry computer to write on the scoresheets
  * A paper cutter is handy for the score sheets during head to head as many times they are printed off 2 to a page and the page needs to be cut in half  


## Tournament day

For the most part you can just walk down the links on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp).

The other computers on the network will need to know the address of the server. You can find this information at the bottom of the main page.
  
The scoreboard, welcome page and head to head brackets have a place at the top for text to be displayed, this can be changed from the [admin index](http://localhost:9080/fll-sw/admin/). It's labeled Score Display Text.
  
If a team does not show up you should mark that team as a No Show in both the subjective application and in all performance runs. 
When you get to the brackets page this team will show up as last place and will still be placed on the brackets. 
You may need to enter a dummy score for the team that competes against the no show team and a no show for the team that isn't there for the brackets to properly advance the team.
  
### Display Computer

On the display computer bring up the Big Screen Display page. This will display the welcome page, the scoreboard and the head to head brackets as needed. This page is controlled by the Remote Control Page found under administration. **Important note**: Do not close the background window that launched the fullscreen window. This background window actually controls the fullscreen window. Note that when the scoreboard comes up the All Teams column will be blank until any scores are entered.
Once the page comes up , use F11 to make the web page go full screen without the address bar and title bar.

#### multiple displays

If you have multiple displays and want to display different information on each one, then you will want to name them. 
This is done by switching to the background page (Alt-Tab), typing in a name and then pressing submit. 
Once you have done this the named display will show up on the display remote control page.

On the display remote control page you can then select what to show on each page. 
There is a column for default and for each named display.
Any display that doesn't have a name shows what is selected in the default column.
Initially each named display is set to follow default, meaning that it will show what is selected in the default column.

    
### Performance score entry

Enter scores for the performance rounds on the [score entry](http://localhost:9080/fll-sw/scoreEntry/select_team.jsp) page. This page is linked from the main index.

I suggest at least two people entering scores. 
One person enters the scores and the second person goes back and verifies the scores using the score edit feature. 
This has caught many data entry errors in previous tournaments.

[Detailed performance score entry instructions](performance-entry-instructions.md)

### Subjective Score Entry

[Subjective score entry instructions](subjective-instructions.md)
    

### Head to Head
  * When it comes time to do head to head, make sure you initialize the brackets on the [head to head page](http://localhost:9080/fll-sw/playoff/index.jsp) before entering scores. If you attempt to enter head to head scores before initializing the brackets, you will get an error message. Don't worry, nothing is broken, you just need to initialize the brackets first.
    * When you initialize head to head brackets you will be asked how to seed the teams into the brackets. In most cases you want to choose "Use the best score from the seeding rounds".
  * You can print out the brackets by going to the admin/printable bracket page, under the Head to Head link on the main index, for each division and printing that out.
  * You can goto the printable bracket page and then keep refreshing the page. As scores are entered the bracket will fill out. Clicking the print scoresheets button at the top will create a PDF of the score sheets for the rounds that can be determined and haven't been printed yet. If you want to reprint some scoresheets you just check the box next to the round to print and then click the print scoresheets button.
  * Scores are entered per the [Performance Score Entry Instructions](performance-entry-instructions.md)
  * The final scores of each head to head bracket are not displayed on the screen. This is because they are revealed at the awards ceremony. *Because of this it is critical that the computer person check the final scores and make sure there are no ties at the end of the bracket.*
  
### Optional extra performance round

At some tournaments we don't run head to head, instead we schedule an extra performance round.
This round is in the schedule just like the normal 3 rounds.
Like head to head this round is not used for advancement.
To be able to enter these scores there are a couple of things that need to happen.
1. Create a single head to head bracket with all teams and initialize it. This allows one to enter at least 1 more score for each team.
1. Edit the max scoreboard round to be 10, this allows the extra round scores to be displayed
    1. Admin Index 
    1. Edit All Parameters
    1. Under the tournament change "Max Scoreboard Round" to be 10

## End of subjective judging

  * If using the stand-alone subjective scoring application, make sure you upload all data from the subjective scoring computers via the [admin index](http://localhost:9080/fll-sw/admin/index.jsp).
  * If using the web based subjective scoring application (usually on tablets), make sure that all devices have uploaded their scores.
  * Follow the compute summarized scores link on the [reporting page](http://localhost:9080/fll-sw/report/index.jsp) to summarize the scores. Make sure the judges shown are the ones you'd expect and that the number of teams seen by each judge are correct.
  * [View the reports](http://localhost:9080/fll-sw/report/index.jsp) and print them out through your web browser. You can visit these pages as many times as you like. Each time you visit them the report page will be recomputed. So if you find any missing scores, just enter them and goto the reporting pages again and make sure you compute summarized scores before going to the reports.
  
### Finalist Scheduling

If the tournament has multiple judging groups in the same award group, then finalist judging must be done to determine the winner in each subjective category.
This usually involves selecting the top 1 or 2 teams from each judging group and judging them again.

To aide in the scheduling of this final judging there is a finalist scheduling feature on the [reporting page](http://localhost:9080/fll-sw/report/index.jsp).
Once all of the subjective scores are entered and the initial head to head brackets have been created you should follow the link to schedule finalists.
Once the page has loaded you can disconnect from the network and finish the scheduling somewhere else and then reconnect when you are ready to store the finalist schedule.
Once the finalist schedule has been stored, the schedule can be printed or displayed on the big screen display.


## End of tournament

  * Download the database from the admin page and send it to your head computer person.

## Troubleshooting

If something blows up, take a look at the message that was spit out. It should be the first thing at the top of the page. Usually this will be something descriptive like "Not enough teams to compute standardized score" followed by a bunch of filenames and line numbers.

If you encounter an error, please file a ticket. Save the error log that is displayed (if there is one) as well as `tomcat/webapps/fll-sw/fllweb*` and `tomcat/logs` and attach them to the bug report.

In most cases when an error occurs you will be prompted to submit a bug report. 
This packages up the logs and the database into a zip file that can be attached to the ticket. 
After entering a description of what was happening when the error occurred, click submit and you'll be told where to find the zip file.
The bug report will be included in the database dump, so that the lead computer person can find it later when entering the database.

Also take a look at the [troubleshooting link](http://localhost:9080/fll-sw/troubleshooting/index.jsp) on the main page of the software. There are some tips in there as well.

### Missing password

If you are at a tournament and someone else has set the password for the server and you don't know it you can create a new username and password from the admin index. 
Use the "Create User" option under "User Management".
Once you create a new user, both that use and the new user will be able to edit scores.
