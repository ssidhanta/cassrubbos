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
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yahoo.ycsb.JMXClient;

import edu.rice.rubbos.db.CassandraDb;
import edu.rice.rubbos.db.DatabaseClientInterface;
import edu.rice.rubbos.db.DbProcessResult;

public class OlderStories extends RubbosHttpServlet
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
    sp = new ServletPrinter(response, "OlderStories");

    String day, month, year, testpage, testnbOfStories;
    int page = 0, nbOfStories = 0;   

    testpage = request.getParameter("page");
    testnbOfStories = request.getParameter("nbOfStories");
    day = request.getParameter("day");
    month = request.getParameter("month");
    year = request.getParameter("year");

    if (testpage != null)
    {
      page = (Integer.valueOf(request.getParameter("page"))).intValue();
    }

    if (testpage == null)
    {
      page = 0;
    }

    if (month == null)
    {
      month = request.getParameter("month");
    }

    if (day == null)
    {
      day = request.getParameter("day");
    }

    if (year == null)
    {
      year = request.getParameter("year");
    }

    if (testnbOfStories != null)
    {
      nbOfStories = (Integer.valueOf(request.getParameter("nbOfStories")))
          .intValue();
    }
    else
      nbOfStories = 25;

    sp.printHTMLheader("RUBBoS Older Stories");

    // Display the date chooser
    sp
        .printHTML("<form action=\"/rubbos/servlet/edu.rice.rubbos.servlets.OlderStories\" method=POST>\n");
    sp.printHTML("<center><B>Date (day/month/year):</B><SELECT name=day>\n");
    for (int i = 1; i < 32; i++)
      sp.printHTML("<OPTION value=\"" + i + "\">" + i + "</OPTION>\n");
    sp.printHTML("</SELECT>&nbsp/&nbsp<SELECT name=month>\n");
    for (int i = 1; i < 13; i++)
      sp.printHTML("<OPTION value=\"" + i + "\">" + i + "</OPTION>\n");
    sp.printHTML("</SELECT>&nbsp/&nbsp<SELECT name=year>\n");
    for (int i = 2000; i < 2013; i++)
      sp.printHTML("<OPTION value=\"" + i + "\">" + i + "</OPTION>\n");
    sp
        .printHTML("</SELECT><p><input type=submit value=\"Retrieve stories from this date!\"><p>\n");

    if ((day == null) || (month == null) || (year == null))
      sp.printHTML("<br><h2>Please select a date</h2><br>");
    else
    {
      sp.printHTML("<br><h2>Stories of the " + day + "/" + month + "/" + year
          + "</h2></center><br>");
  

      Date dfrom = new Date(Integer.parseInt(year) - 1900, Integer.parseInt(month) -1 ,Integer.parseInt(day), 0,0,0);
      Date dto   = new Date(Integer.parseInt(year) - 1900, Integer.parseInt(month) - 1,Integer.parseInt(day), 23,59,59);
      
              
  	DbProcessResult result = dbClient.getOlderStoriesResult (sp, day, month, year,  page, nbOfStories, dfrom, dto);
      sp=result.sp;
      long end_time = System.nanoTime();
	  double difference = (end_time - start_time)/1e6;
	  JMXClient.cassandraJMX("read",difference);
      if(result.exceptionOccured){
    	  return;
      }

      if (page == 0)
        sp
            .printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.OlderStories?day="
                + day
                + "&month="
                + month
                + "&year="
                + year
                + "&page="
                + (page + 1)
                + "&nbOfStories="
                + nbOfStories
                + "\">Next page</a>\n</CENTER>\n");
      else
        sp
            .printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.OlderStories?day="
                + day
                + "&month="
                + month
                + "&year="
                + year
                + "&page="
                + (page - 1)
                + "&nbOfStories="
                + nbOfStories
                + "\">Previous page</a>\n&nbsp&nbsp&nbsp"
                + "<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.OlderStories?day="
                + day                
                + "&month="
                + month
                + "&year="
                + year
                + "&page="
                + (page + 1)
                + "&nbOfStories="
                + nbOfStories
                + "\">Next page</a>\n\n</CENTER>\n");
    }
    sp.printHTMLfooter();
    
  }



  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doGet(request, response);
  }

}
