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

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for DMS_Content
 *  @author iDempiere (generated) 
 *  @version Release 3.1
 */
@SuppressWarnings("all")
public interface I_DMS_Content 
{

    /** TableName=DMS_Content */
    public static final String Table_Name = "DMS_Content";

    /** AD_Table_ID=1000000 */
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

    /** Column name ContentBaseType */
    public static final String COLUMNNAME_ContentBaseType = "ContentBaseType";

	/** Set ContentBaseType	  */
	public void setContentBaseType (String ContentBaseType);

	/** Get ContentBaseType	  */
	public String getContentBaseType();

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

    /** Column name Description */
    public static final String COLUMNNAME_Description = "Description";

	/** Set Description.
	  * Optional short description of the record
	  */
	public void setDescription (String Description);

	/** Get Description.
	  * Optional short description of the record
	  */
	public String getDescription();

    /** Column name DMS_Content_ID */
    public static final String COLUMNNAME_DMS_Content_ID = "DMS_Content_ID";

	/** Set DMS Content	  */
	public void setDMS_Content_ID (int DMS_Content_ID);

	/** Get DMS Content	  */
	public int getDMS_Content_ID();

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

    /** Column name DMS_FileSize */
    public static final String COLUMNNAME_DMS_FileSize = "DMS_FileSize";

	/** Set DMS_FileSize	  */
	public void setDMS_FileSize (String DMS_FileSize);

	/** Get DMS_FileSize	  */
	public String getDMS_FileSize();

    /** Column name DMS_MimeType_ID */
    public static final String COLUMNNAME_DMS_MimeType_ID = "DMS_MimeType_ID";

	/** Set Mime Type.
	  * Mime Type of the uploaded file
	  */
	public void setDMS_MimeType_ID (int DMS_MimeType_ID);

	/** Get Mime Type.
	  * Mime Type of the uploaded file
	  */
	public int getDMS_MimeType_ID();

	public I_DMS_MimeType getDMS_MimeType() throws RuntimeException;

    /** Column name DMS_Status_ID */
    public static final String COLUMNNAME_DMS_Status_ID = "DMS_Status_ID";

	/** Set Content Status.
	  * Classify Content by his status
	  */
	public void setDMS_Status_ID (int DMS_Status_ID);

	/** Get Content Status.
	  * Classify Content by his status
	  */
	public int getDMS_Status_ID();

	public I_DMS_Status getDMS_Status() throws RuntimeException;

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

    /** Column name IsReadOnly */
    public static final String COLUMNNAME_IsReadOnly = "IsReadOnly";

	/** Set Read Only.
	  * Field is read only
	  */
	public void setIsReadOnly (boolean IsReadOnly);

	/** Get Read Only.
	  * Field is read only
	  */
	public boolean isReadOnly();

    /** Column name M_AttributeSetInstance_ID */
    public static final String COLUMNNAME_M_AttributeSetInstance_ID = "M_AttributeSetInstance_ID";

	/** Set Attribute Set Instance.
	  * Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID);

	/** Get Attribute Set Instance.
	  * Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID();

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException;

    /** Column name Name */
    public static final String COLUMNNAME_Name = "Name";

	/** Set Name.
	  * Alphanumeric identifier of the entity
	  */
	public void setName (String Name);

	/** Get Name.
	  * Alphanumeric identifier of the entity
	  */
	public String getName();

    /** Column name ParentURL */
    public static final String COLUMNNAME_ParentURL = "ParentURL";

	/** Set ParentURL	  */
	public void setParentURL (String ParentURL);

	/** Get ParentURL	  */
	public String getParentURL();

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

    /** Column name ValidFromDate */
    public static final String COLUMNNAME_ValidFromDate = "ValidFromDate";

	/** Set Valid From.
	  * DMS Conent validity From Date
	  */
	public void setValidFromDate (Timestamp ValidFromDate);

	/** Get Valid From.
	  * DMS Conent validity From Date
	  */
	public Timestamp getValidFromDate();

    /** Column name ValidToDate */
    public static final String COLUMNNAME_ValidToDate = "ValidToDate";

	/** Set Valid To.
	  * DMS Conent validity To Date
	  */
	public void setValidToDate (Timestamp ValidToDate);

	/** Get Valid To.
	  * DMS Conent validity To Date
	  */
	public Timestamp getValidToDate();

    /** Column name Value */
    public static final String COLUMNNAME_Value = "Value";

	/** Set Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public void setValue (String Value);

	/** Get Search Key.
	  * Search key for the record in the format required - must be unique
	  */
	public String getValue();
}
