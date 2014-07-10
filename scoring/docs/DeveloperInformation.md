# Latest Development Releases

If you want to test out the latest code you can do so by [downloading the most recent automated packaged development release](http://mtu.net/jenkins/job/fll-sw-devel-release/). These are built automatically by our continuous integration system. They are a good place to see what features are coming up in the next release and a relatively easy way for people to test if a bug has been fixed as they'd like it.


# Getting started as a developer

  1. Make sure you have Java 7 (or greater) installed
  1. Get the source code from git
    * git clone git@github.com:jpschewe/fll-sw (read-only)
    * Or you can create a fork and then submit a pull request when you have changes to merge in
  1. Build by changing to the fll-sw directory and running `./ant.sh`
  1. You can start tomcat with `./ant.sh tomcat.start`
  1. You can stop tomcat with `./ant.sh tomcat.stop`, the target `tomcat.reload` can be useful when working on servlet code.
  1. We have a continuous integration server running using [Jenkins](http://jenkins-ci.org/). You can access it at [http://mtu.net/jenkins/job/fll-sw/](http://mtu.net/jenkins/job/fll-sw/). The master branch is built by the job 'fll-sw', all other branches are built by 'fll-sw-feature-branches'.

You can edit using your favorite Java IDE. We provide project files for Eclipse.

If you're new to git read at least the first 3 chapters of [http://progit.org/book/](http://progit.org/book/)



## GIT rules/guidelines

Do all development on a branch other than master. Branches named "feature.XXX" or "ticket.XXX" are preferred. Where "XXX" can be anything, but typically has the number of the ticket that you're working on in it and some short description of what you're doing. Example usage:


    git branch ticket.11.foo master # creates a new branch "feature.11.foo" based off of the local master branch, you can also use origin/master
    git checkout ticket.11.foo # switch to branch ticket.11.foo
    # make changes
    git commit # commit early and commit often
    # when you have something to share
    ./ant.sh before-checkin # to make sure you didn't break something
    git push origin ticket.11.foo # push all changes in local branch "ticket.11.foo" to a remote branch with the same name on the remote "origin"


Commit early and often.

Don't rebase commits that have already been pushed.

Before merging into master make sure to merge master into your branch, push, and wait for all tests to pass in continuous integration.
When your feature branch is pushed back up to the main repository, it will be built in continuous integration to be sure that nothing is broken.

    # when you want to get changes from master
    git fetch -p # get changes from the remote repository
    git merge origin/master # merge the changes from the remote repository into your current branch

Don't fast forward master, always use `--no-ff` when merging into master. This way it's easier to track features added. Contact Jon Schewe and he'll take care of merging the branch into master as features are finished and tested. 



## Ticket rules/guidelines
When you start work on a ticket, assign it to yourself. Don't work on someone else's assigned tickets unless you talk to them.

### Attachments to tickets
Github allows attachments of images, but not general files. So if you want to attach a file other than an image you need to host the file somewhere and put a link to it in the ticket. I found a solution using Github's Gist service http://feeding.cloud.geek.nz/posts/attaching-files-to-github-issues/.


## Wiki Editing
Please stick to [standard markdown syntax](http://daringfireball.net/projects/markdown/syntax) as much as possible. This is because we package a copy of the wiki in the release and the processor only handles standard markdown plus basic linking.


## Using Eclipse

  1. [Download Eclipse](http://www.eclipse.org/downloads/). Get the Jave EE developer edition.
  1. Tell Eclipse to import an existing project and point it to the root of the git checkout


### Viewing the database diagram

There is a database diagram in scoring/docs/dbModel.clay. You can view this with the [Clay Mark II plugin for Eclipse](http://www.azzurri.jp/en/clay/index.html). The free version is all I'm using, so you can just download it by adding the following update site to your Eclipse installation: http://www.azzurri.co.jp/eclipse/plugins/


### Using Ant in Eclipse

Most ant targets will work out of the box once you tell Eclipse about our ant build file. However if you want to run the test.report target you'll get an error about the style sheet. Using the instructions at http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Ftasks%2Ftasks-ant-version.htm you can point Eclipse at scoring/tools/ant as the Ant Home variable and then the test.report target works. This setting is for the workspace, so you'll want to make sure you're workspace is only used for fll-sw. 


## References to Documentation

  * [Java](http://download.oracle.com/javase/6/docs/api/index.html)
  * [Java Servlet API](http://download.oracle.com/docs/cd/E17802_01/products/products/servlet/2.5/docs/servlet-2_5-mr2/index.html)
  * [JSTL - core tag library](http://download.oracle.com/docs/cd/E17802_01/products/products/jsp/jstl/1.1/docs/tlddocs/index.html)



# Making a release

  1. Get the latest code from git
  1. Create a tag with `git tag -s <tag name>`
    * `tag name` should be `x.y` where `x` is the major version (counting up per season), `y` is the minor version
    * You can optionally use `-a` instead of `-s` to create an unsigned tag
    * You may needed execute `git config user.signingkey 0x<your key id>` before the signing works 
  1. Push the tag with `git push origin <tag name>`
  1. Use Jenkins to run the `fll-release` job and get the resulting archive from there
    * Contact Jon Schewe if you don't have access to create releases
  1. Create a new release on GitHub
    1. Paste the changes since the last release into the release notes
    1. Upload the file created in Jenkins


# Misc Notes


  * [Vendor Branches](VendorBranches.md)
  * [Long Term Plans](LongTermPlans.md)
