package uk.ac.bris.cs.databases.cwk3;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

import uk.ac.bris.cs.databases.api.AdvancedForumSummaryView;
import uk.ac.bris.cs.databases.api.AdvancedForumView;
import uk.ac.bris.cs.databases.api.AdvancedPersonView;
import uk.ac.bris.cs.databases.api.APIProvider;
import uk.ac.bris.cs.databases.api.ForumSummaryView;
import uk.ac.bris.cs.databases.api.ForumView;
import uk.ac.bris.cs.databases.api.PersonView;
import uk.ac.bris.cs.databases.api.PostView;
import uk.ac.bris.cs.databases.api.Result;
import uk.ac.bris.cs.databases.api.SimpleForumSummaryView;
import uk.ac.bris.cs.databases.api.SimplePostView;
import uk.ac.bris.cs.databases.api.SimpleTopicSummaryView;
import uk.ac.bris.cs.databases.api.SimpleTopicView;
import uk.ac.bris.cs.databases.api.TopicSummaryView;
import uk.ac.bris.cs.databases.api.TopicView;

/**
 *
 * @author Feihua Yu
 * @author Jun Zhou
 * @author Siyu Zhang
 * @author Wei-Shen Lo
 */
public class API implements APIProvider {

	private final Connection c;

	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd '-' HH:mm:ss");

	// get the system time
	private Date getTime() {
		Date date = new Date(System.currentTimeMillis());
		return date;
	}

	public API(Connection c) {
		this.c = c;
	}

	/* A.1 */

