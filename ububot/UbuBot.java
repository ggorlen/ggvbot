package ububot;

/* This program scrapes ubuweb and tweets a random page */

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.jsoup.*;
import org.jsoup.helper.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import twitter4j.*;

public class UbuBot {

    private static final int TIMEOUT_MS = 10 * 1000;
    private static final String MY_USERNAME = "amplifiedgravel";
    private static final String[] URLs = {
            "http://www.ubu.com/sound/",
            "http://www.ubu.com/film/",
            "http://www.ubu.com/historical/",
            "http://www.ubu.com/vp/",
            "http://www.ubu.com/contemp/",
            "http://www.ubu.com/dance/",  // check for external links e.g. those not starting with ./ or ubuweb
            "http://www.ubu.com/bidoun/",

            "http://www.ubu.com/outsiders/",
            "http://www.ubu.com/ubu/",
            "http://www.ubu.com/concept/",
            "http://www.ubu.com/papers/",  // check these ones for special cases
    };

    // Access the Twitter API using the twitter4j.properties file
    private static final Twitter TWITTER = TwitterFactory.getSingleton();


    public UbuBot() { }

    public String[] scrape(String url) {

        // Create a document object and scrape our URL
        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(TIMEOUT_MS).get();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Select all a with href tags
        Elements links = doc.select("a[href]");

        // Array to hold valid links
        String s[] = new String[links.size()];

        // Append valid links to an array
        int i = 0;
        for (Element e : links){

            // Create a candidate String using the a text and absolute href attribute
            String candidate = e.text().toString() + "\n" + e.attr("abs:href").toString();

            // Check valididty of each candidate
            if (!candidate.startsWith("UbuWeb")
                    && !candidate.contains("#top")
                    //&& !candidate.startsWith("www")
                    //&& !candidate.startsWith("http")
                    && candidate.contains("http://www.ubu.com/sound/")) {

                // Append a valid candidate to return array
                s[i++] = candidate;
            }
        }

        // Return valid candidates
        return s;
    }

    public String randomChoice(ArrayList<String> list) {
        Random random = new Random();

        String choice = null;

        // Try random indexes until we have a non-null
        if (list.size() > 0) {
            while (true) {
                int randIndex = random.nextInt(list.size());
                choice = list.get(randIndex);

                if (choice != null) {
                    return list.get(randIndex);
                }
            }
        }
        return null;
    }

    public String getToday() {

        // Get a calendar instance initialized to the current time on the computer's clock
        Calendar now = Calendar.getInstance();

        // Return today's date
        return new SimpleDateFormat("MMM dd").format(now.getTime());
    }

    public boolean alreadyTweetedToday() {
        String today = getToday();

        try {

            // Grab own timeline
            Paging paging = new Paging(1, 100);
            List<Status> statuses = TWITTER.getUserTimeline(MY_USERNAME, paging);

            for (Status status : statuses) {
                //System.out.println("@" + status.getUser().getScreenName() + " - " + status.getCreatedAt());
                //System.out.println(today);
                if (status.getCreatedAt().toString().contains(today)) {

                    // We've already tweeted today
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We haven't tweeted yet today
        return false;
    }

    public boolean isAlreadyTweeted(String tweet) {
        try {

            // Grab own timeline
            Paging paging = new Paging(1, 100);
            List<Status> statuses = TWITTER.getUserTimeline(MY_USERNAME, paging);

            for (Status status : statuses) {
                //System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
                if (status.getText().toString().contains(tweet)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // This status has already been tweeted in the past $paging entries
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
        UbuBot bot = new UbuBot();

        if (!bot.alreadyTweetedToday()) {
            // Make an array to store scrape results
            ArrayList<String> candidates = new ArrayList<>();

            // Scrape calendar for each URL and append to candidates array
            for (String URL : URLs){
                candidates.addAll(Arrays.asList(bot.scrape(URL)));
            }

            // Tweet a random choice from all candidates
            try {
                bot.tweet(bot.randomChoice(candidates));
            } catch(TwitterException te) {
                te.printStackTrace();
            }

            // Find and favorite mentions
            bot.findAndFavoriteMentions();
        }
        else {
            System.out.println("Error: This bot already tweeted today.");
        }
    }
}
