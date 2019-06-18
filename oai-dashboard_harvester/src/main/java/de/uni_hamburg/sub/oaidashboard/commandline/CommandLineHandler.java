package de.uni_hamburg.sub.oaidashboard.commandline;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CommandLineHandler {

    // option names
    private static final String OPTION_SHORT_RESET = "R";
    private static final String OPTION_LONG_RESET = "reset-database";
    private static final String OPTION_SHORT_INIT = "I";
    private static final String OPTION_LONG_INIT = "initialize-database";
    private static final String OPTION_SHORT_HELP = "h";
    private static final String OPTION_LONG_HELP = "help";
    private static final String OPTION_SHORT_CONF_DIR = "c";
    private static final String OPTION_LONG_CONF_DIR = "config-directory";
    private static final String OPTION_SHORT_REHARVEST = "REHARVEST";
    private static final String OPTION_LONG_REHARVEST = "";
    private static final String OPTION_START_HARVEST = "harvest";
    private static final String OPTION_SHORT_ONLY_UPDATE_LICENCES = "oul";
    private static final String OPTION_LONG_ONLY_UPDATE_LICENCES = "only-update-licences";
    private static final String OPTION_SHORT_LIST_REPOS = "lr";
    private static final String OPTION_LONG_LIST_REPOS = "list-repositories";
    private static final String OPTION_SHORT_ADD_REPO = "ar";
    private static final String OPTION_LONG_ADD_REPO = "add-repository";

    // flags/flag-vars
    public boolean FLAG_RESET_DB = false;
    public boolean FLAG_INIT_DB = false;
    public boolean FLAG_CONF_DIR = false;
    public String SET_CONF_DIR = "";
    public boolean FLAG_REHARVEST = false;
    public boolean FLAG_START_HARVEST = false;
    public boolean FLAG_ONLY_UPDATE_LICENCES = false;
    public boolean FLAG_LIST_REPOSITORIES = false;
    public boolean FLAG_ADD_REPOSITORY = false;
    public String SET_REPO_JSON_FILE = "";

    private static final String RESET_DATABASE_CONFIRMATION = "RESET-THE-DATABASE";

    private static final Logger LOGGER = LogManager.getLogger(CommandLineHandler.class.getName());

    /**
     * Parses the command line arguments that are given to the harvester at start time
     * @param args, all command line arguments
     * @return true if all necessary command line arguments where provided and correctly formated;
     *         false otherwise
     */
    public boolean parseCommandLineArguments(String[] args) {
        boolean success = true;
        Options options = setUpCommandLineOptions();

        // check conditions, if there are no args, can it run?
        if (args.length == 0) {
            showCommandLineHelp(options);
            success = false;
        }

        if(args.length > 0) {
            LOGGER.info("Parsing command line arguments");

            CommandLine commandLine;

            CommandLineParser parser = new DefaultParser();
            try {
                commandLine = parser.parse(options, args);
                if(commandLine.hasOption(OPTION_SHORT_HELP)){
                    success = false;
                    showCommandLineHelp(options);
                }
                if (commandLine.hasOption(OPTION_SHORT_RESET)) {
                    String reset_option_argument = commandLine.getOptionValue(OPTION_SHORT_RESET);

                    if(reset_option_argument.equals(RESET_DATABASE_CONFIRMATION)) {
                        FLAG_RESET_DB = true;
                    } else {
                        LOGGER.info("RESET DATABASE not confirmed correctly, doing nothing");
                        success = false;
                    }
                }
                if (commandLine.hasOption(OPTION_SHORT_REHARVEST)) {
                    boolean reharvest_option_argument = Boolean.parseBoolean(commandLine.getOptionValue(OPTION_SHORT_REHARVEST));
                    FLAG_REHARVEST = reharvest_option_argument;
                }
                if (commandLine.hasOption(OPTION_SHORT_INIT)) {
                    FLAG_INIT_DB = true;
                }
                if (commandLine.hasOption(OPTION_SHORT_CONF_DIR)) {
                    String config_directory_argument = commandLine.getOptionValue(OPTION_SHORT_CONF_DIR);
                    FLAG_CONF_DIR = true;
                    SET_CONF_DIR = config_directory_argument;
                }
                if(commandLine.hasOption(OPTION_START_HARVEST)) {
                    FLAG_START_HARVEST = true;
                }
                if(commandLine.hasOption(OPTION_SHORT_ONLY_UPDATE_LICENCES)) {
                    FLAG_ONLY_UPDATE_LICENCES = true;
                }
                if(commandLine.hasOption(OPTION_SHORT_LIST_REPOS)) {
                    FLAG_LIST_REPOSITORIES = true;
                }
                if(commandLine.hasOption(OPTION_SHORT_ADD_REPO)) {
                    FLAG_ADD_REPOSITORY = true;
                    String repo_filename_arg = commandLine.getOptionValue(OPTION_SHORT_ADD_REPO);
                    SET_REPO_JSON_FILE = repo_filename_arg;
                }
            } catch (MissingOptionException e) {
                LOGGER.info("Missing option(s): {}", String.join(",", e.getMissingOptions()));
                showCommandLineHelp(options);
                success = false;
            } catch (MissingArgumentException e) {
                LOGGER.info(e.getMessage());
                showCommandLineHelp(options);
                success = false;
            } catch (Exception e) {
                LOGGER.info("Error parsing command line options", e);
                showCommandLineHelp(options);
                success = false;
            }
        }
        return success;
    }

    private static void showCommandLineHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(" ", options);
    }

    private static Options setUpCommandLineOptions() {
        Options options = new Options();

        Option show_help_option = Option.builder(OPTION_SHORT_HELP)
                .required(false)
                .longOpt(OPTION_LONG_HELP)
                .desc("show how to use the harvester from the command line")
                .build();
        Option config_directory_option = Option.builder(OPTION_SHORT_CONF_DIR)
                .required(false)
                .longOpt(OPTION_LONG_CONF_DIR)
                .hasArg()
                .argName("configuration directory")
                .desc("set the configuration directory (default: ~/.oaidashboard)")
                .build();
        Option start_harvest_option = Option.builder(OPTION_START_HARVEST)
                .required(false)
                .desc("start harvesting with current configuration")
                .build();
        Option initialize_database_option = Option.builder(OPTION_SHORT_INIT)
                .required(false)
                .longOpt(OPTION_LONG_INIT)
                .desc("Initialize the database (should only done once when installing the harvester, data preservation " +
                        "not guaranteed)")
                .build();
        Option only_update_licences_option = Option.builder(OPTION_SHORT_ONLY_UPDATE_LICENCES)
                .required(false)
                .longOpt(OPTION_LONG_ONLY_UPDATE_LICENCES)
                .desc("only update licences from licences.json, NO OTHER OPERATIONS WILL TAKE PLACE")
                .build();
        Option list_repositories_option = Option.builder(OPTION_SHORT_LIST_REPOS)
                .required(false)
                .longOpt(OPTION_LONG_LIST_REPOS)
                .desc("List all repositories that are configured as harvesting targets")
                .build();
        Option add_repository_option = Option.builder(OPTION_SHORT_ADD_REPO)
                .longOpt(OPTION_LONG_ADD_REPO)
                .desc("Add a new repository from a json file as harvesting target")
                .hasArg()
                .argName("filepath").optionalArg(false)
                .build();
        // TODO: remove this argument because it is dangerous in a production environment
        Option reset_database_option = Option.builder(OPTION_SHORT_RESET)
                .required(false)
                .longOpt(OPTION_LONG_RESET)
                .hasArg()
                .argName("CONFIRMATION")
                .desc("Reset the database (losing all harvested data), confirm with '" + RESET_DATABASE_CONFIRMATION + "'")
                .build();
        // TODO: remove this argument because it is dangerous in a production environment
        Option reharvest_option = Option.builder(OPTION_SHORT_REHARVEST)
                .required(false)
                .hasArg()
                .desc("NOT TO BE USED IN PRODUCTION, TESTING/DEVELOPING ONLY")
                .build();

        options.addOption(show_help_option);
        options.addOption(config_directory_option);
        options.addOption(start_harvest_option);
        options.addOption(initialize_database_option);
        options.addOption(only_update_licences_option);
        options.addOption(list_repositories_option);
        options.addOption(add_repository_option);
        options.addOption(reset_database_option);
        options.addOption(reharvest_option);

        return options;
    }
}
