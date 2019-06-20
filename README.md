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
  - set character-set/collation to specific utf8 (case/accent-insensitive) (for example --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs, in Ubuntu 18: edit /etc/mysql/mysql.conf.d/mysqld.cnf)
  - database schema is automatically configured by hibernate
  - create a database 
- src/main/resources/hibernate.cfg.xml: Configuration of the database
- Configuration of specific settings via harvester.properties:
  - the properties-file "harvester.properties" is expected in the users home directory `~/.oai-dashboard`
  - current properties are:
    - harvester.metha.path
    - harvester.export.dir
    - harvester.git.persistence.dir
    - ...
  - example properties-file can be found in `src/main/resources/harvester.properties_example`
  - when no properties-file is located in the default configuration directory, the configuration will automatically place all relevant directories and folders into the default configuration directory
- A customized hibernate configuration file `hibernate.cfg.xml` can also be placed in `~/.oai-dashboard` which will then be loaded instead of the one in the classpath (`resources/hibernate.cfg.xml`), this applies only to oai-dashboard_harvester
- When not using a hibernate configuration file in the config directory (`~/.oai-dashboard`), changes to the hibernate configuration (classpath) always require a rebuild (`mvn clean install`) and possible re-deployment of the standalone JAR-file

## Setting-Up (harvester)
The standalone JAR-file of the harvester (by default named: oai-dashboard-harvester-jar-with-dependencies.jar) can be used with a standard `java -jar` command. The harvester can be fully controlled by its command line interface, see `java -jar [...].jar -h` for all provided commands.

- before the first harvest, the Database needs to be initialized with the `-I` option (`java -jar [...].jar -I`)
- after initializing, the Database still has no configured repositories as targets for harvesting by default, they can be added into the database using JSON-files
  - to add new repositories into the database, use the `-ladd <filepath>` option (newly added repositories are ACTIVE by default)
  - a default set of five repositories is provided in the `src/main/resources/default_repositories`-folder

## Using/Running (harvester)
- use option `-harvest` to start harvesting all configured repositories with state ACTIVE
- use option `-harvest_target_repositories <ID_1> <ID_2 <...>` to start harvesting specific configured repositories that are ACTIVE
- use option `-rlist` to list all configured repositories including their IDs and state
- use option `-ractivate <repository_ID>` or `-rdisable <repository_ID>` to activate/disable repositories
- use option `-rupdate <repository_ID> <filepath>` to update/edit a target repository from a JSON-file
- when you do not want to use the default configuration directory (`~/.oai-dashboard`) you can use the option `-c <configuration directory>` to provide a different one

## Configuration (REST-API)
- resources/hibernate.cfg.xml: Configuration of the database (currently the hibernate.cfg.xml exists in both projects but should be identical, except when the access to the database is different from the server running tomcat, i.e. docker-compose)
- Changes to the hibernate configuration always require a rebuild (`mvn clean install`) and possible re-deployment of the standalone WAR-file

## Running (REST-API)
- place the WAR-file as output of the build process in the appropriate tomcat folder
- See the automatically generated documentation of the API: `{IP:PORT of Tomcat}/oai-dashboard-rest`
- test the general functionality of the REST-API:
  - `{IP:PORT of Tomcat}/oai-dashboard-rest/rest/api/...`

## Using the docker-compose for fast install:
If you have installed docker and docker-compose, you can use the command `docker-compose up` when in the directory `oai-dashboard_rest/docker` which will automatically create two containers with appropriate settings, one for MySQL and one for Tomcat. The maven build will automatically copy the resulting WAR-file of oai-dashboard_rest into the correct Tomcat folder.

With docker compose the `{IP:PORT of Tomcat}` equals `127.0.0.1` or `localhost` (linux/ubuntu) to access the REST-API (for example: `127.0.0.1/oai-dashboard-rest/rest/api/ListRepos`)

