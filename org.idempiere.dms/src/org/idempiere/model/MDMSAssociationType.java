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

import org.compiere.util.DB;

public class MDMSAssociationType extends X_DMS_AssociationType
{
	/**
	 * 
	 */
	private static final long	serialVersionUID			= 1L;

	public static final String	SQL_GET_ASSOCIATION_TYPE	= "SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE Upper(name) = UPPER(?)";

	public static final String	AssociationType_Version		= "Version";
	public static final String	AssociationType_Parent		= "Parent";
	public static final String	AssociationType_Record		= "Record";
	public static final String	AssociationType_Link		= "Link";

	public static final int		AssociationType_ID_Version	= 1000000;
	public static final int		AssociationType_ID_Parent	= 1000001;
	public static final int		AssociationType_ID_Record	= 1000002;
	public static final int		AssociationType_ID_Link		= 1000003;

	public MDMSAssociationType(Properties ctx, int DMS_AssociationType_ID, String trxName)
	{
		super(ctx, DMS_AssociationType_ID, trxName);
	}

	public MDMSAssociationType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public static int getVersionType(boolean isParent)
	{
		int versionTypeID = 0;

		if (isParent)
			versionTypeID = DB.getSQLValue(null, SQL_GET_ASSOCIATION_TYPE, AssociationType_Parent);
		else
			versionTypeID = DB.getSQLValue(null, SQL_GET_ASSOCIATION_TYPE, AssociationType_Version);

		return versionTypeID;
	}

}
