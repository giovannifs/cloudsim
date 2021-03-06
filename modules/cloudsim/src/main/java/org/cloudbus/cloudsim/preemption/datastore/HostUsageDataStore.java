package org.cloudbus.cloudsim.preemption.datastore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.preemption.*;
import org.cloudbus.cloudsim.preemption.util.DecimalUtil;

public class HostUsageDataStore extends DataStore {

	public static final String DATABASE_URL_PROP = "utilization_database_url";
	private static final String UTILIZATION_TABLE_NAME = "usage";
	
	public HostUsageDataStore(Properties properties) {
		super(properties.getProperty(DATABASE_URL_PROP));

		Statement statement = null;
		Connection connection = null;
		try {
			Log.printLine("utilization_database_url=" + getDatabaseURL());

			Class.forName(DATASTORE_SQLITE_DRIVER);

			connection = getConnection();
			statement = connection.createStatement();
			statement
					.execute("CREATE TABLE IF NOT EXISTS usage("
							+ "hostId INTEGER NOT NULL, "
							+ "time REAL NOT NULL, "
							+ "usage REAL, "
							+ "vms INTEGER, "
							+ "priority INTEGER, "
							+ "availableMips REAL, "
							+ "PRIMARY KEY (hostId, time, priority)"
							+ ")");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Error while initializing the host usage database store.");
		} finally {
			close(statement, connection);
		}
	}
	
	private static final String INSERT_USAGE_ENTRY_SQL = "INSERT INTO " + UTILIZATION_TABLE_NAME
			+ " VALUES(?, ?, ?, ?, ?, ?)";
	
