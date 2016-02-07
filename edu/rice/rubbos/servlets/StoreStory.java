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
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yahoo.ycsb.JMXClient;

import edu.rice.rubbos.db.CassandraDb;
import edu.rice.rubbos.db.DatabaseClientInterface;
import edu.rice.rubbos.db.DbProcessResult;

import java.util.Date;
import java.util.UUID;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.beans.Row;

/**
 * Builds the html page with the list of all categories and provides links to
 * browse all items in a category or items in a category for a given region
 */
public class StoreStory extends RubbosHttpServlet
{

  /*public int getPoolSize()
  {
    return Config.BrowseCategoriesPoolSize;
  }*/
  
  /** Build the html page for the response */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
	  super.doGet(request, response);
	  long start_time = System.nanoTime();
    ServletPrinter    sp = null;
    String  nickname, title, body, category;
    String password = null;
    String userId = null;
    int access;


    sp = new ServletPrinter(response, "StoreStory");

    nickname = request.getParameter("nickname");
    password = request.getParameter("password");
    title = request.getParameter("title");
    body = request.getParameter("body");
    category = request.getParameter("category");

    if (title == null)
    {
      sp.printHTML("You must provide a story title!<br>");
      return;
    }

    if (body == null)
    {
      sp.printHTML("<h3>You must provide a story body!<br></h3>");
      return;
    }

    if (category == null)
    {
      sp.printHTML("<h3>You must provide a category!<br></h3>");
      return;
    }

    sp.printHTMLheader("RUBBoS: Story submission result");

    sp.printHTML("<center><h2>Story submission result:</h2></center><p>\n");

    //Authenticate the user
    userId = "";
    access = 0;

    
	DbProcessResult result = dbClient.getStoreStoryResult(sp,  nickname, title, body,
			category, password, userId, access);

    sp=result.sp;
    long end_time = System.nanoTime();
	  double difference = (end_time - start_time)/1e6;
	  JMXClient.cassandraJMX("read",difference);
    if(result.exceptionOccured){
    	return;
    }

    

    sp.printHTMLfooter();
    
  }


  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doGet(request, response);
  }

}