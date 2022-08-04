package com.logilite.dms.uuid.contant;

/**
 * DMS constant class for UUID
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class DMSContantUUID
{
	public static final String	ARCHIVE_STORAGE_METHOD_DMS		= "DMS";
	public static final String	ARCHIVE_STORAGE_METHOD_DMSUU	= "DMSUU";

	public static final String	SQL_OLD_NEW_PATH				= " 	WITH RECURSIVE SupplyTree(AD_Client_ID, DMS_Content_ID, DMS_Content_Related_ID, DMS_Association_ID) AS 					"
																	+ " ( 																														"
																	+ " 	SELECT 	c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID"
																	+ " 			, v.Value, c.Name, c.ParentURL, 1 AS LEVEL, ARRAY['', v.Value] AS Path, a.DMS_AssociationType_ID 			"
																	+ "  			, '' || CASE WHEN c.IsMounting = 'Y' THEN v.Value ELSE v.DMS_Version_UU END AS ParentValueORUU				"
																	+ "				, '' AS BuildNewParentURL																					"
																	+ " 	FROM DMS_content c 																									"
																	+ " 	INNER JOIN DMS_association a 	ON (a.DMS_Content_ID = c.DMS_Content_ID) 											"
																	+ "		INNER JOIN DMS_Version v	 	ON (v.DMS_Content_ID = c.DMS_Content_ID AND v.SeqNo = 0)							"
																	+ " 	WHERE ParentURL IS NULL AND c.AD_Client_ID = ? 																		"
																	+ " UNION ALL 																												"
																	+ " 	SELECT	c.AD_Client_ID, c.DMS_Content_ID, a.DMS_Content_Related_ID, a.DMS_Association_ID, a.AD_Table_ID, a.Record_ID"
																	+ " 			, v.Value, c.Name, c.ParentURL,  LEVEL + 1, Path || v.Value, a.DMS_AssociationType_ID 						"
																	+ " 		 	, CASE 	WHEN	((c.IsMounting = 'Y' AND COALESCE(a.AD_Table_ID, 0) > 0 AND COALESCE(a.Record_ID, 0) = 0 ) 	"
																	+ " 					     OR (c.IsMounting = 'Y' AND COALESCE(a.AD_Table_ID, 0) = 0 AND COALESCE(a.Record_ID, 0) = 0 ))	" 
																	+ "						THEN	v.Value 	ELSE 	v.DMS_Version_UU 		END												"
																	+ " 			, st.BuildNewParentURL || ? ||  st.ParentValueORUU															"
																	+ " 	FROM  DMS_Association a 																							"
																	+ " 	INNER JOIN supplytree st 		ON (st.DMS_Content_ID = a.DMS_Content_Related_ID AND a.AD_Client_ID = ?) 			"
																	+ " 	INNER JOIN DMS_content c 		ON (c.DMS_Content_ID = a.DMS_Content_ID) 											"
																	+ "		INNER JOIN DMS_Version v	 	ON (v.DMS_Content_ID = c.DMS_Content_ID AND v.SeqNo = 0)							"
																	+ " 	WHERE a.DMS_AssociationType_ID IS NULL OR a.DMS_AssociationType_ID = 1000001										"
																	+ " )																														"
																	+ " SELECT 																													"
																	+ " 	  '' || COALESCE(s.ParentURL, '') || ? || v.Value 			AS OldURL 												"
																	+ " 	, '' || COALESCE(s.ParentURL, '') || ? || v.DMS_Version_UU	AS NewURL 												"
																	+ "		, s.BuildNewParentURL, s.AD_Table_ID, s.Record_ID, s.DMS_content_ID 												"
																	+ " FROM SupplyTree AS s 																									"
																	+ " INNER JOIN DMS_content c 			ON (c.DMS_content_ID = s.DMS_content_ID) 											"
																	+ " INNER JOIN DMS_Version v 			ON (v.DMS_content_ID = c.DMS_content_ID) 											"
																	+ " WHERE (c.IsMounting = 'Y' AND Level > 2) OR (c.IsMounting = 'N')														"
																	+ " ORDER BY	s.AD_Client_ID, COALESCE( s.AD_Table_ID, 0), COALESCE( s.Record_ID, 0), s.Level DESC,						"
																	+ "		 		s.ParentURL, s.Path, s.DMS_Content_ID,	s.DMS_Content_Related_ID											";									
																																	

	public static final String	SQL_COUNT_VERSION				= " 	SELECT COUNT(1) FROM DMS_Version v 									"
																	+ " INNER JOIN DMS_content c ON (c.DMS_content_ID = v.DMS_content_ID)	"
																	+ " WHERE v.AD_Client_ID = ? AND c.IsMounting = 'N' 					";

	public static final String	SQL_MODIFY_CONTENT_PARENTURL	= " 	UPDATE DMS_Content 													"
																	+ " SET ParentURL = data.BuildNewParentURL 								"
																	+ " FROM 	(															"
																	+ SQL_OLD_NEW_PATH
																	+ " 		) AS data 													"
																	+ " INNER JOIN DMS_Content c ON (c.DMS_Content_ID = data.DMS_Content_ID)"
																	+ " WHERE DMS_Content.DMS_Content_ID = c.DMS_Content_ID					";

	public static final String	SQL_GET_ARCHIVE_STORAGE_METHOD	= "     SELECT Method FROM AD_StorageProvider sp																"
																	+ " INNER JOIN AD_ClientInfo c ON (c.StorageArchive_ID = sp.AD_StorageProvider_ID AND c.AD_Client_ID = ?)	";
}
