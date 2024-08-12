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

/** Generated Model for DMS_Permission
 *  @author iDempiere (generated)
 *  @version Release 11 - $Id$ */
@org.adempiere.base.Model(table="DMS_Permission")
public class X_DMS_Permission extends PO implements I_DMS_Permission, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20240812L;

    /** Standard Constructor */
    public X_DMS_Permission (Properties ctx, int DMS_Permission_ID, String trxName)
    {
      super (ctx, DMS_Permission_ID, trxName);
      /** if (DMS_Permission_ID == 0)
        {
			setDMS_Content_ID (0);
			setDMS_Permission_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Permission (Properties ctx, int DMS_Permission_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_Permission_ID, trxName, virtualColumns);
      /** if (DMS_Permission_ID == 0)
        {
			setDMS_Content_ID (0);
			setDMS_Permission_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Permission (Properties ctx, String DMS_Permission_UU, String trxName)
    {
      super (ctx, DMS_Permission_UU, trxName);
      /** if (DMS_Permission_UU == null)
        {
			setDMS_Content_ID (0);
			setDMS_Permission_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_DMS_Permission (Properties ctx, String DMS_Permission_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, DMS_Permission_UU, trxName, virtualColumns);
      /** if (DMS_Permission_UU == null)
        {
			setDMS_Content_ID (0);
			setDMS_Permission_ID (0);
        } */
    }

    /** Load Constructor */
    public X_DMS_Permission (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_DMS_Permission[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return (org.compiere.model.I_AD_Role)MTable.get(getCtx(), org.compiere.model.I_AD_Role.Table_ID)
			.getPO(getAD_Role_ID(), get_TrxName());
	}

	/** Set Role.
		@param AD_Role_ID Responsibility Role
	*/
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0)
			set_Value (COLUMNNAME_AD_Role_ID, null);
		else
			set_Value (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Role.
		@return Responsibility Role
	  */
	public int getAD_Role_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_Value (COLUMNNAME_AD_User_ID, null);
		else
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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
			set_Value (COLUMNNAME_DMS_Content_ID, null);
		else
			set_Value (COLUMNNAME_DMS_Content_ID, Integer.valueOf(DMS_Content_ID));
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

	/** Set DMS Permission.
		@param DMS_Permission_ID DMS Permission
	*/
	public void setDMS_Permission_ID (int DMS_Permission_ID)
	{
		if (DMS_Permission_ID < 1)
			set_ValueNoCheck (COLUMNNAME_DMS_Permission_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_DMS_Permission_ID, Integer.valueOf(DMS_Permission_ID));
	}

	/** Get DMS Permission.
		@return DMS Permission	  */
	public int getDMS_Permission_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DMS_Permission_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set DMS_Permission_UU.
		@param DMS_Permission_UU DMS_Permission_UU
	*/
	public void setDMS_Permission_UU (String DMS_Permission_UU)
	{
		set_Value (COLUMNNAME_DMS_Permission_UU, DMS_Permission_UU);
	}

	/** Get DMS_Permission_UU.
		@return DMS_Permission_UU	  */
	public String getDMS_Permission_UU()
	{
		return (String)get_Value(COLUMNNAME_DMS_Permission_UU);
	}

	/** Set All Permission.
		@param IsAllPermission All Permission
	*/
	public void setIsAllPermission (boolean IsAllPermission)
	{
		set_Value (COLUMNNAME_IsAllPermission, Boolean.valueOf(IsAllPermission));
	}

	/** Get All Permission.
		@return All Permission	  */
	public boolean isAllPermission()
	{
		Object oo = get_Value(COLUMNNAME_IsAllPermission);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Delete.
		@param IsDelete Delete
	*/
	public void setIsDelete (boolean IsDelete)
	{
		set_Value (COLUMNNAME_IsDelete, Boolean.valueOf(IsDelete));
	}

	/** Get Delete.
		@return Delete	  */
	public boolean isDelete()
	{
		Object oo = get_Value(COLUMNNAME_IsDelete);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Navigation.
		@param IsNavigation Navigation
	*/
	public void setIsNavigation (boolean IsNavigation)
	{
		set_Value (COLUMNNAME_IsNavigation, Boolean.valueOf(IsNavigation));
	}

	/** Get Navigation.
		@return Navigation	  */
	public boolean isNavigation()
	{
		Object oo = get_Value(COLUMNNAME_IsNavigation);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Read.
		@param IsRead Read
	*/
	public void setIsRead (boolean IsRead)
	{
		set_Value (COLUMNNAME_IsRead, Boolean.valueOf(IsRead));
	}

	/** Get Read.
		@return Read	  */
	public boolean isRead()
	{
		Object oo = get_Value(COLUMNNAME_IsRead);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Write.
		@param IsWrite Write
	*/
	public void setIsWrite (boolean IsWrite)
	{
		set_Value (COLUMNNAME_IsWrite, Boolean.valueOf(IsWrite));
	}

	/** Get Write.
		@return Write	  */
	public boolean isWrite()
	{
		Object oo = get_Value(COLUMNNAME_IsWrite);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}
}