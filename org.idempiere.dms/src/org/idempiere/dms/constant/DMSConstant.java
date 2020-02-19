/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.dms.constant;

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

import org.compiere.model.MSysConfig;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.idempiere.dms.factories.Utils;

/**
 * DMS Constant
 * 
 * @author Sachin
 */
public final class DMSConstant
{
	// System Configuration
	public static final int					MAX_FILENAME_LENGTH								= MSysConfig.getIntValue("DMS_FILENAME_LENGTH", 150);
	public static final int					MAX_DIRECTORY_LENGTH							= MSysConfig.getIntValue("DMS_DIRECTORY_LENGTH", 50);
	public static final int					DMS_VIEWER_LABLE_FONT_SIZE						= MSysConfig.getIntValue("DMS_VIEWER_LABLE_FONT_SIZE", 11);

	public static final String				DMS_MOUNTING_BASE								= "DMS_MOUNTING_BASE";
	public static final String				DMS_MOUNTING_ARCHIVE_BASE						= "DMS_MOUNTING_ARCHIVE_BASE";

	// Content widget size
	public static final int					CONTENT_LARGE_ICON_WIDTH						= 120;
	public static final int					CONTENT_LARGE_ICON_HEIGHT						= 120;

	// Cell attributes
	public static final String				COMP_ATTRIBUTE_CONTENT							= "ATTR_CONTENT";
	public static final String				COMP_ATTRIBUTE_ASSOCIATION						= "ATTR_ASSOCIATION";

	// View thumbnail toggle action
	public static final String				ICON_VIEW_LIST									= "ICON_VIEW_LIST";
	public static final String				ICON_VIEW_LARGE									= "ICON_VIEW_LARGE";
	public static final String				ICON_VIEW_VERSION								= "ICON_VIEW_VERSION";

	// Regular Expression
	public static final String				REG_SPACE										= "\\S+";
	public static final String				REG_EXP_FILENAME								= "^[A-Za-z0-9\\s\\-\\._\\(\\)]+$";
	public static final String				REG_EXP_WINDOWS_DIRNAME_VALIDATE				= "((^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])$)|([\\\\//:*?\\\"<>|?*\\x00-\\x1F]))";
	public static final String				REG_EXP_LINUX_DIRNAME_VALIDATE					= "(/)";
	public static final String				REG_EXP_VERSION_FILE							= "^.*\\(\\d+\\).\\w+$";
	// Pattern
	public static final Pattern				PATTERN_WINDOWS_DIRNAME_ISVALID					= Pattern.compile(REG_EXP_WINDOWS_DIRNAME_VALIDATE);
	public static final Pattern				PATTERN_LINX_DIRNAME_ISVALID					= Pattern.compile(REG_EXP_LINUX_DIRNAME_VALIDATE);

	// File Separator
	public static final String				STORAGE_PROVIDER_FILE_SEPARATOR					= "STORAGE_PROVIDER_FILE_SEPARATOR";
	public static final String				FILE_SEPARATOR									= Utils.getStorageProviderFileSeparator();

	// Button
	public static final String				TOOLBAR_BUTTON_DOCUMENT_EXPLORER				= "Document Explorer";

	// Event
	public static final String				EVENT_ON_RENAME_COMPLETE						= "onRenameComplete";
	public static final String				EVENT_ON_UPLOAD_COMPLETE						= "onUploadComplete";

	// Context Menu Item
	public static final String				MENUITEM_CUT									= "Cut";
	public static final String				MENUITEM_COPY									= "Copy";
	public static final String				MENUITEM_PASTE									= "Paste";
	public static final String				MENUITEM_RENAME									= "Rename";
	public static final String				MENUITEM_DELETE									= "Delete";
	public static final String				MENUITEM_DOWNLOAD								= "Download";
	public static final String				MENUITEM_ASSOCIATE								= "Associate";
	public static final String				MENUITEM_CREATELINK								= "Create Link";
	public static final String				MENUITEM_VERSIONlIST							= "Version List";
	public static final String				MENUITEM_UPLOADVERSION							= "Upload Version";

