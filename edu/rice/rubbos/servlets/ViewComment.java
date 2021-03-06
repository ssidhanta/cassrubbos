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
import java.util.List;

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
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;

public class ViewComment extends RubbosHttpServlet
{

 /* public int getPoolSize()
  {
    return Config.BrowseCategoriesPoolSize;
  }*/



  /** Build the html page for the response */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
	  super.doGet(request, response);
	  long start_time = System.nanoTime();
    ServletPrinter    sp    = null;
   
    String    filterstring, comment = null,parent = "", displaystring, storyId, commentIdstring, comment_table,  commentId;
    int    filter = 0, display = 0;
	
    sp = new ServletPrinter(response, "ViewComment");

    filterstring = request.getParameter("filter");
    storyId = request.getParameter("storyId");
    displaystring = request.getParameter("display");
    commentIdstring = request.getParameter("commentId");
    comment_table = request.getParameter("comment_table");

    if (filterstring != null)
    {
      filter = (Integer.valueOf(request.getParameter("filter"))).intValue();
    }
    else
      filter = 0;

    if (displaystring != null)
    {
      display = (Integer.valueOf(request.getParameter("display"))).intValue();
    }
    else
      display = 0;

    if (storyId == null)
    {
      sp.printHTML("Viewing comment: You must provide a story identifier!<br>");
      return;
    }

    if (commentIdstring == null)
    {
      sp
          .printHTML("Viewing comment: You must provide a comment identifier!<br>");
      return;
    }
    else
      commentId = request.getParameter("commentId");

    if (comment_table == null)
    {
      sp.printHTML("Viewing comment: You must provide a comment table!<br>");
    }

    
	DbProcessResult result = dbClient.getViewCommentResult( sp, comment,  storyId,  comment_table,
			 parent,   filter,  display,  commentId);

    sp=result.sp;
    long end_time = System.nanoTime();
	  double difference = (end_time - start_time)/1e6;
	  JMXClient.cassandraJMX("read",difference);
    if(result.exceptionOccured){
    	return;
    }


    //closeConnection(stmt, conn);

    sp.printHTMLfooter();
    
  }



  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doGet(request, response);
  }

}
