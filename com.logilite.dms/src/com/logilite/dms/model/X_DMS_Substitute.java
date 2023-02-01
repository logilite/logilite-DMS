/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.logilite.dms.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/**
 * Generated Model for DMS_Substitute
 * 
 * @author  iDempiere (generated)
 * @version Release 5.1 - $Id$
 */
public class X_DMS_Substitute extends PO implements I_DMS_Substitute, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20200914L;

	/** Standard Constructor */
	public X_DMS_Substitute(Properties ctx, int DMS_Substitute_ID, String trxName)
	{
		super(ctx, DMS_Substitute_ID, trxName);
		/**
		 * if (DMS_Substitute_ID == 0)
		 * {
		 * setAD_Table_ID (0);
		 * setDMS_Substitute_ID (0);
		 * setDMS_Substitute_Table_ID (0);
		 * }
		 */
	}

	/** Load Constructor */
	public X_DMS_Substitute(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * AccessLevel
	 * 
	 * @return 6 - System - Client
	 */
	protected int get_AccessLevel()
	{
		return accessLevel.intValue();
	}

	/** Load Meta Data */
	protected POInfo initPO(Properties ctx)
	{
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	public String toString()
	{
		StringBuffer sb = new StringBuffer("X_DMS_Substitute[")
						.append(get_ID())
						.append("]");
		return sb.toString();
	}

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Column) MTable
						.get(getCtx(), org.compiere.model.I_AD_Column.Table_Name)
						.getPO(getAD_Column_ID(), get_TrxName());
	}

	/**
	 * Set Origin Column.
	 * 
	 * @param AD_Column_ID
	 *                     Column in the origin table selected
	 */
	public void setAD_Column_ID(int AD_Column_ID)
	{
		if (AD_Column_ID < 1)
			set_ValueNoCheck(COLUMNNAME_AD_Column_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_AD_Column_ID, Integer.valueOf(AD_Column_ID));
	}

	/**
	 * Get Origin Column.
	 * 
	 * @return Column in the origin table selected
	 */
	public int getAD_Column_ID()
	{
		Integer ii = (Integer) get_Value(COLUMNNAME_AD_Column_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table) MTable
						.get(getCtx(), org.compiere.model.I_AD_Table.Table_Name)
						.getPO(getAD_Table_ID(), get_TrxName());
	}

	/**
	 * Set Origin Table.
	 * 
	 * @param AD_Table_ID
	 *                    Database Table information
	 */
	public void setAD_Table_ID(int AD_Table_ID)
	{
		if (AD_Table_ID < 1)
			set_Value(COLUMNNAME_AD_Table_ID, null);
		else
			set_Value(COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/**
	 * Get Origin Table.
	 * 
	 * @return Database Table information
	 */
	public int getAD_Table_ID()
	{
		Integer ii = (Integer) get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	/**
	 * Set Description.
	 * 
	 * @param Description
	 *                    Optional short description of the record
	 */
	public void setDescription(String Description)
	{
		set_Value(COLUMNNAME_Description, Description);
	}

	/**
	 * Get Description.
	 * 
	 * @return Optional short description of the record
	 */
	public String getDescription()
	{
		return (String) get_Value(COLUMNNAME_Description);
	}

	/**
	 * Set Detail.
	 * 
	 * @param Detail Detail
	 */
	public void setDetail(String Detail)
	{
		throw new IllegalArgumentException("Detail is virtual column");
	}

	/**
	 * Get Detail.
	 * 
	 * @return Detail
	 */
	public String getDetail()
	{
		return (String) get_Value(COLUMNNAME_Detail);
	}

	/**
	 * Set DMS Substitute.
	 * 
	 * @param DMS_Substitute_ID DMS Substitute
	 */
	public void setDMS_Substitute_ID(int DMS_Substitute_ID)
	{
		if (DMS_Substitute_ID < 1)
			set_ValueNoCheck(COLUMNNAME_DMS_Substitute_ID, null);
		else
			set_ValueNoCheck(COLUMNNAME_DMS_Substitute_ID, Integer.valueOf(DMS_Substitute_ID));
	}

	/**
	 * Get DMS Substitute.
	 * 
	 * @return DMS Substitute
	 */
	public int getDMS_Substitute_ID()
	{
		Integer ii = (Integer) get_Value(COLUMNNAME_DMS_Substitute_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_Table getDMS_Substitute_Table() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Table) MTable
						.get(getCtx(), org.compiere.model.I_AD_Table.Table_Name)
						.getPO(getDMS_Substitute_Table_ID(), get_TrxName());
	}

	/**
	 * Set Substitute Table.
	 * 
	 * @param DMS_Substitute_Table_ID
	 *                                Substitute Table information
	 */
	public void setDMS_Substitute_Table_ID(int DMS_Substitute_Table_ID)
	{
		if (DMS_Substitute_Table_ID < 1)
			set_Value(COLUMNNAME_DMS_Substitute_Table_ID, null);
		else
			set_Value(COLUMNNAME_DMS_Substitute_Table_ID, Integer.valueOf(DMS_Substitute_Table_ID));
	}

	/**
	 * Get Substitute Table.
	 * 
	 * @return Substitute Table information
	 */
	public int getDMS_Substitute_Table_ID()
	{
		Integer ii = (Integer) get_Value(COLUMNNAME_DMS_Substitute_Table_ID);
		if (ii == null)
			return 0;
		return ii.intValue();
	}

	/**
	 * Set DMS_Substitute_UU.
	 * 
	 * @param DMS_Substitute_UU DMS_Substitute_UU
	 */
	public void setDMS_Substitute_UU(String DMS_Substitute_UU)
	{
		set_Value(COLUMNNAME_DMS_Substitute_UU, DMS_Substitute_UU);
	}

	/**
	 * Get DMS_Substitute_UU.
	 * 
	 * @return DMS_Substitute_UU
	 */
	public String getDMS_Substitute_UU()
	{
		return (String) get_Value(COLUMNNAME_DMS_Substitute_UU);
	}
}
