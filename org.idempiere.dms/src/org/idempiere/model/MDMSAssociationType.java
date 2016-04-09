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
	private static final long	serialVersionUID		= 1L;

	private static final String	ASSOCIATIONTYPE_VERSION	= "Version";
	private static final String	ASSOCIATIONTYPE_PARENT	= "Parent";
	private static final String	SQL_GETASSOCIATIONTYPE	= "SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE name ilike ?";

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
			versionTypeID = DB.getSQLValue(null, SQL_GETASSOCIATIONTYPE, ASSOCIATIONTYPE_PARENT);
		else
			versionTypeID = DB.getSQLValue(null, SQL_GETASSOCIATIONTYPE, ASSOCIATIONTYPE_VERSION);

		return versionTypeID;
	}

}
