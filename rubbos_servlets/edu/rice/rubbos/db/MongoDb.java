package edu.rice.rubbos.db;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.GroupCommand;
import com.mongodb.Mongo;

import edu.rice.rubbos.servlets.ServletPrinter;



public class MongoDb implements DatabaseClientInterface{

	public Mongo m ;
	public DB db;		

	public MongoDb(Mongo m, DB db) {		
		this.m = m;
		this.db = db;
	}
	
	public String authenticate(String nickname, String password) {
		try
		{     
			//find if nickname password present in db
			//if no match found return 0 else return id
			DBCollection coll = db.getCollection("users");
			BasicDBObject query=new BasicDBObject();
			query.put("nickname", nickname);
			query.put("password", password);
			DBCursor cursor = coll.find(query);
			if(!cursor.hasNext()){
				return "0";
			}
			else{
				return cursor.next().get("_id").toString();
			}

		}
		catch (Exception e)
		{
			return e + "Authenticate function error";

		}
	}
	
	public String getUserName(String userId) throws Exception {
		DBCollection coll = db.getCollection("users");
		BasicDBObject query=new BasicDBObject();
		query.put("_id", getIdQuery(userId));    
		DBCursor cursor = coll.find(query);
		if(!cursor.hasNext()){
			return "0";
		}
		else{
			return cursor.next().get("nickname").toString();
		}
	}		

	public DbProcessResult getAcceptStoryResult(ServletPrinter sp,String storyId) {
		//SELECT * FROM submissions WHERE id= storyId
		DBCursor cursor ;
		try
		{
			DBCollection coll = db.getCollection("submissions");
			BasicDBObject query=new BasicDBObject();
			query.put("_id", getIdQuery(storyId));			
			cursor = coll.find(query);

		}
		catch (Exception e)
		{
			sp.printHTML(" Failed to execute Query for AcceptStory: " + e);	      
			return new DbProcessResult(sp, true);
		}
		try
		{
			if(!cursor.hasNext()){				
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>");	        
				return new DbProcessResult(sp, true);
			}

			//Add story to database
			DBObject submission = cursor.next();
			String categoryTitle = submission.get("title").toString();
			//			int categoryDate = Integer.parseInt(submission.get("date").toString());						
			Date categoryDate=parseMongoDbDate(submission.get("date").toString());
			String categoryBody = submission.get("text").toString();
			String categoryWriter = submission.get("writer").toString();
			String category = submission.get("category").toString();			

			DBCollection coll = db.getCollection("stories");
			sp
			.printHTML("title : "+categoryTitle+"<br>");
			BasicDBObject newStory = new BasicDBObject();						
			newStory.put("title", categoryTitle);
			newStory.put("date", categoryDate);
			newStory.put("body", categoryBody);
			newStory.put("writer", categoryWriter);
			newStory.put("category", category);	       	       

			coll.insert(newStory);
			//"DELETE FROM submissions WHERE id=storyId");
			coll = db.getCollection("submissions");
			coll.remove(submission);

		}
		catch (Exception e)
		{
			sp.printHTML("Exception accepting stories: " + e + "<br>");	
			return new DbProcessResult(sp, true);
		}			
		return new DbProcessResult(sp, false);
	}	
	public DbProcessResult getAuthorResult(ServletPrinter sp, String nickname, String password)
	{		
		DBCursor cursor;
		int  access = 0;
		String userId="";
		if ((nickname != null) && (password != null))
		{
			try
			{
				//				 stmt = conn
				//				            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
				//				                + nickname + "\" AND password=\"" + password + "\"");
				DBCollection coll = db.getCollection("users");
				BasicDBObject query=new BasicDBObject();
				query.put("nickname", nickname);	
				query.put("password", password);
				cursor = coll.find(query);
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
				if (cursor.hasNext())
				{
					DBObject user = cursor.next();
					userId = user.get("_id").toString();					
					access = Integer.parseInt(user.get("access").toString());
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
		if (("".equals(userId)) || (access == 0))
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
					+ "<p><p><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ReviewStories?authorId="
					+ userId + "\">Review submitted stories</a><br>\n");
		}
		sp.printHTMLfooter();
		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getBrowseCategoriesResult(ServletPrinter sp) {
		DBCursor cursor;
		try
		{
			DBCollection coll = db.getCollection("categories");
			cursor = coll.find();
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for BrowseCategories: " + e);

			return new DbProcessResult(sp, true);
		}
		try
		{
			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h2>Sorry, but there is no category available at this time. Database table is empty</h2><br>\n");
				return new DbProcessResult(sp, true);
			}
			else
				sp.printHTML("<h2>Currently available categories</h2><br>\n");

			String categoryId;
			String categoryName;

			do
			{
				DBObject category = cursor.next();

				categoryId = category.get("_id").toString();
				categoryName = category.get("name").toString();
				sp
				.printHTMLHighlighted("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.BrowseStoriesByCategory?category="
						+ categoryId
						+ "&categoryName="
						+ URLEncoder.encode(categoryName)
						+ "\">"
						+ categoryName
						+ "</a><br>\n");
			}
			while (cursor.hasNext());
		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting categories: " + e + "<br>");
		} 
		return new DbProcessResult(sp, false);

	}
	public DbProcessResult getBrowseStoriesByCategoryResult(ServletPrinter sp,int nbOfStories,int page,String categoryId, String categoryName) {
		int matchFound = 0;	
		DBCursor cursor;
		try
		{
			/*stmt = conn.prepareStatement("SELECT * FROM stories WHERE category= "
		          + categoryId + " ORDER BY date DESC LIMIT " + page * nbOfStories
		          + "," + nbOfStories);
		      rs = stmt.executeQuery();*/
			DBCollection coll = db.getCollection("stories");
			BasicDBObject query=new BasicDBObject();
			query.put("category", categoryId);			
			cursor = coll.find(query).sort(new BasicDBObject("date",-1)).skip(page*nbOfStories).limit(nbOfStories);

		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for BrowseStoriesByCategory: " + e);
			return new DbProcessResult(sp, true);
		}
		try
		{
			if (!cursor.hasNext())
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
		                  + "&nbOfStories="+nbOfStories+"\">Previous page</a>\n</CENTER>\n");
		        }
		        sp.printHTMLfooter();
		        
		        return new DbProcessResult(sp, true);
		      }
		      
