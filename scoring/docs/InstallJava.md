Installing Java
===============
All of the fll-sw software is written in Java. This means that it will run on any computer with Java installed without any other steps. However this does mean that you need to have Java installed. Below are instructions that will get you going in Windows, Mac and Linux

Supported Versions of Java:
  * Java 8.x/1.8.x

JRE
-----
The JRE is required to run Java applications.

To check if you have the JRE already installed, open up a command prompt and execute `java -version`. If you get some version information, then you're all set. If you get an error about command not found or something like that, keep reading.

These instructions don't directly link to the download location so that one can find the download when the website changes in the future.

  1. Visit http://java.com
  1. Click on Free Java Download
  1. Install downloaded file
      * Windows & Mac - just double click on the file
        * watch out for the installer attempting to add on extra applications or toolbars
      * Linux - download the tar.gz file
        1. Extract the downloaded file: tar -xzf filename.tar.gz
        2. Create a tools directory where the software is uncompressed: mkdir .../fll-sw-<version>/tools
        3. Move the uncompressed Java system to the tools directory as jdk-linux: mv jdk... .../fll-sw-<version>/tools/jdk-linux
        4. Now all scripts in the bin directory will find this JDK install
