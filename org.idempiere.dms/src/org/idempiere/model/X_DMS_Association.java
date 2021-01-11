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
package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for DMS_Association
 *  @author iDempiere (generated) 
 *  @version Release 5.1 - $Id$ */
public class X_DMS_Association extends PO implements I_DMS_Association, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20210105L;

    /** Standard Constructor */
    public X_DMS_Association (Properties ctx, int DMS_Association_ID, String trxName)
    {
      super (ctx, DMS_Association_ID, trxName);
      /** if (DMS_Association_ID == 0)
        {
			setDMS_Association_ID (0);
        } */
    }

    /** Load Constructor */
    public X_DMS_Association (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
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
      StringBuffer sb = new StringBuffer ("X_DMS_Association[")
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

	/** Set DMS Association ID.
		@param DMS_Association_ID DMS Association ID	  */
	public void setDMS_Association_ID (int DMS_Association_ID)
	{
		if (DMS_Association_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_Association_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_Association_ID, Integer.valueOf(DMS_Association_ID));
	}

	/** Get DMS Association ID.
		@return DMS Association ID	  */
	public int getDMS_Association_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Association_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.idempiere.model.I_DMS_AssociationType getDMS_AssociationType() throws RuntimeException
    {
		return (org.idempiere.model.I_DMS_AssociationType)MTable.get(getCtx(), org.idempiere.model.I_DMS_AssociationType.Table_Name)
			.getPO(getDMS_AssociationType_ID(), get_TrxName());	}

	/** Set DMS AssociationType ID.
		@param DMS_AssociationType_ID DMS AssociationType ID	  */
	public void setDMS_AssociationType_ID (int DMS_AssociationType_ID)
	{
		if (DMS_AssociationType_ID < 1) 
			set_Value (COLUMNNAME_DMS_AssociationType_ID, null);
		else 
			set_Value (COLUMNNAME_DMS_AssociationType_ID, Integer.valueOf(DMS_AssociationType_ID));
	}

	/** Get DMS AssociationType ID.
		@return DMS AssociationType ID	  */
	public int getDMS_AssociationType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_AssociationType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.idempiere.model.I_DMS_Content getDMS_Content() throws RuntimeException
    {
		return (org.idempiere.model.I_DMS_Content)MTable.get(getCtx(), org.idempiere.model.I_DMS_Content.Table_Name)
			.getPO(getDMS_Content_ID(), get_TrxName());	}

	/** Set DMS Content.
		@param DMS_Content_ID DMS Content	  */
	public void setDMS_Content_ID (int DMS_Content_ID)
	{
		if (DMS_Content_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_Content_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_Content_ID, Integer.valueOf(DMS_Content_ID));
	}

	/** Get DMS Content.
		@return DMS Content	  */
	public int getDMS_Content_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Content_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.idempiere.model.I_DMS_Content getDMS_Content_Related() throws RuntimeException
    {
		return (org.idempiere.model.I_DMS_Content)MTable.get(getCtx(), org.idempiere.model.I_DMS_Content.Table_Name)
			.getPO(getDMS_Content_Related_ID(), get_TrxName());	}

	/** Set DMS Content Related.
		@param DMS_Content_Related_ID DMS Content Related	  */
	public void setDMS_Content_Related_ID (int DMS_Content_Related_ID)
	{
		if (DMS_Content_Related_ID < 1) 
			set_Value (COLUMNNAME_DMS_Content_Related_ID, null);
		else 
			set_Value (COLUMNNAME_DMS_Content_Related_ID, Integer.valueOf(DMS_Content_Related_ID));
	}

	/** Get DMS Content Related.
		@return DMS Content Related	  */
	public int getDMS_Content_Related_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Content_Related_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Record ID.
		@param Record_ID 
		Direct internal record ID
	  */
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0) 
			set_ValueNoCheck (COLUMNNAME_Record_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Record ID.
		@return Direct internal record ID
	  */
	public int getRecord_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}