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
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yahoo.ycsb.JMXClient;

public class PostComment extends RubbosHttpServlet
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
    ServletPrinter    sp   = null;

    sp = new ServletPrinter(response, "BrowseCategoriesByCategory");

    String storyIdtest, testparent, comment_table;
   String parent = "0", storyId = "";

    storyIdtest = request.getParameter("storyId");
    testparent = request.getParameter("parent");

    if (storyIdtest != null)
    {
      storyId = request.getParameter("storyId");
    }

    if (testparent != null)
    {
      parent = request.getParameter("parent");
    }

    comment_table = request.getParameter("comment_table");

    if (comment_table == null)
    {
      sp.printHTML("Viewing comment, You must provide a comment table!<br>");
      return;
    }
    long end_time = System.nanoTime();
	  double difference = (end_time - start_time)/1e6;
	  JMXClient.cassandraJMX("read",difference);
    sp.printHTMLheader("RUBBoS: Comment submission");
    sp
        .printHTML("<p><br><center><h2>Post a comment !</h2><br>\n"
            + "<form action=\"/rubbos/servlet/edu.rice.rubbos.servlets.StoreComment\" method=POST>\n"
            + "<input type=hidden name=storyId value="
            + storyId
            + ">\n"
            + "<input type=hidden name=parent value="
            + parent
            + ">\n"
            + "<input type=hidden name=comment_table value="
            + comment_table
            + ">\n"
            + "<center><table>\n"
            + "<tr><td><b>Nickname</b><td><input type=text size=20 name=nickname>\n"
            + "<tr><td><b>Password</b><td><input type=text size=20 name=password>\n"
            + "<tr><td><b>Subject</b><td><input type=text size=100 name=subject>\n"
            + "</SELECT></table><p><br>\n"
            + "<TEXTAREA rows=\"20\" cols=\"80\" name=\"body\">Write your comment here</TEXTAREA><br><p>\n"
            + "<input type=submit value=\"Post your comment now!\"></center><p>\n");

    sp.printHTMLfooter();       
    
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doGet(request, response);
  }

}
