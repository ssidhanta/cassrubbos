package edu.rice.rubbos.db;

import java.util.Date;

import edu.rice.rubbos.servlets.ServletPrinter;

public interface DatabaseClientInterface {
	public String authenticate(String nickname, String password);
	public String getUserName(String userId) throws Exception;
	public DbProcessResult getAcceptStoryResult(ServletPrinter sp,String storyId);
	public DbProcessResult getAuthorResult(ServletPrinter sp, String nickname, String password);
	public DbProcessResult getBrowseCategoriesResult(ServletPrinter sp);
	public DbProcessResult getBrowseStoriesByCategoryResult(ServletPrinter sp,int nbOfStories,int page,String categoryId, String categoryName);
	public DbProcessResult getModerateCommentResult(ServletPrinter sp,String commentId, String comment_table) ;
	public DbProcessResult getOlderStoriesResult(ServletPrinter sp, String day, String month,
			String year,  int page, int nbOfStories, Date dfrom,Date dto);
	public DbProcessResult getRegisterUserResult(ServletPrinter sp,String nickname, String firstname, String lastname, String email,
			String password,  int access, int rating, long en, long st) ;
	public DbProcessResult getRejectStoryResult(ServletPrinter sp, String storyId) ;
	public DbProcessResult getReviewStoriesResult(ServletPrinter sp);
	
	public DbProcessResult getStoreCommentResult(ServletPrinter sp, String storyId,
			String parent, String subject, String body, String comment_table,
			String userId);
	
	public DbProcessResult getStoreModLogResult(ServletPrinter sp, String nickname,
			String password, String comment_table, String commentId, int access,
			String userId, int rating);
	public DbProcessResult getStoreStoryResult(ServletPrinter sp,
			String nickname, String title, String body,
			String category, String password, String userId,
			int access);
	
	public DbProcessResult getStoriesOfTheDayResult(ServletPrinter sp,int bodySizeLimit);
	
	public DbProcessResult getSubmitStoryResult(ServletPrinter sp);
	public void viewCommentDisplayFollowUp(String cid, int level, int display, int filter, String comment_table, 
			boolean separator, ServletPrinter sp) throws Exception ;
	
	public DbProcessResult getViewCommentResult(ServletPrinter sp,String comment, String storyId, String comment_table,
			String parent,  int filter, int display, String commentId);
	
	public void viewStoryDisplayFollowUp(String cid, int level, int display, int filter,String comment_table,ServletPrinter sp)			
			throws Exception;
	
	public DbProcessResult getViewStoryResult(ServletPrinter sp,String title, String body,
			String username, String storyId,  String comment_table) ;
}
