--
-- 1) Remove check constraint
--
ALTER TABLE DMS_Association
DROP CONSTRAINT dms_association_isactive_check
;

ALTER TABLE DMS_Content
DROP CONSTRAINT dms_content_isactive_check
;

--
-- 2) Create version records from existing content and association. also its linkable & version of the versioning content too.
--
WITH Whole_Data AS 
(
	WITH RECURSIVE SupplyTree(AD_Client_ID, DMS_Content_ID, DMS_Content_Related_ID, DMS_Association_ID) AS
	(
		SELECT c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.DMS_AssociationType_ID, a.AD_Table_ID, a.Record_ID, c.AD_Org_ID, c.Value, c.Name, c.ParentURL, a.SeqNo, c.DMS_FileSize, 1 AS LEVEL, ARRAY['', c.Value] AS Path
		FROM DMS_content c
		INNER JOIN DMS_association a ON (a.DMS_Content_ID = c.DMS_Content_ID) 
		WHERE ParentURL IS NULL AND COALESCE(a.DMS_AssociationType_ID, 0) <> 1000003

	UNION ALL

		SELECT c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.DMS_AssociationType_ID, a.AD_Table_ID, a.Record_ID, c.AD_Org_ID, c.Value, c.Name, c.ParentURL, a.SeqNo, c.DMS_FileSize, LEVEL + 1, Path || c.Value
		FROM  DMS_Association a 
		INNER JOIN supplytree st ON (st.DMS_Content_ID = a.DMS_Content_Related_ID)
		INNER JOIN DMS_content c ON (c.DMS_Content_ID = a.DMS_Content_ID)
		WHERE COALESCE(a.DMS_AssociationType_ID, 0) <> 1000003
	)
	SELECT s.*, c.IsActive, c.Created, c.CreatedBy, c.Updated, c.UpdatedBy
	FROM SupplyTree AS s 
	INNER JOIN DMS_content c ON (c.DMS_content_ID = s.DMS_content_ID)
	ORDER BY  Path
),

Version_Data AS
(
	WITH RECURSIVE SubSupplyTree( DMS_Content_ID, DMS_Content_Related_ID, DMS_Association_ID) AS
	(
		SELECT 	c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.DMS_AssociationType_ID, c.DMS_Content_ID AS Actual_DMS_Content_ID, 
				a.AD_Table_ID, a.Record_ID, c.Value, c.Name, c.ParentURL, a.SeqNo, 0 AS LEVEL, ARRAY['', c.Value] AS Path
		FROM DMS_content c
		INNER JOIN DMS_association a ON (a.DMS_Content_ID = c.DMS_Content_ID) 
		WHERE a.DMS_AssociationType_ID = 1000001 AND c.ContentBaseType = 'CNT'
			AND EXISTS ( SELECT * FROM DMS_association WHERE DMS_association.DMS_Content_Related_ID=c.DMS_Content_ID AND DMS_association.DMS_AssociationType_ID=1000000 )

	UNION ALL

		SELECT 	c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.DMS_AssociationType_ID, st.Actual_DMS_Content_ID, 
				a.AD_Table_ID, a.Record_ID, c.Value, c.Name, c.ParentURL, a.SeqNo, LEVEL + 1, Path || c.Value
		FROM  DMS_Association a 
		INNER JOIN SubSupplyTree st ON (st.DMS_Content_ID = a.DMS_Content_Related_ID )
		INNER JOIN DMS_content c ON (c.DMS_Content_ID = a.DMS_Content_ID)
		WHERE a.DMS_AssociationType_ID = 1000000 AND c.ContentBaseType = 'CNT' 
	)
	SELECT s.*
	FROM SubSupplyTree AS s 
	ORDER BY  Actual_DMS_Content_ID, SeqNo
),

