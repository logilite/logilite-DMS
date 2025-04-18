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

/** Generated Model for DMS_ContentType_Filter
 *  @author iDempiere (generated) 
 *  @version Release 7.1 - $Id$ */
public class X_DMS_ContentType_Filter extends PO implements I_DMS_ContentType_Filter, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20240208L;

    /** Standard Constructor */
    public X_DMS_ContentType_Filter (Properties ctx, int DMS_ContentType_Filter_ID, String trxName)
    {
      super (ctx, DMS_ContentType_Filter_ID, trxName);
      /** if (DMS_ContentType_Filter_ID == 0)
        {
        } */
    }

    /** Load Constructor */
    public X_DMS_ContentType_Filter (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 6 - System - Client 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_DMS_ContentType_Filter[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
    {
		return (org.compiere.model.I_AD_Table)MTable.get(getCtx(), org.compiere.model.I_AD_Table.Table_Name)
			.getPO(getAD_Table_ID(), get_TrxName());	}

	/** Set Table.
		@param AD_Table_ID 
		Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1) 
			set_Value (COLUMNNAME_AD_Table_ID, null);
		else 
			set_Value (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get Table.
		@return Database Table information
	  */
	public int getAD_Table_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS Content Type Filter.
		@param DMS_ContentType_Filter_ID DMS Content Type Filter	  */
	public void setDMS_ContentType_Filter_ID (int DMS_ContentType_Filter_ID)
	{
		if (DMS_ContentType_Filter_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_ContentType_Filter_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_ContentType_Filter_ID, Integer.valueOf(DMS_ContentType_Filter_ID));
	}

	/** Get DMS Content Type Filter.
		@return DMS Content Type Filter	  */
	public int getDMS_ContentType_Filter_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_ContentType_Filter_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS Content Type Filter.
		@param DMS_ContentType_Filter_UU DMS Content Type Filter	  */
	public void setDMS_ContentType_Filter_UU (String DMS_ContentType_Filter_UU)
	{
		set_ValueNoCheck (COLUMNNAME_DMS_ContentType_Filter_UU, DMS_ContentType_Filter_UU);
	}

	/** Get DMS Content Type Filter.
		@return DMS Content Type Filter	  */
	public String getDMS_ContentType_Filter_UU () 
	{
		return (String)get_Value(COLUMNNAME_DMS_ContentType_Filter_UU);
	}

	public I_DMS_ContentType getDMS_ContentType() throws RuntimeException
    {
		return (I_DMS_ContentType)MTable.get(getCtx(), I_DMS_ContentType.Table_Name)
			.getPO(getDMS_ContentType_ID(), get_TrxName());	}

	/** Set DMS Content Type.
		@param DMS_ContentType_ID 
		DMS Content Type of document like pdf, MS Word, Excel etc.
	  */
	public void setDMS_ContentType_ID (int DMS_ContentType_ID)
	{
		if (DMS_ContentType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_ContentType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_ContentType_ID, Integer.valueOf(DMS_ContentType_ID));
	}

	/** Get DMS Content Type.
		@return DMS Content Type of document like pdf, MS Word, Excel etc.
	  */
	public int getDMS_ContentType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_ContentType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Filter Clause.
		@param FilterClause Filter Clause	  */
	public void setFilterClause (String FilterClause)
	{
		set_Value (COLUMNNAME_FilterClause, FilterClause);
	}

	/** Get Filter Clause.
		@return Filter Clause	  */
	public String getFilterClause () 
	{
		return (String)get_Value(COLUMNNAME_FilterClause);
	}
}