	// DMS MimeType
	public static final String				DEFAULT											= "Default";
	public static final String				DIRECTORY										= "Directory";

	// AD_Image
	public static final String				DOWNLOAD										= "Download";

	// constant for index fields
	public static final String				NAME											= "Name";
	public static final String				CREATED											= "created";
	public static final String				UPDATED											= "updated";
	public static final String				CREATEDBY										= "createdBy";
	public static final String				UPDATEDBY										= "updatedBy";
	public static final String				RECORD_ID										= "Record_ID";
	public static final String				DESCRIPTION										= "description";
	public static final String				CONTENTTYPE										= "contentType";
	public static final String				AD_Table_ID										= "AD_Table_ID";
	public static final String				AD_CLIENT_ID									= "AD_Client_ID";
	public static final String				SHOW_INACTIVE									= "Show_InActive";
	public static final String				DMS_CONTENT_ID									= "DMS_Content_ID";

	// Msg translate
	public static final String				MSG_NAME										= Msg.translate(Env.getCtx(), "Name");
	public static final String				MSG_CREATED										= Msg.translate(Env.getCtx(), "Created");
	public static final String				MSG_UPDATED										= Msg.translate(Env.getCtx(), "Updated");
	public static final String				MSG_FILESIZE									= Msg.translate(Env.getCtx(), "FileSize");
	public static final String				MSG_CREATEDBY									= Msg.translate(Env.getCtx(), "CreatedBy");
	public static final String				MSG_UPDATEDBY									= Msg.translate(Env.getCtx(), "UpdatedBy");
	public static final String				MSG_DESCRIPTION									= Msg.translate(Env.getCtx(), "Description");
	public static final String				MSG_CONTENT_TYPE								= Msg.translate(Env.getCtx(), "Content Type");
	public static final String				MSG_CONTENT_META								= Msg.translate(Env.getCtx(), "Content Meta");
	public static final String				MSG_FILL_MANDATORY								= Msg.translate(Env.getCtx(), "FillMandatory");
	public static final String				MSG_CONTENT_NAME								= Msg.translate(Env.getCtx(), "DMS_CONTENT_ID");
	public static final String				MSG_ADVANCE_SEARCH								= Msg.translate(Env.getCtx(), "Advance Search");
	public static final String				MSG_DIRECTORY_NAME								= Msg.translate(Env.getCtx(), "Directory Name");
	public static final String				MSG_DMS_CONTENT_TYPE							= Msg.translate(Env.getCtx(), "DMS_ContentType_ID");

	// Msg
	public static final String				MSG_RENAME										= Msg.getMsg(Env.getCtx(), "Rename");
	public static final String				MSG_EXPLORER									= Msg.getMsg(Env.getCtx(), "Explorer");
	public static final String				MSG_ATTRIBUTES									= Msg.getMsg(Env.getCtx(), "Attributes");
	public static final String				MSG_SELECT_FILE									= Msg.getMsg(Env.getCtx(), "SelectFile");
	public static final String				MSG_ATTRIBUTE_SET								= Msg.getMsg(Env.getCtx(), "attribute.set");
	public static final String				MSG_SIZE										= "Size";
	public static final String				MSG_LINK										= "Link";
	public static final String				MSG_FILE_TYPE									= "File Type";
	public static final String				MSG_FILE_FOLDER									= "File Folder";
	public static final String				MSG_SHOW_IN_ACTIVE								= "Show InActive";
	public static final String				MSG_UPLOAD_CONTENT								= "Upload Content";
	public static final String				MSG_VERSION_HISTORY								= "Version History";
	public static final String				MSG_DMS_VERSION_LIST							= "DMS Version List";
	public static final String				MSG_CREATE_DIRECTORY							= "Create Directory";
	public static final String				MSG_ENTER_DIRETORY_NAME							= "Enter Directory Name";
	public static final String				MSG_NO_VERSION_DOC_EXISTS						= "No version Document available.";
	public static final String				MSG_ENTER_NEW_NAME_FOR_ITEM						= "Please enter a new name for the item:";