Insert_Data_IN_DMS_Version AS
(
	INSERT INTO DMS_Version (	DMS_Version_ID, AD_Client_ID, AD_Org_ID, Created, CreatedBy, DMS_Version_UU, DMS_FileSize, IsActive, IsIndexed, Updated, UpdatedBy, Value, DMS_Content_ID, SeqNo)
	SELECT 
		(row_number() OVER (ORDER BY AD_Client_ID, AD_Org_ID)) + (SELECT CASE WHEN COALESCE(MAX(DMS_Version_ID),0)> 0 THEN COALESCE(MAX(DMS_Version_ID),0) ELSE 1000000 END FROM DMS_Version),
		d.AD_Client_ID, d.AD_Org_ID, d.Created, d.CreatedBy, Generate_UUID(), d.DMS_FileSize, d.IsActive, 'N', DATE_TRUNC('day', NOW()), 100, d.Value, 
		CASE WHEN COALESCE(d.DMS_AssociationType_ID, 0) = 1000000 THEN vd.Actual_DMS_Content_ID ELSE d.DMS_Content_ID END, 
		d.SeqNo
	FROM Whole_Data d
	LEFT JOIN Version_Data vd	ON (vd.DMS_Content_ID = d.DMS_Content_ID)
	WHERE COALESCE(d.DMS_AssociationType_ID, 0) <> 1000003
),

Make_InActive_DMS_Content_For_VersioningContent AS 
(
	UPDATE DMS_Content
	SET IsActive = 'V',
		Updated = DATE_TRUNC('day', NOW()),
		UpdatedBy = 100
	FROM Version_Data
	WHERE Version_Data.DMS_AssociationType_ID = 1000000 AND Version_Data.DMS_Content_ID = DMS_Content.DMS_Content_ID
),

Make_InActive_DMS_Association_For_VersioningAssociation AS 
(
	UPDATE DMS_Association
	SET IsActive = 'V',
		Updated = DATE_TRUNC('day', NOW()),
		UpdatedBy = 100
	FROM Version_Data
	WHERE Version_Data.DMS_AssociationType_ID = 1000000 AND Version_Data.DMS_Association_ID = DMS_Association.DMS_Association_ID
)

SELECT * FROM dual
;

--
-- 3) UPDATE SEQUENCE of the DMS_Version table based on flag for maintaining @ ERP or Native DB side
--

DO
$$
DECLARE
	isNativeSeq boolean;
	isVersionSeqExists boolean;
	newSeqNo int;
BEGIN
	SELECT Value='Y' FROM AD_SysConfig WHERE Name = 'SYSTEM_NATIVE_SEQUENCE'
	INTO isNativeSeq;

	SELECT COUNT(1) > 0 FROM pg_class WHERE relname ILIKE 'DMS_Version_SQ'
	INTO isVersionSeqExists;

	IF isNativeSeq IS TRUE THEN
		SELECT MAX(DMS_Version_ID)+1 FROM DMS_Version 
		INTO newSeqNo;

		IF isVersionSeqExists IS NOT TRUE THEN
			CREATE SEQUENCE DMS_Version_SQ
			INCREMENT BY 1 
			MINVALUE 1000000
			MAXVALUE 2147483647 
			START WITH 1000000;

			Raise Notice 'Creating new sequence in DB... %', (SELECT setval('DMS_Version_SQ', newSeqNo));
		ELSE
			Raise Notice 'Altering sequence in DB... %', (SELECT setval('DMS_Version_SQ', newSeqNo));
		END IF;
	ELSE
		UPDATE AD_Sequence SET CurrentNext=(SELECT MAX(DMS_Version_ID)+1 FROM DMS_Version) 
		WHERE AD_Sequence_ID=(SELECT AD_Sequence_ID FROM AD_Sequence WHERE Name='DMS_Version' AND IsActive='Y' AND IsTableID='Y' AND IsAutoSequence='Y');

		Raise Notice 'Altering AD_Sequence in DB... %', (SELECT MAX(DMS_Version_ID)+1 FROM DMS_Version);
	END IF;
END
$$
;