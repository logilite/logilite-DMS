-- 
-- @Author Sachin Bhimani - DMS Directory structure hierachy tree
-- 

--	SELECT * FROM DMS_Content		ORDER BY created desc
--	SELECT * FROM DMS_Association	ORDER BY created desc

WITH RECURSIVE SupplyTree(AD_Client_ID, DMS_Content_ID, DMS_Content_Related_ID, DMS_Association_ID) AS
(
	SELECT c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID, c.Value, c.ParentURL, 1 AS LEVEL, ARRAY['', c.Value] AS Path
	FROM DMS_content c
	INNER JOIN DMS_association a ON (a.DMS_Content_ID = c.DMS_Content_ID) 
	WHERE ParentURL IS NULL
	-- WHERE c.Value = 'Attachment' AND c.Name = 'Attachment'
	--WHERE c.Value = 'Archive' AND c.Name = 'Archive'

UNION ALL

	SELECT c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID, c.Value, c.ParentURL,  LEVEL + 1, Path || c.Value
	FROM  DMS_Association a 
	INNER JOIN supplytree st ON (st.DMS_Content_ID = a.DMS_Content_Related_ID)
	INNER JOIN DMS_content c ON (c.DMS_Content_ID = a.DMS_Content_ID)
	WHERE c.ContentBaseType='DIR'
)
SELECT c.IsActive, s.*
FROM SupplyTree AS s 
INNER JOIN DMS_content c ON (c.DMS_content_ID = s.DMS_content_ID)		
-- WHERE Level <= 4
-- ORDER BY  s.AD_Client_ID, Level, ParentURL, DMS_Content_ID, DMS_Content_Related_ID, AD_Table_ID, Record_ID, Path
ORDER BY  s.AD_Client_ID, Path, ParentURL, DMS_Content_ID, AD_Table_ID, Record_ID, DMS_Content_Related_ID
;