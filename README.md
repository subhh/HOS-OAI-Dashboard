# HOS-OAI-Dashboard
Dashboard presentation of OAI repository key data (in early development)

## Installation
Dependencies: 
- Linux OS (e.g. Ubuntu 18)
- [Metha](https://github.com/miku/metha) 0.1.42 or later
- [MySQL](https://www.mysql.com/), 8.0 or later
- [Apache Tomcat](https://tomcat.apache.org/) 8.5 or later
- [Java OpenJDK](http://openjdk.java.net/) 1.8 or later
- [Maven](https://maven.apache.org/) 3.6.0 or later
- Git (bash/command-line tool) 2.7 or later
- Optional (only if you want to use docker for MySQL and Tomcat):
  - [Docker](https://www.docker.com/), latest version,
  - [Docker Compose](https://docs.docker.com/compose/), latest version

Install:
- clone from GitHub
- `mvn clean install`

oai-dashboard_harvester: creates a standalone JAR-file

oai-dashboard_rest: creates a standalone WAR-file

### Installation notes (Ubuntu 18)
#### Metha
You can download the deb-Package from the sourceforge repository  and install it with
`sudo dpkg -i <filename>`
This way is easier than the source installation.

#### MySQL
You'll find the MySQL 8 apt repository here: https://dev.mysql.com/downloads/repo/apt/ 

`sudo dpkg -i <filename>`

install MySQL 8.x via apt.

EXCEPT FOR LINK TO APT REPOSITORY you may use this installation guide:
https://www.tecmint.com/install-mysql-8-in-ubuntu/
  
#### Tomcat, Java, Git, Maven
These packages can be installed via apt.

## Configuration (harvester)
- Configure the MySQL database:
  - set character-set/collation to specific utf8 (case/accent-insensitive) (for example --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs)
  - database schema is automatically configured by hibernate
  - create a database 
- resources/hibernate.cfg.xml: Configuration of the database
- Configuration of specific settings via harvester.properties:
  - the properties-file "harvester.properties" is expected in the users home directory `~/.oai-dashboard`
  - current properties are:
    - harvester.metha.path
    - harvester.export.dir
    - harvester.git.persistence.dir
    - ...
  - example properties-file can be found in `src/main/resources/harvester.properties_example`
- A customized hibernate configuration file `hibernate.cfg.xml` can also be placed in `~/.oai-dashboard` which will then be loaded insteadt of the on in the classpath (`resources/hibernate.cfg.xml`), this applies only to oai-dashboard_harvester
- When not using a hibernate configuration file in the config directory (`~/.oai-dashboard`), changes to the hibernate configuration (classpath) always require a rebuild (`mvn clean install`) and possible re-deployment of the standalone JAR-file

## Configuration (REST-API)
- resources/hibernate.cfg.xml: Configuration of the database (currently the hibernate.cfg.xml exists in both projects but should be identical, except when the access to the database is different from the server running tomcat, i.e. docker-compose)
- Changes to the hibernate configuration always require a rebuild (`mvn clean install`) and possible re-deployment of the standalone WAR-file

## Running (harvester)
Parameters:
- `-RESET true/false` -> resets the database, for development only
- `-REHARVEST true/false`-> restarts harvesting. Harvesting is done automatically when no data is found.
harvesting sould be run every day

## Running (REST-API)
- place the WAR-file as output of the build process in the appropriate tomcat folder
- See the automatically generated documentation of the API: `{IP:PORT of Tomcat}/oai-dashboard-rest`
- test the general functionality of the REST-API:
  - `{IP:PORT of Tomcat}/oai-dashboard-rest/rest/api/...`

## Using the docker-compose for fast install:
If you have installed docker and docker-compose, you can use the command `docker-compose up` when in the directory `oai-dashboard_rest/docker` which will automatically create two containers with appropriate settings, one for MySQL and one for Tomcat. The maven build will automatically copy the resulting WAR-file of oai-dashboard_rest into the correct Tomcat folder.

With docker compose the `{IP:PORT of Tomcat}` equals `127.0.0.1` or `localhost` (linux/ubuntu) to access the REST-API (for example: `127.0.0.1/oai-dashboard-rest/rest/api/ListRepos`)

