-- Delete the unused content and association records
DELETE FROM DMS_Association		WHERE IsActive ='V';
DELETE FROM DMS_Content 		WHERE IsActive ='V';

-- Drop column which is not used after DMS Versioning implementation
ALTER TABLE DMS_Content 		DROP COLUMN Value;
ALTER TABLE DMS_Content 		DROP COLUMN DMS_FileSize;
ALTER TABLE DMS_Content 		DROP COLUMN IsIndexed;
ALTER TABLE DMS_Association 	DROP COLUMN SeqNo;

-- Mark DMS_Content table having Value column to InActive
UPDATE AD_Column
SET IsActive = 'N'
WHERE AD_Column_UU='9125047d-e029-420e-ab27-d6ca29d2c6c3'
;

-- Re-create check constraint
ALTER TABLE DMS_Association ADD CONSTRAINT dms_association_isactive_check CHECK (isactive = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]))
;

ALTER TABLE DMS_Content ADD CONSTRAINT dms_content_isactive_check CHECK (isactive = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]))
;