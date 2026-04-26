-- 자유게시판 테이블 rename
RENAME TABLE post                TO freeboard_posts;
RENAME TABLE post_image          TO freeboard_post_images;
RENAME TABLE post_like           TO freeboard_post_likes;
RENAME TABLE comments            TO freeboard_comments;
RENAME TABLE comment_likes       TO freeboard_comment_likes;

-- 투표게시판 테이블 rename
RENAME TABLE polls               TO pollboard_posts;
RENAME TABLE votesboard_image    TO pollboard_post_images;
RENAME TABLE votesboard_like     TO pollboard_post_likes;
RENAME TABLE votesboard_comments TO pollboard_comments;
RENAME TABLE votesboard_comment_like TO pollboard_comment_likes;
RENAME TABLE vote_option         TO pollboard_vote_options;
RENAME TABLE vote_result         TO pollboard_vote_results;

-- FK 컬럼명 rename (votesboard_id → poll_id)
ALTER TABLE pollboard_comments    RENAME COLUMN votesboard_id TO poll_id;
ALTER TABLE pollboard_post_images RENAME COLUMN votesboard_id TO poll_id;
ALTER TABLE pollboard_post_likes  RENAME COLUMN votesboard_id TO poll_id;
ALTER TABLE pollboard_vote_options RENAME COLUMN votesboard_id TO poll_id;
ALTER TABLE pollboard_vote_results RENAME COLUMN votesboard_id TO poll_id;
ALTER TABLE pollboard_vote_results RENAME COLUMN vote_option_id TO poll_option_id;