	@Override
	public Result<Map<String, String>> getUsers() {
		Map<String, String> userMap = new HashMap<String, String>();
		String sql = "SELECT username, name FROM Person";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			ResultSet r = p.executeQuery();
			while (r.next()) {
				String username = r.getString("username");
				String name = r.getString("name");
				userMap.put(username, name);
			}
			return Result.success(userMap);
		} catch (SQLException e) {
			return Result.fatal("getUsers Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result<PersonView> getPersonView(String username) {
		PersonView pv;
		if(username == null || username.isEmpty()) return Result.failure("Username cannot be empty.");
		String sql = "SELECT name, username, stuId FROM Person WHERE username = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, username);
			ResultSet r = p.executeQuery();
			r.next();
			String name = r.getString("name");
			username = r.getString("username");
			String studentId = r.getString("stuId");
			if (studentId == null || studentId.isEmpty()) {
				studentId = "";
			}
			pv = new PersonView(name, username, studentId);
			return Result.success(pv);
		} catch (SQLException e) {
			return Result.fatal("getPersonView Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result addNewPerson(String name, String username, String studentId) {
		if (name == null || name.isEmpty() || username == null || username.isEmpty())
			return Result.failure("Something cannot be Null.");
		if (name.equals(username))
			return Result.failure("You can't get the same name and username.");
		if (existString("Person", "username", username))
			return Result.failure("Username was duplicated.");

		String sql = "INSERT INTO Person (name, username, stuId) VALUES (?, ?, ?)";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, name);
			p.setString(2, username);
			p.setString(3, studentId);
			p.execute();
			c.commit();
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("addNewPerson rollback Fatal username: " + f.getMessage());
			}
			return Result.fatal("addNewPerson Fatal: " + e.getMessage());
		}
	}

	/* A.2 */

	@Override
	public Result<List<SimpleForumSummaryView>> getSimpleForums() {
		String sql = "SELECT forum_id, forum_title FROM Forum ORDER BY forum_title ASC";
		List<SimpleForumSummaryView> list = new ArrayList<SimpleForumSummaryView>();
		try (PreparedStatement p = c.prepareStatement(sql)) {
			ResultSet r = p.executeQuery();
			while (r.next()) {
				Long id = r.getLong("forum_id");
				String title = r.getString("forum_title");
				SimpleForumSummaryView forum = new SimpleForumSummaryView(id, title);
				list.add(forum);
			}
			return Result.success(list);
		} catch (SQLException e) {
			return Result.fatal("getSimpleForums Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result createForum(String title) {
		if (title == null || title.isEmpty())
			return Result.failure("The title cannot be NULL.");
		if (existString("Forum", "forum_title", title))
			return Result.failure("Forum was duplicated.");

		String sql = "INSERT INTO Forum (forum_title) VALUES (?)";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, title);
			p.execute();
			c.commit();
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("createForum rollback Fatal error: " + f.getMessage());
			}
			return Result.fatal("createForum Fatal: " + e.getMessage());
		}
	}

	/* A.3 */

	@Override
	public Result<List<ForumSummaryView>> getForums() {
		final String sql = "SELECT f1.forum_id, f1.forum_title , t1.topic_id, t1.topic_title FROM (SELECT e.topic_forum,e.topic_id FROM (SELECT c.topic_forum, MAX(c.date)AS date FROM (SELECT t.topic_forum, t.topic_id,b.date FROM (SELECT post_topic, post_num, date FROM (SELECT MAX(post_date) AS date, post_topic AS topic FROM Post GROUP BY post_topic) AS a JOIN Post ON Post.post_topic = a.topic AND Post.post_date = a.date) AS b JOIN Topic t ON t.topic_id = b.post_topic)AS c GROUP BY c.topic_forum) AS d JOIN (SELECT t.topic_forum, t.topic_id,b.date FROM (SELECT post_topic, post_num, date FROM (SELECT MAX(post_date) AS date, post_topic AS topic FROM Post GROUP BY post_topic) AS a JOIN Post ON Post.post_topic = a.topic AND Post.post_date = a.date) AS b JOIN Topic t ON t.topic_id = b.post_topic) AS e ON e.topic_forum = d.topic_forum AND e.date = d.date) AS f JOIN Topic t1 ON f.topic_id = t1.topic_id RIGHT JOIN Forum f1 ON f1.forum_id = t1.topic_forum";
		List<ForumSummaryView> list = new ArrayList<ForumSummaryView>();

		try (PreparedStatement p = c.prepareStatement(sql)) {
			ResultSet r = p.executeQuery();
			SimpleTopicSummaryView topic;
			while (r.next()) {
				Long forumID = r.getLong("forum_id");
				String forumTitle = r.getString("forum_title");
				Long topicID = r.getLong("topic_id");
				String topictitle = r.getString("topic_title");
				if (topictitle != null)
					topic = new SimpleTopicSummaryView(topicID, forumID, topictitle);
				else
					topic = null;
				ForumSummaryView f = new ForumSummaryView(forumID, forumTitle, topic);
				list.add(f);
			}
		} catch (SQLException e) {
			return Result.fatal("getForums Fatal Error: " + e.getMessage());
		}
		return Result.success(list);
	}

	@Override
	public Result<ForumView> getForum(long id) {
		ForumView forum;
		String sql = "SELECT forum_id,forum_title, topic_id,topic_title FROM Forum "
				+ "LEFT JOIN Topic ON topic_forum = forum_id "
				+ "LEFT JOIN Person ON topic_creator = username WHERE forum_id = ?";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, id);
			ResultSet r = p.executeQuery();
			boolean exist;
			List<SimpleTopicSummaryView> topics = new ArrayList<SimpleTopicSummaryView>();
			exist = r.next();
			id = r.getLong("forum_id");
			String forumtitle = r.getString("forum_title");
			while (exist) {
				Long topicID = r.getLong("topic_id");
				String topictitle = r.getString("topic_title");
				if (forumtitle == null)
				{
					return Result.failure("No forum of this id");
				}
				if(topictitle == null){break;}
				SimpleTopicSummaryView topic = new SimpleTopicSummaryView(topicID, id, topictitle);
				topics.add(topic);
				exist = r.next();
			}
			forum = new ForumView(id, forumtitle, topics);
			return Result.success(forum);
		} catch (SQLException e) {
			return Result.fatal("getForum Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result<SimpleTopicView> getSimpleTopic(long topicId) {
		String sql = "SELECT topic_id,topic_title,post_num,post_author,post_text, post_date FROM Topic "
				+ "LEFT JOIN Post ON topic_id = post_topic WHERE topic_id = ?";
		SimplePostView post;
		SimpleTopicView topic;
		List<SimplePostView> posts = new ArrayList<SimplePostView>();
		getLatestPost(1);
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, topicId);
			ResultSet r = p.executeQuery();
			boolean exist;
			exist = r.next();
			if(!exist) return Result.failure("Topic does not exist");
			topicId = r.getLong("topic_id");
			String title = r.getString("topic_title");
			while (exist) {
				int postNumber = r.getInt("post_num");
				String author = r.getString("post_author");
				String text = r.getString("post_text");
				String postedAt = r.getString("post_date");
				post = new SimplePostView(postNumber, author, text, postedAt);
				posts.add(post);
				exist = r.next();
			}
			topic = new SimpleTopicView(topicId, title, posts);

			Result<PostView> latest = getLatestPost(1);

			return Result.success(topic);
		} catch (SQLException e) {
			return Result.fatal("getSimpleTopic Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result<PostView> getLatestPost(long topicId) {
		if (!existLong("Topic", "topic_id", topicId)){
					return Result.failure("Topic does not exist");
		}

		PostView pv;
		String s = "SELECT Person.name,forum_id,p.post_num, p.post_text, p.post_author,p.post_topic,p.post_date, IFNULL(l.likes,0)AS likes FROM Post p LEFT JOIN (SELECT likepost_PostId, likepost_TopicId, COUNT(*)AS likes FROM Likepost GROUP BY likepost_PostId, likepost_TopicId)l ON l.likepost_PostId = p.post_num AND l.likepost_TopicId = p.post_topic JOIN Person ON Person.username = p.post_author JOIN Topic t1 ON t1.topic_id = p.post_topic JOIN Forum ON Forum.forum_id = t1.topic_forum WHERE post_topic = ? ORDER BY post_date DESC LIMIT 1";

		try (PreparedStatement p = c.prepareStatement(s)) {
			p.setLong(1, topicId);
			ResultSet r = p.executeQuery();
			if(!r.next()) {
				return Result.failure("Topic does not exist");
		 }
		Long fid = r.getLong("forum_id");
			Long tid = r.getLong("post_topic");
			int pnum = r.getInt("post_num");
			String aname = r.getString("name");
			String auname = r.getString("post_author");
			String txt = r.getString("post_text");
			String date = r.getString("post_date");
			int likes = r.getInt("likes");
			pv = new PostView(fid, tid, pnum, aname, auname, txt, date, likes);
		} catch (SQLException e) {
			return Result.fatal("getLatestPost Fatal: " + e.getMessage());
		}
		return Result.success(pv);
	}

	@Override
	public Result createPost(long topicId, String username, String text) {
		if (!existLong("Topic", "topic_id", topicId)) {
			return Result.failure("Topic does not exist.");
		}
		if (!existString("Person", "username", username)) {
			return Result.failure("User does not exist.");
		}
		if (text == null || text.isEmpty()) {
			return Result.failure("Contents cannot be Null.");
		}
		String sql = "INSERT INTO Post (post_num, post_topic, post_author, post_text, post_date) VALUES(?, ?, ?, ?, ?)";

		int postNumber;
		postNumber = countPostsInTopic(topicId).getValue();
		if (postNumber != 0) {
			postNumber++;
		} else {
			postNumber = 1;
		}

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setInt(1, postNumber);
			p.setLong(2, topicId);
			p.setString(3, username);
			p.setString(4, text);
			p.setString(5, formatter.format(getTime()));
			p.execute();
			c.commit();
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("createPost Fatal: " + f.getMessage());
			}
			return Result.fatal("createPost Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result createTopic(long forumId, String username, String title, String text) {
		if (!existLong("Forum", "forum_id", forumId)) {
			Result.failure("Forum does not exist.");
		}
		if (!existString("Person", "username", username)) {
			Result.failure("User does not exist.");
		}
		if (title == null || title.isEmpty()) {
			return Result.failure("Title cannot be empty.");
		}
		if (text == null || text.isEmpty()) {
			return Result.failure("Text cannot be empty.");
		}
		if (existTopic(forumId, title)) {
			return Result.failure("Topic was duplicated.");
		}
		String sql = "INSERT INTO Topic (topic_title, topic_forum, topic_creator, topic_date) VALUES(?, ?, ?, ?);";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, title);
			p.setLong(2, forumId);
			p.setString(3, username);
			p.setString(4, formatter.format(getTime()));
			p.execute();
			c.commit();
			createPost(getTopicId(title, forumId), username, text);
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("createTopic rollback fatal error" + f.getMessage());
			}
			return Result.fatal("createTopic SQL fatal error" + e.getMessage());
		}
	}

	@Override
	public Result<Integer> countPostsInTopic(long topicId) {
		if (!existLong("Topic", "topic_id", topicId)) {
			return Result.failure("Topic does not exist");
		}
		String sql = "SELECT COUNT(*) AS counter FROM Topic " + "JOIN Post ON topic_id = post_topic WHERE topic_id = ?";
		int count;
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, topicId);
			ResultSet r = p.executeQuery();
			r.next();
			count = r.getInt("counter");
			if (r.next()) {
				throw new RuntimeException("There should not be another row!");
			}
			return Result.success(count);

		} catch (SQLException e) {
			return Result.fatal("countPostsInTopic Fatal: " + e.getMessage());
		}
	}

	/* B.1 */

	@Override
	public Result likeTopic(String username, long topicId, boolean like) {
		if (!existString("Person", "username", username)) {
			Result.failure("User does not exist.");
		}
		if (!existLong("Topic", "topic_id", topicId)) {
			return Result.failure("Topic does not exist");
		}
		String sql = "INSERT INTO Liketopic (liketopic_user, liketopic_TopicId) VALUES (?, ?)";
		String sql2 = "DELETE FROM Liketopic WHERE liketopic_user = ? AND liketopic_TopicId = ?";
		String k = sql2;
		if (like) {
			k = sql;
		}
		if (checkLike(username, topicId) && (like == true)) {
			System.out.println("repeatlike");
			return Result.success();
		}

		try (PreparedStatement p = c.prepareStatement(k)) {
			p.setString(1, username);
			p.setLong(2, topicId);
			p.execute();
			c.commit();
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("likeTopic Fatal: " + f.getMessage());
			}
			return Result.fatal("likeTopic Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result likePost(String username, long topicId, int post, boolean like) {
		if (!existString("Person", "username", username)) {
			Result.failure("User does not exist.");
		}
		if (!existLong("Topic", "topic_id", topicId)) {
			return Result.failure("Topic does not exist");
		}

		String sqlCheckpostnum = "SELECT post_num FROM Post WHERE post_num = ? AND post_topic = ?";
		try (PreparedStatement q = c.prepareStatement(sqlCheckpostnum)) {
			q.setInt(1, post);
			q.setLong(2, topicId);
			ResultSet s = q.executeQuery();
			boolean exist = s.next();
			if (!exist)
				return Result.failure("No post with this post number");
		} catch (SQLException e) {
			return Result.fatal("Fatal Error " + e.getMessage());
		}

		String sqlLike = "INSERT INTO Likepost (likepost_user, likepost_PostId, likepost_TopicId) VALUES (?, ?, ?)";
		String sqlDislike = "DELETE FROM Likepost WHERE likepost_user = ? AND likepost_PostId = ? AND likepost_TopicId = ?";
		String sql = sqlLike;
		if (!like)
			sql = sqlDislike;
		if (checkLikePost(username, topicId, post) && like == true) {
			return Result.success();
		}

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, username);
			p.setLong(2, post);
			p.setLong(3, topicId);
			p.execute();
			c.commit();
			return Result.success();
		} catch (SQLException e) {
			try {
				c.rollback();
			} catch (SQLException f) {
				return Result.fatal("Error near rollback" + f.getMessage());
			}
			return Result.fatal("likePost Fatal: " + e.getMessage());
		}
	}

	@Override
	public Result<List<PersonView>> getLikers(long topicId) {
		if (!existLong("Topic", "topic_id", topicId)) {
			return Result.failure("Topic does not exist");
		}
		List<PersonView> personViewLikers = new ArrayList<PersonView>();
		PersonView personView;
		String sql = "SELECT Person.name, Person.username, Person.stuId FROM Topic "
				+ "JOIN Liketopic ON Topic.topic_id = Liketopic.liketopic_TopicId "
				+ "JOIN Person ON Person.username = liketopic_user  " + "WHERE topic_id = ? ORDER BY Person.name ASC";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, topicId);
			ResultSet r = p.executeQuery();
			while (r.next()) {
				String name = r.getString("name");
				String username = r.getString("username");
				String stuId = r.getString("stuId");
				personView = new PersonView(name, username, stuId);
				personViewLikers.add(personView);
			}
		} catch (SQLException e) {
			return Result.fatal("getLikers Fatal: " + e.getMessage());
		}
		return Result.success();
	}

	@Override
	public Result<TopicView> getTopic(long topicId) {
		TopicView topicView;
		if (!existLong("Topic", "topic_id", topicId)){
					return Result.failure("Topic does not exist");
		}
		String sql = "SELECT b.num, b.topic, b.likes, p.post_text, p.post_author, p.post_date, "
				+ "Person.name, t.topic_title, f.forum_id, f.forum_title FROM "
				+ "(SELECT Post.post_num AS num, Post.post_topic AS topic, "
				+ "IFNULL (a.likes, 0) AS likes FROM (SELECT COUNT(*) AS likes, post_num, p.post_topic "
				+ "FROM Post AS p JOIN Likepost AS l ON p.post_num = l.likepost_PostId "
				+ "WHERE l.likepost_TopicId = p.post_topic GROUP BY post_num, post_topic) AS a RIGHT "
				+ "JOIN Post ON Post.post_num = a.post_num AND Post.post_topic = a.post_topic) AS b "
				+ "JOIN Post AS p ON p.post_num = b.num AND p.post_topic = b.topic "
				+ "JOIN Person ON Person.username = p.post_author "
				+ "JOIN Topic AS t ON t.topic_id = p.post_topic JOIN Forum AS f "
				+ "ON f.forum_id = t.topic_forum WHERE t.topic_id = ?";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, topicId);
			ResultSet r = p.executeQuery();
			List<PostView> postViews = new ArrayList<PostView>();
			PostView pv;
			Long forumId = (long) 0;
			long temp = 0, temp1 = 0;
			String forumName = "";
			String title = "";
			boolean exist = r.next();
			while (exist) {
				forumId = r.getLong("forum_id");
				String toId = r.getString("topic");
				temp1 = Long.parseLong(toId);
				int postNumber = r.getInt("num");
				String authorName = r.getString("name");
				String authorUserName = r.getString("post_author");
				String text = r.getString("post_text");
				String postedAt = r.getString("post_date");
				forumName = r.getString("forum_title");
				title = r.getString("topic_title");
				int likes = r.getInt("likes");
				pv = new PostView(temp, temp1, postNumber, authorName, authorUserName, text, postedAt, likes);
				postViews.add(pv);
				exist = r.next();
			}
			topicView = new TopicView(forumId, temp1, forumName, title, postViews);
			return Result.success(topicView);
		} catch (SQLException e) {
			return Result.fatal("getTopic Fatal: " + e.getMessage());
		}
	}

	/* B.2 */

	@Override
	public Result<List<AdvancedForumSummaryView>> getAdvancedForums() {
		 throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Result<AdvancedPersonView> getAdvancedPersonView(String username) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Result<AdvancedForumView> getAdvancedForum(long id) {

		String sql = "SELECT forum_id, forum_title,c.likes,Person.name, Topic.topic_id,Topic.topic_creator, Topic.topic_title, Topic.topic_date, b.postnum AS postcount FROM (SELECT post_topic,COUNT(*) AS postnum FROM Post GROUP BY post_topic) b JOIN Topic ON Topic.topic_id = b.post_topic JOIN Person ON Person.username = Topic.topic_creator JOIN (SELECT topic_id, IFNULL(a.likes,0) AS likes FROM (SELECT COUNT(*) AS likes,liketopic_TopicId AS id FROM Liketopic GROUP BY liketopic_TopicId) a RIGHT JOIN Topic t ON t.topic_id = a.id) c ON c.topic_id = Topic.topic_id JOIN Forum ON Forum.forum_id = Topic.topic_forum WHERE forum_id = ?";
		AdvancedForumView forum;
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, id);
			ResultSet r = p.executeQuery();
			boolean exist = r.next();
			if (!exist) {
				return Result.failure("No topic in this forum, please create a new one.");
			}

			List<TopicSummaryView> topics = new ArrayList<TopicSummaryView>();
			String forumTitle = r.getString("forum_title");
			while (exist) {
				Long topicID = r.getLong("topic_id");
				Result<PostView> pv = getLatestPost(topicID);

				String topicTitle = r.getString("topic_title");
				int likes = r.getInt("likes");
				String creatorName = r.getString("name");
				String creatorUsername = r.getString("topic_creator");
				int postCount = r.getInt("postcount");
				String lastPostName = pv.getValue().getAuthorName();
				String lastPostTime = pv.getValue().getPostedAt();
				String created = r.getString("topic_date");
				if (postCount == 0)
					break;
				TopicSummaryView topic = new TopicSummaryView(topicID, id, topicTitle, postCount, created, lastPostTime,
						lastPostName, likes, creatorName, creatorUsername);
				topics.add(topic);
				exist = r.next();
			}
			forum = new AdvancedForumView(id, forumTitle, topics);
			return Result.success(forum);
		} catch (SQLException e) {
			return Result.fatal("getAdvancedForum Fatal: " + e.getMessage());
		}
	}

	private boolean existString(String table, String column, String value) {
		String sql = "SELECT " + column + " FROM " + table + " WHERE " + column + " = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, value);
			ResultSet r = p.executeQuery();
			boolean exist = r.next();
			if (exist)
				return true;
			else
				return false;
		} catch (SQLException e) {
			return false;
		}
	}


	private boolean existLong(String table, String column, Long value) {
		String sql = "SELECT " + column + " FROM " + table + " WHERE " + column + " = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setLong(1, value);
			ResultSet r = p.executeQuery();
			boolean exist = r.next();
			if (exist)
				return true;
			else
				return false;
		} catch (SQLException e) {
			return false;
		}
	}

