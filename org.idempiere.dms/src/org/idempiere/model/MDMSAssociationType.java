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

public class MDMSAssociationType extends X_DMS_AssociationType
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 262213468229169099L;

	public static final String	TYPE_VERSION		= "Version";
	public static final String	TYPE_PARENT			= "Parent";
	public static final String	TYPE_RECORD			= "Record";
	public static final String	TYPE_LINK			= "Link";
	/**
	 * Extra Type for Renaming the content [ Out of the Record ]
	 */
	public static final String	TYPE_VERSIONPARENT	= "VersionParent";

	// public static final int VERSION_ID = 1000000;
	public static final int		PARENT_ID			= 1000001;
	public static final int		RECORD_ID			= 1000002;
	public static final int		LINK_ID				= 1000003;

	public MDMSAssociationType(Properties ctx, int DMS_AssociationType_ID, String trxName)
	{
		super(ctx, DMS_AssociationType_ID, trxName);
	}

	public MDMSAssociationType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Check Association Type is Link
	 * 
	 * @param  association
	 * @return             TRUE if Link
	 */
	public static boolean isLink(I_DMS_Association association)
	{
		return isLink(association.getDMS_AssociationType_ID());
	} // isLink

	/**
	 * Check Association Type is Link
	 * 
	 * @param  associationTypeID
	 * @return                   TRUE if Link
	 */
	public static boolean isLink(int associationTypeID)
	{
		return associationTypeID == LINK_ID;
	} // isLink

}
