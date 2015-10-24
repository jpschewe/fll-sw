**Please fully read all instructions before you find the computers to use or going through with actual installation of software**

Required files
==============
To follow these instructions you will need the following files:

  * The latest version of the software

Types of Computers
==================
To run a tournament 4 types of computers will be required. These types define what software is to run on the computers and how they are used. The software required for each type of machine is listed below the description of the machine as links to pieces of this document.

Server
------
This computer is the brains behind the whole system. It contains the database and the webserver. This is also the computer that requires the most horsepower. The minimum hardware that I've successfully run the server on is a Pentium 166Mhz with 64MB of memory, however it was rather slow. 
I would suggest something with a 1Ghz processor and at least 512MB of RAM, preferably 1GB, just to be comfortable.
This computer must have a network connection and be able to be networked to the performance score entry computers and the display computer.

This computer requires the [Java JDK](InstallJava.md) and the [server software](InstallServerSoftware.md) and a [Supported Web browser](SupportedWebBrowsers.md). It also requires that the [JAVA_HOME environment variable be set properly](SettingUpJavaHome.md).

This computer is also the one that is used to print out reports. So it needs to have a printer attached. Technically any computer on the network can be used for printing, but the server is usually a logical choice. I would stick with a small laser printer as they print reasonably fast and everything is black and white. Any printer will work though, it just may get slow during the head to head competition.

Display
-------
This computer is used to display scores, either on it's screen or on an LCD projector. LCD projector is recommended. I've used a Pentium 133Mhz with 32MB RAM for this function at the state tournament without problems, so anything that powerful or greater would work fine. The important part here is that the display be good. The monitor/projector and video card need to be capable of at least 800x600 resolution, 1024x768 is better, however I understand that LCD projectors of this resolution are rather expensive. Anything bigger than 1024x768 really won't buy you much though. This computer needs to be networked to the server.

The only software required on this computer is a [Supported Web browser](SupportedWebBrowsers.md). 

When setting up the tournament, make sure there is a good place for teams to see the scores. A large white wall high up in a gym works pretty well, provided the gym is dark enough. At larger tournaments we've tapped into the building's TV monitors or setup extra large monitors with extra computers around the performance and pit areas.


Performance Score Entry
-----------------------

This computer is used to enter performance scores. The hardware requirements are the same as the display computer with the exception that output to an LCD projector is not needed. Users will just be using a web browser to enter scores. So a reasonable size screen is nice and a good mouse is a plus, but trackpads on laptops work fine too.

The only software required on this computer is a [Supported Web browser](SupportedWebBrowsers.md). 

Subjective Score Entry
-----------------------
This computer is used to enter the scores from subjective judging. A Pentium 166Mhz with 32MB of RAM running Windows 95 would be ok. I'd suggest a Pentium II 266Mhz or better if possible though as this computer needs to do a little more work than the Performance score entry computer. The display is also important here. 1024x768 is the recommended display, although it should work with 800x600 as well. A network card is not required for this machine if floppies or USB drives are used to transfer the scores to a networked machine. Otherwise this computer needs to be networked to the server and have a [Supported Web browser](SupportedWebBrowsers.md).

This computer requires the [Java JRE](InstallJava.md) and the [subjective software](InstallSubjective.md).


Number of computers
===================
Of course now that you know which types of computer you need and what hardware is required you'll want to know how many computers you really need. It is conceivable that a tournament be run all on one computer (this would not include a display computer) and all other software would be installed on this one computer, although that's not suggested. You'll also need one person per computer (except the display computer) for the whole tournament, this is sometimes handled by taking shifts.

Minimum
-------
The smallest tournament I've run has been an 8 team tournament with 2 computers. The configuration looks like this:

  * 1 computer that is used for a server, performance score entry, and subjective score entry.
  * 1 display computer

