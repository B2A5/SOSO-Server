-- created_date 컬럼(구버전)의 값을 created_at(신버전)으로 복사
-- post 테이블: created_at이 전부 NULL, created_date에 실제 값 존재
UPDATE post SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE post SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;

-- polls 테이블: 일부 행 created_at NULL
UPDATE polls SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE polls SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;

-- comments 테이블: created_at은 이미 정상, 혹시 모를 NULL만 처리
UPDATE comments SET created_at = created_date WHERE created_at IS NULL AND created_date IS NOT NULL;
UPDATE comments SET updated_at = last_modified_date WHERE updated_at IS NULL AND last_modified_date IS NOT NULL;

-- 위에서도 못 채운 경우 현재 시각으로 폴백
UPDATE post SET created_at = NOW() WHERE created_at IS NULL;
UPDATE post SET updated_at = NOW() WHERE updated_at IS NULL;
UPDATE polls SET created_at = NOW() WHERE created_at IS NULL;
UPDATE polls SET updated_at = NOW() WHERE updated_at IS NULL;
UPDATE comments SET created_at = NOW() WHERE created_at IS NULL;
UPDATE comments SET updated_at = NOW() WHERE updated_at IS NULL;
