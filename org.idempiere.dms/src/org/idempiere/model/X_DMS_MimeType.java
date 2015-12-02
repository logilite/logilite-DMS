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

/** Generated Model for DMS_MimeType
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_DMS_MimeType extends PO implements I_DMS_MimeType, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20151201L;

    /** Standard Constructor */
    public X_DMS_MimeType (Properties ctx, int DMS_MimeType_ID, String trxName)
    {
      super (ctx, DMS_MimeType_ID, trxName);
      /** if (DMS_MimeType_ID == 0)
        {
			setDMS_MimeType_ID (0);
			setFileExtension (null);
			setIsDefault (false);
			setMimeType (null);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_DMS_MimeType (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_DMS_MimeType[")
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

	/** Set Mime Type.
		@param DMS_MimeType_ID 
		Mime Type of the uploaded file
	  */
	public void setDMS_MimeType_ID (int DMS_MimeType_ID)
	{
		if (DMS_MimeType_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_DMS_MimeType_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_DMS_MimeType_ID, Integer.valueOf(DMS_MimeType_ID));
	}

	/** Get Mime Type.
		@return Mime Type of the uploaded file
	  */
	public int getDMS_MimeType_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_MimeType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set File Extension.
		@param FileExtension 
		Extension of the uploaded file
	  */
	public void setFileExtension (String FileExtension)
	{
		set_ValueNoCheck (COLUMNNAME_FileExtension, FileExtension);
	}

	/** Get File Extension.
		@return Extension of the uploaded file
	  */
	public String getFileExtension () 
	{
		return (String)get_Value(COLUMNNAME_FileExtension);
	}

	/** Set Icon.
		@param Icon_ID Icon	  */
	public void setIcon_ID (int Icon_ID)
	{
		if (Icon_ID < 1) 
			set_Value (COLUMNNAME_Icon_ID, null);
		else 
			set_Value (COLUMNNAME_Icon_ID, Integer.valueOf(Icon_ID));
	}

	/** Get Icon.
		@return Icon	  */
	public int getIcon_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Icon_ID);
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

	/** Set Mime Type.
		@param MimeType 
		Mime Type of the uploaded file
	  */
	public void setMimeType (String MimeType)
	{
		set_ValueNoCheck (COLUMNNAME_MimeType, MimeType);
	}

	/** Get Mime Type.
		@return Mime Type of the uploaded file
	  */
	public String getMimeType () 
	{
		return (String)get_Value(COLUMNNAME_MimeType);
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