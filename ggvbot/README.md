#GGVBot
Twitter bot for Golden Gate Valley Branch Library

URL: https://twitter.com/goldengate_sfpl

Scrapes events from the Golden Gate Valley Branch calendar located at sfpl.org and tweets upcoming events. Also queries for mentions and favorites them. Intended to be run automatically once daily when a circulation computer boots up. Twitter prevents duplicate tweets should the bot run multiple times in a day.

Uses jsoup 1.9.2 and twitter4j 4.0.4

Todos:

-Post day of week for events
-Deal with events landing on January 2nd of each year
-Add check for missed days
