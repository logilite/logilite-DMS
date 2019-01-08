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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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

	public static final int		MAX_FILENAME_LENGTH			= 250;

	// Content widget size
	public static final int		CONTENT_COMPONENT_HEIGHT	= 120;
	public static final int		CONTENT_COMPONENT_WIDTH		= 120;

	// Context Menu Item
	public static final String	MENUITEM_UPLOADVERSION		= "Upload Version";
	public static final String	MENUITEM_VERSIONlIST		= "Version List";
	public static final String	MENUITEM_RENAME				= "Rename";
	public static final String	MENUITEM_CUT				= "Cut";
	public static final String	MENUITEM_COPY				= "Copy";
	public static final String	MENUITEM_PASTE				= "Paste";
	public static final String	MENUITEM_DOWNLOAD			= "Download";
	public static final String	MENUITEM_CREATELINK			= "Create Link";
	public static final String	MENUITEM_DELETE				= "Delete";
	public static final String	MENUITEM_ASSOCIATE			= "Associate";

	// ASSOCIATION TYPE
	public static final String	LINK						= "Link";
	public static final String	RECORD						= "Record";
	public static final String	DEFAULT						= "Default";
	public static final String	DIRECTORY					= "Directory";
	public static final String	DOWNLOAD					= "Download";

	// constant for index fields
	public static final String	NAME						= "Name";
	public static final String	CREATED						= "created";
	public static final String	UPDATED						= "updated";
	public static final String	CREATEDBY					= "createdBy";
	public static final String	UPDATEDBY					= "updatedBy";
	public static final String	RECORD_ID					= "Record_ID";
	public static final String	AD_Table_ID					= "AD_Table_ID";
	public static final String	DESCRIPTION					= "description";
	public static final String	CONTENTTYPE					= "contentType";
	public static final String	DMS_CONTENT_ID				= "DMS_Content_ID";
	public static final String	SHOW_INACTIVE				= "Show_InActive";
	public static final String	AD_CLIENT_ID				= "AD_Client_ID";

	public static final String	REG_EXP_FILENAME			= "^[A-Za-z0-9\\s\\-\\._\\(\\)]+$";
	public static final String	REG_SPACE					= "\\S+";

	public static final String	FILE_SEPARATOR				= Utils.getStorageProviderFileSeparator();

	public static String		MSG_FILL_MANDATORY			= Msg.translate(Env.getCtx(), "FillMandatory");

	public static DateFormat	dateFormatWithTime			= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
}
