/* This program scrapes Golden Gate Valley Branch's calendar and tweets events */

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jsoup.*;
import org.jsoup.helper.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import twitter4j.*;

public class GGVBot {

    private static final String URL_1 = "http://sfpl.org/index.php?pg=1100000001&m=all&t=1&l=8"; // GGV calendar
    private static final String URL_2 = "http://sfpl.org/index.php?pg=1100000001&m=all&t=3&l=8"; // GGV classes
    private static final String SFPL_BASE_URL = "http://sfpl.org/index.php?pg=";
    private static final int BUFFER_DAYS = 2;

    // Access the Twitter API using the twitter4j.properties file
    private static final Twitter TWITTER = TwitterFactory.getSingleton();


    public GGVBot() { }

    public void scrape(String url, int bufferDays) {

        // Create a document object and scrape our URL
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // SFPL's calendar currently uses "fineprint" div classes
        Elements classes = doc.getElementsByClass("fineprint");

        // Create a String to store an event and accompanying boolean variables
        String anEvent = "";
        String eventURL = "";
        boolean done = false;
        boolean tweet = false;

        for (Element e : classes) {

            // Create Strings of the current Element's attributes and text to process
            String attr = e.attributes().toString();
            String text = e.text().toString();

            // Grab the date and time of the event
            if (Character.isDigit(text.charAt(0)) || text.startsWith("Today, ")) {
                anEvent += text;

                // If this event is a day from now and hasn't already been tweeted, flag to tweet it
                if (!anEvent.contains("Today, ") && isValidDate(text, bufferDays)
                        && !isAlreadyTweeted(anEvent)) {
                    tweet = true;
                }

                // Flag to reset variables
                done = true;
            }

            // Grab the title of the event
            if (!text.equals("Golden Gate Valley")
                    && !anEvent.contains(text)
                    && !text.startsWith("Results")
                    && !text.startsWith(" Today, ")) {
                anEvent += text + "\n";
            }

            // Grab the URL of the event
            if (attr.startsWith(" href")) {
                eventURL = "\n" + SFPL_BASE_URL + attr.replaceAll("\\D+","");
            }

            // Check if event is complete; if so, tweet if flagged and reset variables
            if (done) {
                anEvent += eventURL;

                if (tweet) {
                    System.out.println(anEvent);

                    try {
                        tweet(anEvent);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }

                // Reset variables
                anEvent = "";
                eventURL = "";
                tweet = false;
                done = false;
            }
        }
    }

    public boolean isAlreadyTweeted(String searchTerm) {
        try {

            // Grab own timeline
            Paging paging = new Paging(1, 100);
            List<Status> statuses = TWITTER.getUserTimeline("goldengate_sfpl", paging);

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

    public boolean isValidDate(String input, int days) {

        // Create new DateFormat and Date objects
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        Date targetDate;

        // Initialize calendar objects for current time
        Calendar eventTime = Calendar.getInstance();
        Calendar now = Calendar.getInstance();

        try {

            // Convert input String to DateFormat
            targetDate = df.parse(input);

            // Set event Calendar object
            eventTime.setTime(targetDate);

            // Subtract set days from calendar
            eventTime.add(Calendar.DATE, -days);

            if (now.get(Calendar.YEAR) == eventTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == eventTime.get(Calendar.DAY_OF_YEAR)) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void findAndFavoriteQuery(String searchTerm) {

        try {
            // Create query
            Query query = new Query(searchTerm);

            // Execute query
            QueryResult result = TWITTER.search(query);

            // Favorite results
            for (Status status : result.getTweets()) {
                if (!status.isFavorited()) {
                    TWITTER.createFavorite(status.getId());
                }
            }
        } catch (TwitterException te) {
            te.printStackTrace();
        }
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

        System.out.println("Tweeted a status.");
    }

    public static void main (String[] args) {
        GGVBot bot = new GGVBot();

        // Scrape calendar and tweet results
        bot.scrape(URL_1, BUFFER_DAYS);
        bot.scrape(URL_2, BUFFER_DAYS);

        // Check for day of week; if Monday, tweet with a 1-day buffer in addition to the above buffer
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.DAY_OF_WEEK) == 2) {
            bot.scrape(URL_1, 1);
            bot.scrape(URL_2, 1);
        }

        // Search for mentions and favorite them
        bot.findAndFavoriteQuery("\"golden gate valley branch\"");
        bot.findAndFavoriteMentions();
    }
}
