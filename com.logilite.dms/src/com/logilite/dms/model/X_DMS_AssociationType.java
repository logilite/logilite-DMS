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

/** Generated Model for DMS_AssociationType
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="DMS_AssociationType")
public class X_DMS_AssociationType extends PO implements I_DMS_AssociationType, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20240812L;

    /** Standard Constructor */
    public X_DMS_AssociationType (Properties ctx, int DMS_AssociationType_ID, String trxName)
    {
      super (ctx, DMS_AssociationType_ID, trxName);
      /** if (DMS_AssociationType_ID == 0)
        {
			setDMS_AssociationType_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_AssociationType (Properties ctx, int DMS_AssociationType_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_AssociationType_ID, trxName, virtualColumns);
      /** if (DMS_AssociationType_ID == 0)
        {
			setDMS_AssociationType_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_AssociationType (Properties ctx, String DMS_AssociationType_UU, String trxName)
    {
      super (ctx, DMS_AssociationType_UU, trxName);
      /** if (DMS_AssociationType_UU == null)
        {
			setDMS_AssociationType_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_AssociationType (Properties ctx, String DMS_AssociationType_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_AssociationType_UU, trxName, virtualColumns);
      /** if (DMS_AssociationType_UU == null)
        {
			setDMS_AssociationType_ID (0);
        } */
    }

    /** Load Constructor */
    public X_DMS_AssociationType (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_DMS_AssociationType[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
      return sb.toString();
    }

	/** Set DMS AssociationType ID.
		@param DMS_AssociationType_ID DMS AssociationType ID
	*/
	public void setDMS_AssociationType_ID (int DMS_AssociationType_ID)
	{
		if (DMS_AssociationType_ID < 1)
			set_ValueNoCheck (COLUMNNAME_DMS_AssociationType_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_DMS_AssociationType_ID, Integer.valueOf(DMS_AssociationType_ID));
	}

	/** Get DMS AssociationType ID.
		@return DMS AssociationType ID	  */
	public int getDMS_AssociationType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_AssociationType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS_AssociationType_UU.
		@param DMS_AssociationType_UU DMS_AssociationType_UU
	*/
	public void setDMS_AssociationType_UU (String DMS_AssociationType_UU)
	{
		set_Value (COLUMNNAME_DMS_AssociationType_UU, DMS_AssociationType_UU);
	}

	/** Get DMS_AssociationType_UU.
		@return DMS_AssociationType_UU	  */
	public String getDMS_AssociationType_UU()
	{
		return (String)get_Value(COLUMNNAME_DMS_AssociationType_UU);
	}

	/** EntityType AD_Reference_ID=389 */
	public static final int ENTITYTYPE_AD_Reference_ID=389;
	/** Set Entity Type.
		@param EntityType Dictionary Entity Type; Determines ownership and synchronization
	*/
	public void setEntityType (String EntityType)
	{

		set_Value (COLUMNNAME_EntityType, EntityType);
	}

	/** Get Entity Type.
		@return Dictionary Entity Type; Determines ownership and synchronization
	  */
	public String getEntityType()
	{
		return (String)get_Value(COLUMNNAME_EntityType);
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
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