// get the topic id from a forumId
	private long getTopicId(String title, long forumId) throws SQLException {
		String sql = "SELECT topic_id FROM Topic " + "LEFT JOIN Forum ON topic_forum = forum_id "
				+ "WHERE topic_title = ? AND topic_forum = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, title);
			p.setLong(2, forumId);
			ResultSet r = p.executeQuery();
			r.next();
			return r.getLong("topic_id");
		}
	}

// check if the topic existed
	private boolean existTopic(long forum, String topic) {
		String sql = "SELECT topic_title FROM Topic " + "WHERE topic_title = ? AND topic_forum = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, topic);
			p.setLong(2, forum);
			ResultSet r = p.executeQuery();
			if (r.next())
				return true;
			else
				return false;
		} catch (SQLException e) {
			return true;
		}
	}

	// if the topic is already being liked by the user
	private boolean checkLike(String value, long topicID) {
		String sql = "SELECT * FROM Liketopic " + "WHERE liketopic_user = ? AND liketopic_TopicId = ?";
		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, value);
			p.setLong(2, topicID);
			ResultSet r = p.executeQuery();
			return r.next();
		} catch (SQLException e) {
			return false;
		}
	}

	// check if the post is already being liked by the user
	private boolean checkLikePost(String username, long topicId, int post) {
		String sql = "SELECT * FROM Likepost "
				+ "WHERE likepost_user = ? AND likepost_PostId = ? AND  likepost_TopicId = ?";

		try (PreparedStatement p = c.prepareStatement(sql)) {
			p.setString(1, username);
			p.setLong(2, post);
			p.setLong(3, topicId);
			ResultSet r = p.executeQuery();
			return r.next();
		} catch (SQLException e) {
			return false;
		}
	}

}
