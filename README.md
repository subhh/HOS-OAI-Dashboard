# HOS-OAI-Dashboard
Dashboard presentation of OAI repository key data (in early development)

## Installation
Dependencies: 
- Linux OS (e.g. Ubuntu 18)
- [Metha](https://github.com/miku/metha) 0.1.42 or later
- [Docker](https://www.docker.com/), latest version, only if you want to use docker for services like the database
- [MySQL](https://www.mysql.com/), 8.0 or later
- [Apache Tomcat](https://tomcat.apache.org/) 8.5 or later
- [Java OpenJDK](http://openjdk.java.net/) 1.8 or later
- [Maven](https://maven.apache.org/) 4.15 or later
- Git client

Install:
- clone from GitHub
- `maven clean install`

oai-dashboard_harvester: creates a standalone JAR-file
oai-dashboard_rest: creates a standalone WAR-file

## Configuration (harvester)
- Configure the MySQL database:
 - set character-set/collation to specific utf8 (case/accent-insensitive) (for example --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs)
 - database schema is automatically configured by hibernate
 - create a database 
- resources/hibernate.cfg.xml: Configuration of the database

## Configuration (REST-API)
- resources/hibernate.cfg.xml: Configuration of the database (currently the hibernate.cfg.xml exists in both projects but should be identical)

## Running (harvester)
Parameters:
- `-RESET true/false` -> resets the database, for development only
- `-REHARVEST true/false`-> restarts harvesting. Harvesting is done automatically when no data is found.
harvesting sould be run every day

## Running (REST-API)
- place the WAR-file as output of the build process in the appropriate tomcat folder


