# Health Fit Cloud Bot #

This is the repository for a Slack bot powering and organizing the :tednology: *Stretch-a-Day™ Program for Optimal :pc: Health, Well-being and Performance*

It is written in [Kotlin](https://kotlin-lang.org) on top of [Spring 5](http://docs.spring.io/spring/docs/5.0.0.BUILD-SNAPSHOT/spring-framework-reference/htmlsingle/) / [Spring Boot 2](http://docs.spring.io/spring-boot/docs/2.0.0.BUILD-SNAPSHOT/reference/htmlsingle/) / [Reactor](https://projectreactor.io/) / [Netty](http://netty.io/).

### How do I get set up? ###
For best results, use [IntelliJ IDEA](https://www.jetbrains.com/idea/download/); simply open the project directory and it should auto-detect everything and set up correctly (just open `NotificationApplication.kt` and right-click + Run/Debug).

* Locally you will need to set 3 secrets from your Slack in your `application.yml` file: 
 1. `webhook-url` - Your Incoming Webhook URL
 2. `bot-token1` - Your Bot token
 3. `slash-command-token` - Your Slash Command token
* Additionally, to tunnel traffic into your locally running instance, you can use `ngrok http localhost:8080`
* To run the project: `./gradlew bootRun` (runs on port 8080)
* To build the project: `./gradlew build`
* To deploy: `git push heroku master`

### Contribution guidelines ###

* Writing tests (we wrote those, right!?!?)

### Who do I talk to? ###

* Ted Smith - ted@pagecloud.com
