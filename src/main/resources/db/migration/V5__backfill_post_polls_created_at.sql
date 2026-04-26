-- V4 rename 이후 테이블명 기준으로 created_at 백필
-- freeboard_posts: created_at이 전부 NULL, created_date에 실제 값 존재
UPDATE freeboard_posts SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE freeboard_posts SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;
UPDATE freeboard_posts SET created_at = NOW() WHERE created_at IS NULL;
UPDATE freeboard_posts SET updated_at = NOW() WHERE updated_at IS NULL;

-- pollboard_posts: 일부 행 created_at NULL
UPDATE pollboard_posts SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE pollboard_posts SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;
UPDATE pollboard_posts SET created_at = NOW() WHERE created_at IS NULL;
UPDATE pollboard_posts SET updated_at = NOW() WHERE updated_at IS NULL;

-- freeboard_comments: 혹시 모를 NULL 처리
UPDATE freeboard_comments SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE freeboard_comments SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;
UPDATE freeboard_comments SET created_at = NOW() WHERE created_at IS NULL;
UPDATE freeboard_comments SET updated_at = NOW() WHERE updated_at IS NULL;
