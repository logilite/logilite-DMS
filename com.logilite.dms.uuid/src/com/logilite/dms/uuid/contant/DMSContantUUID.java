package com.logilite.dms.uuid.contant;

import org.idempiere.dms.constant.DMSConstant;

/**
 * DMS constant class for UUID
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMSContantUUID
{

	public static final String	SQL_OLD_NEW_PATH	= " WITH RECURSIVE SupplyTree(AD_Client_ID, DMS_Content_ID, DMS_Content_Related_ID, DMS_Association_ID) AS 							"
														+ " ( 																															"
														+ " 	SELECT 	c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID,	"
														+ " 			c.Value, c.Name, c.ParentURL, 1 AS LEVEL, ARRAY['', c.Value] AS Path, a.DMS_AssociationType_ID 					"
														+ " 	FROM DMS_content c 																										"
														+ " 	INNER JOIN DMS_association a 	ON (a.DMS_Content_ID = c.DMS_Content_ID) 												"
														+ " 	WHERE ParentURL IS NULL AND c.AD_Client_ID = ? 																			"
														+ " UNION ALL 																													"
														+ " 	SELECT	c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID,	"
														+ " 			c.Value, c.Name, c.ParentURL,  LEVEL + 1, Path || c.Value, a.DMS_AssociationType_ID 							"
														+ " 	FROM  DMS_Association a 																								"
														+ " 	INNER JOIN supplytree st 		ON (st.DMS_Content_ID = a.DMS_Content_Related_ID AND a.AD_Client_ID = ?) 				"
														+ " 	INNER JOIN DMS_content c 		ON (c.DMS_Content_ID = a.DMS_Content_ID) 												"
														+ " 	WHERE a.DMS_AssociationType_ID IS NULL OR a.DMS_AssociationType_ID = 1000001											"
														+ " )																															"
														+ " SELECT 																														"
														+ " 	'mv -v ' AS Command 																									"
														+ " 	, '' || COALESCE(s.ParentURL, '') || '" + DMSConstant.FILE_SEPARATOR
														+ "' 			|| v.Value || '' AS oldURL 																						"
														+ " 	, '' || COALESCE(s.ParentURL, '') || '" + DMSConstant.FILE_SEPARATOR
														+ "' 			|| v.DMS_Version_UU || '' AS NewURL 																			"
														// + " , s.level, s.* "
														+ " FROM SupplyTree AS s 																										"
														+ " INNER JOIN DMS_content c 			ON (c.DMS_content_ID = s.DMS_content_ID) 												"
														+ " INNER JOIN DMS_Version v 			ON (v.DMS_content_ID = c.DMS_content_ID) 												"
														+ " WHERE (c.IsMounting = 'Y' AND Level > 2) OR (c.IsMounting = 'N')															"
														+ " ORDER BY Level DESC, s.AD_Client_ID, s.ParentURL, Path, s.DMS_Content_ID, s.AD_Table_ID, s.Record_ID, s.DMS_Content_Related_ID";

	public static final String	SQL_COUNT_VERSION	= "SELECT COUNT(1) FROM DMS_Version v "
														+ " INNER JOIN DMS_content c ON (c.DMS_content_ID = v.DMS_content_ID)"
														+ " WHERE v.AD_Client_ID = ? AND c.IsMounting = 'N' ";

}
