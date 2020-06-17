-- 
-- @Author Sachin Bhimani - 2020-06-17 - DMS correction for wrong structure
-- 
-- README FIRST...!!!
-- 
-- Correction of existing DMS mounting structure having issue of multiple time same content created like
--  /Attachment
--  /Attachment
--  /Attachment
--  /Attachment/EBX_Listings
--  /Attachment/EBX_Listings
--  /Attachment/EBX_Listings
--  /Attachment/EBX_TradeRecords
--  /Attachment/EBX_TradeRecords
--
--
-- Ideally it should be like:
--  /Attachment
--  /Attachment/EBX_Listings
--  /Attachment/EBX_TradeRecords
-- 
-- 
-- Must deploy below changeset when before/after apply this sql script [Branch: iDempiere-4.1]
-- Changeset:340 (52492bdd9aa0) Prevent to show root content in New record entry in DMS as Tab configured.
-- Changeset:341 (1772501ba64d) Prevent to create duplicate mounting content entry.
-- Changeset:342 (            ) Allow to create content entry when folder exists but no DB records. - Correction SQL script for wrong DMS Mounting structure.
-- 


-- 1)
-- Set Table and Record_ID as NULL for Mounting Root content
--
UPDATE 	DMS_Association
SET 	AD_Table_ID = NULL, 
	Record_ID = NULL
FROM DMS_Content 
WHERE 		DMS_Content.ParentURL IS NULL 
	AND 	DMS_Content.ContentBaseType = 'DIR' 
	AND 	DMS_Content.IsMounting = 'Y'
	AND (	DMS_Association.AD_Table_ID IS NOT NULL 	OR 	DMS_Association.Record_ID IS NOT NULL	)
	AND 	DMS_Association.DMS_Content_ID = DMS_Content.DMS_Content_ID
;

-- 2)
-- Set Record_ID as NULL for Mounting subroot content
--
UPDATE 	DMS_Association
SET 	Record_ID = NULL
FROM 	DMS_Content 
WHERE 	(	DMS_Content.ParentURL = '\' || (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE') 
		OR
		DMS_Content.ParentURL = '/' || (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	)
	AND 	DMS_Content.ContentBaseType = 'DIR' 
	AND 	DMS_Content.IsMounting = 'Y'
	AND 	DMS_Association.Record_ID IS NOT NULL
	AND 	DMS_Association.DMS_Content_ID = DMS_Content.DMS_Content_ID
;

-- 3)
-- Correction for multiple Attachment content created
-- Do InActive - DMS_Association
WITH ActiveAttachmentContent AS
(
	SELECT 	AD_Client_ID, MIN(DMS_Content_ID) AS DMS_Content_ID
	FROM 	DMS_Content
	WHERE		DMS_Content.ParentURL IS NULL 
		AND 	DMS_Content.ContentBaseType = 'DIR' 
		AND 	DMS_Content.IsMounting = 'Y'
		AND 	DMS_Content.IsActive = 'Y'
		AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	GROUP BY AD_Client_ID
	ORDER BY 1
),

DoInActive AS
(
	SELECT 	c.DMS_Content_ID, a.DMS_Association_ID
	FROM 	DMS_Content c
	INNER JOIN DMS_Association a 		ON (a.DMS_Content_ID = c.DMS_Content_ID)
	INNER JOIN ActiveAttachmentContent aac 	ON (aac.AD_Client_ID = c.AD_Client_ID AND aac.DMS_Content_ID <> c.DMS_Content_ID)
	WHERE		c.ParentURL IS NULL 
		AND 	c.ContentBaseType = 'DIR' 
		AND 	c.IsMounting = 'Y'
		AND 	c.IsActive = 'Y'
		AND 	c.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
)

-- SELECT * FROM DoInActive

UPDATE 	DMS_Association
SET 	IsActive = 'N'
FROM 	DoInActive
WHERE 	DoInActive.DMS_Association_ID = DMS_Association.DMS_Association_ID
;

-- 4)
-- Correction for multiple Attachment content created
-- Do InActive - DMS_Content
WITH ActiveAttachmentContent AS
(
	SELECT 	AD_Client_ID, MIN(DMS_Content_ID) AS DMS_Content_ID
	FROM 	DMS_Content
	WHERE		DMS_Content.ParentURL IS NULL 
		AND 	DMS_Content.ContentBaseType = 'DIR' 
		AND 	DMS_Content.IsMounting = 'Y'
		AND 	DMS_Content.IsActive = 'Y'
		AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	GROUP BY AD_Client_ID
	ORDER BY 1
),

DoInActive AS
(
	SELECT 	c.DMS_Content_ID, a.DMS_Association_ID
	FROM 	DMS_Content c
	INNER JOIN DMS_Association a 		ON (a.DMS_Content_ID = c.DMS_Content_ID)
	INNER JOIN ActiveAttachmentContent aac 	ON (aac.AD_Client_ID = c.AD_Client_ID AND aac.DMS_Content_ID <> c.DMS_Content_ID)
	WHERE		c.ParentURL IS NULL 
		AND 	c.ContentBaseType = 'DIR' 
		AND 	c.IsMounting = 'Y'
		AND 	c.IsActive = 'Y'
		AND 	c.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
)

-- SELECT * FROM DoInActive

UPDATE 	DMS_Content
SET 	IsActive = 'N'
FROM 	DoInActive
WHERE 	DoInActive.DMS_Content_ID = DMS_Content.DMS_Content_ID
;

