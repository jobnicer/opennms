package org.opennms.client;

import java.util.*;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * TODO: Do we need additional functions?
 * <ul>
 * <li>isDatabaseUpToDate()</li>
 * <li>setDatabaseConfiguration() with public access</li>
 * </ul>
 */
@RemoteServiceRelativePath("install") // This path must match the value of the servlet mapping in web.xml
public interface InstallService extends RemoteService {
    /**
     * Check to see if the ownership file exists in the OpenNMS home directory.
     * @return True if the file exists, false otherwise
     */
    public boolean checkOwnershipFileExists();

    /**
     * Fetch the expected name of the ownership file. This name will be randomly
     * generated each time the webapp is started up.
     */
    public String getOwnershipFilename();

    /**
     * Reset the name of the ownership file to a new value.
     */
    public void resetOwnershipFilename();

    /**
     * Check to see if the admin password has been set.
     * 
     * @return True if the password is not null, false otherwise
     */
    public boolean isAdminPasswordSet() throws OwnershipNotConfirmedException;

    /**
     * Update the admin password to the specified value.
     * @throws UserUpdateException 
     * @throws UserConfigFileException 
     */
    public void setAdminPassword(String password) throws OwnershipNotConfirmedException, UserConfigFileException, UserUpdateException;

    /**
     * Fetch the current database settings from the <code>opennms-datasources.xml</code>
     * configuration file. This call is used to prepopulate the database settings form
     * with default or existing data.
     * @throws DatabaseConfigFileException 
     */
    public DatabaseConnectionSettings getDatabaseConnectionSettings() throws IllegalStateException, OwnershipNotConfirmedException, DatabaseConfigFileException;

    /**
     * Attempt to connect to the database and perform a lightweight database
     * test to ensure that our database connection parameters are successfully
     * connecting to a proper OpenNMS database. This method will throw exceptions
     * if the connection failed or the parameters cannot be stored.
     * @throws DatabaseDriverException 
     * @throws DatabaseAccessException 
     * @throws DatabaseConfigFileException 
     * @throws IllegalDatabaseArgumentException 
     */
    public void connectToDatabase(String driver, String dbName, String dbAdminUser, String dbAdminPassword, String dbAdminUrl, String dbNmsUser, String dbNmsPassword, String dbNmsUrl) throws IllegalStateException, DatabaseDoesNotExistException, OwnershipNotConfirmedException, DatabaseDriverException, DatabaseAccessException, DatabaseConfigFileException, IllegalDatabaseArgumentException;

    /**
     * Attempt to connect to the database and perform a lightweight database
     * test to ensure that our database connection parameters are successfully
     * connecting to a proper OpenNMS database. This method will throw exceptions
     * if the connection failed or the parameters cannot be stored.
     * @throws DatabaseCreationException 
     * @throws DatabaseUserCreationException 
     * @throws DatabaseAlreadyExistsException 
     * @throws DatabaseAccessException 
     * @throws DatabaseDriverException 
     * @throws IllegalDatabaseArgumentException 
     */
    public void createDatabase(String driver, String dbName, String dbAdminUser, String dbAdminPassword, String dbAdminUrl, String dbNmsUser, String dbNmsPassword) throws OwnershipNotConfirmedException, DatabaseDriverException, DatabaseAccessException, DatabaseAlreadyExistsException, DatabaseUserCreationException, DatabaseCreationException, IllegalDatabaseArgumentException;

    // protected void setDatabaseConfig(String dbName, String user, String password, String driver, String url, String binaryDirectory);

    /**
     * Fetch all of the Log4J logs that have been generated by code running in the webapp JVM.
     * @return List of serializable {@link LoggingEvent} instances
     */
    public List<LoggingEvent> getDatabaseUpdateLogs(int offset) throws OwnershipNotConfirmedException;

    /**
     * Fetch all of the Log4J log messages (as rendered strings) that have been generated by 
     * code running in the webapp JVM.
     * @return List of log message String instances
     */
    public List<String> getDatabaseUpdateLogsAsStrings() throws OwnershipNotConfirmedException;

    /**
     * Fetch the progress of the database installer subtasks.
     */
    public List<InstallerProgressItem> getDatabaseUpdateProgress() throws OwnershipNotConfirmedException;

    /**
     * Flush all of the accumulated log entries to start over with a blank list of log entries.
     */
    public void clearDatabaseUpdateLogs() throws OwnershipNotConfirmedException;

    /**
     * Run the OpenNMS installer code to update the database schema.
     * This method will spawn a new thread to perform the updates and 
     * while the thread runs, the return value of {@link #updateDatabase()}
     * will be <code>true</code>.
     */
    public void updateDatabase() throws OwnershipNotConfirmedException;

    /**
     * @return True if the thread spawned by {@link #updateDatabase()} is
     * alive, false otherwise
     */
    public boolean isUpdateInProgress() throws OwnershipNotConfirmedException;

    /**
     * @return True if the thread spawned by {@link #updateDatabase()} completed 
     * successfully, false otherwise
     */
    public boolean didLastUpdateSucceed() throws OwnershipNotConfirmedException;

    /**
     * Check to see if the <code>IPLIKE</code> database procedure is working
     * properly on the currently configured database connection.
     */
    public boolean checkIpLike() throws OwnershipNotConfirmedException;
}
