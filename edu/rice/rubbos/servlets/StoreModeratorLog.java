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
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yahoo.ycsb.JMXClient;

import edu.rice.rubbos.db.CassandraDb;
import edu.rice.rubbos.db.DatabaseClientInterface;
import edu.rice.rubbos.db.DbProcessResult;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;

public class StoreModeratorLog extends RubbosHttpServlet
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

    String nickname, password, comment_table, commentId, ratingstring,userId = "";
    int access = 0,  rating;
    

    sp = new ServletPrinter(response, "StoreModeratorLog");

    nickname = request.getParameter("nickname");
    password = request.getParameter("password");

    comment_table = request.getParameter("comment_table");
    commentId = request.getParameter("commentId");
    ratingstring = request.getParameter("rating");

    if (nickname == null)
    {
      sp.printHTML("Author, You must provide a nick name!<br>");
      return;
    }

    if (password == null)
    {
      sp.printHTML("Author, You must provide a password!<br>");
      return;
    }

    if (comment_table == null)
    {
      sp.printHTML("Moderating comment, You must provide a comment table!<br>");
      return;
    }

    if (commentId == null)
    {
      sp
          .printHTML("Moderating comment, You must provide a comment identifier!<br>");
      return;
    }

    if (ratingstring == null)
    {
      sp.printHTML("Moderating comment, You must provide a rating!<br>");
      return;
    }
    else
      rating = (Integer.valueOf(request.getParameter("rating"))).intValue();

    
	DbProcessResult result = dbClient.getStoreModLogResult(sp, nickname, password, comment_table,
			commentId, access, userId, rating);
    
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
