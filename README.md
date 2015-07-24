FLL Software
============
This is the home of the Minnesota FIRST Lego League (FLL) software. This software has been written by individuals in Minnesota and has been used extensively there. This software is configurable for other tournaments as well.

Where Used
==========
Here is a list of tournaments that have used the software over the years.

  * [Minnesota First Lego League](http://hightechkids.org/programs/mn-first-lego-league-grades-4-9) (2001 - Present)
  * [Minnesota Renewable Energy Challenge](http://hightechkids.org/programs/mn-renewable-energy-challenge-4-12-grade) (2012 - Present)
  * GEMs/GISE drag race (2009)
  * Illinois (before 2005)
  * San Paulo, Brazil (before 2005)

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
  1. The [setup instructions](scoring/docs/SetupInstructions.md) include information on preparing to run the software, including the hardware and software for tournaments of different sizes.
  1. You should look at the [instructions for running the software](scoring/docs/InstructionsForRunningTheSoftware.md) for how to use the software to run a tournament.
  1. There are some training materials in (training). This includes a presentation used to train new head computer people in Minnesota and a sample tournament database. 


Descriptions of the design
==========================
  * Why is the subjective scoring application a separate app rather than a web based app? This is done because at many of our tournaments in the beginning we entered the subjective scores down at the location of the subjective judging and the network is in the performance area which is not close enough to network to the subjective judging area. We still run our state tournament this way, although most of our regionals aren't run this way.
  * We used Java when writing the application because it is a language that we're familiar with and a number of other people know as well to help develop. We also needed a language that is capable of doing SQL processing and a fair amount of math. We also wanted a language that is platform independent so we can run on both Windows and Linux.
  * We made the application web-based as it's mostly just a bunch of forms without much need for user interaction and this makes it easy to use other devices for clients to enter scores in the future.
  * The software is web-based, but not internet-based because most of our sites do not have a readily accessible internet connection.
  * We are using the database     [HSQL](http://www.hsqldb.org/) because it is a pure Java SQL database that can be run in memory. This makes it easier to install the software as another installer doesn't need to be run. We started with Access and moved off of that because it's Microsoft only and we wanted a database that is more robust. MySQL was used after that as it runs on Windows and Linux, however it still needs a separate installer.
  * For an explanation of how the scores are totaled up read [this document](scoring/docs/ScoreExplaination.pdf). You may need to click "View Raw" on the next page.

Scheduling Software
===================
In 2008 I started taking over the scheduling of the regional tournaments in Minnesota. So I took the constraints that had previously been used to determine schedules and wrote them down so that I could write some software to help me with this task. This software is available as part of FLL-SW. 

[Details on the scheduler are available](scoring/docs/scheduler.md).

Future plans
============
Fix the bugs and make the software more user-friendly. If you have suggestions on this, please add them to the issues list (requires registration). The link is at the top of this page.

Developers
==========
If you are interested in digging in, see the [Developer Information](scoring/docs/DeveloperInformation.md) page.

Minnesota
=========
[Note for Minnesota](scoring/docs/MinnesotaNotes.md)
