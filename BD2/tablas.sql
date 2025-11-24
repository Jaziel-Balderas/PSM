CREATE DATABASE IF NOT EXISTS psm2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE psm2;

DROP TABLE IF EXISTS users;
CREATE TABLE users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  nameuser VARCHAR(100) NOT NULL,
  lastnames VARCHAR(150) NULL,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(25) NULL,
  direccion VARCHAR(255) NULL,
  profile_image_url MEDIUMBLOB NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- =====================
-- Tabla: posts
-- =====================
DROP TABLE IF EXISTS posts;
CREATE TABLE posts (
  post_id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  title VARCHAR(255) NULL,
  content TEXT NOT NULL,
  location VARCHAR(255) NULL,
  is_public TINYINT NOT NULL DEFAULT 1,
  likes_count INT NOT NULL DEFAULT 0,
  dislikes_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_posts_user ON posts(user_id);
CREATE INDEX idx_posts_created ON posts(created_at);

-- =====================
-- Tabla: post_comments
-- =====================
DROP TABLE IF EXISTS post_comments;
CREATE TABLE post_comments (
  comment_id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  comment_text TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  likes_count INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_post ON post_comments(post_id);
CREATE INDEX idx_comments_user ON post_comments(user_id);

-- =====================
-- Tabla: post_votes
-- =====================
DROP TABLE IF EXISTS post_votes;
CREATE TABLE post_votes (
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  vote TINYINT NOT NULL, -- 1 like, -1 dislike
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (post_id, user_id),
  CONSTRAINT fk_votes_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
  -- MySQL 8.0 opcional: , CHECK (vote IN (-1,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_votes_post ON post_votes(post_id);
CREATE INDEX idx_votes_user ON post_votes(user_id);

-- =====================
-- Tabla: post_images
-- =====================
DROP TABLE IF EXISTS post_images;
CREATE TABLE post_images (
  image_id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  description VARCHAR(255) NULL,
  image_data LONGBLOB NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_images_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_images_post ON post_images(post_id);

-- =====================
-- Tabla: comment_likes
-- =====================
DROP TABLE IF EXISTS comment_likes;
CREATE TABLE comment_likes (
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (comment_id, user_id),
  CONSTRAINT fk_commentlikes_comment FOREIGN KEY (comment_id) REFERENCES post_comments(comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_commentlikes_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_commentlikes_comment ON comment_likes(comment_id);
CREATE INDEX idx_commentlikes_user ON comment_likes(user_id);

-- =====================
-- Tabla: comment_replies
-- =====================
DROP TABLE IF EXISTS comment_replies;
CREATE TABLE comment_replies (
  reply_id INT AUTO_INCREMENT PRIMARY KEY,
  comment_id INT NOT NULL,
  user_id INT NOT NULL,
  reply_text TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_replies_comment FOREIGN KEY (comment_id) REFERENCES post_comments(comment_id) ON DELETE CASCADE,
  CONSTRAINT fk_replies_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_replies_comment ON comment_replies(comment_id);
CREATE INDEX idx_replies_user ON comment_replies(user_id);

SET FOREIGN_KEY_CHECKS=1;

-- =====================
-- Stored Procedure: sp_obtener_publicaciones_v2
-- =====================
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_obtener_publicaciones_v2$$

CREATE PROCEDURE sp_obtener_publicaciones_v2(
  IN p_user_id INT,
  IN p_limit INT,
  IN p_offset INT
)
BEGIN
  SELECT 
    p.post_id,
    p.user_id,
    p.title,
    p.content,
    p.location,
    p.is_public,
    p.likes_count,
    p.dislikes_count,
    p.created_at,
    COALESCE(v.vote, 0) AS user_vote
  FROM posts p
  LEFT JOIN post_votes v ON p.post_id = v.post_id AND v.user_id = p_user_id
  ORDER BY p.created_at DESC
  LIMIT p_limit OFFSET p_offset;
END$$

DELIMITER ;