FLL Software
============
This is the home of the Minnesota FIRST Lego League (FLL) software. This software has been written by individuals in Minnesota and has been used extensively there. This software is configurable for other tournaments as well. It has been used to score drag race tournaments in Minnesota as well.

Where Used
==========
Partners known to be running the software include

  * Minnesota
  * Illinois
  * San Paulo, Brazil

If you are using the software for your tournaments, please let me know
at jpschewe@mtu.net so that I can you add to this list.

Features
========
  * Open Source Software
  * Freely distributable
  * Platform Independent (written in Java)
  * Scalable - Successfully used in tournaments with 8 to 80 teams
  * Easy to use web interface
  * Allows hierarchical tournament structure to do tournament advancement from regional tournaments to state and beyond
  * Easy to configure for different tournaments with an XML file, no code changes required

Instructions
============
  1. The [setup instructions](SetupInstructions.md) include information on preparing to run the software, including the hardware and software for tournaments of different sizes.
  1. You should look at the [instructions for running the software](InstructionsForRunningTheSoftware.md) for how to use the software to run a tournament.


Descriptions of the design
==========================
  * Why is the subjective scoring application a separate app rather than a web based app? This is done because at many of our tournaments in the beginning we entered the subjective scores down at the location of the subjective judging and the network is in the performance area which is not close enough to network to the subjective judging area. We still run our state tournament this way, although most of our regionals aren't run this way.
  * We used Java when writing the application because it is a language that we're familiar with and a number of other people know as well to help develop. We also needed a language that is capable of doing SQL processing and a fair amount of math. We also wanted a language that is platform independent so we can run on both Windows and Linux.
  * We made the application web-based as it's mostly just a bunch of forms without much need for user interaction and this makes it easy to use other devices for clients to enter scores in the future.
  * The software is web-based, but not internet-based because most of our sites do not have a readily accessible internet connection.
  * We are using the database     [HSQL](http://www.hsqldb.org/) because it is a pure Java SQL database that can be run in memory. This makes it easier to install the software as another installer doesn't need to be run. We started with Access and moved off of that because it's Microsoft only and we wanted a database that is more robust. MySQL was used after that as it runs on Windows and Linux, however it still needs a separate installer.
  * For an explanation of how the scores are totaled up read [this document](ScoreExplaination.pdf)

Scheduling Software
===================
In 2008 I started taking over the scheduling of the regional tournaments in Minnesota. So I took the constraints that had previously been used to determine schedules and wrote them down so that I could write some software to help me with this task. This software is available as part of FLL-SW. 

SchedulerUI
-----------
In the bin directory you will find an application `SchedulerUI`. Once you run this application you will need to open a file from the menu or the toolbar. The file being asked for is a spreadsheet of your schedule. At this point only Excel spreadsheets can be read and they must match the expected format. The easiest way to get this format is to take one of our [blank schedules](https://sourceforge.net/p/fll-sw/code/ci/master/tree/scheduling/datafiles) and just fill it in with your team information. The schedules are named `#-#.xls` to state how many teams are in each judging group. Unless you're running a large tournament with finalist judging you need to have all teams for a given division seen by the same judge. So the schedule named `11-6.xls` is a schedule that has 11 teams in one judging group and 6 teams in the other judging group. Do not change the headers or number of columns before loading into the `SchedulerUI`. Once you have loaded the spreadsheet in, you will see problems highlighted in red and yellow. The details of the problems will be shown at the bottom of the screen. If you haven't changed any times and you started with one of our blank schedules, you should not have any problems highlighted. You can then write out the detailed schedules by clicking on the icon in the toolbar with an up arrow. This will write out PDF files with schedules suitable for use by an MC in the performance area and the judges in each subjective scoring area. The PDF files will be written to the same directory that the schedule was loaded from and will have the same base filename.

GreedySolver
------------
In 2011 I have added some more tools to help with this. There is now an application `GreedySolver` that will generate an empty schedule for a given tournament size. The tournament must be an even number of teams, so you may need to add a dummy team to create the schedule and then hand modify the schedule a bit. You should probably start from one of our [previous datafiles](https://sourceforge.net/p/fll-sw/code/ci/master/tree/scheduling/datafiles). Once that has run you'll have a blank schedule to fill in with teams. 

TableOptimizer
--------------
I've also added the application `TableOptimizer` to read in an existing schedule and to swap teams around across tables to minimize the number of times that a team is on a table and how many times that 2 teams are across the table from each other. This was added because the `GreedySolver` doesn't do this optimization and it was much easier to write a separate application to do the job.


Future plans
============
Fix the bugs and make the software more user-friendly. If you have suggestions on this, please add them to the issues list (requires registration). The link is at the top of this page.

Developers
==========
If you are really interested in digging in, see the [Developer Information](DeveloperInformation.md) page.

Minnesota
=========
[Note for Minnesota](MinnesotaNotes)
