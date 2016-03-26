Installing Java
===============
All of the fll-sw software is written in Java. This means that it will run on any computer with Java installed without any other steps. However this does mean that you need to have Java installed. Below are instructions that will get you going in Windows and Linux. For Mac it's typically included, you just need to run Software Update to ensure that you have the latest version.

Supported Versions of Java:
  * Java 8.x/1.8.x

JDK
-----
The JDK is required to run and build Java applications. This is required for some special Java applications like the server software for fll-sw.

To check if you have the JDK already installed, open up a command prompt and execute `javac -version`. If you get some version information, then you're all set. If you get an error about command not found or something like that, keep reading.

These instructions don't directly link to the download location so that one can find the download when the website changes in the future.

  1. Visit http://www.oracle.com/technetwork/java/index.html
  1. Look for a link to Java SE, Java Standard Edition
  1. Select downloads
  1. Click `Download JDK`
  1. Select your platform and check the box
  1. Select continue
  1. Select the file to download
  1. Install downloaded file
      * Windows - just double click on the file
      * Linux - download the tar.gz file
        1. Extract the downloaded file: tar -xzf filename.tar.gz
        2. Create a tools directory where the software is uncompressed: mkdir .../fll-sw-<version>/tools
        3. Move the uncompressed Java system to the tools directory as jdk-linux: mv jdk... .../fll-sw-<version>/tools/jdk-linux
        4. Now all scripts in the bin directory will find this JDK install
  1. Keep track of where you install the JDK, this is the path that you need to find when [setting up the JAVA_HOME environment variable](SettingUpJavaHome.md)
    * Settings the JAVA_HOME environment variable is not required on Linux when you have put the Java install in the tools directory as jdk-linux. 
