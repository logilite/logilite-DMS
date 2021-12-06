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

package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;
import org.compiere.util.DB;

public class MDMSContentType extends X_DMS_ContentType
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= -7974749939408476322L;

	static CCache<String, Integer>	cache_contentType	= new CCache<String, Integer>("ContentTypeCache", 100);

	//
	public MDMSContentType(Properties ctx, int DMS_ContentType_ID, String trxName)
	{
		super(ctx, DMS_ContentType_ID, trxName);
	}

	public MDMSContentType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static int getContentTypeIDFromName(String contentType, int AD_Client_ID)
	{
		Integer contentTypeID = cache_contentType.get(AD_Client_ID + "_" + contentType);
		if (contentTypeID == null || contentTypeID <= 0)
		{
			contentTypeID = DB.getSQLValue(	null, "SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE IsActive = 'Y' AND Value = ? AND AD_Client_ID IN (0, ?)",
											contentType, AD_Client_ID);
			if (contentTypeID > 0)
				cache_contentType.put(AD_Client_ID + "_" + contentType, contentTypeID);
		}
		return contentTypeID;
	} // getContentTypeIDFromName

}
