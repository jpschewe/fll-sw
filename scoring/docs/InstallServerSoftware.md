The server software needs to be installed on any computer used as a server, should only be 1 at each tournament. This software can be installed on multiple computers so that they can all be used as servers, but the database only lives on one, so you can only use one at a time for the same tournament.


Get the software installed
==========================

  1. Download the latest version of the software
  1. Extract the zip file to a location that you can remember. This is where the software will run from. 
    * It can be run from a USB drive too, so you could pick it up and move it from computer to computer.
  
Test the software
=================

Windows
-------
  1. Open up the install directory and go into the bin directory
  1. Double click on start-tomcat.bat
    * It should have a nice icon on it that looks like a gear
  1. You should see a command prompt window open and stay open with some logging information about tomcat running. If not, see troubleshooting below.
  1. Double click on fll-sw.html
    * may have a web browser icon on it
  1. Your web browser should now open and you should see the setup page. If you get an error that tomcat is not running, see troubleshooting.

Troubleshooting
---------------
If the tomcat window didn't stay open, then tomcat couldn't start for some reason. Here are some things to try.

  1. Start tomcat directly
    1. Open up the install directory and go into the tomcat directory
    1. Go into the bin directory
    1. Double click on startup.bat
    1. If the window opens and stays open, then you're all set.
  1. Start tomcat from a command window so that you can see the errors
    1. Open a command prompt
      * Start->Run (or search) cmd
    1. Change to the directory that the server software is installed in
      * This is done as a set of cd commands eg. 

~~~~
cd Downloads
cd fll-sw-5.11
~~~~

    1. Change to the tomcat\bin directory with `cd tomcat\bin`
    1. Execute `startup`
    1. Check the error messages and see if you can figure out what the problem is. 
      * It may be that you don't have `JAVA_HOME` setup properly, see [setting up JAVA_HOME](SettingUpJavaHome).
      * Try searching on Google for the error messages
      * File a ticket using the "Issues" link at the top (requires registration to keep spam out)


Linux
------
  1. Open up a command prompt
  1. cd to the directory that you uncompressed the software
  1. `cd bin`
  1. Execute `./start-tomcat.sh`
  1. You should see some text about CATALINA_BASE and CLASSPATH and then get your prompt back.
  1. Open the directory that you uncompressed the software in a file browser
  1. Open bin
  1. Double click on fll-sw.html
  1. Your web browser should now open and you should see the setup page. If you get an error that tomcat is not running, see troubleshooting.

Troubleshooting
---------------
Look at the error messages and check that `JAVA_HOME` is set properly. Try searching Google or file a ticket with "New Ticket" above (requires registration).


Setting up the database
=======================
If the instructions above worked, you should have a web browser that is open to http://localhost:9080/fll-sw/setup. If not, go there now.

There are two ways to get the database setup.  One is from scratch and one is from a prebuilt database dump. Most users will need to start from scratch. I typically create database dumps for those in Minnesota.

From Scratch
-------------
Pick the appropriate challenge descriptor built into the software or download a [challenge descriptor](../src/fll/resources/challenge-descriptors/) from our site or write your own. Use the top part of the setup page and select this challenge descriptor. If you had previously setup the database with team information and you want that information to go away, check `Rebuild the whole database, including team data`. Then click `Initialize Database`. 

If it worked then you will return to the setup page with a nice message at the top, otherwise you'll get some nasty error. If you get an error, file a ticket with what's on the screen.

From a Database Dump
--------------------
Use the bottom part of the setup page and select the database dump that you've been given and click `Create Database`. 

If it worked then you will return to the setup page with a nice message at the top, otherwise you'll get some nasty error. If you get an error, file a ticket with what's on the screen.

This is also an easy way to setup a database on one computer and move it to another. You can get the database setup and then use the `download database` link on ad the administration page and then upload it here on another computer.


Shutting down the software
===========================
Run stop-tomcat or tomcat\bin\shutdown (depending on how you started it above) when you are done running the tournament. This will shutdown the server and make sure the database is properly closed.

