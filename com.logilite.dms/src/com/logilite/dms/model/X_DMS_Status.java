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

package com.logilite.dms.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for DMS_Status
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_DMS_Status extends PO implements I_DMS_Status, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20151201L;

    /** Standard Constructor */
    public X_DMS_Status (Properties ctx, int DMS_Status_ID, String trxName)
    {
      super (ctx, DMS_Status_ID, trxName);
      /** if (DMS_Status_ID == 0)
        {
			setDMS_ContentType_ID (0);
			setDMS_Status_ID (0);
			setIsDefault (false);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_DMS_Status (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_DMS_Status[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Description.
		@param Description 
		Optional short description of the record
	  */
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription () 
	{
		return (String)get_Value(COLUMNNAME_Description);
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

	/** Set Content Status.
		@param DMS_Status_ID 
		Classify Content by his status
	  */
	public void setDMS_Status_ID (int DMS_Status_ID)
	{
		if (DMS_Status_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_Status_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_Status_ID, Integer.valueOf(DMS_Status_ID));
	}

	/** Get Content Status.
		@return Classify Content by his status
	  */
	public int getDMS_Status_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Status_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Default.
		@param IsDefault 
		Default value
	  */
	public void setIsDefault (boolean IsDefault)
	{
		set_Value (COLUMNNAME_IsDefault, Boolean.valueOf(IsDefault));
	}

	/** Get Default.
		@return Default value
	  */
	public boolean isDefault () 
	{
		Object oo = get_Value(COLUMNNAME_IsDefault);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Set Search Key.
		@param Value 
		Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue () 
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}