	public boolean addUsageEntries(List<UsageEntry> usageEntries) {
		if (usageEntries == null) {
			Log.printLine("usageEntries must no be null.");
			return false;
		}		
		Log.printLine("Adding " + usageEntries.size() + " usage entries into database.");
		
		if (usageEntries.isEmpty()) {
			return true;
		}
		
		PreparedStatement insertMemberStatement = null;
		
		Connection connection = null;
		
		try {
			connection = getConnection();
			connection.setAutoCommit(false);
			
			insertMemberStatement = connection.prepareStatement(INSERT_USAGE_ENTRY_SQL);
			insertMemberStatement = connection
					.prepareStatement(INSERT_USAGE_ENTRY_SQL);
			
			for (UsageEntry entry : usageEntries) {
				insertMemberStatement.setInt(1, entry.getHostId());
				insertMemberStatement.setDouble(2, entry.getTime());
				insertMemberStatement.setDouble(3, entry.getUsage());
				insertMemberStatement.setInt(4, entry.getNumberOfVms());
				insertMemberStatement.setInt(5, entry.getPriority());
				insertMemberStatement.setDouble(6, entry.getAvailableMips());
				insertMemberStatement.addBatch();
			}
			
			int[] executeBatch = insertMemberStatement.executeBatch();
			
			if (executionFailed(connection, executeBatch)){
				Log.printLine("Rollback will be executed.");
				connection.rollback();
				return false;
			}
			
			connection.commit();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			Log.printLine("Couldn't add utilization entries.");
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
				Log.printLine("Couldn't rollback transaction.");
			}
			return false;
		} finally {
			close(insertMemberStatement, connection);
		}
	}
	
	private static final String SELECT_ALL_USAGE_ENTRIES_SQL = "SELECT * FROM " + UTILIZATION_TABLE_NAME;
	
	public List<UsageEntry> getAllUsageEntries() {
		Statement statement = null;
		Connection conn = null;
		List<UsageEntry> entries = new ArrayList<UsageEntry>();
		
		try {
			conn = getConnection();
			statement = conn.createStatement();
			
			statement.execute(SELECT_ALL_USAGE_ENTRIES_SQL);
			ResultSet rs = statement.getResultSet();
			
			while (rs.next()) {
				entries.add(new UsageEntry(rs.getInt("hostId"), rs
						.getDouble("time"), rs.getDouble("usage"), rs
						.getInt("vms"), rs.getInt("priority"), rs
						.getDouble("availableMips")));
			}
			return entries;
		} catch (SQLException e) {
			Log.print(e);
			Log.printLine("Couldn't get tasks from DB.");
			return null;
		}
	}
	
	private boolean executionFailed(Connection connection, int[] executeBatch) {
		for (int i : executeBatch) {
			if (i == PreparedStatement.EXECUTE_FAILED) {
				return true;
			}
		}
		return false;
	}

	public List<UsageEntry> getUsageEntriesFinishedBefore(double interestedTime) {
		Statement statement = null;
		Connection conn = null;

		try {
			conn = getConnection();
			statement = conn.createStatement();

			statement.execute("SELECT * FROM " + UTILIZATION_TABLE_NAME
					+ " WHERE time < '" + interestedTime + "'");
			ResultSet rs = statement.getResultSet();

			return generateUsageEntriesList(rs);
		} catch (SQLException e) {
			Log.print(e);
			Log.printLine("Couldn't get tasks from DB.");
			return null;
		}
	}

	private List<UsageEntry> generateUsageEntriesList(ResultSet rs) throws SQLException{
		List<UsageEntry> usageEntries = new ArrayList<>();
		while (rs.next()) {
			usageEntries.add(new UsageEntry(rs.getInt("hostId"), rs
					.getDouble("time"), rs.getDouble("usage"), rs
					.getInt("vms"), rs.getInt("priority"), rs
					.getDouble("availableMips")));
		}

		return usageEntries;
	}

	public List<UsageEntry> getUsageEntryInterval(int intervalIndex,
											double intervalSize, double minInterestedTime, double maxInterestedTime) throws SQLException, ClassNotFoundException {

		if (intervalIndex < 0){
			throw new IllegalArgumentException("Interval index must be not negative");
		}

		if (intervalSize <= 0){
			throw new IllegalArgumentException("Interval size must be positive");
		}

		if (!hasMoreEvents(intervalIndex, intervalSize)) {
			Log.printLine("The Interval index is "
					+ intervalIndex
					+ " and there are not more events to be treated.");
			return null;
		}

		double minTime = Math.max(minInterestedTime, (intervalIndex * intervalSize));
		double maxTime = Math.min(maxInterestedTime, ((intervalIndex + 1) * intervalSize));


		List<UsageEntry> usageEntries = new ArrayList<UsageEntry>();

		Statement statement = null;
		Connection connection;
		try {
			connection = getConnection();

			if (connection != null) {
				statement = connection.createStatement();

				String sql = "SELECT hostId, time, usage, vms,priority, availableMips FROM usage WHERE time >= '"
						+ minTime + "' AND time < '" + maxTime + "'";

				ResultSet rs = statement.executeQuery(sql);

				while (rs.next()) {
					/*
					 * TODO We need to check if this nextTaskId variable is considered for post processing.
					 * If we execute part of the trace more than one time, the same task can be have different
					 * taskId (depending of the interval size).
					 */
					//TODO review it
					usageEntries.add(new UsageEntry(rs.getInt("hostId"), rs
							.getDouble("time"), rs.getDouble("usage"), rs
							.getInt("vms"), rs.getInt("priority"), rs
							.getDouble("availableMips")));
				}

				Log.printLine(CloudSim.clock() + ": Interval index is " + intervalIndex + " and number of tasks is " + usageEntries.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return usageEntries;
	}

	public boolean hasMoreEvents(int intervalIndex,
								 double intervalSize) throws SQLException, ClassNotFoundException {
		return (intervalIndex >= 0 && (intervalIndex * intervalSize) <= getMaxTraceTime());
	}

	public double getMaxTraceTime() throws ClassNotFoundException,
			SQLException {
		Statement statement = null;
		Connection connection = null;

		try {
			Class.forName(DATASTORE_SQLITE_DRIVER);
			connection = getConnection();

			if (connection != null) {
				Log.printLine("Connected to the database: " + getDatabaseURL());

				statement = connection.createStatement();

				// getting the max submitTime from database
				ResultSet results = statement
						.executeQuery("SELECT MAX(time) FROM usage");

				while (results.next()) {
					return results.getDouble("MAX(time)");
				}
			}
		} finally {
			close(statement, connection);
		}
		// It should never return this value.
		return -1;
	}
}