Average
--------
An average tournament that has between 16 and 32 teams runs pretty well with the following configuration.

  * 1 Server that is also used for subjective score entry
  * 2 performance score entry computers. In this case I'm assuming that 2 full tables are being used (4 teams competing simultaneously). If more tables are used I would have one performance score entry computer for each full table.
  * 1 Display computer The bigger the tournament the more important it is to have a 1024x768 display and a good place to display the scores.

Large
-----
Our state tournaments tend to be rather large, so I use this for the large tournament example. In Minnesota this is between 64 and 75 teams and has thousands of people and lots of hype and pressure, so it's really nice to have extra computers.

  * 1 Server
  * 1 performance score entry computer for each full table. A full table is one in which two teams compete simultaneously. Our state tournaments typically have 4 tables and therefore 4 of these computers.
  * 1 Display computer, and a nice AV guy that that send the display to all of the LCD projectors. At this point you should really have a computer and LCD projector combination that is capable of 1024x768 and big screens. Ours are about 10 feet on a side.
  * 1 subjective score entry computer for each subjective category. At the time of this writing, this is 4. You may think this is overkill, but I have each of the 4 major subjective categories on a dedicated computer (in the case of the research category this tends to be 3 categories on the same computer though). This seems to be a nice way to break up duties and keeps the score entry people busy without getting them too stressed out towards the end of the tournament when all of the scores come in and need to be checked.


Extra computers that are nice to have
-------------------------------------
  * 1 computer that is capable of being used to lookup scores and print reports while any issues/problems are being worked out on the server. Sometimes 2 printers are useful, one on the server and one on this machine because one person wants to print out playoff brackets while another person wants the score report printed for determining medals at the same instant (I speak from experience). I've not actually had this computer at a tournament, but have definitely seen the need for it.
  * 1 computer that is capable of being used by the refs only for looking up performance scores for disputes and for displaying the admin version of the brackets to keep track of what's going on during the playoff brackets. I've not actually had this computer at a tournament, but during the 2002 state tournament I found that having such a computer for the refs to use to show the playoff brackets would be invaluable.
  * 1 or more extra display computers that just have monitors on them and can be looked at by teams around the performance area or, if a good network is available, placed around the tournament site. This idea has yet to have been tried and is by no means required for a tournament, but if used one could get away with only a single LCD projector and just have these computers scattered around. I'd make sure to remove the keyboard and mouse from these machines so that people don't get into them and mess with the scores. Better yet, build a password into the score entry web pages so that this isn't a problem. Now if someone would write it...

