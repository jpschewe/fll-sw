The server software needs to be installed on any computer used as a server or to run the scheduling software, should only be 1 at each tournament. This software can be installed on multiple computers so that they can all be used as servers, but the database only lives on one, so you can only use one at a time for the same tournament.


Get the software installed
==========================

  1. Download the [latest version of the software](https://github.com/jpschewe/fll-sw/releases/latest)
  1. Extract the zip file to a location that you can remember. This is where the software will run from. 
    * It can be run from a USB drive too, so you could pick it up and move it from computer to computer.
  
Test the software
=================

  1. Open up the install directory
  1. Double click on fll-sw (fll-sw.sh for Linux and Mac)
    * If windows smart screen comes up, click on More Details, then Run Anyway
  1. Click on Start web server
    * If windows asks about allowing access for Java, you need to click allow access
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
    1. Open a command prompt (terminal or xterm in Linux and Mac)
      * Start->Run (or search) cmd
    1. Change to the directory that the server software is installed in
      * This is done as a set of cd commands eg.
        * ```cd Downloads```
        * ```cd fll-sw-5.11```
    1. Change to the tomcat\bin directory with `cd tomcat\bin`
    1. `startup`
    1. Check the error messages and see if you can figure out what the problem is. 
      * It may be that you don't have `JAVA_HOME` setup properly, see [setting up JAVA_HOME](SettingUpJavaHome).
      * Try searching on Google for the error messages
      * File a ticket using the "Issues" link at the top (requires registration to keep spam out)


Setting up the database
=======================
If the instructions above worked, you should have a web browser that is open to http://localhost:9080/fll-sw/setup. If not, go there now.

There are two ways to get the database setup.  One is from scratch and one is from a pre-built database dump. Most users will need to start from scratch. I typically create database dumps for those in Minnesota.

From Scratch
-------------
Pick the appropriate challenge descriptor built into the software or download a [challenge descriptor](../src/fll/resources/challenge-descriptors/) from our site or write your own. Use the top part of the setup page and select this challenge descriptor. If you had previously setup the database with team information and you want that information to go away, check `Rebuild the whole database, including team data`. Then click `Initialize Database`. 

If it worked then you will return to the setup page with a nice message at the top, otherwise you'll get some nasty error. If you get an error, file a ticket with what's on the screen.

From a Saved Database
--------------------
Use the portion of the setup page that talks about uploading a saved database. Select the saved database file that you've been given and click `Create Database`. 

If it worked then you will be prompted to create a username and password for the software.
If you get an error, file a ticket with what's on the screen.

This is also an easy way to setup a database on one computer and move it to another. You can get the database setup and then use the `Download database` link on the administration page and then upload it here on another computer.


Shutting down the software
===========================

  1. Open up the install directory
  1. Double click on fll-sw.exe (fll-sw.sh for Linux and Mac)
  1. Click on Stop web server
  

