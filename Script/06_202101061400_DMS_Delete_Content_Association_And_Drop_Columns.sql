-- Delete the unused content and association records
DELETE FROM DMS_Association		WHERE IsActive ='V';
DELETE FROM DMS_Content 		WHERE IsActive ='V';

-- Drop column which is not used after DMS Versioning implementation
ALTER TABLE DMS_Content 		DROP COLUMN Value;
ALTER TABLE DMS_Content 		DROP COLUMN DMS_FileSize;
ALTER TABLE DMS_Content 		DROP COLUMN IsIndexed;
ALTER TABLE DMS_Association 	DROP COLUMN SeqNo;

-- Re-create check constraint
ALTER TABLE DMS_Association ADD CONSTRAINT dms_association_isactive_check CHECK (isactive = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]))
;

ALTER TABLE DMS_Content ADD CONSTRAINT dms_content_isactive_check CHECK (isactive = ANY (ARRAY['Y'::bpchar, 'N'::bpchar]))
;

-- Do inActive fields of Content & Association tabs
UPDATE AD_Field
SET 	IsActive = 'N',
		Updated = NOW()
WHERE AD_Field_ID IN 
	(SELECT AD_Field_ID FROM AD_Field
	INNER JOIN AD_Column  ON (AD_Column.AD_Column_ID = AD_Field.AD_Column_ID)
	WHERE(	(	AD_Column.ColumnName='Value' 
			 OR AD_Column.ColumnName='DMS_FileSize' 
			 OR AD_Column.ColumnName='IsIndexed'	
			 )
	   		AND AD_Column.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content')
		 )
		OR
		(	(	AD_Column.ColumnName='SeqNo'	) 
		 	AND AD_Column.AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Association')
		)
	)
;

-- Do inActive columns of DMS_Content & DMS_Association table
UPDATE AD_Column
SET 	IsActive = 'N',
		Updated = NOW()
WHERE	(	(  ColumnName='Value' 
			OR ColumnName='DMS_FileSize' 
			OR ColumnName='IsIndexed' 
			) 
			AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Content')
		)
		OR
		(	ColumnName='SeqNo' 
		 	AND AD_Table_ID = (SELECT AD_Table_ID FROM AD_Table WHERE TableName = 'DMS_Association')
		)
;