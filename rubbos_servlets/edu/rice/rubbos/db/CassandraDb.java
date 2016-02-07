package edu.rice.rubbos.db;

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.model.IndexedSlicesQuery;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import edu.rice.rubbos.servlets.ServletPrinter;


public class CassandraDb implements DatabaseClientInterface{
	Keyspace myKeySpace;
	StringSerializer stringSerializer;
	BytesArraySerializer bas;
	LongSerializer longSerializer;

	public CassandraDb(Keyspace myKeySpace,StringSerializer stringSerializer,BytesArraySerializer bas,LongSerializer longSerializer){
		this.myKeySpace=myKeySpace;;
		this.bas=bas;
		this.stringSerializer=stringSerializer;
		this.longSerializer=longSerializer;
	}
	
	public String authenticate(String nickname, String password) {
		CqlQuery<String, String, String> query;
		QueryResult<OrderedRows<String, String, String>> result;
		try
		{     
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, stringSerializer);
			query.setQuery("select nickname from users where nickname='"+nickname+"' and password='"+password+"'");

			QueryResult<CqlRows<String, String, String>> cresult = query.execute();
			CqlRows<String, String, String> crows = (CqlRows<String, String, String> )cresult.get();

			Row<String, String, String> row = (crows.getList()).get(0);

			return row.getKey();

		}
		catch (Exception e)
		{
			return e + "Authenticate function error";

		}
	}
	
	public String getUserName(String userId) throws Exception {
		CqlQuery<String, String, String> 				 query;
		QueryResult<OrderedRows<String, String, String>> result;

		try
		{
			/*PreparedStatement stmt = conn
          .prepareStatement("SELECT nickname FROM users WHERE id=?");
      stmt.setInt(1, UserId);
      ResultSet rs = stmt.executeQuery();
      rs.first();
      return rs.getString("nickname");*/

			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, stringSerializer);
			query.setQuery("select nickname from users where key='"+userId+"'");

			QueryResult<CqlRows<String, String, String>> cresult = query.execute();
			CqlRows<String, String, String> crows = (CqlRows<String, String, String> )cresult.get();

			Row<String, String, String> row = (crows.getList()).get(0);

			ColumnSlice<String, String> tcol = row.getColumnSlice();

			return (tcol.getColumnByName("nickname")).getValue();

		}
		catch (Exception e)
		{     

			throw e;
		}
	}

	public DbProcessResult getAcceptStoryResult(ServletPrinter sp,String storyId) {
		ResultSet rs = null;
		int updateResult;

		QueryResult<Rows<String, String, byte[]>> results;
		QueryResult<?> qr ;
		try
		{
			/*stmt = conn
	          .prepareStatement("SELECT * FROM submissions WHERE id= storyId");
	      rs = stmt.executeQuery();*/
			// Setup the query. Looks huge but its pretty simple
			MultigetSliceQuery<String, String, byte[]> multigetSlicesQuery =
					HFactory.createMultigetSliceQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			multigetSlicesQuery.setColumnFamily("submissions");
			multigetSlicesQuery.setColumnNames("title", "date", "body", "writer", "category");        
			multigetSlicesQuery.setKeys(storyId);

			results = multigetSlicesQuery.execute();
			qr = (QueryResult)results;
		}
		catch (Exception e)
		{
			sp.printHTML(" Failed to execute Query for AcceptStory: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true);
		}
		try
		{
			Rows<?,?,?> rows = (Rows)qr.get();	
			if (rows == null)
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}
			String categoryTitle = new String();
			String categoryBody  = new String();
			long categoryDate = 0, categoryWriter = 0, category = 0;

			for(Row row : rows)  // Not necessary as there is only one row
			{
				// Get each column and then the value
				HColumn<String, byte[]> titleColumn = row.getColumnSlice().getColumnByName("title");
				//System.out.println("Title is " + stringSerializer.fromBytes(titleColumn.getValue()));
				categoryTitle = stringSerializer.fromBytes(titleColumn.getValue());
				HColumn<String, byte[]> dateColumn = row.getColumnSlice().getColumnByName("date");
				categoryDate = longSerializer.fromBytes(dateColumn.getValue());
				HColumn<String, byte[]> bodyColumn = row.getColumnSlice().getColumnByName("body");
				categoryBody = stringSerializer.fromBytes(bodyColumn.getValue());
				HColumn<String, byte[]> writerColumn = row.getColumnSlice().getColumnByName("writer");
				categoryWriter = longSerializer.fromBytes(writerColumn.getValue());
				HColumn<String, byte[]> catColumn = row.getColumnSlice().getColumnByName("category");
				category = longSerializer.fromBytes(catColumn.getValue());

				System.out.println(categoryTitle + " |" + categoryDate + "| " + categoryBody + "| " + categoryWriter + "| " + category);        
			}


			//Add story to database
			/*String categoryTitle = rs.getString("title");
	      int categoryDate = rs.getInt("date");
	      String categoryBody = rs.getString("body");
	      String categoryWriter = rs.getString("writer");
	      String category = rs.getString("category");*/
			Mutator<String> mutator = HFactory.createMutator(myKeySpace, stringSerializer);
			UUID id1 = UUID.randomUUID();

			mutator.addInsertion(id1.toString(), "stories", 
					HFactory.createStringColumn("title",categoryTitle));
			mutator.addInsertion(id1.toString(), "stories", 
					HFactory.createStringColumn("body",categoryBody));
			mutator.addInsertion(id1.toString(), "stories", 
					HFactory.createColumn("date" ,categoryDate, stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), "stories", 
					HFactory.createColumn("writer" ,categoryWriter, stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), "stories", 
					HFactory.createColumn("category" ,category, stringSerializer, longSerializer));
			mutator.execute();

			mutator = HFactory.createMutator(myKeySpace, 
					stringSerializer);
			mutator.delete(storyId, "submissions", null, stringSerializer);
			mutator.execute();

			Mutator<String> mutator1 = HFactory.createMutator(myKeySpace, stringSerializer);
			mutator1.delete("date", "submissions_time", categoryDate, longSerializer);
			mutator1.execute();

		}
		catch (Exception e)
		{
			sp.printHTML("Exception accepting stories: " + e + "<br>");	    
			return new DbProcessResult(sp, true);
		}
		return new DbProcessResult(sp, false);
	}


	public DbProcessResult getAuthorResult(ServletPrinter sp, String nickname,
			String password)
	{
		QueryResult<OrderedRows<String, String, byte[]>> result;
		int userId = 0, access = 0;
		if ((nickname != null) && (password != null))
		{
			try
			{
				/*stmt = conn
	            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
	                + nickname + "\" AND password=\"" + password + "\"");
	        rs = stmt.executeQuery();*/
				IndexedSlicesQuery<String, String, byte[]> indexedSlicesQuery = 
						HFactory.createIndexedSlicesQuery(myKeySpace, stringSerializer, 
								stringSerializer, bas);

				indexedSlicesQuery.setColumnFamily("users");
				indexedSlicesQuery.setColumnNames("access");
				indexedSlicesQuery.addEqualsExpression("nickname", stringSerializer.toBytes(nickname));
				indexedSlicesQuery.addEqualsExpression("password", stringSerializer.toBytes(password));
				result = indexedSlicesQuery.execute();
			}
			catch (Exception e)
			{
				sp.printHTML(" Failed to execute Query for Author: " + e);
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}
			try
			{
				/*if (rs.first())
	        {
	          userId = rs.getInt("id");
	          access = rs.getInt("access");
	        }*/
				Rows<?, ?, ?> rows = result.get();
				if(rows.getCount() != 1)
				{
					sp.printHTML("Authentication failed");
					//myCluster.getConnectionManager().shutdown();
					return new DbProcessResult(sp, true);
				}
				for(Row row : rows)
				{
					try
					{
						userId = (Long.valueOf((String)row.getKey())).intValue();
					}
					catch(NumberFormatException ne) // In case the userId is UUID
					{
						userId = 1;
					}

					HColumn<String, byte[]> col = row.getColumnSlice().getColumnByName("access"); 

					//String str = col.getValue();
					Long laccess = new Long(longSerializer.fromBytes(col.getValue()));

					access = laccess.intValue();	    	
				}
			}
			catch (Exception e)
			{
				sp.printHTML("Exception verifying author: " + e + "<br>");
				//closeConnection(stmt, conn);
				/* To make sure there is no double free for the connection 
	        conn = null;
	        stmt = null;*/
				return new DbProcessResult(sp, true);
			}
		}
		if ((userId == 0) || (access == 0))
		{
			sp.printHTMLheader("RUBBoS: Author page");
			sp
			.printHTML("<p><center><h2>Sorry, but this feature is only accessible by users with an author access.</h2></center><p>\n");
		}
		else
		{
			sp.printHTMLheader("RUBBoS: Author page");
			sp
			.printHTML("<p><center><h2>Which administrative task do you want to do ?</h2></center>\n"
					+ "<p><p><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ReviewStories?authorId= \""
					+ userId + "\"\">Review submitted stories</a><br>\n");
		}
		sp.printHTMLfooter();
		return new DbProcessResult(sp, false);
	}

	public DbProcessResult getBrowseCategoriesResult(ServletPrinter sp) {
		QueryResult<OrderedRows<String, String, byte[]>> result;
		ResultSet rs = null;
		CqlQuery<String, String, String> query;
		try
		{
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, 
					stringSerializer);
			query.setQuery("select * from categories");
		}
		catch (Exception e)
		{	
			sp.printHTML("Failed to set Query for BrowseCategories: " + e + "\n");
			sp.printHTML("ST : " +e.getStackTrace());	      
			return new DbProcessResult(sp, true);
		}

		try
		{
			QueryResult<CqlRows<String, String, String>> cresult = query.execute();
			CqlRows<String, String, String> crows = cresult.get();
			if(crows == null || crows.getCount() == 0)
			{
				sp.printHTML("<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>\n");

				return new DbProcessResult(sp, true);
			}
			else
				sp.printHTML("<h2>Currently available categories</h2><br>\n");

			String categoryId;
			String categoryName;
			int i = 0;
			for (Row<String, String, String> row : crows.getList()) 
			{
				i++;
				categoryId = row.getKey();
				ColumnSlice <String, String> cs = row.getColumnSlice();
				HColumn<String, String> c  = cs.getColumnByName("name");
				categoryName = c.getValue();;
				sp.printHTMLHighlighted("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
						+ categoryId
						+ "&categoryName="
						+ URLEncoder.encode(categoryName)
						+ "\">"
						+ categoryName
						+ "</a><br>\n");
			}
		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting categories: " + e + "<br>");
		} 
		return new DbProcessResult(sp, false);

	}

	public DbProcessResult getBrowseStoriesByCategoryResult(ServletPrinter sp,
			int nbOfStories, int page, String categoryId, String categoryName){
		int matchFound = 0;
		CqlQuery<String, byte[], String> query;
		QueryResult<OrderedRows<String, byte[], String>> result;
		try
		{
			/*stmt = conn.prepareStatement("SELECT * FROM stories WHERE category= "
		          + categoryId + " ORDER BY date DESC LIMIT " + page * nbOfStories
		          + "," + nbOfStories);
		      rs = stmt.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, bas, stringSerializer);
			query.setQuery("select first "+nbOfStories+" reversed  * from stories_time");
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for BrowseStoriesByCategory: " + e);
			return new DbProcessResult(sp, true);
		}
		try
		{
			QueryResult<CqlRows<String, byte[], String>> cresult = query.execute();
			CqlRows<String, byte[], String> crows = cresult.get();
			Row<String, byte[], String> row = crows.getByKey("date");

			List<HColumn<byte[], String>> clist = (row.getColumnSlice()).getColumns();

			if (clist.size() == 0)
			{
				if (page == 0)
				{
					sp
					.printHTML("<h2>Sorry, but there is no story available in this category !</h2>");
				}
				else
				{
					sp
					.printHTML("<h2>Sorry, but there are no more stories available at this time.</h2><br>\n");
					sp
					.printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
							+ categoryId
							+ "&categoryName="
							+ URLEncoder.encode(categoryName)
							+ "&page="
							+ (page - 1)
							+ "&nbOfStories=nbOfStories\">Previous page</a>\n</CENTER>\n");
				}
				sp.printHTMLfooter();
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}

			for(int i = 0 ; i < clist.size() - 1 ; i++)
			{

				Long ttime = new Long(longSerializer.fromBytes((clist.get(i)).getName()));
				String tcat = (clist.get(i)).getValue();

				CqlQuery<String, String, byte[]> 				 tquery;
				QueryResult<CqlRows<String, String, byte[]>>     tresult;

				tquery = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				tquery.setQuery("select * from stories where key='"+tcat+"'");
				tresult = tquery.execute();

				ColumnSlice<String, byte[]> tcol = ((tresult.get()).getByKey(tcat)).getColumnSlice();
				Long lcat = new Long(longSerializer.fromBytes((tcol.getColumnByName("category")).getValue()));


				if((lcat.toString()).equals(categoryId))
				{
					matchFound = 1;
					String title = stringSerializer.fromBytes((tcol.getColumnByName("title")).getValue());
					Long writer  = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));

					Date date = new Date(ttime.longValue());
					String username    = writer.toString();
					String id   = tcat;

					sp
					.printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
							+ id
							+ "\">"
							+ title
							+ "</a> by "
							+ username
							+ " on "
							+ date.toString()
							+ "<br>\n");
				}
			}

		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting categories: " + e + "<br>");
		}


		if (matchFound == 0)
		{
			if (page == 0)
			{
				sp
				.printHTML("<h2>Sorry, but there is no story available in this category !</h2>");
			}
			else
			{
				sp
				.printHTML("<h2>Sorry, but there are no more stories available at this time.</h2><br>\n");
				sp
				.printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
						+ categoryId
						+ "&categoryName="
						+ URLEncoder.encode(categoryName)
						+ "&page="
						+ (page - 1)
						+ "&nbOfStories=nbOfStories\">Previous page</a>\n</CENTER>\n");
			}
			sp.printHTMLfooter();
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true);
		}
		if (page == 0)
			sp
			.printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
					+ categoryId
					+ "&categoryName="
					+ URLEncoder.encode(categoryName)
					+ "&page="
					+ (page + 1)
					+ "&nbOfStories="+nbOfStories+"\">Next page</a>\n</CENTER>\n");
		else
			sp
			.printHTML("<p><CENTER>\n<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
					+ categoryId
					+ "&categoryName="
					+ URLEncoder.encode(categoryName)
					+ "&page="
					+ (page - 1)
					+ "&nbOfStories="+nbOfStories+"\">Previous page</a>\n&nbsp&nbsp&nbsp"
					+ "<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
					+ categoryId
					+ "&categoryName="
					+ URLEncoder.encode(categoryName)
					+ "&page="
					+ (page + 1)
					+ "&nbOfStories="+nbOfStories+"\">Next page</a>\n\n</CENTER>\n");

		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getModerateCommentResult(ServletPrinter sp,
			String commentId, String comment_table)  {
		CqlQuery<String, String, byte[]> query;
		QueryResult<OrderedRows<String, String, byte[]>> result;

		QueryResult<Rows<String, String, byte[]>> results;
		QueryResult<?> qr ;
		try
		{
			/*stmt = conn.prepareStatement("SELECT * FROM " + comment_table
	          + " WHERE id=" + commentId);
	      rs = stmt.executeQuery();*/
			MultigetSliceQuery<String, String, byte[]> multigetSlicesQuery =
					HFactory.createMultigetSliceQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			multigetSlicesQuery.setColumnFamily(comment_table);
			multigetSlicesQuery.setColumnNames("story_id", "writer", "date", "subject", "rating", "comment");        
			multigetSlicesQuery.setKeys(commentId);
			results = multigetSlicesQuery.execute();
			qr = (QueryResult)results;
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for ModerateComment: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}
		Long lwriter;
		long storyId, Date, rating, writer;
		String subject, comment;
		try
		{
			/*if (!rs.first())
	      {
	        sp
	            .printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");
	        closeConnection(stmt, conn);
	        return;
	      }*/
			Rows<String,String,byte[]> rows = (Rows)qr.get();	
			if(rows.getCount() == 0)
			{
				System.out.println("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");
				//myCluster.getConnectionManager().shutdown();
				return new DbProcessResult(sp,true);
			}

			Row<String, String, byte[]>row = rows.getByKey(commentId); 

			HColumn<String, byte[]> sidColumn = row.getColumnSlice().getColumnByName("story_id");
			storyId = longSerializer.fromBytes(sidColumn.getValue());
			HColumn<String, byte[]> wrtColumn = (row.getColumnSlice()).getColumnByName("writer");
			writer = longSerializer.fromBytes(wrtColumn.getValue());
			lwriter = new Long(writer);
			HColumn<String, byte[]> dateColumn = row.getColumnSlice().getColumnByName("date");
			Date = longSerializer.fromBytes(dateColumn.getValue());
			HColumn<String, byte[]> subColumn = row.getColumnSlice().getColumnByName("subject");
			subject = stringSerializer.fromBytes(subColumn.getValue());
			HColumn<String, byte[]> writerColumn = row.getColumnSlice().getColumnByName("rating");
			rating = longSerializer.fromBytes(writerColumn.getValue());
			HColumn<String, byte[]> catColumn = row.getColumnSlice().getColumnByName("comment");
			comment = stringSerializer.fromBytes(catColumn.getValue());

		}
		catch (Exception e)
		{
			sp.printHTML("Exception moderating comments: " + e + "<br>");
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		try
		{
			//String storyId = rs.getString("story_id");
			sp
			.printHTML("<p><br><center><h2>Moderate a comment !</h2></center><br>\n<br><hr><br>");
			String username = getUserName(lwriter.toString());

			sp
			.printHTML("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\"><TR><TD><FONT size=\"4\" color=\"#000000\"><center><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
					+ comment_table
					+ "&storyId="
					+ storyId
					+ "&commentId="
					+ commentId
					+ "\">"
					+ subject
					+ "</a></B>&nbsp</FONT> (Score:"
					+ rating
					+ ")</center></TABLE>\n");
			sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
					+ Date + "</B><p>\n");
			sp
			.printHTML("<TR><TD>"
					+ comment
					+ "</TABLE><p><hr><p>\n"
					+ "<form action=\"/rubbos/servlet/edu.rice.rubbos.servlets.StoreModeratorLog\" method=POST>\n"
					+ "<input type=hidden name=commentId value="
					+ commentId
					+ ">\n"
					+ "<input type=hidden name=comment_table value="
					+ comment_table
					+ ">\n"
					+ "<center><table>\n"
					+ "<tr><td><b>Nickname</b><td><input type=text size=20 name=nickname>\n"
					+ "<tr><td><b>Password</b><td><input type=text size=20 name=password>\n"
					+ "<tr><td><b>Rating</b><td><SELECT name=rating>\n"
					+ "<OPTION value=\"-1\">-1: Offtopic</OPTION>\n"
					+ "<OPTION selected value=\"0\">0: Not rated</OPTION>\n"
					+ "<OPTION value=\"1\">1: Interesting</OPTION>\n"
					+ "</SELECT></table><p><br>\n"
					+ "<input type=submit value=\"Moderate this comment now!\"></center><p>\n");
		}
		catch (Exception e2)
		{
			sp.printHTML("Exception moderating comments part 2: " + e2 + "<br>");
		} 

		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getOlderStoriesResult(ServletPrinter sp, String day,
			String month, String year, int page, int nbOfStories, Date dfrom,
			Date dto) {
		String id;
		CqlQuery<String, byte[], String> 				 query;
		QueryResult<OrderedRows<String, byte[], String>> result;

		Rows<String, byte[], String> rows;
		List<HColumn<byte[], String>> clist;
		String table;
		long from = dfrom.getTime();
		long to 	= dto.getTime(); 
		try
		{

			RangeSlicesQuery<String, byte[], String> rangeSlicesQuery =
					HFactory.createRangeSlicesQuery(myKeySpace, stringSerializer, bas, stringSerializer);
			rangeSlicesQuery.setColumnFamily("stories_time");
			rangeSlicesQuery.setRange(longSerializer.toBytes(from), longSerializer.toBytes(to), false, nbOfStories);

			QueryResult<OrderedRows<String, byte[], String>> results = rangeSlicesQuery.execute();
			rows = results.get();

			Row<String, byte[], String> row = rows.getByKey("date");

			clist = (row.getColumnSlice()).getColumns();
			table = "stories";

			if(clist.size() == 0)
			{	
				rangeSlicesQuery =
						HFactory.createRangeSlicesQuery(myKeySpace, stringSerializer, bas, stringSerializer);
				rangeSlicesQuery.setColumnFamily("old_stories_time");
				rangeSlicesQuery.setRange(longSerializer.toBytes(from), longSerializer.toBytes(to), false, nbOfStories);

				results = rangeSlicesQuery.execute();
				rows = results.get();

				row = rows.getByKey("date");

				clist = (row.getColumnSlice()).getColumns();
				table = "old_stories";
			}

			if(clist.size() == 0)
			{
				if (page == 0)
					sp
					.printHTML("<h2>Sorry, but there are no story available for this date !</h2>");
				else
				{
					sp
					.printHTML("<h2>Sorry, but there is no more stories available for this date.</h2><br>\n");
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
							+ "\">Previous page</a>\n</CENTER>\n");
				}
				sp.printHTMLfooter();
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}
		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting older stories: " + e + "<br>");
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true);
		}

		String title, date;
		// Print the story titles and author

		CqlQuery<String, String, byte[]> 				 tquery;
		QueryResult<CqlRows<String, String, byte[]>> tresult;
		try
		{
			for(int i = 0 ; i < clist.size(); i++)
			{

				Long ttime = new Long(longSerializer.fromBytes((clist.get(i)).getName()));
				String tcat = (clist.get(i)).getValue();

				Date tempDate = new Date(ttime.longValue());
				Date tcurrDate = new Date();

				tquery = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				tquery.setQuery("select * from " + table + " where key='"+tcat+"'");
				tresult = tquery.execute();

				ColumnSlice<String, byte[]> tcol = ((tresult.get()).getByKey(tcat)).getColumnSlice();
				Long lcat = new Long(longSerializer.fromBytes((tcol.getColumnByName("category")).getValue()));

				title = stringSerializer.fromBytes((tcol.getColumnByName("title")).getValue());
				Long writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));

				id = tcat;
				/*title = rs.getString("title");*/
				String username = getUserName(writer.toString());
				Date currDate = new Date(ttime.longValue());
				sp
				.printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
						+ id
						+ "\">"
						+ title
						+ "</a> by "
						+ username
						+ " on "
						+ currDate.toString()
						+ "<br>\n");
			}
		}
		catch (Exception e2)
		{
			sp.printHTML("Exception getting strings: " + e2 + "<br>");
		}
		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getRegisterUserResult(ServletPrinter sp,
			String nickname, String firstname, String lastname, String email,
			String password, int access, int rating, long en, long st) {
		Date date;
		CqlQuery<String, String, String>  query;
		QueryResult<OrderedRows<String, String, byte[]>> result;
		String id, creation_date;
		//System.out.println("**** 10oct2014 entering getRegisterUserResult method");
		try
		{
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, stringSerializer);
			query.setQuery("select * from users where nickname='" + nickname + "'");
		}
		catch (Exception e)
		{
			sp.printHTML("ERROR: Nickname query failed" + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}
		try
		{
			//System.out.println("**** 10oct2014 in  getRegisterUserResult method in 2nd try");
			QueryResult<CqlRows<String, String, String>> cresult = query.execute();
			CqlRows<String, String, String> crows = cresult.get();
			if(crows != null && crows.getCount() != 0)
			{
				sp.printHTML("The nickname you have chosen is already taken by someone else. Please choose a new nickname.<br>\n");
				//myCluster.getConnectionManager().shutdown();
				return new DbProcessResult(sp,true);
			}

			Mutator<String> mutator = HFactory.createMutator(myKeySpace, stringSerializer);	
			// Generate a UUID
			UUID id1 = UUID.randomUUID();
			System.out.println("Adding row for "+ id1.toString());
			mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("firstname",firstname));
			mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("lastname",lastname));
			mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("nickname",nickname));
			mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("password",password));
			mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("email",email));
			mutator.addInsertion(id1.toString(), "users", HFactory.createColumn("rating", new Long(0), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), "users", HFactory.createColumn("access", new Long(0), stringSerializer, longSerializer));
			date = new Date();
			//mutator.addInsertion(id1.toString(), "users", HFactory.createStringColumn("date",date.toString()));
			//mutator.addInsertion(id1.toString(), "users", HFactory.createColumn("date",date.getTime(), stringSerializer, longSerializer));
			mutator.execute();

			CqlQuery<String, String, byte[]> bquery = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			bquery.setQuery("select * from users where nickname='" + nickname + "'");

			QueryResult<CqlRows<String, String, byte[]>> bcresult = bquery.execute();
			CqlRows<String, String, byte[]> bcrows = bcresult.get();

			Row<String, String, byte[]> row = (bcrows.getList()).get(0);

			id = row.getKey();
			System.out.println("**** 10oct2014 rbefore cassandra jmx analysis id:="+id);
			JMXClientAnalysis.cassandraJMXAnalysis("read",id,en,st);
			ColumnSlice<String, byte[]> col = row.getColumnSlice();
			long ldate = longSerializer.fromBytes(col.getColumnByName("date").getValue());
			creation_date = (new Date(ldate)).toString();
			rating = (longSerializer.fromBytes(col.getColumnByName("rating").getValue())).intValue();
			access = (longSerializer.fromBytes(col.getColumnByName("access").getValue())).intValue();

		}
		catch (Exception e)
		{
			sp.printHTML("Exception registering user " + e + "<br>");
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}	   

		sp
		.printHTML("<h2>Your registration has been processed successfully</h2><br>\n");
		sp.printHTML("<h3>Welcome " + nickname + "</h3>\n");
		sp
		.printHTML("RUBBoS has stored the following information about you:<br>\n");
		sp.printHTML("First Name : " + firstname + "<br>\n");
		sp.printHTML("Last Name  : " + lastname + "<br>\n");
		sp.printHTML("Nick Name  : " + nickname + "<br>\n");
		sp.printHTML("Email      : " + email + "<br>\n");
		sp.printHTML("Password   : " + password + "<br>\n");
		sp
		.printHTML("<br>The following information has been automatically generated by RUBBoS:<br>\n");
		sp.printHTML("User id       :" + id + "<br>\n");
		sp.printHTML("Creation date :" + creation_date + "<br>\n");
		sp.printHTML("Rating        :" + rating + "<br>\n");
		sp.printHTML("Access        :" + access + "<br>\n");
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getRejectStoryResult(ServletPrinter sp,
			String storyId) {
		CqlQuery<String, String, byte[]> 				 query;
		QueryResult<OrderedRows<String, String, byte[]>> result;


		QueryResult<Rows<String, String, byte[]>> results;
		QueryResult<?> qr ;
		try
		{
			/*stmt = conn.prepareStatement("SELECT id FROM submissions WHERE id="
	          + storyId);
	      rs = stmt.executeQuery();*/
			MultigetSliceQuery<String, String, byte[]> multigetSlicesQuery =
					HFactory.createMultigetSliceQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			multigetSlicesQuery.setColumnFamily("submissions");
			multigetSlicesQuery.setColumnNames("title","date");       
			multigetSlicesQuery.setKeys(storyId);

			results = multigetSlicesQuery.execute();
			//qr = (QueryResult)results;
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for RejectStory: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true) ;
		}
		try
		{

			Rows<String,String,byte[]> rows = results.get();	
			if(rows.getCount() == 0)
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>\n");
				//myCluster.getConnectionManager().shutdown();
				return new DbProcessResult(sp, true) ;
			}	    	 

			Row<String, String, byte[]> row = rows.getByKey(storyId);

			HColumn<String, byte[]> dateColumn = (row.getColumnSlice()).getColumnByName("date");

			long categoryDate = longSerializer.fromBytes(dateColumn.getValue());

			Mutator<String> mutator = HFactory.createMutator(myKeySpace, 
					stringSerializer);
			mutator.delete(storyId, "submissions", null, stringSerializer);
			mutator.execute();

			Mutator<String> mutator1 = HFactory.createMutator(myKeySpace, stringSerializer);
			mutator1.delete("date", "submissions_time", categoryDate, longSerializer);
			mutator1.execute();

		}
		catch (Exception e)
		{
			sp.printHTML("Exception rejecting story: " + e + "<br>");	     
			return new DbProcessResult(sp, true) ;
		}
		return new DbProcessResult(sp, false) ;
	}
	public DbProcessResult getReviewStoriesResult(ServletPrinter sp) {
		String date;
		String title;
		String id;
		String body;
		long username;
		CqlQuery<String, byte[], String> 				 query;
		QueryResult<CqlRows<String, byte[], String>>     result;
		try
		{
			/*stmt = conn
	          .prepareStatement("SELECT * FROM submissions ORDER BY date DESC LIMIT 10");
	      rs = stmt.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, bas, stringSerializer);
			query.setQuery("select first 10 reversed  * from submissions_time");
			result = query.execute();
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for ReviewStories " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true);
		}

		try
		{
			CqlRows<String, byte[], String> crows = result.get();
			Row<String, byte[], String> row = crows.getByKey("date");

			if (row == null)
			{
				sp
				.printHTML("<h2>Sorry, but there is no submitted story available at this time.</h2><br>\n");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}

			List<HColumn<byte[], String>> clist = (row.getColumnSlice()).getColumns();

			if (clist.size() <= 1)
			{
				sp
				.printHTML("<h2>Sorry, but there is no submitted story available at this time.</h2><br>\n");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}

			sp.printHTML("size is " + clist.size());
			for(int i = 0 ; i < clist.size() - 1; i++)
			{
				Long ttime = new Long(longSerializer.fromBytes((clist.get(i)).getName()));
				String key = (clist.get(i)).getValue();

				CqlQuery<String, String, byte[]> 				 tquery;
				QueryResult<CqlRows<String, String, byte[]>>     tresult;
				tquery = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				tquery.setQuery("select * from submissions where key='"+key+"'");
				tresult = tquery.execute();

				// Not a good way to get the value as things can be null
				ColumnSlice<String, byte[]> tcol = ((tresult.get()).getByKey(key)).getColumnSlice();

				title = stringSerializer.fromBytes((tcol.getColumnByName("title")).getValue());
				body = stringSerializer.fromBytes((tcol.getColumnByName("body")).getValue());
				username = longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue());
				date = (new Date(ttime.longValue())).toString();
				id = key;

				sp.printHTML("<br><hr>\n");
				sp.printHTMLHighlighted(title);

				sp.printHTML("<B>Posted by " + username + " on " + date + "</B><br>\n");
				sp.printHTML(body);
				sp
				.printHTML("<br><p><center><B>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.AcceptStory?storyId="
						+ id
						+ "\">Accept</a> | <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.RejectStory?storyId="
						+ id + "\">Reject</a> ]</B><p>\n");
			}      
		}
		catch (Exception e)
		{
			sp.printHTML("Exception reviewing story: " + e + "<br>");	 
			return new DbProcessResult(sp, true);
		}
		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getStoreCommentResult(ServletPrinter sp,
			String storyId, String parent, String subject, String body,
			String comment_table, String userId){
		CqlQuery<String, String, byte[]> query;
		QueryResult<CqlRows<String, String, byte[]>> result;
		try
		{
			Mutator<String> mutator = HFactory.createMutator(myKeySpace, stringSerializer);	
			// Generate a UUID
			UUID id1 = UUID.randomUUID();
			//System.out.println("Adding row for "+ id1.toString());
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createStringColumn("subject",subject));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createStringColumn("comment",body));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("writer", new Long(userId), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("story_id", new Long(storyId), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("parent", new Long(parent), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("childs", new Long(0), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("ratings", new Long(0), stringSerializer, longSerializer));
			Date date = new Date();
			mutator.addInsertion(id1.toString(), comment_table, HFactory.createColumn("date",new Long(date.getTime()), stringSerializer, longSerializer));
			mutator.execute();

			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			query.setQuery("select childs from "+comment_table+" where key='"+parent+"'");

			result = query.execute();
			CqlRows<String, String, byte[]> crows = result.get() ;

			Row<String, String, byte[]> row = (crows.getList()).get(0);        

			ColumnSlice<String, byte[]> tcol = row.getColumnSlice();

			long c = longSerializer.fromBytes(tcol.getColumnByName("childs").getValue());
			c++;

			query.setQuery("update "+comment_table+" set childs='"+c+"' where key='"+parent+"'");
			query.execute();
			/*stmt = conn.prepareStatement("INSERT INTO " + comment_table
	          + " VALUES (NULL, " + userId + ", " + storyId + ", " + parent
	          + ", 0, 0, NOW(), \"" + subject + "\", \"" + body + "\")");
	      updateResult = stmt.executeUpdate();

	      stmt.close();

	      stmt = conn.prepareStatement("UPDATE " + comment_table
	          + " SET childs=childs+1 WHERE id=" + parent);
	      updateResult = stmt.executeUpdate();*/
		}
		catch (Exception e)
		{
			//closeConnection(stmt, conn);
			sp.printHTML("Exception gstoring categories: " + e + "<br>");
			return new DbProcessResult(sp,true);
		}
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getStoreModLogResult(ServletPrinter sp,
			String nickname, String password, String comment_table,
			String commentId, int access, String userId, int rating) {
		CqlQuery<String, String, byte[]> query;
		QueryResult<CqlRows<String, String, byte[]>> result;
		if ((nickname != null) && (password != null))
		{
			try
			{
				/*stmt = conn
	            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
	                + nickname + "\" AND password=\"" + password + "\"");
	        rs = stmt.executeQuery();*/
				query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				query.setQuery("select access from users where nickname='"+nickname+"' and password='"+password+"'");
				result = query.execute();
			}
			catch (Exception e)
			{
				sp.printHTML("Failed to execute Query for BrowseStoriesByCategory1: "
						+ e);
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}

			try
			{
				if (result != null && result.get() != null)
				{
					CqlRows<String, String, byte[]> crows = (CqlRows<String, String, byte[]> )result.get();
					Row<String, String, byte[]> row = (crows.getList()).get(0);

					ColumnSlice<String, byte[]> tcol = row.getColumnSlice();

					Long laccess     = new Long(longSerializer.fromBytes((tcol.getColumnByName("access")).getValue()));
					String strKey    = row.getKey();

					userId = strKey;
					access = laccess.intValue();
				}
				//stmt.close();
			}
			catch (Exception e)
			{
				sp.printHTML("Exception StoreModeratorLog: " + e + "<br>");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}
		}

		if (("".equals(userId)) || (access == 0))
		{
			sp.printHTMLheader("RUBBoS: Moderation");
			sp
			.printHTML("<p><center><h2>Sorry, but this feature is only accessible by users with an author access.</h2></center><p>\n");
		}
		else
		{
			sp.printHTMLheader("RUBBoS: Comment moderation result");
			sp.printHTML("<center><h2>Comment moderation result:</h2></center><p>\n");

			try
			{
				/*stmt = conn.prepareStatement("SELECT writer,rating FROM "
	            + comment_table + " WHERE id=" + commentId);
	        rs = stmt.executeQuery();

	        if (!rs.first())
	        {
	          sp
	              .printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");
	        }*/
				query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				query.setQuery("select writer,rating from "+comment_table+" where key='"+commentId+"'");
				result = query.execute();

				CqlRows<String, String, byte[]> crows = (CqlRows<String, String, byte[]> )result.get();
				Row<String, String, byte[]> row = (crows.getList()).get(0);

				ColumnSlice<String, byte[]> tcol = row.getColumnSlice();

				Long lwriter = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
				Long lrating = new Long(longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue()));

				int rsrating = lrating.intValue();
				String writer = lwriter.toString();

				//stmt.close();

				if (((rsrating == -1) && (rating == -1))
						|| ((rsrating == 5) && (rating == 1)))
					sp
					.printHTML("Comment rating is already to its maximum, updating only user's rating.");
				else
				{
					// Update ratings
					if (rating != 0)
					{
						query.setQuery("select rating from users where key='"+writer+"'");

						result = query.execute();
						crows = result.get() ;

						row = (crows.getList()).get(0);        

						tcol = row.getColumnSlice();

						long r = longSerializer.fromBytes(tcol.getColumnByName("rating").getValue());
						r += rating;

						query.setQuery("update users set rating='"+r+"' where key='"+writer+"'");
						query.execute();

						/*stmt = conn.prepareStatement("UPDATE users SET rating=rating+"
	                + rating + " WHERE id=" + writer);
	            updateResult = stmt.executeUpdate();
		        stmt.close();*/

						query.setQuery("update "+comment_table+" set rating='"+r+"' where key='"+commentId+"'");
						query.execute();
						/*stmt = conn.prepareStatement("UPDATE " + comment_table
	                + " SET rating=rating+" + rating + " WHERE id=" + commentId);
	            updateResult = stmt.executeUpdate();
		    	stmt.close();*/
					}
				}

				query.setQuery("select rating from "+comment_table+" where key='"+commentId+"'");
				result =  query.execute();
				/*stmt = conn.prepareStatement("SELECT rating FROM " + comment_table
	            + " WHERE id=" + commentId);
	        rs = stmt.executeQuery();*/
				String user_row_rating = null, comment_row_rating = null;

				result = query.execute();
				if(result != null && result.get() != null)
				{
					crows = (CqlRows<String, String, byte[]> )result.get();

					row = (crows.getList()).get(0);

					tcol = row.getColumnSlice();

					lrating = longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue());

					comment_row_rating = lrating.toString();
				}

				query.setQuery("select rating from users where key='"+writer+"'");
				result =  query.execute();

				if(result != null && result.get() != null)
				{
					crows = (CqlRows<String, String, byte[]> )result.get();

					row = (crows.getList()).get(0);

					tcol = row.getColumnSlice();

					lrating = new Long(longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue()));

					user_row_rating = lrating.toString();
				}

				else
					sp
					.printHTML("<h3>ERROR: Sorry, but this user does not exist.</h3><br>\n");

				Mutator<String> mutator = HFactory.createMutator(myKeySpace, stringSerializer);	
				// Generate a UUID
				UUID id1 = UUID.randomUUID();
				//System.out.println("Adding row for "+ id1.toString());
				mutator.addInsertion(id1.toString(), "moderator_log", HFactory.createColumn("moderator_id", new Long(userId), stringSerializer, longSerializer));
				mutator.addInsertion(id1.toString(), "moderator_log", HFactory.createColumn("comment_id", new Long(commentId), stringSerializer, longSerializer));
				mutator.addInsertion(id1.toString(), "moderator_log", HFactory.createColumn("rating", new Long(rating), stringSerializer, longSerializer));
				Date date = new Date();
				mutator.addInsertion(id1.toString(), "moderator_log", HFactory.createColumn("date",new Long(date.getTime()), stringSerializer, longSerializer));
				mutator.execute();

				/*stmt = conn
	            .prepareStatement("INSERT INTO moderator_log VALUES (NULL, "
	                + userId + ", " + commentId + ", " + rating + ", NOW())");
	        updateResult = stmt.executeUpdate();*/

				sp.printHTML("New comment rating is :" + comment_row_rating + "<br>\n");
				sp.printHTML("New user rating is :" + user_row_rating + "<br>\n");
				sp
				.printHTML("<center><h2>Your moderation has been successfully stored.</h2></center>\n");

			}
			catch (Exception e3)
			{
				sp.printHTML("Exception StoreModeratorLog stmts: " + e3 + "<br>");
				return new DbProcessResult(sp,true);
			}

		}
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getStoreStoryResult(ServletPrinter sp,
			 String nickname, String title, String body,
			String category, String password, String userId,
			int access) {
		Date date;
		String table;

		CqlQuery<String, String, byte[]> 				 query;
		QueryResult<OrderedRows<String, String, byte[]>> result;		


		if ((nickname != null) && (password != null))
		{      
			try
			{
				/*stmt = conn
	            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
	                + nickname + "\" AND password=\"" + password + "\"");
	        rs = stmt.executeQuery();*/
				query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				query.setQuery("select * from users where nickname='" + nickname + "' and password='" + password + "'");

			}
			catch (Exception e)
			{
				sp.printHTML("ERROR: Authentification query failed" + e);	
				return new DbProcessResult(sp,true);
			}
			try
			{
				QueryResult<CqlRows<String, String, byte[]>> cresult = query.execute();
				CqlRows<String, String, byte[]> crows = cresult.get();

				if(crows == null || crows.getCount() == 0)
				{
					sp.printHTML("Authentication failure\n");
					//access.myCluster.getConnectionManager().shutdown();
					return new DbProcessResult(sp,true);
				}

				Row<String, String, byte[]> row = (crows.getList()).get(0);
				userId = row.getKey().toString();				
				userId = ((String)row.getKey());



				HColumn<String, byte[]> col = row.getColumnSlice().getColumnByName("access"); 
				Long laccess = new Long(longSerializer.fromBytes(col.getValue()));		
				access = laccess.intValue();
				/*if (rs.first())
	        {
	          userId = rs.getInt("id");
	          access = rs.getInt("access");
	        }
			stmt.close();*/
			}
			catch (Exception e)
			{
				sp.printHTML("Exception storing story " + e + "<br>");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}
		}

		table = "submissions";
		if (userId == null || "".equals(userId))
			sp.printHTML("Story stored by the 'Anonymous Coward'<br>\n");
		else
		{
			if (access == 0)
				sp.printHTML("Story submitted by regular user " + userId + "<br>\n");
			else
			{
				sp.printHTML("Story posted by author " + userId + "<br>\n");
				table = "stories";
			}
		}

		// Add story to database

		try
		{
			Mutator<String> mutator = HFactory.createMutator(myKeySpace, stringSerializer);	
			// Generate a UUID
			UUID id1 = UUID.randomUUID();

			mutator.addInsertion(id1.toString(), table, HFactory.createStringColumn("title",title));
			mutator.addInsertion(id1.toString(), table, HFactory.createStringColumn("body",body));	
			//mutator.addInsertion(id1.toString(), table, HFactory.createStringColumn("writer",struserId));
			mutator.addInsertion(id1.toString(), table, HFactory.createColumn("writer",new Long(userId), stringSerializer, longSerializer));
			//mutator.addInsertion(id1.toString(), table, HFactory.createColumn("access", new Long(0), stringSerializer, longSerializer));
			date = new Date();	    	
			mutator.addInsertion(id1.toString(), table, HFactory.createColumn("date",new Long(date.getTime()), stringSerializer, longSerializer));
			mutator.addInsertion(id1.toString(), table, HFactory.createColumn("category",new Long(category), stringSerializer, longSerializer));
			mutator.execute();

		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for StoreStory: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		sp.printHTML("Your story has been successfully stored in the " + table
				+ " database table<br>\n");
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getStoriesOfTheDayResult(ServletPrinter sp,
			int bodySizeLimit) {
		CqlQuery<String, byte[], String> 				 query;
		QueryResult<OrderedRows<String, byte[], String>> result;
		try
		{
			/*stmt = conn
	          .prepareStatement("SELECT * FROM stories ORDER BY date DESC LIMIT 10");
	      rs = stmt.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, bas, stringSerializer);
			query.setQuery("select first 10 reversed  * from stories_time");
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for stories of the day: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}
		try
		{
			QueryResult<CqlRows<String, byte[], String>> cresult = query.execute();
			CqlRows<String, byte[], String> crows = cresult.get();

			if (crows == null)
			{
				sp
				.printHTML("<h2>Sorry, but there is no story available at this time.</h2><br>\n");

				return new DbProcessResult(sp,true);
			}

			Row<String, byte[], String> row = crows.getByKey("date");
			List<HColumn<byte[], String>> clist = (row.getColumnSlice()).getColumns();

			if (clist.size() == 1)
			{
				sp
				.printHTML("<h2>Sorry, but there is no story available at this time.</h2><br>\n");

				return new DbProcessResult(sp,true);
			}



			for(int i = 0 ; i < clist.size() -1 ; i++)
			{
				String storyId;
				String storyTitle;
				int writerId;
				String userName = "prem";
				String date;
				String body;

				Long ttime = new Long(longSerializer.fromBytes((clist.get(i)).getName()));
				String tkey = (clist.get(i)).getValue();

				CqlQuery<String, String, byte[]> 				 tquery;
				QueryResult<CqlRows<String, String, byte[]>>     tresult;

				tquery = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				tquery.setQuery("select * from stories where key='"+tkey+"'");
				tresult = tquery.execute();

				// Not a good way to get the value as things can be null
				ColumnSlice<String, byte[]> tcol = ((tresult.get()).getByKey(tkey)).getColumnSlice();

				sp.printHTML("<br><hr>\n");
				storyId = tkey;        

				//storyTitle = rs.getString("title");
				storyTitle = stringSerializer.fromBytes((tcol.getColumnByName("title")).getValue());
				sp
				.printHTMLHighlighted("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
						+ storyId + "\">" + storyTitle + "</a>");

				//writerId = rs.getInt("writer");
				Long writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
				writerId = writer.intValue();

				userName = getUserName(""+writerId);

				Date formatDate = new Date(ttime.longValue());

				sp.printHTML("<B>Posted by " + userName + " on " + formatDate.toString() + "</B><br>\n");
				//body = rs.getString("body");
				body = stringSerializer.fromBytes((tcol.getColumnByName("body")).getValue());
				if (body.length() > bodySizeLimit)
				{
					sp.printHTML(body.substring(0, bodySizeLimit));
					sp.printHTML("<br><B>...</B>");
				}
				else
					sp.printHTML(body);
				sp.printHTML("<br>\n");
			}
			//while (rs.next());
		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting stories of the day: " + e + " " + e.toString() +"<br>");
			e.printStackTrace();
		}
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getSubmitStoryResult(ServletPrinter sp) {
		String userid;
		String name;
		CqlQuery<String, String, String> 				 query;
		QueryResult<OrderedRows<String, String, String>> result;


		try
		{
			/*stmt = conn.prepareStatement("SELECT * FROM categories");
	      rs = stmt.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, stringSerializer);
			query.setQuery("select * from categories");
		}
		catch (Exception e)
		{
			sp.printHTML(" Failed to execute Query for SubmitStory: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		try
		{
			/*if (!rs.first())
	      {
	        sp
	            .printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>");
	        closeConnection(stmt, conn);
	        return;
	      }*/
			QueryResult<CqlRows<String, String, String>> cresult = query.execute();
			CqlRows<String, String, String> crows = cresult.get();
			if(crows == null || crows.getCount() == 0)
			{
				System.out.println("<h3>ERROR: Sorry, but this story does not exist.</h3><br>");
				//myCluster.getConnectionManager().shutdown(); //todo ask Anuprem
				return new DbProcessResult(sp,true);
			}

			for(int i = 0; i < crows.getCount(); i++)
			{
				Row<String, String, String> row = (crows.getList()).get(i);
				userid = row.getKey();
				ColumnSlice <String, String> cs = row.getColumnSlice();
				HColumn<String, String> c  = cs.getColumnByName("name");
				name = c.getValue();


				String Name;
				int Id;

				//Name = rs.getString("name");
				//Id = rs.getInt("id");
				sp.printHTML("<OPTION value=\"" + userid + "\">" + name + "</OPTION>\n");

			}
		}
		catch (Exception e)
		{
			sp.printHTML("Exception accepting stories: " + e + "<br>");
		}
		return new DbProcessResult(sp,false);

	}
	public void viewCommentDisplayFollowUp(String cid, int level, int display,
			int filter, String comment_table, boolean separator,
			ServletPrinter sp) throws Exception
			{
		ResultSet         follow;
		int                rating, i;
		String            subject, username, date, comment, id;
		Long 			  parent, story_id, childs;
		PreparedStatement stmtfollow = null;
		CqlQuery<String, String, byte[]> 				 query;
		QueryResult<CqlRows<String, String, byte[]>>	 result;

		try
		{
			/*stmtfollow = conn.prepareStatement("SELECT * FROM " + comment_table
+ " WHERE parent=" + cid);
//+" AND rating>="+filter);
follow = stmtfollow.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			query.setQuery("SELECT * FROM "+ comment_table + " WHERE parent='" + cid+"'");
			result = query.execute();
			CqlRows<String, String, byte[]> crows ;
			if(result != null)
			{
				crows = result.get();
				if(crows != null)
				{    		 
					List<Row<String, String, byte[]>> rowList = crows.getList();     
					for(int k = 0 ; k < crows.getCount(); k++)
					{
						/*story_id = follow.getInt("story_id");
id = follow.getInt("id");
subject = follow.getString("subject");
username = sp.getUserName(follow.getInt("writer"), conn);
date = follow.getString("date");
rating = follow.getInt("rating");
parent = follow.getInt("parent");
comment = follow.getString("comment");
childs = follow.getInt("childs");*/

						ColumnSlice<String, byte[]> tcol = (rowList.get(k)).getColumnSlice();
						Long ldate = new Long(longSerializer.fromBytes((tcol.getColumnByName("date")).getValue()));
						story_id = new Long(longSerializer.fromBytes((tcol.getColumnByName("story_id")).getValue()));
						subject = stringSerializer.fromBytes((tcol.getColumnByName("subject")).getValue());
						comment = stringSerializer.fromBytes((tcol.getColumnByName("comment")).getValue());
						Long writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
						Long lrating = new Long(longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue()));
						parent = new Long(longSerializer.fromBytes((tcol.getColumnByName("parent")).getValue()));
						childs = new Long(longSerializer.fromBytes((tcol.getColumnByName("childs")).getValue()));
						id = (rowList.get(k)).getKey();

						date = ldate.toString();
						username =getUserName(writer.toString());
						rating = lrating.intValue();

						if (rating >= filter)
						{
							if (!separator)
							{
								sp.printHTML("<br><hr><br>");
								separator = true;
							}
							if (display == 1) // Preview nested comments
							{
								for (i = 0; i < level; i++)
									sp.printHTML(" &nbsp &nbsp &nbsp ");
								sp
								.printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
										+ comment_table
										+ "&storyId="
										+ story_id
										+ "&commentId="
										+ id
										+ "&filter="
										+ filter
										+ "&display="
										+ display
										+ "\">"
										+ subject
										+ "</a> by "
										+ username
										+ " on "
										+ date
										+ "<br>\n");
							}
							else
							{
								sp.printHTML("<TABLE bgcolor=\"#CCCCFF\"><TR>");
								for (i = 0; i < level; i++)
									sp.printHTML("<TD>&nbsp&nbsp&nbsp");
								sp
								.printHTML("<TD><FONT size=\"4\" color=\"#000000\"><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
										+ comment_table
										+ "&storyId="
										+ story_id
										+ "&commentId="
										+ id
										+ "&filter="
										+ filter
										+ "&display="
										+ display
										+ "\">"
										+ subject
										+ "</a></B>&nbsp</FONT> (Score:"
										+ rating
										+ ")</TABLE>\n");
								sp.printHTML("<TABLE>");
								for (i = 0; i < level; i++)
									sp.printHTML("<TD>&nbsp&nbsp&nbsp");
								sp.printHTML("<TD><B>Posted by " + username + " on " + date
										+ "</B><p><TR>\n");
								for (i = 0; i < level; i++)
									sp.printHTML("<TD>&nbsp&nbsp&nbsp");
								sp.printHTML("<TD>" + comment + "<TR>");
								for (i = 0; i < level; i++)
									sp.printHTML("<TD>&nbsp&nbsp&nbsp");
								sp
								.printHTML("<TD><p>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
										+ comment_table
										+ "&storyId="
										+ story_id
										+ "&parent="
										+ id
										+ "\">Reply to this</a>"
										+ "&nbsp|&nbsp<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
										+ comment_table
										+ "&storyId="
										+ story_id
										+ "&commentId="
										+ parent
										+ "&filter="
										+ filter
										+ "&display="
										+ display
										+ "\">Parent</a>&nbsp|&nbsp<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ModerateComment?comment_table="
										+ comment_table
										+ "&commentId="
										+ id
										+ "\">Moderate</a> ]</TABLE><br>");
							}
						}
						if (childs > 0)
							viewCommentDisplayFollowUp(id, level + 1, display, filter, 
									comment_table, separator, sp);
					}
				}
			}
		}
		catch (Exception e)
		{
			sp.printHTML("Failure at display_follow_up: " + e);      
			try 
			{
				stmtfollow.close();
			} 
			catch (Exception ignore) 
			{
			}
			throw e;
		}
		stmtfollow.close();
			}
	public DbProcessResult getViewCommentResult(ServletPrinter sp,
			String comment, String storyId, String comment_table,
			String parent, int filter, int display, String commentId){
		CqlQuery<String, String, byte[]> query;
		QueryResult<CqlRows<String, String, byte[]>> result;
		if (commentId == null || "".equals(commentId))
			parent = "";
		else
		{
			try
			{
				query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
				query.setQuery("SELECT parent FROM "+comment_table+" WHERE key='"+ commentId + "'");
				result = query.execute();

				/*stmt = conn.prepareStatement("SELECT parent FROM " + comment_table
	            + " WHERE id=" + commentId);
	        rs = stmt.executeQuery();*/
				CqlRows<String, String, byte[]> crows = result.get();

				if (crows.getCount() == 0)
				{
					sp
					.printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");
					//closeConnection(stmt, conn);
					return new DbProcessResult(sp,true);
				}
				//parent = rs.getInt("parent");
				//stmt.close();       
				Row<String, String, byte[]> row = (crows.getList()).get(0);        

				ColumnSlice<String, byte[]> tcol = row.getColumnSlice();

				Long c = longSerializer.fromBytes(tcol.getColumnByName("parent").getValue());
				parent = c.toString();
			}
			catch (Exception e)
			{
				sp.printHTML("Failure at 'SELECT parent' stmt: " + e);
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}
		}

		sp.printHTMLheader("RUBBoS: Viewing comments");
		sp
		.printHTML("<center><form action=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment\" method=POST>\n"
				+ "<input type=hidden name=commentId value="
				+ commentId
				+ ">\n"
				+ "<input type=hidden name=storyId value="
				+ storyId
				+ ">\n"
				+ "<input type=hidden name=comment_table value="
				+ comment_table
				+ ">\n" + "<B>Filter :</B>&nbsp&nbsp<SELECT name=filter>\n");

		/*try
	    {
	      stmt = conn
	          .prepareStatement("SELECT rating, COUNT(rating) AS count FROM "
	              + comment_table + " WHERE story_id=" + storyId
	              + " GROUP BY rating ORDER BY rating");
	      rs = stmt.executeQuery();

	      i = -1;
	      if (rs.first())
	      {
	        do
	        {
	          rating = rs.getInt("rating");
	          count = rs.getInt("count");
	          while ((i < 6) && (rating != i))
	          {
	            if (i == filter)
	              sp.printHTML("<OPTION selected value=\"" + i + "\">" + i
	                  + ": 0 comment</OPTION>\n");
	            else
	              sp.printHTML("<OPTION value=\"" + i + "\">" + i
	                  + ": 0 comment</OPTION>\n");
	            i++;
	          }
	          if (rating == i)
	          {
	            if (i == filter)
	              sp.printHTML("<OPTION selected value=\"" + i + "\">" + i + ": "
	                  + count + " comments</OPTION>\n");
	            else
	              sp.printHTML("<OPTION value=\"" + i + "\">" + i + ": " + count
	                  + " comments</OPTION>\n");
	            i++;
	          }
	        }
	        while (rs.next());
	      }
	      stmt.close();
	    }
	    catch (Exception e)
	    {
	      sp.printHTML("Failed to execute Query for View Comment: " + e);
	      closeConnection(stmt, conn);
	      return;
	    }*/
		int i=0;
		while (i < 6)
		{
			sp.printHTML("<OPTION value=\"" + i + "\">" + i
					+ ": 0 comment</OPTION>\n");
			i++;
		}

		sp.printHTML("</SELECT>&nbsp&nbsp&nbsp&nbsp<SELECT name=display>\n"
				+ "<OPTION value=\"0\">Main threads</OPTION>\n");
		if (display == 1)
			sp.printHTML("<OPTION selected value=\"1\">Nested</OPTION>\n");
		else
			sp.printHTML("<OPTION value=\"1\">Nested</OPTION>\n");
		if (display == 2)
			sp.printHTML("<OPTION selected value=\"2\">All comments</OPTION>\n");
		else
			sp.printHTML("<OPTION value=\"2\">All comments</OPTION>\n");
		sp
		.printHTML("</SELECT>&nbsp&nbsp&nbsp&nbsp<input type=submit value=\"Refresh display\"></center><p>\n");

		String subject, date;
		int id;
		boolean separator;
		try
		{
			/*stmt = conn.prepareStatement("SELECT * FROM " + comment_table
	          + " WHERE story_id=" + storyId + " AND parent=0"); //+ parent+
	      //" AND rating>="+filter);
	      rs = stmt.executeQuery();*/
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			query.setQuery("SELECT * FROM " + comment_table+ " WHERE story_id='" + storyId + "' AND parent='0'"); 

			result = query.execute();
			CqlRows<String, String, byte[]> crows = null;

			if(result != null)
			{
				crows = result.get();
				if(crows != null)
				{
					sp.printHTML("canuprem: num rows 2 " + crows.getCount());
				}
				else
				{
					sp.printHTML("No Comments");
					System.out.println("1crows is null but results is nto");
					return new DbProcessResult(sp,true);
				}
			}
			else
			{
				sp.printHTML("1No Comments");
				System.out.println("1No comments for this story");
				return new DbProcessResult(sp,true);
			}
			String username;
			int childs=0,rating=0;
			if (crows.getCount() != 0)
			{
				List<Row<String, String, byte[]>> rowList = crows.getList();
				for(int k = 0; k < crows.getCount() ; k++)
				{
					ColumnSlice<String, byte[]> tcol = (rowList.get(k)).getColumnSlice();
					Long writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
					username = getUserName(writer.toString());
					rating = (new Long(longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue()))).intValue();
					parent = (longSerializer.fromBytes((tcol.getColumnByName("parent")).getValue())).toString();
					//id = rs.getInt("id");
					subject = stringSerializer.fromBytes((tcol.getColumnByName("subject")).getValue());
					Long ldate = new Long(longSerializer.fromBytes((tcol.getColumnByName("parent")).getValue()));
					date = ldate.toString();
					childs = (new Long(longSerializer.fromBytes((tcol.getColumnByName("childs")).getValue()))).intValue();
					comment = stringSerializer.fromBytes((tcol.getColumnByName("comment")).getValue());
					separator = false;

					String strId   = (rowList.get(k)).getKey();

					sp.printHTML("obtained all the atributes");
					if (rating >= filter)
					{
						sp.printHTML("<br><hr><br>");
						separator = true;
						sp
						.printHTML("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\"><TR><TD><FONT size=\"4\" color=\"#000000\"><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
								+ comment_table
								+ "&storyId="
								+ storyId
								+ "&commentId="
								+ strId
								+ "&filter="
								+ filter
								+ "&display="
								+ display
								+ "\">"
								+ subject
								+ "</a></B>&nbsp</FONT> (Score:"
								+ rating
								+ ")</TABLE>\n");
						sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
								+ date + "</B><p>\n");
						sp.printHTML("<TR><TD>" + comment);
						sp
						.printHTML("<TR><TD><p>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
								+ comment_table
								+ "&storyId="
								+ storyId
								+ "&parent="
								+ strId
								+ "\">Reply to this</a>&nbsp|&nbsp"
								+ "<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
								+ comment_table
								+ "&storyId="
								+ storyId
								+ "&commentId="
								+ parent
								+ "&filter="
								+ filter
								+ "&display="
								+ display
								+ "\">Parent</a>"
								+ "&nbsp|&nbsp<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ModerateComment?comment_table="
								+ comment_table
								+ "&commentId="
								+ strId
								+ "\">Moderate</a> ]</TABLE>\n");
					}
					if ((display > 0) && (childs > 0))
						viewCommentDisplayFollowUp(strId, 1, display, filter,  comment_table,
								separator, sp);
				}
			}

		}
		catch (Exception e)
		{
			//closeConnection(stmt, conn);
			sp.printHTML("Exception getting what categories: " + e + "<br>");
			return new DbProcessResult(sp,true);
		}
		return new DbProcessResult(sp,false);
	}

	public void viewStoryDisplayFollowUp(String cid, int level, int display,
			int filter, String comment_table, ServletPrinter sp)
					throws Exception 
					{
		int 			  i;
		Long              childs, story_id, writer;
		String            subject, id;
		Long 			  date;
		ResultSet         rs;
		PreparedStatement stmt = null;

		CqlQuery<String, String, byte[]> 				 query;
		QueryResult<CqlRows<String, String, byte[]>> result;

		try
		{
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			query.setQuery("SELECT * FROM "
					+ comment_table + " WHERE parent='" + cid+"'");
			result = query.execute();
			CqlRows<String, String, byte[]> crows ;

			/*stmt = conn
.prepareStatement("SELECT id,subject,writer,date,story_id,childs FROM "
+ comment_table + " WHERE parent=" + cid);
rs = stmt.executeQuery();*/

			if(result != null)
			{
				crows = result.get();
				if(crows != null)
				{

					List<Row<String, String, byte[]>> rowList = crows.getList();

					for(int k = 0 ; k < crows.getCount(); k++)
					{
						for (i = 0; i < level; i++)
							sp.printHTML("&nbsp&nbsp&nbsp");

						ColumnSlice<String, byte[]> tcol = (rowList.get(k)).getColumnSlice();
						date = new Long(longSerializer.fromBytes((tcol.getColumnByName("date")).getValue()));
						story_id = new Long(longSerializer.fromBytes((tcol.getColumnByName("story_id")).getValue()));
						subject = stringSerializer.fromBytes((tcol.getColumnByName("subject")).getValue());
						writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
						childs = new Long(longSerializer.fromBytes((tcol.getColumnByName("childs")).getValue()));
						id = (rowList.get(k)).getKey();

						/*date = rs.getString("date");
story_id = rs.getInt("story_id");
id = rs.getInt("id");
subject = rs.getString("subject");
writer = rs.getInt("writer");
childs = rs.getInt("childs");*/

						sp
						.printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
								+ comment_table
								+ "&storyId="
								+ story_id
								+ "&commentId="
								+ id
								+ "&filter="
								+ filter
								+ "&display="
								+ display
								+ "\">"
								+ subject + "</a> by " + writer + " on " + date + "<br>\n");
						if (childs > 0)
							viewStoryDisplayFollowUp(id, level + 1, display, filter,
									comment_table, sp);
					}
				}
			}

		}
		catch (Exception e3)
		{
			sp.printHTML(e3 + ": Exception in method display_follow_up");
			e3.printStackTrace();
			try 
			{
				//stmt.close();
			} 
			catch (Exception ignore) 
			{
			}      
			throw e3;
		}

		//stmt.close();

					}
	public DbProcessResult getViewStoryResult(ServletPrinter sp, String title,
			String body, String username, String storyId, String comment_table) {
		CqlQuery<String, String, byte[]> 				 query;
		QueryResult<CqlRows<String, String, byte[]>> result;
		long date;
		try
		{
			query = new CqlQuery(myKeySpace, stringSerializer, stringSerializer, bas);
			query.setQuery("SELECT * FROM stories WHERE key='"+ storyId + "'");
			result = query.execute();
		}

		catch (Exception e)
		{
			sp.printHTML("ERROR: ViewStory query failed" + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		try
		{
			CqlRows<String, String, byte[]> crows = result.get();
			if (crows.getCount() == 0)
			{
				query.setQuery("SELECT * FROM old_stories WHERE key='" + storyId + "'");
				result = query.execute();

				comment_table = "old_comments";

				crows = result.get();
			}
			else
			{
				comment_table = "comments";
			}

			if (crows.getCount() == 0)
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>\n");
				return new DbProcessResult(sp,true);
			}

			ColumnSlice<String, byte[]> tcol = ((result.get()).getByKey(storyId)).getColumnSlice();
			Long   writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
			date   = new Long(longSerializer.fromBytes((tcol.getColumnByName("date")).getValue()));
			title  = stringSerializer.fromBytes((tcol.getColumnByName("title")).getValue());
			body   = stringSerializer.fromBytes((tcol.getColumnByName("body")).getValue());

			username =getUserName(writer.toString());
			/*date = rs.getString("date");
	      title = rs.getString("title");
	      body = rs.getString("body");*/
		}

		catch (Exception e)
		{
			sp.printHTML("Exception viewing story " + e + "<br>");

			return new DbProcessResult(sp,true);
		}

		sp.printHTMLheader("RUBBoS: Viewing story " + title);
		sp.printHTMLHighlighted(title);
		sp.printHTML("Posted by " + username + " on " + date + "<br>\n");
		sp.printHTML(body + "<br>\n");
		sp
		.printHTML("<p><center><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
				+ comment_table
				+ "&storyId="
				+ storyId
				+ "&parent=0\">Post a comment on this story</a></center><p>");

		// Display filter chooser header
		sp.printHTML("<br><hr><br>");
		sp
		.printHTML("<center><form action=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment\" method=POST>\n"
				+ "<input type=hidden name=commentId value=0>\n"
				+ "<input type=hidden name=storyId value="
				+ storyId
				+ ">\n"
				+ "<input type=hidden name=comment_table value="
				+ comment_table
				+ ">\n" + "<B>Filter :</B>&nbsp&nbsp<SELECT name=filter>\n");

		int i = -1;
		String count;
		int filter=0, display;

		/*try
	    {
	      stmt = conn
	          .prepareStatement("SELECT rating, COUNT(rating) AS count FROM "
	              + comment_table + " WHERE story_id=" + storyId
	              + " GROUP BY rating ORDER BY rating");
	      count_result = stmt.executeQuery();

	      while (count_result.next())
	      {
	        rating = count_result.getInt("rating");
	        count = count_result.getString("count");
	        filter = 0;
	        while ((i < 6) && (rating != i))
	        {
	          if (i == filter)
	            sp.printHTML("<OPTION selected value=\"" + i + "\">" + i
	                + ": 0 comment</OPTION>\n");
	          else
	            sp.printHTML("<OPTION value=\"" + i + "\">" + i
	                + ": 0 comment</OPTION>\n");
	          i++;
	        }
	        if (rating == i)
	        {
	          if (i == filter)
	            sp.printHTML("<OPTION selected value=\"" + i + "\">" + i + ": "
	                + count + " comments</OPTION>\n");
	          else
	            sp.printHTML("<OPTION value=\"" + i + "\">" + i + ": " + count
	                + " comments</OPTION>\n");
	          i++;
	        }
	      }
	      stmt.close();
	    }
	    catch (Exception e2)
	    {
	      sp.printHTML("count_result failed " + e2 + "<br>");
	    }*/

		while (i < 6)
		{
			sp.printHTML("<OPTION value=\"" + i + "\">" + i
					+ ": 0 comment</OPTION>\n");
			i++;
		}

		sp
		.printHTML("</SELECT>&nbsp&nbsp&nbsp&nbsp<SELECT name=display>\n"
				+ "<OPTION value=\"0\">Main threads</OPTION>\n"
				+ "<OPTION selected value=\"1\">Nested</OPTION>\n"
				+ "<OPTION value=\"2\">All comments</OPTION>\n"
				+ "</SELECT>&nbsp&nbsp&nbsp&nbsp<input type=submit value=\"Refresh display\"></center><p>\n");
		display = 1;
		filter = 0;

		try
		{
			query.setQuery("SELECT * FROM " + comment_table 
					+ " WHERE story_id='" + storyId + "' AND parent='0' AND rating>='"+filter+"'");

			result = query.execute();
			//String subject, comment, writer, link;
			//long childs, parent, id;
			CqlRows<String, String, byte[]> crows = null;
			if(result != null)
			{
				crows = result.get();
				if(crows != null)
				{
					System.out.println("canuprem: num rows 2 " + crows.getCount());
				}
				else
				{
					sp.printHTML("No Comments");
					System.out.println("crows is null but results is nto");
					return new DbProcessResult(sp,true);
				}
			}
			else
			{
				sp.printHTML("No Comments");
				System.out.println("No comments for this story");
				return new DbProcessResult(sp,true);
			}


			if (crows.getCount() != 0)
			{
				List<Row<String, String, byte[]>> rowList = crows.getList();
				for(int k = 0; k < crows.getCount(); k++)
				{
					ColumnSlice<String, byte[]> tcol = (rowList.get(k)).getColumnSlice();
					Long writer = new Long(longSerializer.fromBytes((tcol.getColumnByName("writer")).getValue()));
					Long rating   = new Long(longSerializer.fromBytes((tcol.getColumnByName("rating")).getValue()));
					String comment = stringSerializer.fromBytes((tcol.getColumnByName("comment")).getValue());

					String subject = stringSerializer.fromBytes((tcol.getColumnByName("subject")).getValue());
					String strId   = (rowList.get(k)).getKey();
					Long   parent  = new Long(longSerializer.fromBytes((tcol.getColumnByName("parent")).getValue()));
					Long   childs  = new Long(longSerializer.fromBytes((tcol.getColumnByName("childs")).getValue()));


					username = getUserName(writer.toString());

					sp.printHTML("<br><hr><br>");
					sp
					.printHTML("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\"><TR><TD><FONT size=\"4\" color=\"#000000\"><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
							+ "&commentId="
							+ strId
							+ "&filter="
							+ filter
							+ "&display="
							+ display
							+ "\">"
							+ subject
							+ "</a></B>&nbsp</FONT> (Score:"
							+ rating
							+ ")</TABLE>\n");
					sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
							+ date + "</B><p>\n");
					sp.printHTML("<TR><TD>" + comment);
					sp
					.printHTML("<TR><TD><p>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
							+ "&parent="
							+ strId
							+ "\">Reply to this</a>&nbsp|&nbsp"
							+ "<a href=\"  /rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
							+ "&commentId="
							+ parent
							+ "&filter="
							+ filter
							+ "&display="
							+ display
							+ "\">Parent</a>"
							+ "&nbsp|&nbsp<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ModerateComment?comment_table="
							+ comment_table
							+ "&commentId="
							+ strId
							+ "\">Moderate</a> ]</TABLE>\n");
					if (childs > 0)
						viewStoryDisplayFollowUp(strId, 1, display, filter,  comment_table,
								sp);

				}
			}

		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for ViewStory: " + e);
			e.printStackTrace();

			return new DbProcessResult(sp,true);
		}
		return new DbProcessResult(sp,false);
	}

}