Install Instructions
====================
These instructions are geared towards Windows 2K and XP, if installing on Linux the instructions are mostly the same, with the exception of getting prompted for installation locations. In these instructions locations are given for all installs, these are not hard coded into the software anywhere, but it'll make things a lot easier for me to debug over the phone if you use the same directories.
It is assumed that you have the computers used for the server, scoreboard display and performance score entry are already networked and you MUST know the IP address of the computer that is the server.
You can download the current version of the software by clicking on the button below.
[[download_button]] 
Unzipping this file will create a directory called fll-sw that contains the various zip files referenced here.
If you do not have a zip tool to open up the archives you can use the jar tool that comes with Java and follow the instructions below.

  1. Install Java
  1. Copy the zip file to the directory you want to uncompress it in
  1. Open a command prompt, on windows use Start->Run->command return
  1. Change to the directory you put the zip file in, on windows cd <path to zip file>
  1. Execute jar -xf zipfile
  1. You should now have the files uncompressed in a directory and you can move them around as needed.
  1. [Install Java (the JDK) 1.6 or higher](InstallJava.md) (the language everything is written in). If you downloaded a version of the software is the JDK bundled in, then you can skip this step for the server. This needs to be done on the server and on any machines that will be used to enter subjective scores.
      * When prompted for where to install it choose c:\packages\jdk-1.5.0
      * Set the environment variable JAVA_HOME to c:\packages\jdk-1.5.0
          * Under `*nix` this can be done by doing "JAVA_HOME=path; export JAVA_HOME"
  1. Test the server
      1. Open an explorer window to the directory you you unzipped install file into. It should be called something like fll-sw-<version>
      1. Unzip the install file to a location that you want to run from.
      1. Now goto bin and run start-tomcat.bat (start-tomcat.sh on Linux and Mac)
        * This will cause a command window to come up, don't close it, you can minimize it though. 
        * Server errors will show up here. 
        * If you are running Windows XP, you may get prompted by the firewall dialog asking if Java has permission to use the network, you need to click Unlock otherwise clients won't be able to connect
        * If on Linux and double clicking on start-tomcat.sh doesn't work, then you'll need to modify your file browser settings. Goto Edit->Preference->Behavior and select Ask Each Time. Now you should be able to select execute file when you double click on it.
      1. Double click on fll-sw.html in the bin directory. This will open up a web browser to the software main page
      1. If the database has already been initialized, then you're done unless you want to change the challenge descriptor. If so, click on the setup database link. If the database has not been initialized, then you'll be redirected to the setup page.
          1. Select the challenge descriptor or upload one and click submit
          1. If this succeeds, click on the link to the main page
          1. From here you'll be instructed to enter a username and password. This is used to access all pages that can make changes or display information that the teams should not see.
      1. Goto the Instructions page for information on using these pages
      1. Note that you will need a username and password to access all pages that can make changes or display information that the teams should not see. Other pages can be viewable by the general public, so the network can be shared.
      1. From the score entry computers and scoreboard computers make sure you can also get to this page, replacing localhost with the IP address of the server
      * You can find links to copy to the score entry computers at the bottom of the main page
      1. When done for the day shutdown tomcat by going to bin and running stop-tomcat.bat.
  1. Subjective
      1. Follow the instructions above for installing Java
      1. Unzip subjective.zip to a location on the computer
      1. Goto a computer that's connected to the server and goto the main page, then Administration then Download the datafile for subjective score entry
      1. Save this file somewhere on the computer used for subjective score entry, this can be copied around with a floppy
      1. Run subjective.bat from the location you unzipped subjective.zip and when prompted give it the location of the subjective.zip file that was downloaded from the server
      1. Enter scores for teams by selecting the category tab at the top then entering the scores under each subcategory
      1. Save often
      1. When done, quit the application and copy the file back to a computer that has access to the server
      1. Goto Adminstration and click on the Browse button next to Upload the datafile for subjective scores
      1. Select the file you just copied
      1. Click upload and if all is well you'll be returned to the Adminstration page again
  1. When you're done for the day you should make a copy of the database for the head of your state.
      1. Shut down tomcat
      1. Send a copy of tomcat/webapps/fll-sw/WEB-INF/flldb.* to the head of your tournaments
  1. For those interested in looking at the source and possibly making changes the source can be found [here](Home.md). Developers please look at README.developer. Any changes that you do make need to be sent back to me under the terms of the GPL. I look forward to any fixes you make. Please follow the coding standards listed [here](http://mtu.net/%7Ejpschewe/java/CodingStandards.html).
  1. Once you're done with the tournament you might want to uninstall everything. This can be accomplished by deleting the JAVA_HOME environment variable. Then uninstalling the JDK from the control panel.

Sponsor Logos
=============
In version 4 of the software we added the ability to have sponsor logos displayed. These logos are displayed on the main welcome page, in between team scores on the scoreboard and on the bottom of the playoff brackets page. Any files with image extensions (currently the list is: `.jpg`, `.jpeg`, `.gif`, `.png`) put into the directory `tomcat/webapps/fll-sw/sponsor_logos` will be displayed on these pages. See the `README.txt` file in this directory for information about sizing. The welcome page lists the logos sorted by filename, so you can to show the sponsors in a particular order, say by amount donated. The logos are displayed randomly on the other pages.    
