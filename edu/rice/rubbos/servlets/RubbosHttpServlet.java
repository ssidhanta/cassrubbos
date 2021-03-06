/**
 * RUBBoS: Rice University Bulletin Board System.
 * Copyright (C) 2001-2004 Rice University and French National Institute For 
 * Research In Computer Science And Control (INRIA).
 * Contact: jmob@objectweb.org
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or any later
 * version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * Initial developer(s): Emmanuel Cecchet.
 * Contributor(s): Niraj Tolia.
 */

package edu.rice.rubbos.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lsu.ml.Learner;
import com.mongodb.DB;
import com.mongodb.Mongo;

import edu.rice.rubbos.db.CassandraDb;
import edu.rice.rubbos.db.DatabaseClientInterface;
import edu.rice.rubbos.db.MongoDb;
import me.prettyprint.cassandra.connection.HClientPool;
import me.prettyprint.cassandra.connection.HConnectionManager;
import me.prettyprint.cassandra.model.ConfigurableConsistencyLevel;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.HConsistencyLevel;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.*;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.*;
import me.prettyprint.hector.api.mutation.Mutator;
/**
 * Provides the method to initialize connection to the database. All the
 * servlets inherit from this class 
 */
public abstract class RubbosHttpServlet extends HttpServlet
{
	DatabaseClientInterface dbClient=null;
	String dbclienttype="cassandra";
	private Properties properties;
	//MongoDb
	private Mongo m ;
	private DB db;

	//Cassandra
	private Cluster            myCluster;
	private Keyspace           myKeySpace;

	private StringSerializer stringSerializer; 
	private LongSerializer longSerializer;
	private BytesArraySerializer bas;

	/** Load the driver and get a connection to the database */
	public void init() throws ServletException
	{	  
		InputStream inputStream = getServletContext().getResourceAsStream("/WEB-INF/dbconfig.properties");  

		properties = new Properties(); 
		try {
			properties.load(inputStream);
			dbclienttype = properties.getProperty("dbtype");
			if(dbclienttype==null)
				dbclienttype = "cassandra";
		} catch (IOException e) {	
			e.printStackTrace();
			throw new ServletException("DB Properties not available");
		}  
		if(dbclienttype.equals("cassandra")){
			System.out.println("***1111 test rubbos dbclienttype:="+dbclienttype);
			//System.err.println("*** test rubbos dbclienttype:="+dbclienttype);
			initCassandra();
		}
		if(dbclienttype.equals("mongodb")){
			try {
				initMongoDb();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new ServletException("DB host not found");
			}
		}	  
	}

	private void initMongoDb() throws UnknownHostException{	  
		m = new Mongo( properties.getProperty("host") );
		db = m.getDB( properties.getProperty("db") );	
		dbClient = new MongoDb(m, db);
	}

	private void initCassandra() {
		stringSerializer = StringSerializer.get();
		longSerializer = LongSerializer.get(); 
		bas = BytesArraySerializer.get();
		String[] cLevel = null;
		File f_test = new File("//home//user//Downloads//test_data.arff");
		//File f = new File("/tmp/training_data_final.arff");
		//chooseConsistency(f,5366,200,3637475.387755102,10,0,50);
		cLevel = Learner.predict_Final(f_test);
		ConfigurableConsistencyLevel configurableConsistencyLevel = new ConfigurableConsistencyLevel();
		Map<String, HConsistencyLevel> clmap = new HashMap<String, HConsistencyLevel>();

		// Define CL.ONE for ColumnFamily "MyColumnFamily"

		//clmap.put("MyColumnFamily", HConsistencyLevel.ONE);
		clmap.put("MyColumnFamily", HConsistencyLevel.valueOf(cLevel[0]));

		// In this we use CL.ONE for read and writes. But you can use different CLs if needed.
		configurableConsistencyLevel.setReadCfConsistencyLevels(clmap);
		configurableConsistencyLevel.setWriteCfConsistencyLevels(clmap);
		myCluster = HFactory.getOrCreateCluster("Test Cluster", "172.31.32.82:9160");
		myKeySpace = HFactory.createKeyspace("tutorial", myCluster);
		dbClient = new CassandraDb(myKeySpace,stringSerializer,bas,longSerializer);
	}
    
	
	/**
	 * Initialize the pool of connections to the database.
	 * The caller must ensure that the driver has already been
	 * loaded else an exception will be thrown.
	 *
	 * @exception SQLException if an error occurs
	 */
	public synchronized void initializeConnections() throws SQLException
	{

	}




	/**
	 * Closes a <code>Connection</code>.
	 * @param connection to close 
	 */
	private void closeConnection(Connection connection)
	{

	}

	/**
	 * Gets a connection from the pool (round-robin)
	 *
	 * @return a <code>Connection</code> or 
	 * null if no connection is available
	 */
	public synchronized Connection getConnection()
	{
		return null;
	}

	/**
	 * Releases a connection to the pool.
	 *
	 * @param c the connection to release
	 */
	public synchronized void releaseConnection(Connection c)
	{

	}

	/**
	 * Release all the connections to the database.
	 *
	 * @exception SQLException if an error occurs
	 */
	public synchronized void finalizeConnections() throws SQLException
	{

	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
		System.out.println("***in rubbos servlet do GET super method***");
			}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
				System.out.println("***in rubbos servlet do post super method***");
			}
 
	/**
	 * Clean up database connections.
	 */
	public void destroy()
	{
		if(dbclienttype.equals("mongodb")){
			m.close();
		}
	}
}
