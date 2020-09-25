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
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MTable;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;

public class MDMSAssociation extends X_DMS_Association
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4536595975268643483L;

	static CLogger				log					= CLogger.getCLogger(MDMSAssociation.class);

	//
	public MDMSAssociation(Properties ctx, int DMS_Association_ID, String trxName)
	{
		super(ctx, DMS_Association_ID, trxName);
	}

	public MDMSAssociation(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Update Table & Record Reference and Save it
	 * 
	 * @param association
	 * @param tableID
	 * @param recordID
	 */
	public void updateTableRecordRef(int tableID, int recordID)
	{
		setAD_Table_ID(tableID);
		setRecord_ID(recordID);
		saveEx();
	} // updateTableRecordRef

	/**
	 * Create Association
	 * 
	 * @param  contentID
	 * @param  contentRelatedID
	 * @param  Record_ID
	 * @param  AD_Table_ID
	 * @param  associationTypeID
	 * @param  seqNo
	 * @param  trxName
	 * @return                   DMS_Association_ID
	 */
	public static int create(int contentID, int contentRelatedID, int Record_ID, int AD_Table_ID, int associationTypeID, int seqNo, String trxName)
	{
		MDMSAssociation association = (MDMSAssociation) MTable.get(Env.getCtx(), MDMSAssociation.Table_Name).getPO(0, trxName);
		association.setSeqNo(seqNo);
		association.setDMS_Content_ID(contentID);
		association.setDMS_Content_Related_ID(contentRelatedID);
		if (Record_ID > 0)
			association.setRecord_ID(Record_ID);
		if (AD_Table_ID > 0)
			association.setAD_Table_ID(AD_Table_ID);
		if (associationTypeID > 0)
			association.setDMS_AssociationType_ID(associationTypeID);
		association.saveEx();

		log.log(Level.INFO, "New DMS_Association_ID = " + association.getDMS_Association_ID() + " of Content=" + contentID + " TableID=" + AD_Table_ID
							+ " RecordID=" + Record_ID);
		return association.getDMS_Association_ID();
	} // create

	/**
	 * Get association from content ID with referring linkable association
	 * 
	 * @param  contentID
	 * @return           {@link MDMSAssociation} - Linkable association
	 */
	public static MDMSAssociation getLinkableAssociationFromContent(int contentID)
	{
		return MDMSAssociation.getAssociationFromContent(contentID, true, null);
	} // getLinkableAssociationFromContent

	/**
	 * Get association from content ID without referring linkable association
	 * 
	 * @param  contentID
	 * @param  trxName
	 * @return           {@link MDMSAssociation} - Non-Linkable Association
	 */
	public static MDMSAssociation getAssociationFromContent(int contentID, String trxName)
	{
		return MDMSAssociation.getAssociationFromContent(contentID, false, trxName);
	} // getAssociationFromContent

	/**
	 * Get association from content ID with/without referring linkable association
	 * 
	 * @param  contentID
	 * @param  isLinkAssociationOnly - True if only Get Linkable Association
	 * @param  trxName
	 * @return                       {@link MDMSAssociation}
	 */
	public static MDMSAssociation getAssociationFromContent(int contentID, boolean isLinkAssociationOnly, String trxName)
	{
		String sql = DMSConstant.SQL_GET_ASSOCIATION_ID_FROM_CONTENT + (isLinkAssociationOnly ? " = " : " <> " + MDMSAssociationType.LINK_ID);
		int DMS_Association_ID = DB.getSQLValue(trxName, sql, contentID);
		if (DMS_Association_ID > 0)
			return new MDMSAssociation(Env.getCtx(), DMS_Association_ID, trxName);
		return null;
	} // getAssociationFromContent

}
