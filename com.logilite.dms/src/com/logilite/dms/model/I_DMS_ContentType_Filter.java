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

/** Generated Interface for DMS_ContentType_Filter
 *  @author iDempiere (generated) 
 *  @version Release 7.1
 */
@SuppressWarnings("all")
public interface I_DMS_ContentType_Filter 
{

    /** TableName=DMS_ContentType_Filter */
    public static final String Table_Name = "DMS_ContentType_Filter";

    /** AD_Table_ID=1000012 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 6 - System - Client 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(6);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name AD_Table_ID */
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";

	/** Set Table.
	  * Database Table information
	  */
	public void setAD_Table_ID (int AD_Table_ID);

	/** Get Table.
	  * Database Table information
	  */
	public int getAD_Table_ID();

	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException;

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name DMS_ContentType_Filter_ID */
    public static final String COLUMNNAME_DMS_ContentType_Filter_ID = "DMS_ContentType_Filter_ID";

	/** Set DMS Content Type Filter	  */
	public void setDMS_ContentType_Filter_ID (int DMS_ContentType_Filter_ID);

	/** Get DMS Content Type Filter	  */
	public int getDMS_ContentType_Filter_ID();

    /** Column name DMS_ContentType_Filter_UU */
    public static final String COLUMNNAME_DMS_ContentType_Filter_UU = "DMS_ContentType_Filter_UU";

	/** Set DMS Content Type Filter	  */
	public void setDMS_ContentType_Filter_UU (String DMS_ContentType_Filter_UU);

	/** Get DMS Content Type Filter	  */
	public String getDMS_ContentType_Filter_UU();

    /** Column name DMS_ContentType_ID */
    public static final String COLUMNNAME_DMS_ContentType_ID = "DMS_ContentType_ID";

	/** Set DMS Content Type.
	  * DMS Content Type of document like pdf, MS Word, Excel etc.
	  */
	public void setDMS_ContentType_ID (int DMS_ContentType_ID);

	/** Get DMS Content Type.
	  * DMS Content Type of document like pdf, MS Word, Excel etc.
	  */
	public int getDMS_ContentType_ID();

	public I_DMS_ContentType getDMS_ContentType() throws RuntimeException;

    /** Column name FilterClause */
    public static final String COLUMNNAME_FilterClause = "FilterClause";

	/** Set Filter Clause	  */
	public void setFilterClause (String FilterClause);

	/** Get Filter Clause	  */
	public String getFilterClause();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
