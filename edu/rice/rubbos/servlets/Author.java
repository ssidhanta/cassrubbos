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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yahoo.ycsb.JMXClient;

import edu.rice.rubbos.db.CassandraDb;
import edu.rice.rubbos.db.DatabaseClientInterface;
import edu.rice.rubbos.db.DbProcessResult;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.*;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.beans.*;
import me.prettyprint.hector.api.mutation.Mutator;

public class Author extends RubbosHttpServlet
{

	/*public int getPoolSize()
	{
		return Config.BrowseCategoriesPoolSize;
	}*/



	/** Build the html page for the response */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
			{
		long start_time = System.nanoTime();
		ServletPrinter    sp   = null;
		super.doGet(request, response);

		sp = new ServletPrinter(response, "Author");



		// int storyId =
		// (Integer.valueOf(request.getParameter("storyId"))).intValue();

		/*
		 * if (storyId == 0) { sp.printHTML( " <h3> You must provide a story
		 * identifier ! <br></h3> "); return; }
		 */

		String nickname, password;		

		nickname = request.getParameter("nickname");
		password = request.getParameter("password");

		if (nickname == null)
		{
			sp.printHTML("Author: You must provide a nick name!<br>");			
			return;
		}

		if (password == null)
		{
			sp.printHTML("Author: You must provide a password!<br>");			
			return;
		}
		
		DbProcessResult result = dbClient.getAuthorResult(sp,nickname,password);
		sp=result.sp;
		if(result.exceptionOccured){
			return;
		} 
		long end_time = System.nanoTime();
		  double difference = (end_time - start_time)/1e6;
		  JMXClient.cassandraJMX("read",difference);

	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		doGet(request, response);
	}

}
