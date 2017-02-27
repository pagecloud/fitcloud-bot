# Health Fit Cloud Bot #

This is the repository for a Slack bot powering and organizing the :tednology: *Stretch-a-Day™ Program for Optimal :pc: Health, Well-being and Performance*

It is written in Kotlin on top of Spring 5 / Reactor / Netty.

### How do I get set up? ###

* Locally you will need to set 3 credentials in your `application.yml` file: `webhook-url`, `bot-token`, `slash-command-token` from your Slack account
* Additionally, to tunnel traffic into your locally running instance, you can use `ngrok http localhost:8080`
* To run the project: `./gradlew bootRun` (runs on port 8080)
* To build the project: `./gradlew build`
* To deploy: `git push heroku master`

### Contribution guidelines ###

* Writing tests (we wrote those, right!?!?)

### Who do I talk to? ###

* Ted Smith - ted@pagecloud.com
