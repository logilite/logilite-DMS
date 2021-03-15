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

-- Delete various fields and column which are not used anymore in future

-- Delete field of Value column in content tab
DELETE FROM AD_Field
WHERE AD_Field_ID IN (SELECT AD_Field_ID FROM AD_Field f 
INNER JOIN AD_Column c ON (c.AD_Column_ID = f.AD_Column_ID)
WHERE c.ColumnName='Value' AND c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content'))
;

-- Delete field of DMS_FileSize column in content tab
DELETE FROM AD_Field
WHERE AD_Field_ID IN (SELECT AD_Field_ID FROM AD_Field f 
INNER JOIN AD_Column c ON (c.AD_Column_ID = f.AD_Column_ID)
WHERE c.ColumnName='DMS_FileSize' AND c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content'))
;

-- Delete field of IsIndexed column in content tab
DELETE FROM AD_Field
WHERE AD_Field_ID IN (SELECT AD_Field_ID FROM AD_Field f 
INNER JOIN AD_Column c ON (c.AD_Column_ID = f.AD_Column_ID)
WHERE c.ColumnName='IsIndexed' AND c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content'))
;

-- Delete field of SeqNo column in Association tab
DELETE FROM AD_Field
WHERE AD_Field_ID IN (SELECT AD_Field_ID FROM AD_Field f 
INNER JOIN AD_Column c ON (c.AD_Column_ID = f.AD_Column_ID)
WHERE c.ColumnName='SeqNo' AND c.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Association'))
;

-- Delete Value column from DMS_Content table
DELETE FROM AD_Column
WHERE ColumnName='Value' AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content')
;

-- Delete DMS_FileSize column from DMS_Content table
DELETE FROM AD_Column
WHERE ColumnName='DMS_FileSize' AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content')
;

-- Delete IsIndexed column from DMS_Content table
DELETE FROM AD_Column
WHERE ColumnName='IsIndexed' AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content')
;

-- Delete SeqNo column from DMS_Association table
DELETE FROM AD_Column
WHERE ColumnName='SeqNo' AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Association')
;