	// ToolTipText
	public static final String				TTT_SAVE										= "Save";
	public static final String				TTT_EDIT										= "Edit";
	public static final String				TTT_SEARCH										= "Search";
	public static final String				TTT_DOWNLOAD									= "Download";
	public static final String				TTT_NEXT_RECORD									= "Next Record";
	public static final String				TTT_UPLOAD_VERSION								= "Upload Version";
	public static final String				TTT_PREVIOUS_RECORD								= "Previous Record";
	public static final String				TTT_DISPLAYS_ITEMS_LAYOUT						= "Displays items Layout";

	
	// DMS Contents
	// TODO typo correction
	public static final String				CONTNET_FILE									= "file";
	public static final String				CONTENT_DIR										= "dir";
	// TODO purpose declare
	public static final String				OPERATION_CREATE								= "create";
	public static final String				OPERATION_RENAME								= "rename";
	public static final String				OPERATION_COPY									= "copy";

	public static final String				CONTENT_TYPE_PARENT								= "Parent";
	public static final String				CONTENT_TYPE_VERSION							= "Version";
	public static final String				CONTENT_TYPE_VERSIONPARENT						= "VersionParent";

	// Date Format
	public static final SimpleDateFormat	SDF												= new SimpleDateFormat("yyyy-MM-dd hh:mm z");
	public static final SimpleDateFormat	SDF_WITH_TIME									= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
	public static final SimpleDateFormat	SDF_DATE_FORMAT_WITH_TIME						= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	// CSS Style
	public static final String				CSS_DATEBOX										= "width: 100%; display:flex; flex-direction: row;";
	public static final String				CSS_BREAD_CRUMB_LINK							= "font-weight: bold; font-size: small; padding-left: 5px; color: dimgray;";
	public static final String				CSS_FLEX_ROW_DIRECTION							= "display: flex; flex-direction: row; flex-wrap: wrap; height: 100%;";

	public static final String				CSS_CONTENT_COMP_VIEWER_LARGE_NORMAL			= "margin: 5px; background: #e2e2e2; border: 4px double #ffffff !important; border-radius: 5px;";
	public static final String				CSS_CONTENT_COMP_VIEWER_LARGE_SELECTED			= "margin: 5px; background: #abcdff; border: 4px double #3363ad !important; border-radius: 5px;";

	public static final String				CSS_CONTENT_COMP_VIEWER_LIST_NORMAL				= "border-top: 1px solid #ffffff; border-bottom: 0px double #ffffff;";
	public static final String				CSS_CONTENT_COMP_VIEWER_LIST_SELECTED			= "border-top: 1px solid #3363ad; border-bottom: 1px double #3363ad;";

	public static final String				CSS_HIGHLIGHT_LABEL								= "font-weight: bold; text-align: center; border: 4px double #909090; padding: 4px 0px;";

	// Queries
	public static final String				SQL_GET_CONTENT_LATEST_VERSION_NONLINK			= "SELECT DMS_Content_ID, DMS_Association_ID, SeqNo FROM DMS_Association "
																									+ "WHERE DMS_Content_Related_ID = ? OR DMS_Content_ID = ? AND NVL(DMS_AssociationType_ID, 0) <> 1000003"
																									+ "GROUP BY DMS_Content_ID, DMS_Association_ID 	ORDER BY MAX(SeqNo) DESC ";

	public static final String				SQL_GET_ASSOCIATION_ID_FROM_CONTENT				= "SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ? AND NVL(DMS_AssociationType_ID, 0)";

