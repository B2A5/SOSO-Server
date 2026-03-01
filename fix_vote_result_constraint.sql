-- Fix vote_result unique constraint
-- Date: 2026-02-09
-- Purpose: Remove incorrect UK constraint and ensure correct one exists

USE soso;

-- Step 1: Drop the incorrect unique constraint (user_id, votesboard_id only)
ALTER TABLE vote_result DROP INDEX uk_vote_result_user_post;

-- Step 2: Ensure the correct unique constraint exists (user_id, votesboard_id, vote_option_id)
-- If it doesn't exist, this will create it. If it exists, this will fail but it's safe to ignore.
ALTER TABLE vote_result
ADD CONSTRAINT uk_vote_result_user_post_option
UNIQUE KEY (user_id, votesboard_id, vote_option_id);

-- Verification
SHOW CREATE TABLE vote_result\G
