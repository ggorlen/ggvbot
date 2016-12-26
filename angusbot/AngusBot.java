/* This program is intended to be run once daily.
 * Tweets the corresponding day name from Angus Maclise's "Year" */

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import twitter4j.*;

public class AngusBot {
    private static final String FILENAME = "angus maclise - year.txt";
    private static final String MY_USERNAME = "MacLiseYEAR";

    // Access the Twitter API using the twitter4j.properties file
    private static final Twitter TWITTER = TwitterFactory.getSingleton();

    private HashMap<String, String> yearMap;

    private AngusBot() { }

    private void doATweet() {
        // Read contents of file to yearMap
        readFile(FILENAME);

        // Get today's date
        String today = getToday();

        // Tweet if today's tweet is not among this account's last 3 tweets
        if (!isAlreadyTweeted(MY_USERNAME, yearMap.get(today), 3)) {
            try {
                tweet(yearMap.get(today));
            } catch (TwitterException te) {
                te.printStackTrace();
            }
        }
    }

    private void readFile(String filename) {
        yearMap = new HashMap<>();
        InputStream in = this.getClass().getClassLoader()
                           .getResourceAsStream(filename);

        try (BufferedReader br = new BufferedReader(
                          new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] lineSplit = line.split("\t");
                yearMap.put(lineSplit[0], lineSplit[1]);
            }
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
    }

    private String getToday() {
        // Get a calendar instance initialized to the
        // current time on the computer's clock
        Calendar now = Calendar.getInstance();

        // Return today's date in a format that matches the hash key
        return new SimpleDateFormat("MMM dd").format(now.getTime());
    }

    private boolean isAlreadyTweeted(String user, String searchTerm, int time) {
        try {
            // Grab user's timeline
            Paging paging = new Paging(1, time); // change paging here if
                                                 // year old duplicate tweets fail

            List<Status> statuses = TWITTER.getUserTimeline(user, paging);

            for (Status status : statuses) {
                //System.out.println("@" + status.getUser()
                //    .getScreenName() + " - " + status.getText());
                if (status.getText().equals(searchTerm)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void findAndFavoriteMentions() {
        try {
            // Create list of mentions
            List<Status> statuses = TWITTER.getMentionsTimeline();

            // Favorite results
            for (Status status : statuses) {
                if (!status.isFavorited()) {
                    TWITTER.createFavorite(status.getId());
                }
            }
        } catch (TwitterException te) {
            te.printStackTrace();
        }
    }

    private void tweet(String tweet) throws TwitterException {

        // Update Twitter status
        Status status = TWITTER.updateStatus(tweet);

        System.out.println("Tweeted: " + tweet);
    }

    public static void main (String[] args) {
        AngusBot bot = new AngusBot();

        // Check system clock's day and tweet corresponding text from file
        bot.doATweet();

        // Search for mentions and favorite them
        bot.findAndFavoriteMentions();
    }
}
