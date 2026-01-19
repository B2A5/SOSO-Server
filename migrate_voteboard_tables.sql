-- Migration script: Rename vote_post tables to votesboard
-- Date: 2026-01-19
-- Purpose: Rename VotePost → Votesboard for consistency

USE soso;

-- Step 1: Drop all foreign key constraints that reference vote_post
ALTER TABLE vote_option DROP FOREIGN KEY FK6rhes1ro1e1hmcqxtk48ic2wj;
ALTER TABLE vote_post_image DROP FOREIGN KEY FKoatckkxyb3lcrr4njn30uloly;
ALTER TABLE vote_post_like DROP FOREIGN KEY FKhboicurwjn8qnvbxs7yxt9xj0;
ALTER TABLE vote_result DROP FOREIGN KEY FKdk0hps6077koawa2b41eddg46;
ALTER TABLE voteboard_comments DROP FOREIGN KEY FK16iffasjtihye2wo8sru5ebwi;

-- Step 2: Rename the main table
ALTER TABLE vote_post RENAME TO votesboard;

-- Step 3: Rename related tables
ALTER TABLE vote_post_image RENAME TO votesboard_image;
ALTER TABLE vote_post_like RENAME TO votesboard_like;

-- Step 4: Rename foreign key columns in all tables
ALTER TABLE vote_option CHANGE COLUMN vote_post_id votesboard_id BIGINT NOT NULL;
ALTER TABLE votesboard_image CHANGE COLUMN vote_post_id votesboard_id BIGINT NOT NULL;
ALTER TABLE votesboard_like CHANGE COLUMN vote_post_id votesboard_id BIGINT NOT NULL;
ALTER TABLE vote_result CHANGE COLUMN vote_post_id votesboard_id BIGINT NOT NULL;
ALTER TABLE voteboard_comments CHANGE COLUMN vote_post_id votesboard_id BIGINT NOT NULL;

-- Step 5: Re-create foreign key constraints with new names
ALTER TABLE vote_option
    ADD CONSTRAINT FK_vote_option_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

ALTER TABLE votesboard_image
    ADD CONSTRAINT FK_votesboard_image_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

ALTER TABLE votesboard_like
    ADD CONSTRAINT FK_votesboard_like_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

ALTER TABLE vote_result
    ADD CONSTRAINT FK_vote_result_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

ALTER TABLE voteboard_comments
    ADD CONSTRAINT FK_voteboard_comments_votesboard
    FOREIGN KEY (votesboard_id) REFERENCES votesboard(id);

-- Verification queries
SELECT 'Tables after migration:' AS status;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA='soso' AND TABLE_NAME LIKE '%vote%'
ORDER BY TABLE_NAME;

SELECT 'Foreign keys after migration:' AS status;
SELECT TABLE_NAME, COLUMN_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='soso' AND REFERENCED_TABLE_NAME='votesboard'
ORDER BY TABLE_NAME;
