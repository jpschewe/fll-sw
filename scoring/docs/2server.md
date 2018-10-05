There are times when you want to have a separate server in the judges room from the main one in the performance area.
This is useful when you want to be able to have the subjective scores upload and collated in the judges room without needing to go into the performance area.   

This setup is useful when the judges room cannot reach the server due to limitations of the network.
When using the site's wireless or the judges room is close to the performance area, this setup isn't very useful.


# Initial setup
  1. Initialize performance server as usual
  1. Download the database from the performance server
  1. Initialize the judges server with the database from performance
  1. Initialize tablets from the judges server
    * The tablets need to be initialized from the server they will upload to, otherwise the app won't work
  1. Don't make any changes to the judges server other than uploading subjective scores

# Getting scores from performance to judges
  1. After performance seeding is complete, download the database from the performance server
  1. Download the subjective data from the judges server
  1. Initialize the database in the judges server with the new database from performance
    * **Note that this will replace any changes and scores made on the judges server.**
    * use the same username and password as before to avoid confusing the tablets
  1. Set the tournament on the judges server
  1. Upload to the judges server the subjective data that was downloaded above

Repeat this process as necessary noting that only the subjective score changes to the judges server are persevered with this process.

# End of tournament

Make sure to download the database from both servers and name the downloaded files differently so that they can be merged later.