	public static final String				SQL_GET_CONTENTID_FROM_CONTENTNAME				= "SELECT DMS_Content_ID FROM DMS_Content WHERE Name = ? AND AD_Client_ID = ?";

	public static final String				SQL_GET_MOUNTING_BASE_CONTENT					= "SELECT DMS_Content_ID FROM DMS_Content WHERE Name = ? AND AD_Client_ID = ? AND ContentBaseType = 'DIR' AND ParentUrl IS NULL";

	public static final String				SQL_GET_MOUNTING_CONTENT_FOR_TABLE				= "SELECT dc.DMS_Content_ID FROM DMS_Content dc "
																									+ " INNER JOIN DMS_Association da ON (dc.DMS_Content_ID = da.DMS_Content_ID) "
																									+ " WHERE dc.Name = ? AND dc.IsMounting = 'Y' AND da.AD_Table_ID = ? AND da.Record_ID = ?";

	/*
	 * Pass any version content ID to get whole list of its hierarchy
	 */
	public static final String				SQL_FETCH_CONTENT_VERSION_LIST					= " WITH RootContent AS (	 																			"
																									+ " 	SELECT NVL((SELECT DMS_Content_Related_ID FROM DMS_Association 							"
																									+ "		WHERE DMS_AssociationType_ID = ? AND DMS_Content_ID = ?), ?) AS RootContentID			"
																									+ "	) 																							"
																									+ "		SELECT DISTINCT DMS_Content_ID, SeqNo	FROM DMS_Association, RootContent 				"
																									+ " 	WHERE DMS_AssociationType_ID = ? AND DMS_Content_Related_ID = RootContent.RootContentID "
																									+ " UNION 																						"
																									+ " 	SELECT DMS_Content_ID, NULL 			FROM DMS_Content, RootContent 					"
																									+ " 	WHERE DMS_Content_ID = RootContent.RootContentID AND ContentBaseType <> 'DIR' 			"
																									+ " 	ORDER BY DMS_Content_ID DESC 															";

	/*
	 * Get Link Association ID from Any with/without Versioning Content
	 */
	public static final String				SQL_LINK_ASSOCIATIONS_FROM_RELATED_TO_CONTENT	= "SELECT DMS_Association_ID 		FROM DMS_Association 			"
																									+ "	WHERE 	DMS_AssociationType_ID = 1000003 AND 			"
																									+ "			DMS_Content_ID IN (	SELECT DMS_Content_ID FROM ("
																									+ DMSConstant.SQL_FETCH_CONTENT_VERSION_LIST
																									+ " ) AS DATA )												";
	
																										+ " ) AS DATA )												";
	// TODO Looks like same sql twice, optimize with below approach
	// IsSearchWithParentURL = true
	// WHERE (ParentURL = ? AND True=?) OR (ParentURL IS NULL AND False = ? )
	
	// TODO Use alias name instead of table name and remove adempiere 
	// Maintain camel case
	// maintain code format
	public static final String				SQL_CHECK_CONTETNT_NAME_EXISTS					= "SELECT DMS_Content_ID from DMS_Content where ParentURL = ? AND Name = ? AND IsActive = 'Y'";

	public static final String				SQL_CHECK_CONTENT_NAME_EXISTS_NO_PARENT			= "SELECT DMS_Content_ID from DMS_Content where ParentURL IS NULL AND Name = ? AND IsActive = 'Y'";

	public static final String				SQL_CHECK_FILE_EXISTS							= "SELECT Value FROM DMS_Content WHERE ( DMS_Content.Value = ? OR DMS_Content.Value LIKE ? ) AND ParentURL = ?";

	public static final String				SQL_CHECK_FILE_EXISTS_NO_PARENT					= "SELECT Value FROM DMS_Content WHERE ( DMS_Content.Value = ? OR DMS_Content.Value LIKE ? ) AND ParentURL IS NULL";

