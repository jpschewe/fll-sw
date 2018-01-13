Notes about things to remember in Minnesota
===========================================

  * When printing, make sure you select the correct printer. There are 2 models of printers used. If the wrong printer is used, then the margins are wrong and text gets chopped off the top and the left edge. Match up the printer model with the name of the printer in the print dialog.
  * As of 2016 each server is paired with a router. This means the computer has a static address on it's wireless card and will not work properly with a different router.
    * If a server is used with a router other than it's own, the best option is to plug the server into the router with an ethernet cable and then use the address that the server gets from the wired connection. You will see 2 addresses on the main page, only 1 will work with the other computers.
  * You will most likely be given a database that already has the teams and schedules in it. So you can skip all of the instructions about how to load teams and schedules.
  * The computers that we have already have Java installed, so you only need to install the software.
  
  * After setting up for the Head to Head some MCs (and registration) like to have a tablet so they can more easily see the brackets. Bring up the printable brackets page for each head to head bracket on the tablet and then they can just refresh the pages to see who won each round.
  
State
------

  * Make sure to have a computer on the performance floor for use by the MCs so they can see the head to head brackets when the screens are showing the cameras.
  * Make sure we have at least 1 screen dedicated to scores during the head to head brackets, otherwise people can't see all of the information before it flips to the cameras.
  * When scheduling state make one award group finish subjective before lunch so that we get all of the score sheets for that award group before lunch. This helps spread out the score entry.

Public Wireless
==============

*This is still experimental and not in general use yet.*

Setup of public wireless. Each server needs to be matched with a public router. This router is configured specifically for the specified server.

Setup of public routers
-----------------------
  * Install dd-wrt on the router
  * Set the wireless name to "FLL Public"
  * On the setup page set the domain to "fll"
  * Disable the NTP client to keep it from getting unhappy with our DNS settings
  * Go into Services->Services and add a static lease for the server's MAC address
  * In "Additional DNSMasq Options" add the folowing where "addr" is the IP address in the static lease
    * "address=/net/addr" 
    * "address=/com/addr" 
    * "address=/org/addr" 
    * "address=/gov/addr" 
  * Disable Telnet
  * Disable WAN Traffic Counter
  * Each server needs to have a web server running on port 80 that is redirecting all traffic to port 9080 on the same host. This configuration requires that the machine have a static IP address.
