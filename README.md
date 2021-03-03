# GREG Bot
This is a Discord bot for the G&K Arma III group. It provides utilities to create
dedicated text channels for individual missions, and send alerts when a mission is
about to begin. It's written (really poorly) using Spring Boot, and JDA.

## Setup
The bot requires 3 environment variables to be set in order to run:
```
GREG_TOKEN= Discord bot token
GREG_DB_URI= Mongo Atlas URI
GREG_DB_NAME= Database name to use (greg-bot or greg-bot-dev)
```