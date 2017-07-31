# Integration with the FLLTools project

This document documents integration between FLL-SW and FLL Tools. The FLLTools project can be found at https://github.com/FirstLegoLeague/.

Integration is done through the [mhub server](https://github.com/poelstra/mhub).
FLL-SW needs to know where to find the mhub server.
  * Visit the admin page
  * Click on `Edit all parameters` under `Advanced`
  * Set the `Mhub hostname` to the host that is running the mhub server
  * Set the `Mhub port` to the port that the mhub server is running on. 

## Display system

As of release 12.0.0 there is basic integration between FLL-SW and the [FLL Tools display system](https://github.com/FirstLegoLeague/displaySystem). The [list module](https://github.com/FirstLegoLeague/displaySystem#list) is used to display the most recent scores and the top scores in each award group.

You will need to edit all parameters (as specified above) and set `Display node` to be the name of the node that the display system is subscribed to on the mhub server.

*The initial implementation sends all of the right information, but it doesn't display well.* This is due to the data FLL-SW is sending being wider than the FLL Tools display expects to handle.

The parameter `Award Group Flip Rate` is used to determine how long to display each list.


## Developers

Those interested in expanding this integration should look at the package `fll.flltools`. The class `MhubMessageHandler` takes care of sending and receiving messages.
