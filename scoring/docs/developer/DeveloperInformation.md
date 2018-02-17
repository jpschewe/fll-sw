# Latest Development Releases

If you want to test out the latest code you can do so by [downloading the most recent automated packaged development release](https://mtu.net/jenkins/job/FLL-SW/job/master/lastSuccessfulBuild/artifact/scoring/build/). These are built automatically by our continuous integration system. They are a good place to see what features are coming up in the next release and a relatively easy way for people to test if a bug has been fixed as they'd like it.

Documentation on the database schema, API, workflows, etc. for the [current release are available on the continuous integration server](https://mtu.net/jenkins/job/FLL-SW/job/current-release/Documentation/). You can also view the documentation in your working directory by running the ant task `docs` and then looking in `scoring/docs/index.html`. 


# Getting started as a developer

  1. Make sure you have Java 8 (or greater) installed
  1. Get the source code from git
    * git clone git@github.com:jpschewe/fll-sw (read-only)
    * Or you can create a fork and then submit a pull request when you have changes to merge in
  1. Build by changing to `fll-sw/scoring` and running `./ant.sh`
  1. You can start tomcat with `./ant.sh tomcat.start`
  1. You can stop tomcat with `./ant.sh tomcat.stop`, the target `tomcat.reload` can be useful when working on servlet code.
  1. We have a continuous integration server running using [Jenkins](http://jenkins-ci.org/). You can access it at [http://mtu.net/jenkins/job/fll-sw/](http://mtu.net/jenkins/job/fll-sw/). The master branch is built by the job 'fll-sw', all other branches are built by 'fll-sw-feature-branches'.

You can edit using your favorite Java IDE. We provide project files for Eclipse. See the eclipse folder for the launchers and the code formatter profile.

If running on 64-bit Linux you will need libc6-i386 installed otherwise you will get a cryptic file not found error when the launch4j task runs.
This is because the launch4j task needs to execute a 32-bit binary.

The labels that are green are good places for a new developer to start. One is the documentation tasks. It's always nice to have new people help with documentation because they have an outside perspective on how the software works. Another is 'good first issue'. These are tasks that should be fairly easy to implement and will give a developer a start on learning the codebase.

# Copyright

All of the code is currently Copyrighted by INSciTE.
Please make sure anything you add has the following sort of header:

    Copyright (c) ${year} INSciTE.  All rights reserved
    INSciTE is on the web at: http://www.hightechkids.org
    This code is released under GPL; see LICENSE.txt for details.


# GIT rules/guidelines

If you're new to git read at least the first 3 chapters of [http://progit.org/book/](http://progit.org/book/)

Do all development on a branch other than master. Branches named "feature.XXX" or "ticket.XXX" are preferred. Where "XXX" can be anything, but typically has the number of the ticket that you're working on in it and some short description of what you're doing. Example usage:


    git branch ticket.11.foo master # creates a new branch "feature.11.foo" based off of the local master branch, you can also use origin/master
    git checkout ticket.11.foo # switch to branch ticket.11.foo
    # make changes
    git commit # commit early and commit often
    # when you have something to share
    ./ant.sh before-checkin # to make sure you didn't break something
    git push origin ticket.11.foo # push all changes in local branch "ticket.11.foo" to a remote branch with the same name on the remote "origin"

Alternatively fork the repository and submit a pull request.

Commit early and often.

Don't rebase commits that have already been pushed.

Before merging into master make sure to merge master into your branch, push, and wait for all tests to pass in continuous integration.
When your feature branch is pushed back up to the main repository, it will be built in continuous integration to be sure that nothing is broken.

    # when you want to get changes from master
    git fetch -p # get changes from the remote repository
    git merge origin/master # merge the changes from the remote repository into your current branch

Don't fast forward master, always use `--no-ff` when merging into master. This way it's easier to track features added. Contact Jon Schewe and he'll take care of merging the branch into master as features are finished and tested. 



# Ticket guidelines
When you start work on a ticket, assign it to yourself. Don't work on someone else's assigned tickets unless you talk to them.

## Attachments to tickets
GitHub allows attachments of certain file types. [You can find the list here](https://help.github.com/articles/file-attachments-on-issues-and-pull-requests/). If you rename the file you want to attach to one of these extensions, they will attach nicely. For databases and subjective data files I add ".zip" a second extension, for log files I add ".txt" as a second extension. This way GitHub accepts the attachment and we still know what type of file it was.


# Wiki Editing
Please stick to [standard markdown syntax](http://daringfireball.net/projects/markdown/syntax) as much as possible. This is because we package a copy of the wiki in the release and the processor only handles standard markdown plus basic linking.


# Using Eclipse

  1. [Download Eclipse](http://www.eclipse.org/downloads/). Get the Jave EE developer edition.
  1. Tell Eclipse to import an existing project and point it to the root of the git checkout

## Using Ant in Eclipse

Most ant targets will work out of the box once you tell Eclipse about our ant build file. However if you want to run the test.report target you'll get an error about the style sheet. Using the instructions at http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-ant-version.htm you can point Eclipse at scoring/tools/ant as the Ant Home variable and then the test.report target works. This setting is for the workspace, so you'll want to make sure you're workspace is only used for fll-sw. 

# Viewing the database diagram

After running the ant target doc.database-diagram the database diagram can be found in docs/database-diagram/index.html.

# Code style

Please follow the coding standards put forward at
http://mtu.net/~jpschewe/java/CodingStandards.html

Before checking in any code, please run the ant target "before-checkin" to
ensure your code is correct.


# References to Documentation

  * [Java](http://download.oracle.com/javase/8/docs/api/index.html)
  * [Tomcat API - 8](http://tomcat.apache.org/tomcat-8.0-doc/api/index.html)
  * [EL - 3.0](http://docs.oracle.com/javaee/7/api/javax/el/package-summary.html)
  * [Servlet - 3.1](http://docs.oracle.com/javaee/7/api/javax/servlet/package-summary.html)
  * [JSP - 2.3](http://docs.oracle.com/javaee/7/api/javax/servlet/jsp/package-summary.html)
  * [WebsSocket - 1.0](http://docs.oracle.com/javaee/7/api/javax/websocket/package-summary.html)
  * [JSTL - core tag library](http://download.oracle.com/docs/cd/E17802_01/products/products/jsp/jstl/1.1/docs/tlddocs/index.html)
  * [HSQL - embedded database](http://www.hsqldb.org/web/hsqlDocsFrame.html)
  * [iText - PDF library](http://api.itextpdf.com/itext/)

# Project goals

Some overall goals for the project to give other developers an understanding of what I want to see in the software.
This can help people understand my design decisions.

## Challenge description agnostic

I don't want to need to change the code when the challenge description changes. 
Ideally one can install this software once and then use it for many years of challenges without needing a code change.
This means that the code should not depend on any specific names that are in the challenge description and
definitely will not crash when specific strings from a challenge description do not exist.

This sometimes means that the score sheets and pages don't always look as pretty as possible, but this allows me to spend time fixing 
bugs and adding desired features rather than making changes to font sizes and spacing when the challenge description changes.

## Platform agnostic

I develop on Linux and want it to run there. 
However not everyone runs Linux and I want everyone to be able to run this software.
So everything is written in Java so that it can run anywhere there is a Java virtual machine.


## Easy to install

In the past the software required the installation of a database server which was difficult for some to install and setup.
I want this software to be easy to install and run.
Everything that the software needs is integrated into the build as much as possible, so that the user can just download a
release and go.



# Making a release

  1. Get the latest code from git
  1. Update Changes.md for the new release and commit that
  1. Create a tag with `git tag -s <tag name>`
    * `tag name` should be `x.y` where `x` is the major version (counting up per season), `y` is the minor version
    * You can optionally use `-a` instead of `-s` to create an unsigned tag
    * You may needed execute `git config user.signingkey 0x<your key id>` before the signing works 
  1. Push the tag with `git push origin master <tag name>`
  1. Use Jenkins to run the `fll-release` job and get the resulting archive from there
    * Contact Jon Schewe if you don't have access to create releases
  1. Create a new release on GitHub
    1. Paste the changes since the last release into the release notes
    1. Upload the file created in Jenkins to GitHub
    1. Update the current release pointer - this updates the website documentation to match this release
      1. git checkout current-release
      1. git merge --ff-only `<tag name>`
      1. Push the updated pointer with `git push origin current-release`

# Confusing terms

Over the years a few confusing terms have been introduced into the software and since then fixed. 
Here is a mapping of the confusing terms to what they are now. 
Any display of the old terms is already replaced by the new term, however there will be variables and SQL columns using the old terms.
 

| Old Terms       | New Term      | Notes      |
|-----------------|---------------|------------|
| event division  | award group   | |
| judging station | judging group | judging station is still used internally to refer to the location that judging for a particular category takes place |
| playoff         | head to head  | |

# Design Documents

  * [Challenge Description](ChallengeDescription.md)
  * [RESTful JSON API](JSON-api.md) 


# Misc Notes


  * [Vendor Branches](VendorBranches.md)
  * [Long Term Plans](LongTermPlans.md)