-- 5) A)
-- Retrieve list of sub-content of InActive Attachment Content record [ After applying correction Query it should be Zero record ]
--
WITH ActiveAttachmentContent AS
(
	SELECT 	AD_Client_ID, DMS_Content_ID
	FROM 	DMS_Content
	WHERE		DMS_Content.ParentURL IS NULL 
		AND 	DMS_Content.ContentBaseType = 'DIR' 
		AND 	DMS_Content.IsMounting = 'Y'
		AND 	DMS_Content.IsActive = 'Y'
		AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	ORDER BY 1
)

SELECT ActiveAttachmentContent.*, DMS_Content.DMS_Content_ID,  DMS_Association.*
FROM DMS_Content
INNER JOIN DMS_Association 		ON (DMS_Association.DMS_Content_Related_ID = DMS_Content.DMS_Content_ID )
INNER JOIN ActiveAttachmentContent 	ON (ActiveAttachmentContent.AD_Client_ID = DMS_Content.AD_Client_ID)
WHERE		DMS_Content.ParentURL IS NULL 
	AND 	DMS_Content.ContentBaseType = 'DIR' 
	AND 	DMS_Content.IsMounting = 'Y'
	AND 	DMS_Content.IsActive = 'N'
	AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
;

-- 5) B)
--  Update Active Attahcment content reference to sub-content of InActive Attachment Content record 
--
WITH ActiveAttachmentContent AS
(
	SELECT 	AD_Client_ID, DMS_Content_ID
	FROM 	DMS_Content
	WHERE		DMS_Content.ParentURL IS NULL 
		AND 	DMS_Content.ContentBaseType = 'DIR' 
		AND 	DMS_Content.IsMounting = 'Y'
		AND 	DMS_Content.IsActive = 'Y'
		AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	ORDER BY 1
)

UPDATE 	DMS_Association	
SET 	DMS_Content_Related_ID = ActiveAttachmentContent.DMS_Content_ID
FROM 	DMS_Content 
INNER JOIN ActiveAttachmentContent 	ON (ActiveAttachmentContent.AD_Client_ID = DMS_Content.AD_Client_ID)
WHERE		DMS_Content.ParentURL IS NULL 
	AND 	DMS_Content.ContentBaseType = 'DIR' 
	AND 	DMS_Content.IsMounting = 'Y'
	AND 	DMS_Content.IsActive = 'N'
	AND 	DMS_Content.Name = (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
	AND 	DMS_Association.DMS_Content_Related_ID = DMS_Content.DMS_Content_ID
;


-- 6)
-- Make InActive SubRoot content and correcting its related content reference
-- 
WITH SubAttachmentContent AS
(
	SELECT c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Association_ID, c.Name, c.ParentURL, a.AD_Table_ID, a.Record_ID, a.DMS_Content_Related_ID
	FROM 	DMS_Content c
	INNER JOIN DMS_Association a	ON ( a.DMS_Content_ID = c.DMS_Content_ID )
	WHERE 	(	c.ParentURL = '\' || (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE') 
			OR
			c.ParentURL = '/' || (SELECT TRIM(VALUE) FROM AD_SysConfig	WHERE NAME ilike 'DMS_MOUNTING_BASE')
		)
		AND 	c.ContentBaseType = 'DIR' 
		AND 	c.IsMounting = 'Y'
		AND 	a.AD_Table_ID IS NOT NULL  
		AND	a.Record_ID IS NULL
		AND 	a.DMS_Content_ID = c.DMS_Content_ID
)

, ActiveSubAttachmentContent AS
(
	SELECT AD_Client_ID, AD_Table_ID, Name, ParentURL, MIN (DMS_Content_ID) AS DMS_Content_ID
	FROM SubAttachmentContent
	GROUP BY AD_Client_ID, AD_Table_ID, Name, ParentURL
)
-- SELECT * FROM ActiveSubAttachmentContent

, DoInActive AS
(
	SELECT * FROM SubAttachmentContent sac
	WHERE sac.DMS_Content_ID NOT IN (SELECT DMS_Content_ID FROM ActiveSubAttachmentContent)
)
-- SELECT * FROM DoInActive

, InActiveAssosiation AS
(
 	UPDATE 	DMS_Association	
 	SET 	IsActive = 'N'
 	FROM 	DoInActive 
 	WHERE DoInActive.DMS_Association_ID = DMS_Association.DMS_Association_ID
)

, InActiveContent AS
(
 	UPDATE 	DMS_Content 
	SET 	IsActive = 'N'
 	FROM 	DoInActive 
 	WHERE DoInActive.DMS_Content_ID = DMS_Content.DMS_Content_ID
)

-- SELECT 	a.DMS_Content_ID, a.DMS_Association_ID, a.DMS_Content_Related_ID, asac.DMS_Content_ID
-- FROM DoInActive dia
-- INNER JOIN DMS_Association a			ON (a.DMS_Content_Related_ID = dia.DMS_Content_ID)
-- INNER JOIN ActiveSubAttachmentContent asac	ON (asac.AD_Client_ID = a.AD_Client_ID AND asac.AD_Table_ID = a.AD_Table_ID) 
-- ORDER BY asac.DMS_Content_ID


UPDATE 	DMS_Association 
SET 	DMS_Content_Related_ID = asac.DMS_Content_ID
FROM 	DoInActive dia
INNER JOIN ActiveSubAttachmentContent asac	ON (asac.AD_Client_ID = dia.AD_Client_ID ) 
WHERE DMS_Association.DMS_Content_Related_ID = dia.DMS_Content_ID AND asac.AD_Client_ID = DMS_Association.AD_Client_ID AND asac.AD_Table_ID = DMS_Association.AD_Table_ID;
