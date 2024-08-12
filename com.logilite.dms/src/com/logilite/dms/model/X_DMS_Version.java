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

/** Generated Model for DMS_Version
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="DMS_Version")
public class X_DMS_Version extends PO implements I_DMS_Version, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20240812L;

    /** Standard Constructor */
    public X_DMS_Version (Properties ctx, int DMS_Version_ID, String trxName)
    {
      super (ctx, DMS_Version_ID, trxName);
      /** if (DMS_Version_ID == 0)
        {
			setDMS_Version_ID (0);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Version (Properties ctx, int DMS_Version_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_Version_ID, trxName, virtualColumns);
      /** if (DMS_Version_ID == 0)
        {
			setDMS_Version_ID (0);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Version (Properties ctx, String DMS_Version_UU, String trxName)
    {
      super (ctx, DMS_Version_UU, trxName);
      /** if (DMS_Version_UU == null)
        {
			setDMS_Version_ID (0);
			setValue (null);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Version (Properties ctx, String DMS_Version_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_Version_UU, trxName, virtualColumns);
      /** if (DMS_Version_UU == null)
        {
			setDMS_Version_ID (0);
			setValue (null);
        } */
    }

    /** Load Constructor */
    public X_DMS_Version (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_DMS_Version[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public com.logilite.dms.model.I_DMS_Content getDMS_Content() throws RuntimeException
	{
		return (com.logilite.dms.model.I_DMS_Content)MTable.get(getCtx(), com.logilite.dms.model.I_DMS_Content.Table_ID)
			.getPO(getDMS_Content_ID(), get_TrxName());
	}

	/** Set DMS Content.
		@param DMS_Content_ID DMS Content
	*/
	public void setDMS_Content_ID (int DMS_Content_ID)
	{
		if (DMS_Content_ID < 1)
			set_ValueNoCheck (COLUMNNAME_DMS_Content_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_DMS_Content_ID, Integer.valueOf(DMS_Content_ID));
	}

	/** Get DMS Content.
		@return DMS Content	  */
	public int getDMS_Content_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Content_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS_FileSize.
		@param DMS_FileSize DMS_FileSize
	*/
	public void setDMS_FileSize (String DMS_FileSize)
	{
		set_Value (COLUMNNAME_DMS_FileSize, DMS_FileSize);
	}

	/** Get DMS_FileSize.
		@return DMS_FileSize	  */
	public String getDMS_FileSize()
	{
		return (String)get_Value(COLUMNNAME_DMS_FileSize);
	}

	/** Set DMS_Version.
		@param DMS_Version_ID DMS_Version
	*/
	public void setDMS_Version_ID (int DMS_Version_ID)
	{
		if (DMS_Version_ID < 1)
			set_ValueNoCheck (COLUMNNAME_DMS_Version_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_DMS_Version_ID, Integer.valueOf(DMS_Version_ID));
	}

	/** Get DMS_Version.
		@return DMS_Version	  */
	public int getDMS_Version_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Version_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS_Version_UU.
		@param DMS_Version_UU DMS_Version_UU
	*/
	public void setDMS_Version_UU (String DMS_Version_UU)
	{
		set_Value (COLUMNNAME_DMS_Version_UU, DMS_Version_UU);
	}

	/** Get DMS_Version_UU.
		@return DMS_Version_UU	  */
	public String getDMS_Version_UU()
	{
		return (String)get_Value(COLUMNNAME_DMS_Version_UU);
	}

	/** Set Indexed.
		@param IsIndexed Index the document for the internal search engine
	*/
	public void setIsIndexed (boolean IsIndexed)
	{
		set_Value (COLUMNNAME_IsIndexed, Boolean.valueOf(IsIndexed));
	}

	/** Get Indexed.
		@return Index the document for the internal search engine
	  */
	public boolean isIndexed()
	{
		Object oo = get_Value(COLUMNNAME_IsIndexed);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Sequence.
		@param SeqNo Method of ordering records; lowest number comes first
	*/
	public void setSeqNo (int SeqNo)
	{
		set_Value (COLUMNNAME_SeqNo, Integer.valueOf(SeqNo));
	}

	/** Get Sequence.
		@return Method of ordering records; lowest number comes first
	  */
	public int getSeqNo()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SeqNo);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Search Key.
		@param Value Search key for the record in the format required - must be unique
	*/
	public void setValue (String Value)
	{
		set_Value (COLUMNNAME_Value, Value);
	}

	/** Get Search Key.
		@return Search key for the record in the format required - must be unique
	  */
	public String getValue()
	{
		return (String)get_Value(COLUMNNAME_Value);
	}
}