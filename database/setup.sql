DROP TABLE IF EXISTS Liketopic;
DROP TABLE IF EXISTS Likepost;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS Topic;
DROP TABLE IF EXISTS Forum;
DROP TABLE IF EXISTS Person;

CREATE TABLE Person (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    username VARCHAR(10) NOT NULL UNIQUE,
    stuId VARCHAR(10) NULL
);

CREATE TABLE Forum (
    forum_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    forum_title VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE Topic (
    topic_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    topic_title VARCHAR(100) NOT NULL,
    topic_forum INTEGER NOT NULL,
    topic_creator VARCHAR(10) NOT NULL,
    topic_date VARCHAR(50) NOT NULL,
    FOREIGN KEY (topic_creator) REFERENCES Person(username),
    FOREIGN KEY (topic_forum) REFERENCES Forum(forum_id)
);

CREATE TABLE Post (
    post_num INTEGER NOT NULL,
    post_text VARCHAR(200) NOT NULL,
    post_author VARCHAR(10) NOT NULL,
    post_topic INTEGER NOT NULL,
    post_date VARCHAR(50) NOT NULL,
    FOREIGN KEY (post_author) REFERENCES Person(username),
    FOREIGN KEY (post_topic) REFERENCES Topic(topic_id),
    PRIMARY KEY (post_num, post_topic)
);

CREATE TABLE Likepost (
    likepost_user VARCHAR(10),
    likepost_PostId INTEGER,
    likepost_TopicId INTEGER,
    FOREIGN KEY (likepost_user) REFERENCES Person(username),
    FOREIGN KEY (likepost_PostId) REFERENCES Post(post_num),
    FOREIGN KEY (likepost_TopicId) REFERENCES Topic(topic_id),
    PRIMARY KEY (likepost_user, likepost_PostId, likepost_TopicId)
);

CREATE TABLE Liketopic (
    liketopic_user VARCHAR(10),
    liketopic_TopicId INTEGER,
    FOREIGN KEY (liketopic_user) REFERENCES Person(username),
    FOREIGN KEY (liketopic_TopicId) REFERENCES Topic(topic_id),
    PRIMARY KEY (liketopic_user, liketopic_TopicId)
);

INSERT INTO Person (name, username, stuId) VALUES ("Wei-Shen","wilson17",17865 );
INSERT INTO Person (name, username, stuId) VALUES ("Feihua","feihua17",17637 );
INSERT INTO Person (name, username, stuId) VALUES ("Jun","jun17",17501 );
INSERT INTO Person (name, username, stuId) VALUES ("Siyu","siyu17",17513 );
INSERT INTO Person (name, username, stuId) VALUES ("Uob","uob17","" );

INSERT INTO Forum (forum_title) VALUES ("Computers Science");
INSERT INTO Forum (forum_title) VALUES ("Marketing");
INSERT INTO Forum (forum_title) VALUES ("Alpha");

INSERT INTO Topic (topic_title, topic_forum, topic_creator, topic_date) VALUES ("Java",1,"wilson17", "2018.01.20 - 13:11:41");
INSERT INTO Topic (topic_title, topic_forum, topic_creator, topic_date) VALUES ("Databases",1,"wilson17", "2020.07.04 - 19:09:27");
INSERT INTO Topic (topic_title, topic_forum, topic_creator, topic_date) VALUES ("Databases",3,"siyu17", "2018.04.11 - 19:09:27");

INSERT INTO Post VALUES (1, "java assassingment, requirments","wilson17", 1, "2018.01.20 - 19:09:41");
INSERT INTO Post VALUES (1, "MariaDB","wilson17", 2, "2020.07.04 - 19:09:27");
INSERT INTO Post VALUES (1, "MariaDB","siyu17", 3, "2018.04.11 - 19:09:27");

