/* This program is intended to be run once daily.
 * Tweets the corresponding day name from Angus Maclise's "Year" */

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import twitter4j.*;

public class AngusBot {
    private static final String FILENAME = "angus maclise - year.txt";
    private static final String MY_USERNAME = "amplifiedgravel"; // Todo: Change to "MacLiseYEAR" before launch

    // Access the Twitter API using the twitter4j.properties file
    private static final Twitter TWITTER = TwitterFactory.getSingleton();


    public AngusBot() { }

    public void doATweet() {

        // Read contents of file to a String array
        String[] file = readFile(FILENAME);

        // Get today's date
        String today = getToday();

        // Check every item in the array against today's date
        for (int i = 0; i < file.length; i++) {
            if (file[i].startsWith(today)
                && !isAlreadyTweeted(MY_USERNAME, file[i].substring(7))) {
                try {

                    // Tweet text substring without accompanying date
                    tweet(file[i].substring(7));
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                break;
            }
        }
    }

    public String[] readFile(String filename) {
        String input = "";

        // Text file resource will work in JAR
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                input += line + "%";
            }
        } catch (FileNotFoundException ex) {
            System.out.println(ex.toString());
        } catch (IOException ex) {
            System.out.println(ex.toString());
        }
        return input.split("%");
    }

    public String getToday() {

        // Get a calendar instance initialized to the current time on the computer's clock
        Calendar now = Calendar.getInstance();

        // Return today's date
        return new SimpleDateFormat("MMM dd").format(now.getTime());
    }

    public boolean isAlreadyTweeted(String user, String searchTerm) {
        try {

            // Grab own timeline
            Paging paging = new Paging(1, 7);   // change paging here if year old duplicate tweets fail
            List<Status> statuses = TWITTER.getUserTimeline(user, paging);

            for (Status status : statuses) {
                //System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
                if (status.getText().toString().contains(searchTerm)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void findAndFavoriteMentions() {
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

    public void tweet(String tweet) throws TwitterException {

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