	public static final String				SQL_CHECK_COPY_FILE_EXISTS_FOR					= "SELECT Value FROM DMS_Content WHERE ( DMS_Content.Value LIKE ? OR DMS_Content.Value LIKE ? ) AND ParentURL = ?";

	public static final String				SQL_CHECK_COPY_FILE_EXISTS_NO_PARENT			= "SELECT Value FROM DMS_Content WHERE ( DMS_Content.Value LIKE ? OR DMS_Content.Value LIKE ? ) AND ParentURL IS NULL";

	public static final String				SQL_GET_FILE_NAME_BY_FOR_VERSION				= "SELECT Value FROM DMS_Content WHERE DMS_Content_ID = ?";

	public static final String				SQL_GET_CONTENT_TYPE							= "SELECT DMS_AssociationType.Value from adempiere.DMS_Content LEFT JOIN adempiere.DMS_Association"
			+ " ON adempiere.DMS_Content.dms_content_id = adempiere.DMS_Association.dms_content_id"
			+ " LEFT JOIN adempiere.DMS_AssociationType"
			+ " ON adempiere.DMS_AssociationType.dms_associationtype_id = adempiere.DMS_Association.dms_associationtype_id WHERE DMS_Content.dms_content_id = ?";

	public static final String				SQL_GET_COPIED_CONTENT_NAME						= "SELECT Name FROM adempiere.DMS_Content WHERE adempiere.DMS_Content.dms_content_id = ? ";

	public static final String				SQL_GET_ANOTHER_VERSION_IDS						= "SELECT DMS_Content.* FROM adempiere.DMS_Content LEFT JOIN adempiere.DMS_Association ON DMS_Association.DMS_Content_ID = DMS_Content.DMS_Content_ID "
			+ "WHERE DMS_Content_Related_ID = "
			+ "( SELECT DMS_Association.DMS_Content_Related_ID FROM adempiere.DMS_Association WHERE DMS_Association.DMS_Content_ID = ? ) UNION "
			+ "SELECT DMS_Content.* FROM adempiere.DMS_Content WHERE DMS_Content.DMS_Content_ID = ( SELECT DMS_Association.DMS_Content_Related_ID FROM adempiere.DMS_Association WHERE DMS_Association.DMS_Content_ID = ? )";

	public static final String				SQL_GET_DMS_CONTENT								= "SELECT * from DMS_Content where DMS_Content_ID = ? ";

	public static final String				GET_COPY_DIR_MACHING_CONTENT_NAME				= "SELECT Name FROM DMS_Content WHERE ( DMS_Content.Name = ? OR DMS_Content.Name LIKE ? ) AND ParentURL = ? AND IsActive = 'Y' ";

	public static final String				GET_COPY_DIR_MACHING_CONTENT_NAME_NO_PARENT		= "SELECT Name FROM DMS_Content WHERE ( DMS_Content.Name = ? OR DMS_Content.Name LIKE ? ) AND ParentURL IS NULL AND IsActive = 'Y' ";

	public static final String				SQL_CHECK_ACTUAL_FILE_DIR_EXISTS				= "SELECT DMS_Content_ID from DMS_Content where ParentURL = ? AND Value = ? ";

	public static final String				SQL_CHECK_ACTUAL_FILE_DIR_EXISTS_NO_PARENT		= "SELECT DMS_Content_ID from DMS_Content where ParentURL IS NULL AND Value = ? ";
	
	public static final String				GET_PARENT_FOR_CONTENT							= "SELECT DMS_Content.* from DMS_Content LEFT JOIN DMS_Association on DMS_Association.DMS_CONTENT_ID = DMS_Content.DMS_CONTENT_ID "
			+ "LEFT JOIN DMS_AssociationType ON DMS_AssociationType.DMS_AssociationType_ID = DMS_Association.DMS_AssociationType_ID "
			+ "WHERE ParentURL = ? AND DMS_Content.Name = ?  AND DMS_AssociationType.Name = 'Parent' AND DMS_Content.isactive = 'Y'";
}
