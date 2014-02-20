Notes about things to remember in Minnesota
===========================================

  * Make sure to have a computer on the performance floor for use by the MCs so they can see the playoffs when the screens are showing the video
  * Make sure we have at least 1 screen at state dedicated to scores during the playoffs, otherwise people can't see all of the information before it flips to the video
  * When scheduling state make one division finish subjective before lunch so that we get all of the scoresheets for that division before lunch. This helps spread out the score entry.

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