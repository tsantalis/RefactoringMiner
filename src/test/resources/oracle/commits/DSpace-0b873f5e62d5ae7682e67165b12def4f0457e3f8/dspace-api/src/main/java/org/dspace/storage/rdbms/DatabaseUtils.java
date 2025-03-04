/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.FlywayCallback;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.DbSupportFactory;
import org.flywaydb.core.internal.dbsupport.SqlScript;
import org.flywaydb.core.internal.info.MigrationInfoDumper;

/**
 * Utility class used to manage the Database. This class is used by the
 * DatabaseManager to initialize/upgrade/migrate the Database. It can also
 * be called via the commandline as necessary to get information about
 * the database.
 * <p>
 * Currently, we use Flyway DB (http://flywaydb.org/) for database management.
 *
 * @see org.dspace.storage.rdbms.DatabaseUtils
 * @author Tim Donohue
 */
public class DatabaseUtils
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DatabaseUtils.class);

    // Our Flyway DB object (initialized by setupFlyway())
    private static Flyway flywaydb;

    // When this temp file exists, the "checkReindexDiscovery()" method will auto-reindex Discovery
    // Reindex flag file is at [dspace]/solr/search/conf/reindex.flag
    // See also setReindexDiscovery()/getReindexDiscover()
    private static final String reindexDiscoveryFilePath = ConfigurationManager.getProperty("dspace.dir") +
                            File.separator + "solr" +
                            File.separator + "search" +
                            File.separator + "conf" +
                            File.separator + "reindex.flag";

    // Types of databases supported by DSpace. See getDbType()
    public static final String DBMS_POSTGRES="postgres";
    public static final String DBMS_ORACLE="oracle";
    public static final String DBMS_H2="h2";

    // PostgreSQL pgcrypto extention name, and required versions of Postgres & pgcrypto
    public static final String PGCRYPTO="pgcrypto";
    public static final Double PGCRYPTO_VERSION=1.1;
    public static final Double POSTGRES_VERSION=9.4;

    /**
     * Commandline tools for managing database changes, etc.
     * @param argv
     */
    public static void main(String[] argv)
    {
        // Usage checks
        if (argv.length < 1)
        {
            System.out.println("\nDatabase action argument is missing.");
            System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair' or 'clean'");
            System.out.println("\nOr, type 'database help' for more information.\n");
            System.exit(1);
        }

        try
        {
            // Get a reference to our configured DataSource
            DataSource dataSource = getDataSource();

            // Point Flyway API to our database
            Flyway flyway = setupFlyway(dataSource);

            // "test" = Test Database Connection
            if(argv[0].equalsIgnoreCase("test"))
            {
                // Try to connect to the database
                System.out.println("\nAttempting to connect to database");
                try(Connection connection = dataSource.getConnection())
                {
                    // Just do a high level test by getting our configured DataSource and attempting to connect to it
                    DatabaseMetaData meta = connection.getMetaData();
                    System.out.println("Connected successfully!");
                    System.out.println("Database Software: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
                    System.out.println(" - URL: " + meta.getURL());
                    System.out.println(" - Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());
                    System.out.println(" - Username: " + meta.getUserName());
                    System.out.println(" - Password: [hidden]");
                    System.out.println(" - Schema: " + getSchemaName(connection));
                }
                catch (SQLException sqle)
                {
                    System.err.println("\nError running 'test': ");
                    System.err.println(" - " + sqle);
                    System.err.println("\nPlease see the DSpace documentation for assistance.\n");
                    sqle.printStackTrace();
                    System.exit(1);
                }
            }
            else if(argv[0].equalsIgnoreCase("info") || argv[0].equalsIgnoreCase("status"))
            {
                try(Connection connection = dataSource.getConnection())
                {
                    // Get basic Database info
                    DatabaseMetaData meta = connection.getMetaData();
                    String dbType = getDbType(connection);
                    System.out.println("\nDatabase Type: " + dbType);
                    System.out.println("Database URL: " + meta.getURL());
                    System.out.println("Database Schema: " + getSchemaName(connection));
                    System.out.println("Database Software: " + meta.getDatabaseProductName() + " version " + meta.getDatabaseProductVersion());
                    System.out.println("Database Driver: " + meta.getDriverName() + " version " + meta.getDriverVersion());

                    // For Postgres, report whether pgcrypto is installed
                    // (If it isn't, we'll also write out warnings...see below)
                    if(dbType.equals(DBMS_POSTGRES))
                    {
                        boolean pgcryptoUpToDate = DatabaseUtils.isPgcryptoUpToDate();
                        Double pgcryptoVersion = getPgcryptoInstalledVersion(connection);
                        System.out.println("PostgreSQL '" + PGCRYPTO + "' extension installed/up-to-date? " + pgcryptoUpToDate  + " " + ((pgcryptoVersion!=null) ? "(version=" + pgcryptoVersion + ")" : "(not installed)"));
                    }

                    // Get info table from Flyway
                    System.out.println("\n" + MigrationInfoDumper.dumpToAsciiTable(flyway.info().all()));

                    // If Flyway is NOT yet initialized, also print the determined version information
                    // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
                    // See: http://flywaydb.org/documentation/faq.html#case-sensitive
                    if(!tableExists(connection, flyway.getTable(), true))
                    {
                        System.out.println("\nNOTE: This database is NOT yet initialized for auto-migrations (via Flyway).");
                        // Determine which version of DSpace this looks like
                        String dbVersion = determineDBVersion(connection);
                        if (dbVersion!=null)
                        {
                            System.out.println("\nYour database looks to be compatible with DSpace version " + dbVersion);
                            System.out.println("All upgrades *after* version " + dbVersion + " will be run during the next migration.");
                            System.out.println("\nIf you'd like to upgrade now, simply run 'dspace database migrate'.");
                        }
                    }

                    // For PostgreSQL databases, we need to check for the 'pgcrypto' extension.
                    // If it is NOT properly installed, we'll need to warn the user, as DSpace will be unable to proceed.
                    if(dbType.equals(DBMS_POSTGRES))
                    {
                        // Get version of pgcrypto available in this postgres instance
                        Double pgcryptoAvailable = getPgcryptoAvailableVersion(connection);

                        // Generic requirements message
                        String requirementsMsg = "\n** DSpace REQUIRES PostgreSQL >= " + POSTGRES_VERSION + " AND " + PGCRYPTO + " extension >= " + PGCRYPTO_VERSION + " **\n";

                        // Check if installed in PostgreSQL & a supported version
                        if(pgcryptoAvailable!=null && pgcryptoAvailable.compareTo(PGCRYPTO_VERSION)>=0)
                        {
                            // We now know it's available in this Postgres. Let's see if it is installed in this database.
                            Double pgcryptoInstalled = getPgcryptoInstalledVersion(connection);

                            // Check if installed in database, but outdated version
                            if(pgcryptoInstalled!=null && pgcryptoInstalled.compareTo(PGCRYPTO_VERSION)<0)
                            {
                                System.out.println("\nWARNING: PostgreSQL '" + PGCRYPTO + "' extension is OUTDATED (installed version=" + pgcryptoInstalled + ", available version = " + pgcryptoAvailable + ").");
                                System.out.println(requirementsMsg);
                                System.out.println("To update it, please connect to your DSpace database as a 'superuser' and manually run the following command: ");
                                System.out.println("\n  ALTER EXTENSION " + PGCRYPTO + " UPDATE TO '" + pgcryptoAvailable + "';\n");
                            }
                            else if(pgcryptoInstalled==null) // If it's not installed in database
                            {
                                System.out.println("\nWARNING: PostgreSQL '" + PGCRYPTO + "' extension is NOT INSTALLED on this database.");
                                System.out.println(requirementsMsg);
                                System.out.println("To install it, please connect to your DSpace database as a 'superuser' and manually run the following command: ");
                                System.out.println("\n  CREATE EXTENSION " + PGCRYPTO + ";\n");
                            }
                        }
                        // Check if installed in Postgres, but an unsupported version
                        else if(pgcryptoAvailable!=null && pgcryptoAvailable.compareTo(PGCRYPTO_VERSION)<0)
                        {
                            System.out.println("\nWARNING: UNSUPPORTED version of PostgreSQL '" + PGCRYPTO + "' extension found (version=" + pgcryptoAvailable + ").");
                            System.out.println(requirementsMsg);
                            System.out.println("Make sure you are running a supported version of PostgreSQL, and then install " + PGCRYPTO + " version >= " + PGCRYPTO_VERSION);
                            System.out.println("The '" + PGCRYPTO + "' extension is often provided in the 'postgresql-contrib' package for your operating system.");
                        }
                        else if(pgcryptoAvailable==null) // If it's not installed in Postgres
                        {
                            System.out.println("\nWARNING: PostgreSQL '" + PGCRYPTO + "' extension is NOT AVAILABLE. Please install it into this PostgreSQL instance.");
                            System.out.println(requirementsMsg);
                            System.out.println("The '" + PGCRYPTO + "' extension is often provided in the 'postgresql-contrib' package for your operating system.");
                            System.out.println("Once the extension is installed globally, please connect to your DSpace database as a 'superuser' and manually run the following command: ");
                            System.out.println("\n  CREATE EXTENSION " + PGCRYPTO + ";\n");
                        }
                    }
                }
                catch (SQLException e)
                {
                    System.err.println("Info exception:");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            else if(argv[0].equalsIgnoreCase("migrate"))
            {
                try (Connection connection = dataSource.getConnection())
                {
                    System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());

                    // "migrate" allows for an OPTIONAL second argument:
                    //    - "ignored" = Also run any previously "ignored" migrations during the migration
                    //    - [version] = ONLY run migrations up to a specific DSpace version (ONLY FOR TESTING)
                    if(argv.length==2)
                    {
                        if(argv[1].equalsIgnoreCase("ignored"))
                        {
                            System.out.println("Migrating database to latest version AND running previously \"Ignored\" migrations... (Check logs for details)");
                            // Update the database to latest version, but set "outOfOrder=true"
                            // This will ensure any old migrations in the "ignored" state are now run
                            updateDatabase(dataSource, connection, null, true);
                        }
                        else
                        {
                            // Otherwise, we assume "argv[1]" is a valid migration version number
                            // This is only for testing! Never specify for Production!
                            System.out.println("Migrating database ONLY to version " + argv[1] + " ... (Check logs for details)");
                            System.out.println("\nWARNING: It is highly likely you will see errors in your logs when the Metadata");
                            System.out.println("or Bitstream Format Registry auto-update. This is because you are attempting to");
                            System.out.println("use an OLD version " + argv[1] + " Database with a newer DSpace API. NEVER do this in a");
                            System.out.println("PRODUCTION scenario. The resulting old DB is only useful for migration testing.\n");
                            // Update the database, to the version specified.
                            updateDatabase(dataSource, connection, argv[1], true);
                        }
                    }
                    else
                    {
                        System.out.println("Migrating database to latest version... (Check dspace logs for details)");
                        updateDatabase(dataSource, connection);
                    }
                    System.out.println("Done.");
                }
                catch(SQLException e)
                {
                    System.err.println("Migration exception:");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            // "repair" = Run Flyway repair script
            else if(argv[0].equalsIgnoreCase("repair"))
            {
                try (Connection connection = dataSource.getConnection();)
                {
                    System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());
                    System.out.println("Attempting to repair any previously failed migrations via FlywayDB... (Check dspace logs for details)");
                    flyway.repair();
                    System.out.println("Done.");
                }
                catch(SQLException|FlywayException e)
                {
                    System.err.println("Repair exception:");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            // "clean" = Run Flyway clean script
            else if(argv[0].equalsIgnoreCase("clean"))
            {
                try (Connection connection = dataSource.getConnection())
                {
                    String dbType = getDbType(connection);

                    // Not all Postgres user accounts will be able to run a 'clean',
                    // as only 'superuser' accounts can remove the 'pgcrypto' extension.
                    if(dbType.equals(DBMS_POSTGRES))
                    {
                        // Check if database user has permissions suitable to run a clean
                        if(!checkCleanPermissions(connection))
                        {
                            String username = connection.getMetaData().getUserName();
                            // Exit immediately, providing a descriptive error message
                            System.out.println("\nERROR: The database user '" + username + "' does not have sufficient privileges to run a 'database clean' (via Flyway).");
                            System.out.println("\nIn order to run a 'clean', the database user MUST have 'superuser' privileges");
                            System.out.println("OR the '" + PGCRYPTO + "' extension must be installed in a separate schema (see documentation).");
                            System.out.println("\nOptionally, you could also manually remove the '" + PGCRYPTO + "' extension first (DROP EXTENSION '" + PGCRYPTO + "' CASCADE), then rerun the 'clean'");
                            System.exit(1);
                        }
                    }

                    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

                    System.out.println("\nDatabase URL: " + connection.getMetaData().getURL());
                    System.out.println("\nWARNING: ALL DATA AND TABLES IN YOUR DATABASE WILL BE PERMANENTLY DELETED.\n");
                    System.out.println("There is NO turning back from this action. Backup your DB before continuing.");
                    if(dbType.equals(DBMS_ORACLE))
                    {
                        System.out.println("\nORACLE WARNING: your RECYCLEBIN will also be PURGED.\n");
                    }
                    else if(dbType.equals(DBMS_POSTGRES))
                    {
                        System.out.println("\nPOSTGRES WARNING: the '" + PGCRYPTO + "' extension will be dropped if it is in the same schema as the DSpace database.\n");
                    }
                    System.out.print("Do you want to PERMANENTLY DELETE everything from your database? [y/n]: ");
                    String choiceString = input.readLine();
                    input.close();

                    if (choiceString.equalsIgnoreCase("y"))
                    {
                        System.out.println("Scrubbing database clean... (Check dspace logs for details)");
                        cleanDatabase(flyway, dataSource);
                        System.out.println("Done.");
                    }
                }
                catch(SQLException e)
                {
                    System.err.println("Clean exception:");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
            else
            {
                System.out.println("\nUsage: database [action]");
                System.out.println("Valid actions: 'test', 'info', 'migrate', 'repair' or 'clean'");
                System.out.println(" - test          = Performs a test connection to database to validate connection settings");
                System.out.println(" - info / status = Describe basic info/status about database, including validating the compatibility of this database");
                System.out.println(" - migrate       = Migrate the database to the latest version");
                System.out.println(" - repair        = Attempt to repair any previously failed database migrations (via Flyway repair)");
                System.out.println(" - clean         = DESTROY all data and tables in database (WARNING there is no going back!)");
                System.out.println("");
            }

        }
        catch (Exception e)
        {
            System.err.println("Caught exception:");
            e.printStackTrace();
            System.exit(1);
        }
    }



    /**
     * Setup/Initialize the Flyway API to run against our DSpace database
     * and point at our migration scripts.
     *
     * @param datasource
     *      DataSource object initialized by DatabaseManager
     * @return initialized Flyway object
     */
    private static Flyway setupFlyway(DataSource datasource)
    {
        if (flywaydb==null)
        {
            try(Connection connection = datasource.getConnection())
            {
                // Initialize Flyway DB API (http://flywaydb.org/), used to perform DB migrations
                flywaydb = new Flyway();
                flywaydb.setDataSource(datasource);
                flywaydb.setEncoding("UTF-8");

                // Migration scripts are based on DBMS Keyword (see full path below)
                String dbType = getDbType(connection);
                connection.close();

                // Determine location(s) where Flyway will load all DB migrations
                ArrayList<String> scriptLocations = new ArrayList<String>();

                // First, add location for custom SQL migrations, if any (based on DB Type)
                // e.g. [dspace.dir]/etc/[dbtype]/
                // (We skip this for H2 as it's only used for unit testing)
                if(!dbType.equals(DBMS_H2))
                {
                    scriptLocations.add("filesystem:" + ConfigurationManager.getProperty("dspace.dir") +
                                        "/etc/" + dbType);
                }

                // Also add the Java package where Flyway will load SQL migrations from (based on DB Type)
                scriptLocations.add("classpath:org.dspace.storage.rdbms.sqlmigration." + dbType);

                // Also add the Java package where Flyway will load Java migrations from
                // NOTE: this also loads migrations from any sub-package
                scriptLocations.add("classpath:org.dspace.storage.rdbms.migration");

                // Special scenario: If XMLWorkflows are enabled, we need to run its migration(s)
                // as it REQUIRES database schema changes. XMLWorkflow uses Java migrations
                // which first check whether the XMLWorkflow tables already exist
                if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow"))
                {
                    scriptLocations.add("classpath:org.dspace.storage.rdbms.xmlworkflow");
                }

                // Now tell Flyway which locations to load SQL / Java migrations from
                log.info("Loading Flyway DB migrations from: " + StringUtils.join(scriptLocations, ", "));
                flywaydb.setLocations(scriptLocations.toArray(new String[scriptLocations.size()]));

                // Set flyway callbacks (i.e. classes which are called post-DB migration and similar)
                // In this situation, we have a Registry Updater that runs PRE-migration
                // NOTE: DatabaseLegacyReindexer only indexes in Legacy Lucene & RDBMS indexes. It can be removed once those are obsolete.
                List<FlywayCallback> flywayCallbacks = new DSpace().getServiceManager().getServicesByType(FlywayCallback.class);
                flywaydb.setCallbacks(flywayCallbacks.toArray(new FlywayCallback[flywayCallbacks.size()]));
            }
            catch(SQLException e)
            {
                log.error("Unable to setup Flyway against DSpace database", e);
            }
        }

        return flywaydb;
    }

    /**
     * Ensures the current database is up-to-date with regards
     * to the latest DSpace DB schema. If the scheme is not up-to-date,
     * then any necessary database migrations are performed.
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to perform database migrations.
     * If a Flyway DB migration fails it will be rolled back to the last
     * successful migration, and any errors will be logged.
     *
     * @throws SQLException
     *      If database cannot be upgraded.
     */
    public static synchronized void updateDatabase()
            throws SQLException
    {
        // Get our configured dataSource
        DataSource dataSource = getDataSource();

        try(Connection connection = dataSource.getConnection())
        {
            // Upgrade database to the latest version of our schema
            updateDatabase(dataSource, connection);
        }
    }

    /**
     * Ensures the current database is up-to-date with regards
     * to the latest DSpace DB schema. If the scheme is not up-to-date,
     * then any necessary database migrations are performed.
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to perform database migrations.
     * If a Flyway DB migration fails it will be rolled back to the last
     * successful migration, and any errors will be logged.
     *
     * @param datasource
     *      DataSource object (retrieved from DatabaseManager())
     * @param connection
     *      Database connection
     * @throws SQLException
     *      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection)
            throws SQLException
    {
        // By default, upgrade to the *latest* version and run migrations out-of-order
        updateDatabase(datasource, connection, null, true);
    }

    /**
     * Ensures the current database is up-to-date with regards
     * to the latest DSpace DB schema. If the scheme is not up-to-date,
     * then any necessary database migrations are performed.
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to perform database migrations.
     * If a Flyway DB migration fails it will be rolled back to the last
     * successful migration, and any errors will be logged.
     *
     * @param datasource
     *      DataSource object (retrieved from DatabaseManager())
     * @param connection
     *      Database connection
     * @param targetVersion
     *      If specified, only migrate the database to a particular *version* of DSpace. This is mostly just useful for testing.
     *      If null, the database is migrated to the latest version.
     * @param outOfOrder
     *      If true, Flyway will run any lower version migrations that were previously "ignored".
     *      If false, Flyway will only run new migrations with a higher version number.
     * @throws SQLException
     *      If database cannot be upgraded.
     */
    protected static synchronized void updateDatabase(DataSource datasource, Connection connection, String targetVersion, boolean outOfOrder)
            throws SQLException
    {
        try
        {
            // Setup Flyway API against our database
            Flyway flyway = setupFlyway(datasource);

            // Set whethe Flyway will run migrations "out of order". By default, this is false,
            // and Flyway ONLY runs migrations that have a higher version number.
            flyway.setOutOfOrder(outOfOrder);

            // If a target version was specified, tell Flyway to ONLY migrate to that version
            // (i.e. all later migrations are left as "pending"). By default we always migrate to latest version.
            if(!StringUtils.isBlank(targetVersion))
            {
                flyway.setTargetAsString(targetVersion);
            }

            // Does the necessary Flyway table ("schema_version") exist in this database?
            // If not, then this is the first time Flyway has run, and we need to initialize
            // NOTE: search is case sensitive, as flyway table name is ALWAYS lowercase,
            // See: http://flywaydb.org/documentation/faq.html#case-sensitive
            if(!tableExists(connection, flyway.getTable(), true))
            {
                // Try to determine our DSpace database version, so we know what to tell Flyway to do
                String dbVersion = determineDBVersion(connection);

                // If this is a fresh install, dbVersion will be null
                if (dbVersion==null)
                {
                    // Initialize the Flyway database table with defaults (version=1)
                    flyway.baseline();
                }
                else
                {
                    // Otherwise, pass our determined DB version to Flyway to initialize database table
                    flyway.setBaselineVersionAsString(dbVersion);
                    flyway.setBaselineDescription("Initializing from DSpace " + dbVersion + " database schema");
                    flyway.baseline();
                }
            }

            // Determine pending Database migrations
            MigrationInfo[] pending = flyway.info().pending();

            // As long as there are pending migrations, log them and run migrate()
            if (pending!=null && pending.length>0)
            {
                log.info("Pending DSpace database schema migrations:");
                for (MigrationInfo info : pending)
                {
                    log.info("\t" + info.getVersion() + " " + info.getDescription() + " " + info.getType() + " " + info.getState());
                }

                // Run all pending Flyway migrations to ensure the DSpace Database is up to date
                flyway.migrate();

                // Flag that Discovery will need reindexing, since database was updated
                setReindexDiscovery(true);
            }
            else
                log.info("DSpace database schema is up to date");
        }
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway migration error occurred", fe);
        }
    }

    /**
     * Clean the existing database, permanently removing all data and tables
     * <P>
     * FlywayDB (http://flywaydb.org/) is used to clean the database
     *
     * @param flyway
     *      Initialized Flyway object
     * @param dataSource
     *      Initialized DataSource
     * @throws SQLException
     *      If database cannot be cleaned.
     */
    private static synchronized void cleanDatabase(Flyway flyway, DataSource dataSource)
            throws SQLException
    {
        try
        {
            // First, run Flyway's clean command on database.
            // For MOST database types, this takes care of everything
            flyway.clean();

            try(Connection connection = dataSource.getConnection())
            {
                // Get info about which database type we are using
                String dbType = getDbType(connection);

                // If this is Oracle, the only way to entirely clean the database
                // is to also purge the "Recyclebin". See:
                // http://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_9018.htm
                if(dbType.equals(DBMS_ORACLE))
                {
                    PreparedStatement statement = null;
                    try
                    {
                        statement = connection.prepareStatement("PURGE RECYCLEBIN");
                        statement.executeQuery();
                    }
                    finally
                    {
                        if(statement!=null && !statement.isClosed())
                            statement.close();
                    }
                }
            }
        }
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway clean error occurred", fe);
        }
    }

    /**
     * Attempt to determine the version of our DSpace database,
     * so that we are able to properly migrate it to the latest schema
     * via Flyway
     * <P>
     * This determination is performed by checking which table(s) exist in
     * your database and matching them up with known tables that existed in
     * different versions of DSpace.
     *
     * @param connection
     *          Current Database Connection
     * @throws SQLException if DB status cannot be determined
     * @return DSpace version as a String (e.g. "4.0"), or null if database is empty
     */
    private static String determineDBVersion(Connection connection)
            throws SQLException
    {
        // First, is this a "fresh_install"?  Check for an "item" table.
        if(!tableExists(connection, "Item"))
        {
            // Item table doesn't exist. This database must be a fresh install
            return null;
        }

        // We will now check prior versions in reverse chronological order, looking
        // for specific tables or columns that were newly created in each version.

        // Is this pre-DSpace 5.0 (with Metadata 4 All changes)? Look for the "resource_id" column in the "metadatavalue" table
        if(tableColumnExists(connection, "metadatavalue", "resource_id"))
        {
            return "5.0.2014.09.26"; // This version matches the version in the SQL migration for this feature
        }

        // Is this pre-DSpace 5.0 (with Helpdesk plugin)? Look for the "request_message" column in the "requestitem" table
        if(tableColumnExists(connection, "requestitem", "request_message"))
        {
            return "5.0.2014.08.08"; // This version matches the version in the SQL migration for this feature
        }

        // Is this DSpace 4.x? Look for the "Webapp" table created in that version.
        if(tableExists(connection, "Webapp"))
        {
            return "4.0";
        }

        // Is this DSpace 3.x? Look for the "versionitem" table created in that version.
        if(tableExists(connection, "versionitem"))
        {
            return "3.0";
        }

        // Is this DSpace 1.8.x? Look for the "bitstream_order" column in the "bundle2bitstream" table
        if(tableColumnExists(connection, "bundle2bitstream", "bitstream_order"))
        {
            return "1.8";
        }

        // Is this DSpace 1.7.x? Look for the "dctyperegistry_seq" to NOT exist (it was deleted in 1.7)
        // NOTE: DSPACE 1.7.x only differs from 1.6 in a deleted sequence.
        if(!sequenceExists(connection, "dctyperegistry_seq"))
        {
            return "1.7";
        }

        // Is this DSpace 1.6.x? Look for the "harvested_collection" table created in that version.
        if(tableExists(connection, "harvested_collection"))
        {
            return "1.6";
        }

        // Is this DSpace 1.5.x? Look for the "collection_item_count" table created in that version.
        if(tableExists(connection, "collection_item_count"))
        {
            return "1.5";
        }

        // Is this DSpace 1.4.x? Look for the "Group2Group" table created in that version.
        if(tableExists(connection, "Group2Group"))
        {
            return "1.4";
        }

        // Is this DSpace 1.3.x? Look for the "epersongroup2workspaceitem" table created in that version.
        if(tableExists(connection, "epersongroup2workspaceitem"))
        {
            return "1.3";
        }

        // Is this DSpace 1.2.x? Look for the "Community2Community" table created in that version.
        if(tableExists(connection, "Community2Community"))
        {
            return "1.2";
        }

        // Is this DSpace 1.1.x? Look for the "Community" table created in that version.
        if(tableExists(connection, "Community"))
        {
            return "1.1";
        }

        // IF we get here, something went wrong! This database is missing a LOT of DSpace tables
        throw new SQLException("CANNOT AUTOUPGRADE DSPACE DATABASE, AS IT DOES NOT LOOK TO BE A VALID DSPACE DATABASE.");
    }

    /**
     * Determine if a particular database table exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName)
    {
        //By default, do a case-insensitive search
        return tableExists(connection, tableName, false);
    }

    /**
     * Determine if a particular database table exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @param caseSensitive
     *          When "true", the case of the tableName will not be changed.
     *          When "false, the name may be uppercased or lowercased based on DB type.
     * @return true if table of that name exists, false otherwise
     */
    public static boolean tableExists(Connection connection, String tableName, boolean caseSensitive)
    {
        boolean exists = false;
        ResultSet results = null;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();

            // If this is not a case sensitive search
            if(!caseSensitive)
            {
                // Canonicalize everything to the proper case based on DB type
                schema = canonicalize(connection, schema);
                tableName = canonicalize(connection, tableName);
            }

            // Search for a table of the given name in our current schema
            results = meta.getTables(null, schema, tableName, null);
            if (results!=null && results.next())
            {
                exists = true;
            }
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if table " + tableName + " exists", e);
        }
        finally
        {
            try
            {
                // ensure the ResultSet gets closed
                if(results!=null && !results.isClosed())
                    results.close();
            }
            catch(SQLException e)
            {
                // ignore it
            }
        }

        return exists;
    }

    /**
     * Determine if a particular database column exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param tableName
     *          The name of the table
     * @param columnName
     *          The name of the column in the table
     * @return true if column of that name exists, false otherwise
     */
    public static boolean tableColumnExists(Connection connection, String tableName, String columnName)
    {
        boolean exists = false;
        ResultSet results = null;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Canonicalize everything to the proper case based on DB type
            schema = canonicalize(connection, schema);
            tableName = canonicalize(connection, tableName);
            columnName = canonicalize(connection, columnName);

            // Get information about our database.
            DatabaseMetaData meta = connection.getMetaData();

            // Search for a column of that name in the specified table & schema
            results = meta.getColumns(null, schema, tableName, columnName);
            if (results!=null && results.next())
            {
                exists = true;
            }
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if column " + columnName + " exists", e);
        }
        finally
        {
            try
            {
                // ensure the ResultSet gets closed
                if(results!=null && !results.isClosed())
                    results.close();
            }
            catch(SQLException e)
            {
                // ignore it
            }
        }

        return exists;
    }

    /*
     * Determine if a particular database sequence exists in our database
     *
     * @param connection
     *          Current Database Connection
     * @param sequenceName
     *          The name of the table
     * @return true if sequence of that name exists, false otherwise
     */
    public static boolean sequenceExists(Connection connection, String sequenceName)
    {
        boolean exists = false;
        PreparedStatement statement = null;
        ResultSet results = null;
        // Whether or not to filter query based on schema (this is DB Type specific)
        boolean schemaFilter = false;

        try
        {
            // Get the name of the Schema that the DSpace Database is using
            // (That way we can search the right schema)
            String schema = getSchemaName(connection);

            // Canonicalize everything to the proper case based on DB type
            schema = canonicalize(connection, schema);
            sequenceName = canonicalize(connection, sequenceName);

            // Different database types store sequence information in different tables
            String dbtype = getDbType(connection);
            String sequenceSQL = null;
            switch(dbtype)
            {
                case DBMS_POSTGRES:
                    // Default schema in PostgreSQL is "public"
                    if(schema == null)
                    {
                        schema = "public";
                    }
                    // PostgreSQL specific query for a sequence in a particular schema
                    sequenceSQL = "SELECT COUNT(1) FROM pg_class, pg_namespace " +
                                    "WHERE pg_class.relnamespace=pg_namespace.oid " +
                                    "AND pg_class.relkind='S' " +
                                    "AND pg_class.relname=? " +
                                    "AND pg_namespace.nspname=?";
                    // We need to filter by schema in PostgreSQL
                    schemaFilter = true;
                    break;
                case DBMS_ORACLE:
                    // Oracle specific query for a sequence owned by our current DSpace user
                    // NOTE: No need to filter by schema for Oracle, as Schema = User
                    sequenceSQL = "SELECT COUNT(1) FROM user_sequences WHERE sequence_name=?";
                    break;
                case DBMS_H2:
                    // In H2, sequences are listed in the "information_schema.sequences" table
                    // SEE: http://www.h2database.com/html/grammar.html#information_schema
                    sequenceSQL = "SELECT COUNT(1) " +
                                    "FROM INFORMATION_SCHEMA.SEQUENCES " +
                                    "WHERE SEQUENCE_NAME = ?";
                    break;
                default:
                    throw new SQLException("DBMS " + dbtype + " is unsupported.");
            }

            // If we have a SQL query to run for the sequence, then run it
            if (sequenceSQL!=null)
            {
                // Run the query, passing it our parameters
                statement = connection.prepareStatement(sequenceSQL);
                statement.setString(1, sequenceName);
                if(schemaFilter)
                {
                    statement.setString(2, schema);
                }
                results = statement.executeQuery();

                // If results are non-zero, then this sequence exists!
                if(results!=null && results.next() && results.getInt(1)>0)
                {
                    exists = true;
                }
            }
        }
        catch(SQLException e)
        {
            log.error("Error attempting to determine if sequence " + sequenceName + " exists", e);
        }
        finally
        {
            try
            {
                // Ensure statement gets closed
                if(statement!=null && !statement.isClosed())
                    statement.close();
                // Ensure ResultSet gets closed
                if(results!=null && !results.isClosed())
                    results.close();
            }
            catch(SQLException e)
            {
                // ignore it
            }
        }

        return exists;
    }

    /**
     * Execute a block of SQL against the current database connection.
     * <P>
     * The SQL is executed using the Flyway SQL parser.
     *
     * @param connection
     *            Current Database Connection
     * @param sqlToExecute
     *            The actual SQL to execute as a String
     * @throws SQLException
     *            If a database error occurs
     */
    public static void executeSql(Connection connection, String sqlToExecute) throws SQLException
    {
        try
        {
            // Create a Flyway DbSupport object (based on our connection)
            // This is how Flyway determines the database *type* (e.g. Postgres vs Oracle)
            DbSupport dbSupport = DbSupportFactory.createDbSupport(connection, false);

            // Load our SQL string & execute via Flyway's SQL parser
            SqlScript script = new SqlScript(sqlToExecute, dbSupport);
            script.execute(dbSupport.getJdbcTemplate());
        }
        catch(FlywayException fe)
        {
            // If any FlywayException (Runtime) is thrown, change it to a SQLException
            throw new SQLException("Flyway executeSql() error occurred", fe);
        }
    }

    /**
     * Get the Database Schema Name in use by this Connection, so that it can
     * be used to limit queries in other methods (e.g. tableExists()).
     *
     * @param connection
     *            Current Database Connection
     * @return Schema name as a string, or "null" if cannot be determined or unspecified
     */
    public static String getSchemaName(Connection connection)
            throws SQLException
    {
        String schema = null;
        
        // Try to get the schema from the DB connection itself.
        // As long as the Database driver supports JDBC4.1, there should be a getSchema() method
        // If this method is unimplemented or doesn't exist, it will throw an exception (likely an AbstractMethodError)
        try
        {
            schema = connection.getSchema();
        }
        catch (Exception|AbstractMethodError e)
        {
        }

        // If we don't know our schema, let's try the schema in the DSpace configuration
        if(StringUtils.isBlank(schema))
        {
            schema = canonicalize(connection, ConfigurationManager.getProperty("db.schema"));
        }
            
        // Still blank? Ok, we'll find a "sane" default based on the DB type
        if(StringUtils.isBlank(schema))
        {
            String dbType = getDbType(connection);

            if(dbType.equals(DBMS_POSTGRES))
            {
                // For PostgreSQL, the default schema is named "public"
                // See: http://www.postgresql.org/docs/9.0/static/ddl-schemas.html
                schema = "public";
            }
            else if (dbType.equals(DBMS_ORACLE))
            {
                // For Oracle, default schema is actually the user account
                // See: http://stackoverflow.com/a/13341390
                DatabaseMetaData meta = connection.getMetaData();
                schema = meta.getUserName();
            }
            else // For H2 (in memory), there is no such thing as a schema
                schema = null;
        }

        return schema;
    }

    /**
     * Return the canonical name for a database identifier based on whether this
     * database defaults to storing identifiers in uppercase or lowercase.
     *
     * @param connection 
     *            Current Database Connection
     * @param dbIdentifier 
     *            Identifier to canonicalize (may be a table name, column name, etc)
     * @return The canonical name of the identifier.
     */
    public static String canonicalize(Connection connection, String dbIdentifier)
            throws SQLException
    {
        // Avoid any null pointers
        if(dbIdentifier==null)
            return null;
        
        DatabaseMetaData meta = connection.getMetaData();

        // Check how this database stores its identifiers, etc.
        // i.e. lowercase vs uppercase (by default we assume mixed case)
        if(meta.storesLowerCaseIdentifiers())
        {
            return StringUtils.lowerCase(dbIdentifier);
            
        }
        else if(meta.storesUpperCaseIdentifiers())
        {
            return StringUtils.upperCase(dbIdentifier);
        }
        else // Otherwise DB doesn't care about case
        {    
            return dbIdentifier;
        }
    }
    
    /**
     * Whether or not to tell Discovery to reindex itself based on the updated
     * database.
     * <P>
     * Whenever a DB migration occurs this is set to "true" to ensure the
     * Discovery index is updated. When Discovery initializes it calls
     * checkReindexDiscovery() to reindex if this flag is true.
     * <P>
     * Because the DB migration may be initialized by commandline or any one of
     * the many DSpace webapps, setting this to "true" actually writes a temporary
     * file which lets Solr know when reindex is needed.
     * @param reindex true or false
     */
    public static synchronized void setReindexDiscovery(boolean reindex)
    {
        File reindexFlag = new File(reindexDiscoveryFilePath);

        // If we need to flag Discovery to reindex, we'll create a temporary file to do so.
        if(reindex)
        {
            try
            {
                //If our flag file doesn't exist, create it as writeable to all
                if(!reindexFlag.exists())
                {
                    reindexFlag.createNewFile();
                    reindexFlag.setWritable(true, false);
                }
            }
            catch(IOException io)
            {
                log.error("Unable to create Discovery reindex flag file " + reindexFlag.getAbsolutePath() + ". You may need to reindex manually.", io);
            }
        }
        else // Otherwise, Discovery doesn't need to reindex. Delete the temporary file if it exists
        {
            //If our flag file exists, delete it
            if(reindexFlag.exists())
            {
                boolean deleted = reindexFlag.delete();
                if(!deleted)
                    log.error("Unable to delete Discovery reindex flag file " + reindexFlag.getAbsolutePath() + ". You may need to delete it manually.");
            }
        }
    }

    /**
     * Whether or not reindexing is required in Discovery.
     * <P>
     * Because the DB migration may be initialized by commandline or any one of
     * the many DSpace webapps, this checks for the existence of a temporary
     * file to know when Discovery/Solr needs reindexing.
     * @return whether reindex flag is true/false
     */
    public static boolean getReindexDiscovery()
    {
        // Simply check if the flag file exists
        File reindexFlag = new File(reindexDiscoveryFilePath);
        return reindexFlag.exists();
    }

    /**
     * Method to check whether we need to reindex in Discovery (i.e. Solr). If
     * reindexing is necessary, it is performed. If not, nothing happens.
     * <P>
     * This method is called by Discovery whenever it initializes a connection
     * to Solr.
     *
     * @param indexer
     *          The actual indexer to use to reindex Discovery, if needed
     * @see org.dspace.discovery.SolrServiceImpl
     */
    public static synchronized void checkReindexDiscovery(IndexingService indexer)
    {
        // We only do something if the reindexDiscovery flag has been triggered
        if(getReindexDiscovery())
        {
            // Kick off a custom thread to perform the reindexing in Discovery
            // (See ReindexerThread nested class below)
            ReindexerThread go = new ReindexerThread(indexer);
            go.start();
        }
    }

    /**
     * Internal class to actually perform re-indexing in a separate thread.
     * (See checkReindexDiscovery() method)>
     */
    private static class ReindexerThread extends Thread
    {
        private final IndexingService indexer;

        /**
         * Constructor. Pass it an existing IndexingService
         * @param is
         */
        ReindexerThread(IndexingService is)
        {
            this.indexer = is;
        }

        /**
         * Actually perform Reindexing in Discovery/Solr.
         * This is synchronized so that only one thread can get in at a time.
         */
        @Override
        public void run()
        {
            synchronized(this.indexer)
            {
                // Make sure reindexDiscovery flag is still true
                // If multiple threads get here we only want to reindex ONCE
                if(DatabaseUtils.getReindexDiscovery())
                {
                    Context context = null;
                    try
                    {
                        context = new Context();

                        log.info("Post database migration, reindexing all content in Discovery search and browse engine");

                        // Reindex Discovery completely
                        // Force clean all content
                        this.indexer.cleanIndex(true);
                        // Recreate the entire index (overwriting existing one)
                        this.indexer.createIndex(context);
                        // Rebuild spell checker (which is based on index)
                        this.indexer.buildSpellCheck();

                        log.info("Reindexing is complete");
                    }
                    catch(SearchServiceException sse)
                    {
                        log.warn("Unable to reindex content in Discovery search and browse engine. You may need to reindex manually.", sse);
                    }
                    catch(SQLException | IOException e)
                    {
                        log.error("Error attempting to reindex all contents for search/browse", e);
                    }
                    finally
                    {
                        // Reset our indexing flag. Indexing is done or it threw an error,
                        // Either way, we shouldn't try again.
                        DatabaseUtils.setReindexDiscovery(false);

                        // Clean up our context, if it still exists
                        if(context!=null && context.isValid())
                            context.abort();
                    }
                }
            }
        }
    }

    /**
     * Determine the type of Database, based on the DB connection.
     * 
     * @param connection current DB Connection
     * @return a DB keyword/type (see DatabaseUtils.DBMS_* constants)
     * @throws SQLException
     */
    public static String getDbType(Connection connection)
            throws SQLException
    {
        DatabaseMetaData meta = connection.getMetaData();
        String prodName = meta.getDatabaseProductName();
        String dbms_lc = prodName.toLowerCase(Locale.ROOT);
        if (dbms_lc.contains("postgresql"))
        {
            return DBMS_POSTGRES;
        }
        else if (dbms_lc.contains("oracle"))
        {
            return DBMS_ORACLE;
        }
        else if (dbms_lc.contains("h2")) // Used for unit testing only
        {
            return DBMS_H2;
        }
        else
        {
            return dbms_lc;
        }
    }

    /**
     * Get a reference to the configured DataSource (which can be used to
     * initialize the database using Flyway).
     * <P>
     * This is NOT public, as we discourage direct connections to the database
     * which bypass Hibernate. Only Flyway should be allowed a direct connection.
     * @return DataSource
     */
    private static DataSource getDataSource()
    {
        // DataSource is configured via our ServiceManager (i.e. via Spring).
        return new DSpace().getServiceManager().getServiceByName("dataSource", BasicDataSource.class);
    }

    /**
     * In case of a unit test the flyway db is cached to long leading to exceptions, we need to clear the object
     */
    public static void clearFlywayDBCache()
    {
        flywaydb = null;
    }

    public static String getCurrentFlywayState(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT \"version\" FROM \"schema_version\" ORDER BY \"installed_rank\" desc");
        ResultSet resultSet = statement.executeQuery();
        resultSet.next();
        return resultSet.getString("version");
    }

    public static Double getCurrentFlywayDSpaceState(Connection connection) throws SQLException
    {
        String flywayState = getCurrentFlywayState(connection);
        Matcher matcher = Pattern.compile("^([0-9]*\\.[0-9]*)(\\.)?.*").matcher(flywayState);
        if(matcher.matches())
        {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }

    /**
     * Get version of pgcrypto extension available. The extension is "available"
     * if it's been installed via operating system tools/packages. It also
     * MUST be installed in the DSpace database (see getPgcryptoInstalled()).
     * <P>
     * The pgcrypto extension is required for Postgres databases
     * * @param current database connection
     * @return version number or null if not available
     */
    private static Double getPgcryptoAvailableVersion(Connection connection)
    {
        Double version = null;

        String checkPgCryptoAvailable = "SELECT default_version AS version FROM pg_available_extensions WHERE name=?";

        // Run the query to obtain the version of 'pgcrypto' available
        try (PreparedStatement statement = connection.prepareStatement(checkPgCryptoAvailable))
        {
            statement.setString(1,PGCRYPTO);
            try(ResultSet results = statement.executeQuery())
            {
                if(results.next())
                {
                    version = results.getDouble("version");
                }
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is available.", e);
        }

        return version;
    }

    /**
     * Get version of pgcrypto extension installed in the DSpace database.
     * <P>
     * The pgcrypto extension is required for Postgres databases to support
     * UUIDs.
     * @param current database connection
     * @return version number or null if not installed
     */
    private static Double getPgcryptoInstalledVersion(Connection connection)
    {
        Double version = null;

        String checkPgCryptoInstalled = "SELECT extversion AS version FROM pg_extension WHERE extname=?";

        // Run the query to obtain the version of 'pgcrypto' installed on this database
        try (PreparedStatement statement = connection.prepareStatement(checkPgCryptoInstalled))
        {
            statement.setString(1,PGCRYPTO);
            try(ResultSet results = statement.executeQuery())
            {
                if(results.next())
                {
                    version = results.getDouble("version");
                }
            }
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is available.", e);
        }

        return version;
    }

    /**
     * Check if the pgcrypto extension is BOTH installed AND up-to-date.
     * <P>
     * This requirement is only needed for PostgreSQL databases.
     * It doesn't matter what schema pgcrypto is installed in, as long as it exists.
     * @return true if everything is installed & up-to-date. False otherwise.
     */
    public static boolean isPgcryptoUpToDate()
    {
        // Get our configured dataSource
        DataSource dataSource = getDataSource();

        try(Connection connection = dataSource.getConnection())
        {
            Double pgcryptoInstalled = getPgcryptoInstalledVersion(connection);

            // Check if installed & up-to-date in this DSpace database
            if(pgcryptoInstalled!=null && pgcryptoInstalled.compareTo(PGCRYPTO_VERSION)>=0)
            {
                return true;
            }

            return false;
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is up-to-date.", e);
        }
    }

    /**
     * Check if the pgcrypto extension is installed into a particular schema
     * <P>
     * This allows us to check if pgcrypto needs to be REMOVED prior to running
     * a 'clean' on this database. If pgcrypto is in the same schema as the
     * dspace database, a 'clean' will require removing pgcrypto FIRST.
     *
     * @param schema name of schema
     * @return true if pgcrypto is in this schema. False otherwise.
     */
    public static boolean isPgcryptoInSchema(String schema)
    {
        // Get our configured dataSource
        DataSource dataSource = getDataSource();

        try(Connection connection = dataSource.getConnection())
        {
            // Check if pgcrypto is installed in the current database schema.
            String pgcryptoInstalledInSchema = "SELECT extversion FROM pg_extension,pg_namespace " +
                                                 "WHERE pg_extension.extnamespace=pg_namespace.oid " +
                                                 "AND extname=? " +
                                                 "AND nspname=?;";
            Double pgcryptoVersion = null;
            try (PreparedStatement statement = connection.prepareStatement(pgcryptoInstalledInSchema))
            {
                statement.setString(1,PGCRYPTO);
                statement.setString(2, schema);
                try(ResultSet results = statement.executeQuery())
                {
                    if(results.next())
                    {
                        pgcryptoVersion = results.getDouble("extversion");
                    }
                }
            }

            // If a pgcrypto version returns, it's installed in this schema
            if(pgcryptoVersion!=null)
                return true;
            else
                return false;
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine whether 'pgcrypto' extension is installed in schema '" + schema + "'.", e);
        }
    }


    /**
     * Check if the current user has permissions to run a clean on existing
     * database.
     * <P>
     * Mostly this just checks if you need to remove pgcrypto, and if so,
     * whether you have permissions to do so.
     *
     * @param current database connection
     * @return true if permissions valid, false otherwise
     */
    private static boolean checkCleanPermissions(Connection connection)
    {
        try
        {
            String dbType = getDbType(connection);
           
            // If we are using Postgres, special permissions or setup are
            // necessary to be able to remove the 'pgcrypto' extension.
            if(dbType.equals(DBMS_POSTGRES))
            {
                // get username of our db user
                String username = connection.getMetaData().getUserName();

                // Check their permissions. Are they a 'superuser'?
                String checkSuperuser = "SELECT rolsuper FROM pg_roles WHERE rolname=?;";
                boolean superuser = false;
                try (PreparedStatement statement = connection.prepareStatement(checkSuperuser))
                {
                    statement.setString(1,username);
                    try(ResultSet results = statement.executeQuery())
                    {
                        if(results.next())
                        {
                            superuser = results.getBoolean("rolsuper");
                        }
                    }
                }
                catch(SQLException e)
                {
                    throw new FlywayException("Unable to determine if user '" + username + "' is a superuser.", e);
                }

                // If user is a superuser, then "clean" can be run successfully
                if(superuser)
                {
                    return true;
                }
                else // Otherwise, we'll need to see which schema 'pgcrypto' is installed in
                {
                    // Get current schema name
                    String schema = getSchemaName(connection);

                    // If pgcrypto is installed in this schema, then superuser privileges are needed to remove it
                    if(isPgcryptoInSchema(schema))
                        return false;
                    else // otherwise, a 'clean' can be run by anyone
                        return true;
                }
            }
            else // for all other dbTypes, a clean is possible
                return true;
        }
        catch(SQLException e)
        {
            throw new FlywayException("Unable to determine if DB user has 'clean' privileges.", e);
        }
    }

}
