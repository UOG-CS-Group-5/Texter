# Texter
SMS / Whatsapp / Email message sender

This program attempts to send SMS/Whatsapp/Email messages to a customizable list of recipients (phone numbers or email addresses). The program tries one strategy after another until one works. It is possible that no methods work in which case no messages will be sent. As some methods don't give trustable return statuses, it is also possible that multiple messages are sent to the same number.

## Config
Make sure to set up the api and smtp credentials by creating a `.env` file following the format shown in `sample.env`.

Note if you're using gmail smtp server, make sure to use an [App Password](https://support.google.com/accounts/answer/185833?visit_id=638937894688490087-1800703100&p=InvalidSecondFactor&rd=1).

## Build
This project is built using Maven. Install Maven then run `mvn clean install` to build the project including all its dependencies.

## Run
Run the project with `mvn javafx:run`
