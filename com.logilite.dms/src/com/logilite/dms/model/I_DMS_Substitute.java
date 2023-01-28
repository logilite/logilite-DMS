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
package com.logilite.dms.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/**
 * Generated Interface for DMS_Substitute
 * 
 * @author  iDempiere (generated)
 * @version Release 5.1
 */
@SuppressWarnings("all")
public interface I_DMS_Substitute
{

	/** TableName=DMS_Substitute */
	public static final String	Table_Name				= "DMS_Substitute";

	/** AD_Table_ID=1000011 */
	public static final int		Table_ID				= MTable.getTable_ID(Table_Name);

	KeyNamePair					Model					= new KeyNamePair(Table_ID, Table_Name);

	/**
	 * AccessLevel = 6 - System - Client
	 */
	BigDecimal					accessLevel				= BigDecimal.valueOf(6);

	/** Load Meta Data */

	/** Column name AD_Client_ID */
	public static final String	COLUMNNAME_AD_Client_ID	= "AD_Client_ID";

	/**
	 * Get Client.
	 * Client/Tenant for this installation.
	 */
	public int getAD_Client_ID();

	/** Column name AD_Column_ID */
	public static final String COLUMNNAME_AD_Column_ID = "AD_Column_ID";

	/**
	 * Set Origin Column.
	 * Column in the origin table selected
	 */
	public void setAD_Column_ID(int AD_Column_ID);

	/**
	 * Get Origin Column.
	 * Column in the origin table selected
	 */
	public int getAD_Column_ID();

	public org.compiere.model.I_AD_Column getAD_Column() throws RuntimeException;

	/** Column name AD_Org_ID */
	public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/**
	 * Set Organization.
	 * Organizational entity within client
	 */
	public void setAD_Org_ID(int AD_Org_ID);

	/**
	 * Get Organization.
	 * Organizational entity within client
	 */
	public int getAD_Org_ID();

	/** Column name AD_Table_ID */
	public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/**
	 * Set Origin Table.
	 * Database Table information
	 */
	public void setAD_Table_ID(int AD_Table_ID);

	/**
	 * Get Origin Table.
	 * Database Table information
	 */
	public int getAD_Table_ID();

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

	/** Column name Created */
	public static final String COLUMNNAME_Created = "Created";

	/**
	 * Get Created.
	 * Date this record was created
	 */
	public Timestamp getCreated();

	/** Column name CreatedBy */
	public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/**
	 * Get Created By.
	 * User who created this records
	 */
	public int getCreatedBy();

	/** Column name Description */
	public static final String COLUMNNAME_Description = "Description";

	/**
	 * Set Description.
	 * Optional short description of the record
	 */
	public void setDescription(String Description);

	/**
	 * Get Description.
	 * Optional short description of the record
	 */
	public String getDescription();

	/** Column name Detail */
	public static final String COLUMNNAME_Detail = "Detail";

	/** Set Detail */
	public void setDetail(String Detail);

	/** Get Detail */
	public String getDetail();

	/** Column name DMS_Substitute_ID */
	public static final String COLUMNNAME_DMS_Substitute_ID = "DMS_Substitute_ID";

	/** Set DMS Substitute */
	public void setDMS_Substitute_ID(int DMS_Substitute_ID);

	/** Get DMS Substitute */
	public int getDMS_Substitute_ID();

	/** Column name DMS_Substitute_Table_ID */
	public static final String COLUMNNAME_DMS_Substitute_Table_ID = "DMS_Substitute_Table_ID";

	/**
	 * Set Substitute Table.
	 * Substitute Table information
	 */
	public void setDMS_Substitute_Table_ID(int DMS_Substitute_Table_ID);

	/**
	 * Get Substitute Table.
	 * Substitute Table information
	 */
	public int getDMS_Substitute_Table_ID();

	public org.compiere.model.I_AD_Table getDMS_Substitute_Table() throws RuntimeException;

	/** Column name DMS_Substitute_UU */
	public static final String COLUMNNAME_DMS_Substitute_UU = "DMS_Substitute_UU";

	/** Set DMS_Substitute_UU */
	public void setDMS_Substitute_UU(String DMS_Substitute_UU);

	/** Get DMS_Substitute_UU */
	public String getDMS_Substitute_UU();

	/** Column name IsActive */
	public static final String COLUMNNAME_IsActive = "IsActive";

	/**
	 * Set Active.
	 * The record is active in the system
	 */
	public void setIsActive(boolean IsActive);

	/**
	 * Get Active.
	 * The record is active in the system
	 */
	public boolean isActive();

	/** Column name Updated */
	public static final String COLUMNNAME_Updated = "Updated";

	/**
	 * Get Updated.
	 * Date this record was updated
	 */
	public Timestamp getUpdated();

	/** Column name UpdatedBy */
	public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/**
	 * Get Updated By.
	 * User who updated this records
	 */
	public int getUpdatedBy();
}