		      do
		      {
		    	  matchFound++;
		    	  DBObject story = cursor.next();
		        String title = story.get("title").toString();
		        String date = story.get("date").toString();
		        String username = story.get("writer").toString();
		        String id = story.get("_id").toString();

		        sp
		            .printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
		                + id
		                + "\">"
		                + title
		                + "</a> by "
		                + username
		                + " on "
		                + date
		                + "<br>\n");
		      }
		      while (cursor.hasNext());

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
	public DbProcessResult getModerateCommentResult(ServletPrinter sp,String commentId, String comment_table) {
		DBCursor cursor;
		try
		{
			/* stmt = conn.prepareStatement("SELECT * FROM " + comment_table
	          + " WHERE id=" + commentId);
	      rs = stmt.executeQuery();*/
			DBCollection coll = db.getCollection(comment_table);
			BasicDBObject query=new BasicDBObject();
			query.put("_id", getIdQuery(commentId));			
			cursor = coll.find(query);			
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for ModerateComment: " + e);	      
			return new DbProcessResult(sp,true);
		}

		try
		{
			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");

				return new DbProcessResult(sp,true);
			}
		}
		catch (Exception e)
		{
			sp.printHTML("Exception moderating comments: " + e + "<br>");

			return new DbProcessResult(sp,true);
		}

		try
		{
			DBObject comment = cursor.next();
			String storyId = comment.get("story_id").toString();
			sp
			.printHTML("<p><br><center><h2>Moderate a comment !</h2></center><br>\n<br><hr><br>");
			String username = getUserName(comment.get("writer").toString());
			sp
			.printHTML("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\"><TR><TD><FONT size=\"4\" color=\"#000000\"><center><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
					+ comment_table
					+ "&storyId="
					+ storyId
					+ "&commentId="
					+ comment.get("_id").toString()
					+ "\">"
					+ comment.get("subject").toString()
					+ "</a></B>&nbsp</FONT> (Score:"
					+ comment.get("rating").toString()
					+ ")</center></TABLE>\n");
			sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
					+ comment.get("date").toString() + "</B><p>\n");
			sp
			.printHTML("<TR><TD>"
					+ comment.get("comment").toString()
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
			return new DbProcessResult(sp,true);
		} 


		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getOlderStoriesResult(ServletPrinter sp, String day, String month,
			String year,  int page, int nbOfStories, Date dfrom,
			Date dto) {
		DBCursor cursor=null;
		try
		{
			//	        stmt = conn.prepareStatement("SELECT * FROM stories WHERE date>='"
			//	            + before + "' AND date<='" + after + "' ORDER BY date DESC LIMIT "
			//	            + page * nbOfStories + "," + nbOfStories);
			//	        rs = stmt.executeQuery();

			DBCollection coll = db.getCollection("stories");
			BasicDBObject query=new BasicDBObject();
			query.put("date", new BasicDBObject("$gte",dfrom));
			query.put("date", new BasicDBObject("$lte",dto));				
			cursor = coll.find(query).sort(new BasicDBObject("date",-1)).skip(page*nbOfStories).limit(nbOfStories);
			if (!cursor.hasNext())
			{
				cursor.close();
				//	        	stmt = conn
				//	              .prepareStatement("SELECT * FROM old_stories WHERE date>='"
				//	                  + before + "' AND date<='" + after
				//	                  + "' ORDER BY date DESC LIMIT " + page * nbOfStories + ","
				//	                  + nbOfStories);
				//	          rs = stmt.executeQuery();
				coll = db.getCollection("old_stories");
				query=new BasicDBObject();
				query.put("date", new BasicDBObject("$gte",dfrom));
				query.put("date", new BasicDBObject("$lte",dto));
				cursor = coll.find(query).sort(new BasicDBObject("date",-1)).skip(page*nbOfStories).limit(nbOfStories);
			}
			if (!cursor.hasNext())
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
				cursor.close();
				return new DbProcessResult(sp, true);
			}
		}
		catch (Exception e)
		{
			sp.printHTML("Exception getting older stories: " + e + "<br>");
			cursor.close();
			return new DbProcessResult(sp, true);
		}

		String title;
		Date date;
		// Print the story titles and author

		try
		{
			while (cursor.hasNext())
			{
				DBObject story = cursor.next();
				String id = story.get("_id").toString();
				title = story.get("title").toString();
				String username = getUserName(story.get("writer").toString());

				date=parseMongoDbDate(story.get("date").toString());
				sp
				.printHTML("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
						+ id
						+ "\">"
						+ title
						+ "</a> by "
						+ username
						+ " on "
						+ date
						+ "<br>\n");
			}
		}
		catch (Exception e2)
		{
			sp.printHTML("Exception getting strings: " + e2 + "<br>");
		}
		cursor.close();
		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getRegisterUserResult(ServletPrinter sp, 
			String nickname, String firstname, String lastname, String email,
			String password,  int access, int rating) {
		DBCursor cursor;
		String id;
		Date creation_date =  new Date();
		try
		{
			//	      stmt = conn.prepareStatement("SELECT * FROM users WHERE nickname=\""
			//	          + nickname + "\"");
			//	      rs = stmt.executeQuery();

			DBCollection coll = db.getCollection("users");
			BasicDBObject query=new BasicDBObject();
			query.put("nickname", nickname);			
			cursor = coll.find(query);

		}
		catch (Exception e)
		{
			sp.printHTML("ERROR: Nickname query failed" + e);
			return new DbProcessResult(sp,true);
		}
		try
		{
			if (cursor.hasNext())
			{
				sp
				.printHTML("The nickname you have chosen is already taken by someone else. Please choose a new nickname.<br>\n");
				cursor.close();
				return new DbProcessResult(sp,true);
			}
			//	      stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, \""
			//	          + firstname + "\", \"" + lastname + "\", \"" + nickname + "\", \""
			//	          + password + "\", \"" + email + "\", 0, 0, NOW())");
			//	      updateResult = stmt.executeUpdate();
			//	      stmt.close();
			DBCollection coll = db.getCollection("users");			

			BasicDBObject newUser = new BasicDBObject();						
			newUser.put("firstname", firstname);
			newUser.put("lastname", lastname);
			newUser.put("nickname", nickname);
			newUser.put("password", password);
			newUser.put("email", email);
			newUser.put("rating", rating);
			newUser.put("access", access);
			newUser.put("creation_date", creation_date);

			coll.insert(newUser);

			cursor.close();
			//	      stmt = conn.prepareStatement("SELECT * FROM users WHERE nickname=\""
			//	          + nickname + "\"");
			//	      rs = stmt.executeQuery();

			coll = db.getCollection("users");
			BasicDBObject query=new BasicDBObject();
			query.put("nickname", nickname);			
			cursor = coll.find(query);

			DBObject user = cursor.next();
			id = user.get("_id").toString();		
			creation_date=parseMongoDbDate(user.get("creation_date").toString());
			rating = Integer.parseInt(user.get("rating").toString());
			access = Integer.parseInt(user.get("access").toString());

		}
		catch (Exception e)
		{
			sp.printHTML("Exception registering user " + e + "<br>");
			cursor.close();
			return new DbProcessResult(sp,true);
		}

		cursor.close();

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
	public DbProcessResult getRejectStoryResult(ServletPrinter sp, String storyId) {

		DBCursor cursor=null;
		DBCollection coll;
		try
		{
			//		      stmt = conn.prepareStatement("SELECT id FROM submissions WHERE id="
			//		          + storyId);
			//		      rs = stmt.executeQuery();
			coll = db.getCollection("submissions");
			BasicDBObject query=new BasicDBObject();
			query.put("_id", getIdQuery(storyId));			
			cursor = coll.find(query);

		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for RejectStory: " + e);
			cursor.close();
			return new DbProcessResult(sp, true) ;
		}
		try
		{
			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>\n");
				cursor.close();
				return new DbProcessResult(sp, true) ;
			}

			DBObject story = cursor.next();

			// Delete entry from database
			//		      stmt = conn.prepareStatement("DELETE FROM submissions WHERE id="
			//		          + storyId);
			//		      updateResult = stmt.executeUpdate();
			coll.remove(story);

		}
		catch (Exception e)
		{
			sp.printHTML("Exception rejecting story: " + e + "<br>");
			cursor.close();
			return new DbProcessResult(sp, true) ;
		}
		cursor.close();
		return new DbProcessResult(sp, false) ;
	}
	public DbProcessResult getReviewStoriesResult(ServletPrinter sp) {
		Date date;
		String title;
		String id;
		String body;
		String username;
		DBCursor cursor;

		try
		{
			/*stmt = conn
	          .prepareStatement("SELECT * FROM submissions ORDER BY date DESC LIMIT 10");
	      rs = stmt.executeQuery();*/
			DBCollection coll = db.getCollection("submissions");
			BasicDBObject query=new BasicDBObject();						
			cursor = coll.find(query).sort(new BasicDBObject("date",-1)).limit(10);	        

		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for ReviewStories " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp, true);
		}



		try
		{

			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h2>Sorry, but there is no submitted story available at this time.</h2><br>\n");
				//		        closeConnection(stmt, conn);
				return new DbProcessResult(sp, true);
			}

			do
			{
				DBObject submission = cursor.next();				
				title = submission.get("title").toString();					
				date=parseMongoDbDate(submission.get("date").toString());
				
				id = submission.get("_id").toString();				
				body = submission.get("text").toString();			
				sp.printHTML("<br><hr>\n");
				sp.printHTMLHighlighted(title);
				username = submission.get("writer").toString();
				sp.printHTML("<B>Posted by " + username + " on " + date + "</B><br>\n");
				sp.printHTML(body);
				sp
				.printHTML("<br><p><center><B>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.AcceptStory?storyId="
						+ id
						+ "\">Accept</a> | <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.RejectStory?storyId="
						+ id + "\">Reject</a> ]</B><p>\n");
			}
			while (cursor.hasNext());
		}
		catch (Exception e)
		{
			sp.printHTML("Exception reviewing story: " + StringUtils.join(e.getStackTrace(),"</br>") + "<br>");
			return new DbProcessResult(sp, true);
		}

		//		    closeConnection(stmt, conn);
		return new DbProcessResult(sp, false);
	}
	public DbProcessResult getStoreCommentResult(ServletPrinter sp, String storyId,
			String parent, String subject, String body, String comment_table,
			String userId) {
		try
		{
			//	      stmt = conn.prepareStatement("INSERT INTO " + comment_table
			//	          + " VALUES (NULL, " + userId + ", " + storyId + ", " + parent
			//	          + ", 0, 0, NOW(), \"" + subject + "\", \"" + body + "\")");
			//	      updateResult = stmt.executeUpdate();
			//
			//	      stmt.close();
			//
			//	      stmt = conn.prepareStatement("UPDATE " + comment_table
			//	          + " SET childs=childs+1 WHERE id=" + parent);
			//	      updateResult = stmt.executeUpdate();

			DBCollection coll = db.getCollection(comment_table);

			BasicDBObject newComment = new BasicDBObject();			
			newComment.put("writer", userId);
			newComment.put("story_id", storyId);
			newComment.put("parent", parent);
			newComment.put("comment", body);
			newComment.put("subject", subject);
			newComment.put("childs", 0);
			newComment.put("rating", 0);
			newComment.put("date", new Date());


			coll.insert(newComment);
		}
		catch (Exception e)
		{
			//	      closeConnection(stmt, conn);
			sp.printHTML("Exception getting categories: " + e + "<br>");
			return new DbProcessResult(sp,true);
		}
		return new DbProcessResult(sp,false);
	}
	public DbProcessResult getStoreModLogResult(ServletPrinter sp, String nickname,
			String password, String comment_table, String commentId, int access,
			String userId, int rating) {
		DBCursor cursor;
		if ((nickname != null) && (password != null))
		{
			try
			{
				/*stmt = conn
	            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
	                + nickname + "\" AND password=\"" + password + "\"");
	        rs = stmt.executeQuery();*/
				DBCollection coll = db.getCollection("users");
				BasicDBObject query=new BasicDBObject();
				query.put("nickname", nickname);
				query.put("password", password);
				cursor = coll.find(query);

			}
			catch (Exception e)
			{
				sp.printHTML("Failed to execute Query for StoreModLogResul: "
						+ e);
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}

			try
			{
				if (cursor.hasNext())
				{
					DBObject userDetails = cursor.next();
					userId = userDetails.get("_id").toString();
					access = Integer.parseInt(userDetails.get("access").toString());
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

		if (("0".equals(userId)) || (access == 0))
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
			        rs = stmt.executeQuery();*/
				DBCollection coll = db.getCollection(comment_table);
				BasicDBObject query=new BasicDBObject();
				query.put("_id", getIdQuery(commentId));				
				cursor = coll.find(query);
				if (!cursor.hasNext())
				{
					sp
					.printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");
				}
				DBObject commentsEntry=cursor.next();
				int rsrating = Integer.parseInt(commentsEntry.get("rating").toString());
				String writer = commentsEntry.get("writer").toString();
				
				cursor.close();

				if (((rsrating == -1) && (rating == -1))
						|| ((rsrating == 5) && (rating == 1)))
					sp
					.printHTML("Comment rating is already to its maximum, updating only user's rating.");
				else
				{
					// Update ratings
					if (rating != 0)
					{
						//			            stmt = conn.prepareStatement("UPDATE users SET rating=rating+"
						//			                + rating + " WHERE id=" + writer);
						//			            updateResult = stmt.executeUpdate();
						//				    stmt.close();
						//		
						cursor.close();
						if(!"0".equals(writer)){
							coll = db.getCollection("users");
							query=new BasicDBObject();
							query.put("_id", getIdQuery(writer));
	
							DBObject user = coll.findOne(query);
							BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("rating", rating));
							coll.update(user, set);
						}
						//			            stmt = conn.prepareStatement("UPDATE " + comment_table
						//			                + " SET rating=rating+" + rating + " WHERE id=" + commentId);
						//			            updateResult = stmt.executeUpdate();
						//				    stmt.close();
						coll = db.getCollection(comment_table);
						query=new BasicDBObject();
						query.put("_id", getIdQuery(commentId));

						DBObject comment = coll.findOne(query);
						BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("rating", rating));
						coll.update(comment, set);
					}
				}

				//				stmt = conn.prepareStatement("SELECT rating FROM " + comment_table
				//						+ " WHERE id=" + commentId);
				//				rs = stmt.executeQuery();
				String user_row_rating = null, comment_row_rating = null;
				coll = db.getCollection(comment_table);
				query=new BasicDBObject();
				query.put("_id", getIdQuery(commentId));

				DBObject comment = coll.findOne(query);
				if (comment!=null)
					comment_row_rating = comment.get("rating").toString();				

				//				stmt = conn.prepareStatement("SELECT rating FROM users WHERE id="
				//						+ writer);
				//				rs = stmt.executeQuery();
				coll = db.getCollection("users");
				query=new BasicDBObject();
				query.put("_id", getIdQuery(writer));
				DBObject user = coll.findOne(query);

				if (user!=null)
					user_row_rating = user.get("rating").toString();

				if (user==null)
					sp
					.printHTML("<h3>ERROR: Sorry, but this user does not exist.</h3><br>\n");


				// Update moderator log
				//				stmt = conn
				//						.prepareStatement("INSERT INTO moderator_log VALUES (NULL, "
				//								+ userId + ", " + commentId + ", " + rating + ", NOW())");
				//				updateResult = stmt.executeUpdate();

				coll = db.getCollection("moderator_log");				

				BasicDBObject newLog = new BasicDBObject();			
				newLog.put("userid", userId);
				newLog.put("commentid", commentId);
				newLog.put("rating", rating);
				newLog.put("date", new Date());					       	       

				coll.insert(newLog);


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
		String table;
		DBCursor cursor;

		if ((nickname != null) && (password != null))
		{      
			try
			{
				/*stmt = conn
	            .prepareStatement("SELECT id,access FROM users WHERE nickname=\""
	                + nickname + "\" AND password=\"" + password + "\"");
	        rs = stmt.executeQuery();*/
				DBCollection coll = db.getCollection("users");
				BasicDBObject query=new BasicDBObject();
				query.put("nickname", nickname);
				query.put("password", password);
				cursor = coll.find(query);

			}
			catch (Exception e)
			{
				sp.printHTML("ERROR: Authentification query failed" + e);	
				return new DbProcessResult(sp,true);
			}
			try
			{
				if (cursor.hasNext())
				{
					DBObject user = cursor.next();
					userId = user.get("_id").toString();
					access = Integer.parseInt(user.get("access").toString());;
				}
			}
			catch (Exception e)
			{
				sp.printHTML("Exception storing story " + e + "<br>");
				//closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}
		}

		table = "submissions";
		if ("".equals(userId))
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

			// Generate a UUID
//			UUID id1 = UUID.randomUUID();

			//			stmt = conn.prepareStatement("INSERT INTO " + table
			//			          + " VALUES (NULL, \"" + title + "\", \"" + body + "\", NOW(), \""
			//			          + userId + "\", " + category + ")");

			DBCollection coll = db.getCollection(table);

			BasicDBObject newStory = new BasicDBObject();						
			newStory.put("title", title);
			newStory.put("date", new Date());
			newStory.put("text", body);
			newStory.put("writer", userId);
			newStory.put("category", category);	       	       
			try{
				coll.insert(newStory);
			}
			catch(Exception e)
			{
				sp.printHTML(" ERROR: Failed to insert new story in database. ");
				//			        closeConnection(stmt, conn);
				new DbProcessResult(sp,true);
			}

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
	public DbProcessResult getStoriesOfTheDayResult(ServletPrinter sp,int bodySizeLimit) {
		DBCursor cursor;
		try
		{
			/*stmt = conn
	          .prepareStatement("SELECT * FROM stories ORDER BY date DESC LIMIT 10");
	      rs = stmt.executeQuery();*/
			DBCollection coll = db.getCollection("stories");
			BasicDBObject query=new BasicDBObject();			
			cursor = coll.find(query).sort(new BasicDBObject("date",-1)).limit(10);
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for stories of the day: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}
		try
		{

			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h2>Sorry, but there is no story available at this time.</h2><br>\n");
				cursor.close();
				return new DbProcessResult(sp,true);
			}

			String storyId;
			String storyTitle;
			String writerId;
			String userName;
			Date date;
			String body;
			do
			{
				DBObject story = cursor.next();
				sp.printHTML("<br><hr>\n");
				storyId = story.get("_id").toString();
				storyTitle = story.get("title").toString();
				sp
				.printHTMLHighlighted("<a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewStory?storyId="
						+ storyId + "\">" + storyTitle + "</a>");

				writerId = story.get("writer").toString();
				userName = getUserName(writerId);			
				date=parseMongoDbDate(story.get("date").toString());
				sp.printHTML("<B>Posted by " + userName + " on " + date + "</B><br>\n");
				body = story.get("body").toString();;
				if (body.length() > bodySizeLimit)
				{
					sp.printHTML(body.substring(0, bodySizeLimit));
					sp.printHTML("<br><B>...</B>");
				}
				else
					sp.printHTML(body);
				sp.printHTML("<br>\n");
			}
			while (cursor.hasNext());
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
			
		DBCursor cursor;
		try
		{
			//			stmt = conn.prepareStatement("SELECT * FROM categories");
			//	      rs = stmt.executeQuery();
			DBCollection coll = db.getCollection("categories");					
			cursor = coll.find();
		}
		catch (Exception e)
		{
			sp.printHTML(" Failed to execute Query for SubmitStory: " + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		try
		{
			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>");
				//				closeConnection(stmt, conn);
				return new DbProcessResult(sp,true);
			}

			//Add story to database

			String Name;
			String Id;
			do
			{
				DBObject category = cursor.next();
				Name = category.get("name").toString();
				Id = category.get("_id").toString();
				sp.printHTML("<OPTION value=\"" + Id + "\">" + Name + "</OPTION>\n");
			}
			while (cursor.hasNext());

		}
		catch (Exception e)
		{
			sp.printHTML("Exception accepting stories: " + e + "<br>");
		}
		return new DbProcessResult(sp,false);

	}
	public void viewCommentDisplayFollowUp(String cid, int level, int display, int filter, String comment_table, 
			boolean separator, ServletPrinter sp) throws Exception
			{

		int childs,  rating,  i;
		String id,story_id,parent;
		String subject, username,  comment;
		Date date;
		DBCursor cursor;

		try
		{
			//			stmtfollow = conn.prepareStatement("SELECT * FROM " + comment_table
			//					+ " WHERE parent=" + cid);
			//			//+" AND rating>="+filter);
			//			follow = stmtfollow.executeQuery();

			DBCollection coll = db.getCollection(comment_table);
			BasicDBObject query=new BasicDBObject();
			query.put("parent", cid);
			query.put("rating", new BasicDBObject("$gte",filter));
			cursor = coll.find(query);

			while (cursor.hasNext())
			{
				DBObject next = cursor.next();
				story_id = next.get("story_id").toString();
				id = next.get("_id").toString();
				subject = next.get("subject").toString();
				username = getUserName(next.get("writer").toString());
				
				date=parseMongoDbDate(next.get("date").toString());
				rating = Integer.parseInt(next.get("rating").toString());
				parent = next.get("parent").toString();
				comment = next.get("comment").toString();
				childs = Integer.parseInt(next.get("childs").toString());

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
		catch (Exception e)
		{
			sp.printHTML("Failure at display_follow_up: " + e);      
			try 
			{
				//				stmtfollow.close();
			} 
			catch (Exception ignore) 
			{
			}
			throw e;
		}
		//		stmtfollow.close();
			}
	public DbProcessResult getViewCommentResult(ServletPrinter sp,
			String comment, String storyId, String comment_table,
			String parent,  int filter, int display, String commentId) {
		DBCursor cursor;
		int i=0;
		if ("0".equals(commentId))
			parent = "0";
		else
		{
			try
			{				
				//				stmt = conn.prepareStatement("SELECT parent FROM " + comment_table
				//	            + " WHERE id=" + commentId);
				//	        rs = stmt.executeQuery();
				DBCollection coll = db.getCollection(comment_table);
				BasicDBObject query=new BasicDBObject();
				query.put("_id", getIdQuery(commentId));
				cursor = coll.find(query);
				if (!cursor.hasNext())
		        {
		          sp
		              .printHTML("<h3>ERROR: Sorry, but this comment does not exist.</h3><br>\n");		          
		          return new DbProcessResult(sp,true);
		        }
		        parent = cursor.next().get("parent").toString();
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

		try
		{
			//	      stmt = conn
			//	          .prepareStatement("SELECT rating, COUNT(rating) AS count FROM "
			//	              + comment_table + " WHERE story_id=" + storyId
			//	              + " GROUP BY rating ORDER BY rating");
			//	      rs = stmt.executeQuery();
			DBCollection coll = db.getCollection(comment_table);
			GroupCommand cmd=new GroupCommand(coll, new BasicDBObject("rating",true), new BasicDBObject("story_id",storyId), new BasicDBObject("count",0), "function(obj,prev) { prev.count += 1; }",null );
			DBObject result = coll.group(cmd);
			Set<String> resultKeys = result.keySet();
			ArrayList<BasicDBObject> ratings=new ArrayList<BasicDBObject>();
			for (String string : resultKeys) {
				ratings.add((BasicDBObject)result.get(string));			
			}
			/*Collections.sort(ratings,new Comparator<BasicDBObject>() {
				@Override
				public int compare(BasicDBObject o1, BasicDBObject o2) {
					return new Integer(o2.getInt("rating")).compareTo(o1.getInt("rating"));
				}
			});*/
			i = -1;
			for (BasicDBObject ratingCount : ratings) {						      	      
				{
					int rating = ratingCount.getInt("rating");
					int count = ratingCount.getInt("count");
					/*while ((i < 6) && (rating != i))
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
					}*/
					if(rating!=filter){
						sp.printHTML("<OPTION value=\"" + rating + "\">" + rating + ": "
								+ count + " comments</OPTION>\n");
					}
					else{
						sp.printHTML("<OPTION selected value=\"" + rating + "\">" + rating + ": "
								+ count + " comments</OPTION>\n");
					}
				}	        
			}	      
		}
		catch (Exception e)
		{
			sp.printHTML("Failed to execute Query for View Comment: " + e);
			//	      closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		/*while (i < 6)
		{
			sp.printHTML("<OPTION value=\"" + i + "\">" + i
					+ ": 0 comment</OPTION>\n");
			i++;
		}*/

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

		String subject;
		Date date;
		String id;
		boolean separator;
		try
		{
			//	      stmt = conn.prepareStatement("SELECT * FROM " + comment_table
			//	          + " WHERE story_id=" + storyId + " AND parent=0"); //+ parent+
			//	      //" AND rating>="+filter);
			//	      rs = stmt.executeQuery();
			DBCollection coll = db.getCollection(comment_table);
			BasicDBObject query=new BasicDBObject();
			query.put("story_id", storyId);
			query.put("parent", "0");
//				        cursor.close();
			cursor = coll.find(query);

			while (cursor.hasNext())
			{
				DBObject next = cursor.next();
				String username = getUserName(next.get("writer").toString());
				int rating = Integer.parseInt(next.get("rating").toString());
				parent = next.get("parent").toString();
				id = next.get("_id").toString();
				subject = next.get("subject").toString();
				date=parseMongoDbDate(next.get("date").toString());
				int childs = Integer.parseInt(next.get("childs").toString());
				comment =next.get("comment").toString();;
				separator = false;

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
					sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
							+ date + "</B><p>\n");
					sp.printHTML("<TR><TD>" + comment);
					sp
					.printHTML("<TR><TD><p>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
							+ "&parent="
							+ id
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
							+ id
							+ "\">Moderate</a> ]</TABLE>\n");
				}
				if ((display > 0) && (childs > 0))
					viewCommentDisplayFollowUp(id, 1, display, filter,  comment_table,
							separator, sp);
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
	public void viewStoryDisplayFollowUp(String cid, int level, int display, int filter,String comment_table,ServletPrinter sp)			
		throws Exception	{
		int  i;
		Long childs;
		String subject, id, story_id, writer;
		Date date;		

		try
		{			
			DBCollection coll = db.getCollection(comment_table);
			BasicDBObject query=new BasicDBObject();
			query.put("parent",cid);			
			DBCursor cursor = coll.find(query);
			/*stmt = conn
.prepareStatement("SELECT id,subject,writer,date,story_id,childs FROM "
+ comment_table + " WHERE parent=" + cid);
rs = stmt.executeQuery();*/			
			if(cursor.hasNext())
			{
				do
				{
					for (i = 0; i < level; i++)
						sp.printHTML("&nbsp&nbsp&nbsp");
					DBObject next = cursor.next();

					date=parseMongoDbDate(next.get("date").toString());
					story_id = next.get("story_id").toString();
					id =next.get("_id").toString();
					subject = next.get("subject").toString();
					writer = next.get("writer").toString();
					childs = Long.parseLong(next.get("childs").toString());;

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
				while (cursor.hasNext());
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
	public DbProcessResult getViewStoryResult(ServletPrinter sp,
			String title, String body,
			String username, String storyId,  String comment_table) {		
		Date date;
		DBCursor cursor;
		try
		{
			//			 stmt = conn.prepareStatement("SELECT * FROM stories WHERE id=" + storyId);
			//		      rs = stmt.executeQuery();
			DBCollection coll = db.getCollection("stories");
			BasicDBObject query=new BasicDBObject();
			query.put("_id", getIdQuery(storyId));	        
			cursor = coll.find(query);
		}

		catch (Exception e)
		{
			sp.printHTML("ERROR: ViewStory query failed" + e);
			//closeConnection(stmt, conn);
			return new DbProcessResult(sp,true);
		}

		try
		{
			if (!cursor.hasNext())
			{				
				//		        stmt = conn.prepareStatement("SELECT * FROM old_stories WHERE id="
				//		            + storyId);
				//		        rs = stmt.executeQuery();
				DBCollection coll = db.getCollection("old_stories");
				BasicDBObject query=new BasicDBObject();
				query.put("_id", getIdQuery(storyId));	        
				cursor = coll.find(query);
				comment_table = "old_comments";

			}
			else
			{
				comment_table = "comments";
			}			
			if (!cursor.hasNext())
			{
				sp
				.printHTML("<h3>ERROR: Sorry, but this story does not exist.</h3><br>\n");
				return new DbProcessResult(sp,true);
			}
			DBObject next = cursor.next();
			username = getUserName(next.get("writer").toString());

			date=parseMongoDbDate(next.get("date").toString());
			title = next.get("title").toString();
			body = next.get("body").toString();
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
		int i = -1, rating;
		String count;
		int filter, display;		
		try
		{
			//	      stmt = conn
			//	          .prepareStatement("SELECT rating, COUNT(rating) AS count FROM "
			//	              + comment_table + " WHERE story_id=" + storyId
			//	              + " GROUP BY rating ORDER BY rating");
			//	      count_result = stmt.executeQuery();
			DBCollection coll = db.getCollection(comment_table);
			GroupCommand cmd=new GroupCommand(coll, new BasicDBObject("rating",true), new BasicDBObject("story_id",storyId), new BasicDBObject("count",0), "function(obj,prev) { prev.count += 1; }",null );
			DBObject result = coll.group(cmd);
			Set<String> resultKeys = result.keySet();			
			ArrayList<BasicDBObject> ratings=new ArrayList<BasicDBObject>();
			for (String string : resultKeys) {
				ratings.add((BasicDBObject)result.get(string));			
			}
			
			/*Collections.sort(ratings,new Comparator<BasicDBObject>() {
				@Override
				public int compare(BasicDBObject o1, BasicDBObject o2) {
					return new Integer(o2.getInt("rating")).compareTo(o1.getInt("rating"));
				}
			});*/			
			for (BasicDBObject ratingEntry : ratings) 
			{
				rating = ratingEntry.getInt("rating");
				count = ratingEntry.getString("count");				
				filter = 0;
				/*while ((i < 6) && (rating != i))
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
				}*/
				if(rating!=filter){
					sp.printHTML("<OPTION value=\"" + rating + "\">" + rating + ": "
							+ count + " comments</OPTION>\n");
				}
				else{
					sp.printHTML("<OPTION selected value=\"" + rating + "\">" + rating + ": "
							+ count + " comments</OPTION>\n");
				}
			}			
		}
		catch (Exception e2)
		{
			sp.printHTML("count_result failed " + e2 + "<br>");
		}

		/*while (i < 6)
		{
			sp.printHTML("<OPTION value=\"" + i + "\">" + i
					+ ": 0 comment</OPTION>\n");
			i++;
		}*/

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
			//			stmt = conn.prepareStatement("SELECT * FROM " + comment_table
			//					+ " WHERE story_id=" + storyId + " AND parent=0 AND rating>="
			//					+ filter);
			//			rs = stmt.executeQuery();

			DBCollection coll = db.getCollection(comment_table);
			BasicDBObject query=new BasicDBObject();
			query.put("story_id", storyId);
			query.put("parent", "0");
			query.put("rating", new BasicDBObject("$gte",filter));			
			cursor = coll.find(query);
			String subject, comment;
			int childs;
			String parent, id;

			if (cursor.hasNext())
			{
				do
				{
					DBObject next = cursor.next();
					username = getUserName(next.get("writer").toString());
					subject = next.get("subject").toString();
					rating = Integer.parseInt(next.get("rating").toString());

					date=parseMongoDbDate(next.get("date").toString());
					comment = next.get("comment").toString();
					id = next.get("_id").toString();
					parent = next.get("parent").toString();
					childs = Integer.parseInt(next.get("childs").toString());

					sp.printHTML("<br><hr><br>");
					sp
					.printHTML("<TABLE width=\"100%\" bgcolor=\"#CCCCFF\"><TR><TD><FONT size=\"4\" color=\"#000000\"><B><a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.ViewComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
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
					sp.printHTML("<TABLE><TR><TD><B>Posted by " + username + " on "
							+ date + "</B><p>\n");
					sp.printHTML("<TR><TD>" + comment);
					sp
					.printHTML("<TR><TD><p>[ <a href=\"/rubbos/servlet/edu.rice.rubbos.servlets.PostComment?comment_table="
							+ comment_table
							+ "&storyId="
							+ storyId
							+ "&parent="
							+ id
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
							+ id
							+ "\">Moderate</a> ]</TABLE>\n");
					if (childs > 0)
						viewStoryDisplayFollowUp(id, 1, display, filter, comment_table,
								sp);
				}
				while (cursor.hasNext());
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
	
	private Date parseMongoDbDate(String dateString){		
        String[] split = dateString.split("\\s");
        split[split.length-2]="";
        String datestringnozone=StringUtils.join(split," ").replace("  ", " ");         

        DateTimeFormatter parser2 = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss yyyy");		
		Date date=parser2.parseDateTime(datestringnozone).toDate();
		return date;		
	}
	
	private Object getIdQuery(String id) {
		if(StringUtils.isNumeric(id)){
			return id;
		}
		return new ObjectId(id);
	}

	public DbProcessResult getRegisterUserResult(ServletPrinter sp,
			String nickname, String firstname, String lastname, String email,
			String password, int access, int rating, long en, long st) {
		// TODO Auto-generated method stub
		return null;
	}
}
