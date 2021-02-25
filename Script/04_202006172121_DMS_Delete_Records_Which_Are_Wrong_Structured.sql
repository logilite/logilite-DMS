-- 1)
-- Delete the ignorable data from DMS_Association and DMS_Content table
-- 
DELETE FROM DMS_Association	WHERE IsIgnorable = 'Y'
;

DELETE FROM DMS_Content		WHERE IsIgnorable = 'Y'
;

-- 2)
-- Remove column added for ignorable records based on script: 03_202006172121_DMS_Update_Script_for_Wrong_Structure_Created.sql
-- 
ALTER TABLE DMS_Association
DROP COLUMN IsIgnorable
;

ALTER TABLE DMS_Content
DROP COLUMN IsIgnorable
;