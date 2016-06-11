# FLL Instructions

It is best to go through this documentation the first time from the server so that you can follow the links.
When you click on a link you should right click on it and open it in a new tab or window. 
This way you won't loose the documentation that you're reading.


## Setup of tournament

Links in this section are also found on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp). Not all of these things need to be done the day before, you can do them the morning of, but it helps to prepare as much as you can the first few tournaments you do. You can skip this section if you were provided a database dump to load.

  1. You will need to provide a challenge description, either one from the software itself or a custom one or from a saved database ([instructions below](#loading-from-a-saved-database))
  1. Once the database is initialized, go to the [admin index](http://localhost:9080/fll-sw/admin/index.jsp) and upload the team datafile. This takes in a tab delimited file and uses that information to determine what teams will exist in the database. Note the sizes and datatypes of columns. If the columns in your datafile are too large the text will be truncated.
    * Any tournament that is listed as the initial tournament for a team during upload is automatically created 
  1. If the team data doesn't look quite right you can edit it by following the link to [Edit Team](http://localhost:9080/fll-sw/admin/select_team.jsp) on the admin index.
  1. If you'd like to change some of the information about the tournaments you can [edit the tournament list](http://localhost:9080/fll-sw/admin/tournaments.jsp).
  1. Set the current tournament on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp)
  1. At this point you can just use this computer, or you can download the database file from the admin page and import that into another computer at the tournament from the setup page. In Minnesota we typically have one person create the database that contains all tournament data and then distribute that database to all of the regional tournaments.  


## Loading from a saved database

If you, or someone else, setup the database previously you can then load that database onto the server. To do that visit http://localhost:9080/fll-sw/setup and then use the last box on the page to specify a database to load. If you connect from the server you don't need to know the username and password.


## Things to bring to the tournament

  * Computers for score entry, display and the server
  * Extension cables and power strips. Many times these are provided by the site or other setup people.
  * Network switch and/or router
  * Network cables, even if using wireless, just in case
  * Make sure there is a VGA cable to connect the display computer to the projector. If your display computer doesn't have a VGA connector, make sure you have the appropriate adapter.
  * Bring or already have a couple of volunteers to help enter scores depending on the size of your tournament
  * Make sure you have something to write with for each performance score entry computer to write on the scoresheets
  * A paper cutter is handy for the score sheets during playoffs as many times they are printed off 2 to a page and the page needs to be cut in half  


## Tournament day

For the most part you can just walk down the links on the [admin index](http://localhost:9080/fll-sw/admin/index.jsp).

The other computers on the network will need to know the address of the server. You can find this information at the bottom of the main page.
  
The scoreboard, welcome page and playoff brackets have a place at the top for text to be displayed, this can be changed from the [admin index](http://localhost:9080/fll-sw/admin/). It's labeled Score Display Text.
  
If a team does not show up you should mark that team as a No Show in both the subjective application and in all performance runs. 
When you get to the brackets page this team will show up as last place and will still be placed on the brackets. 
You may need to enter a dummy score for the team that competes against the no show team and a no show for the team that isn't there for the brackets to properly advance the team.
  
### Display Computer
On the display computer bring up the Big Screen Display page. This will display the welcome page, the scoreboard and the playoff brackets as needed. This page is controlled by the Remote Control Page found under administration. **Important note**: Do not close the background window that launched the fullscreen window. This background window actually controls the fullscreen window. Note that when the scoreboard comes up the All Teams column will be blank until any scores are entered.
Once the page comes up , use F11 to make the web page go full screen without the address bar and title bar
    
### Performance score entry
Enter scores for the performance rounds on the [score entry](http://localhost:9080/fll-sw/scoreEntry/select_team.jsp) page. This page is linked from the main index.

I suggest at least two people entering scores. 
One person enters the scores and the second person goes back and verifies the scores using the score edit feature. 
This has caught many data entry errors in previous tournaments.

[Detailed performance score entry instructions](performance-entry-instructions.md)

### Subjective Score Entry
[Subjective score entry instructions](subjective-instructions.md)
    

### Playoffs
  * When it comes time to do the playoffs, make sure you intialize the brackets on the [playoff page](http://localhost:9080/fll-sw/playoff/index.jsp) before entering scores. If you attempt to enter playoff scores before initializing the brackets, you will get an error message. Don't worry, nothing is broken, you just need to initialize the brackets first.
    * When you initialize the playoffs you will be asked how to seed the teams into the brackets. In most cases you want to choose "Use the best score from the seeding rounds".
  * You can print out the brackets by going to the admin/printable bracket page, under the Playoffs link on the main index, for each division and printing that out.
  * You can goto the printable bracket page and then keep refreshing the page. As scores are entered the bracket will fill out. Clicking the print scoresheets button at the top will create a PDF of the score sheets for the rounds that can be determined and haven't been printed yet. If you want to reprint some scoresheets you just check the box next to the round to print and then click the print scoresheets button.
  * If there is a tie during playoffs the software will attempt to break it with the standard tie breaker defined in the software. However if this is not possible TIE will be shown as the winner of a round. This means that these two teams need to compete again. Their previous scores should be deleted, the score sheet printed again and the new scores entered. Then the brackets page will display the new scores as it updates.


## End of tournament

  * Make sure you upload all data from the subjective scoring computers via the [admin index](http://localhost:9080/fll-sw/admin/index.jsp).
  * Follow the compute summarized scores link on the [reporting page](http://localhost:9080/fll-sw/report/index.jsp) to summarize the scores. Make sure the score groups shown are the ones you'd expect and that the number of teams seen by each score group are correct.
  * [View the reports](http://localhost:9080/fll-sw/report/index.jsp) and print them out through your web browser. You can visit these pages as many times as you like. Each time you visit them the report page will be recomputed. So if you find any missing scores, just enter them and goto the reporting pages again and make sure you compute summarized scores before going to the reports.
  * Download the database from the admin page and send it to your head computer person.

## Troubleshooting

If something blows up, take a look at the message that was spit out. It should be the first thing at the top of the page. Usually this will be something descriptive like "Not enough teams to compute standardized score" followed by a bunch of filenames and line numbers.

If you encounter an error, please file a ticket. Save the error log that is displayed (if there is one) as well as `tomcat/webapps/fll-sw/fllweb*` and `tomcat/logs` and attach them to the bug report.

In most cases when an error occurs you will be prompted to submit a bug report. 
This packages up the logs and the database into a zip file that can be attached to the ticket. 
After entering a description of what was happening when the error occured, click submit and you'll be told where to find the zip file.



