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
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for DMS_Content
 *  @author iDempiere (generated) 
 *  @version Release 3.1 - $Id$ */
public class X_DMS_Content extends PO implements I_DMS_Content, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20160603L;

    /** Standard Constructor */
    public X_DMS_Content (Properties ctx, int DMS_Content_ID, String trxName)
    {
      super (ctx, DMS_Content_ID, trxName);
      /** if (DMS_Content_ID == 0)
        {
			setDMS_Content_ID (0);
			setDMS_MimeType_ID (0);
			setName (null);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_DMS_Content (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_DMS_Content[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Content = CNT */
	public static final String CONTENTBASETYPE_Content = "CNT";
	/** Directory = DIR */
	public static final String CONTENTBASETYPE_Directory = "DIR";
	/** Set ContentBaseType.
		@param ContentBaseType ContentBaseType	  */
	public void setContentBaseType (String ContentBaseType)
	{

		set_Value (COLUMNNAME_ContentBaseType, ContentBaseType);
	}

	/** Get ContentBaseType.
		@return ContentBaseType	  */
	public String getContentBaseType () 
	{
		return (String)get_Value(COLUMNNAME_ContentBaseType);
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

	/** Set DMS_FileSize.
		@param DMS_FileSize DMS_FileSize	  */
	public void setDMS_FileSize (String DMS_FileSize)
	{
		set_Value (COLUMNNAME_DMS_FileSize, DMS_FileSize);
	}

	/** Get DMS_FileSize.
		@return DMS_FileSize	  */
	public String getDMS_FileSize () 
	{
		return (String)get_Value(COLUMNNAME_DMS_FileSize);
	}

	public I_DMS_MimeType getDMS_MimeType() throws RuntimeException
    {
		return (I_DMS_MimeType)MTable.get(getCtx(), I_DMS_MimeType.Table_Name)
			.getPO(getDMS_MimeType_ID(), get_TrxName());	}

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

	public I_DMS_Status getDMS_Status() throws RuntimeException
    {
		return (I_DMS_Status)MTable.get(getCtx(), I_DMS_Status.Table_Name)
			.getPO(getDMS_Status_ID(), get_TrxName());	}

	/** Set Content Status.
		@param DMS_Status_ID 
		Classify Content by his status
	  */
	public void setDMS_Status_ID (int DMS_Status_ID)
	{
		if (DMS_Status_ID < 1) 
			set_Value (COLUMNNAME_DMS_Status_ID, null);
		else 
			set_Value (COLUMNNAME_DMS_Status_ID, Integer.valueOf(DMS_Status_ID));
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

	/** Set Read Only.
		@param IsReadOnly 
		Field is read only
	  */
	public void setIsReadOnly (boolean IsReadOnly)
	{
		set_Value (COLUMNNAME_IsReadOnly, Boolean.valueOf(IsReadOnly));
	}

	/** Get Read Only.
		@return Field is read only
	  */
	public boolean isReadOnly () 
	{
		Object oo = get_Value(COLUMNNAME_IsReadOnly);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException
    {
		return (I_M_AttributeSetInstance)MTable.get(getCtx(), I_M_AttributeSetInstance.Table_Name)
			.getPO(getM_AttributeSetInstance_ID(), get_TrxName());	}

	/** Set Attribute Set Instance.
		@param M_AttributeSetInstance_ID 
		Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)
	{
		if (M_AttributeSetInstance_ID < 0) 
			set_Value (COLUMNNAME_M_AttributeSetInstance_ID, null);
		else 
			set_Value (COLUMNNAME_M_AttributeSetInstance_ID, Integer.valueOf(M_AttributeSetInstance_ID));
	}

	/** Get Attribute Set Instance.
		@return Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_AttributeSetInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	/** Set ParentURL.
		@param ParentURL ParentURL	  */
	public void setParentURL (String ParentURL)
	{
		set_Value (COLUMNNAME_ParentURL, ParentURL);
	}

	/** Get ParentURL.
		@return ParentURL	  */
	public String getParentURL () 
	{
		return (String)get_Value(COLUMNNAME_ParentURL);
	}

	/** Set Valid From.
		@param ValidFromDate 
		DMS Conent validity From Date
	  */
	public void setValidFromDate (Timestamp ValidFromDate)
	{
		set_Value (COLUMNNAME_ValidFromDate, ValidFromDate);
	}

	/** Get Valid From.
		@return DMS Conent validity From Date
	  */
	public Timestamp getValidFromDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidFromDate);
	}

	/** Set Valid To.
		@param ValidToDate 
		DMS Conent validity To Date
	  */
	public void setValidToDate (Timestamp ValidToDate)
	{
		set_Value (COLUMNNAME_ValidToDate, ValidToDate);
	}

	/** Get Valid To.
		@return DMS Conent validity To Date
	  */
	public Timestamp getValidToDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_ValidToDate);
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