-- comments 테이블의 created_at null 값을 현재 시각으로 백필
UPDATE comments SET created_at = NOW() WHERE created_at IS NULL;
UPDATE comments SET updated_at = NOW() WHERE updated_at IS NULL;
