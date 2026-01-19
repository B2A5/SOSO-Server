-- Migration script: Rename voteboard_comments tables to votesboard_comments
-- Date: 2026-01-20
-- Purpose: 댓글 테이블명도 voteboard → votesboard로 통일

USE soso;

-- Step 1: voteboard_comment_like 테이블의 외래키 삭제
ALTER TABLE voteboard_comment_like DROP FOREIGN KEY FK6d590owf2twixvwryeoags48f;  -- user_id → users
ALTER TABLE voteboard_comment_like DROP FOREIGN KEY FK7r70dluy21kuvhrpb22eg1h43;  -- comment_id → voteboard_comments

-- Step 2: voteboard_comments 테이블의 외래키 삭제
ALTER TABLE voteboard_comments DROP FOREIGN KEY FK9l7xb562dg8lc56v0qoaww8c4;  -- user_id → users
ALTER TABLE voteboard_comments DROP FOREIGN KEY FKrf279hyo4o6qgyrx5q38i8gr3;  -- parent_id → voteboard_comments
ALTER TABLE voteboard_comments DROP FOREIGN KEY FK_voteboard_comments_votesboard;  -- votesboard_id → votesboard

-- Step 3: 테이블명 변경
ALTER TABLE voteboard_comments RENAME TO votesboard_comments;
ALTER TABLE voteboard_comment_like RENAME TO votesboard_comment_like;

-- Step 4: votesboard_comments 테이블의 외래키 재생성
ALTER TABLE votesboard_comments
    ADD CONSTRAINT FK_votesboard_comments_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE votesboard_comments
    ADD CONSTRAINT FK_votesboard_comments_parent
    FOREIGN KEY (parent_id) REFERENCES votesboard_comments(id);

ALTER TABLE votesboard_comments
    ADD CONSTRAINT FK_votesboard_comments_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

-- Step 5: votesboard_comment_like 테이블의 외래키 재생성
ALTER TABLE votesboard_comment_like
    ADD CONSTRAINT FK_votesboard_comment_like_user
    FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE votesboard_comment_like
    ADD CONSTRAINT FK_votesboard_comment_like_comment
    FOREIGN KEY (comment_id) REFERENCES votesboard_comments(id);

-- Step 6: 검증
SELECT 'Tables after migration:' AS status;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA='soso' AND TABLE_NAME LIKE '%votesboard%'
ORDER BY TABLE_NAME;

SELECT 'Foreign keys after migration:' AS status;
SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='soso'
  AND TABLE_NAME IN ('votesboard_comments', 'votesboard_comment_like')
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME;
