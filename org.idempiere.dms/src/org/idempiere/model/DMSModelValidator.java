package org.idempiere.model;

import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MAttributeInstance;
import org.compiere.model.MClient;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.DMSSearchUtils;

public class DMSModelValidator implements ModelValidator
{
	private int				m_AD_Client_ID;
	private static CLogger	log	= CLogger.getCLogger(DMSModelValidator.class);

	@Override
	public void initialize(ModelValidationEngine engine, MClient client)
	{
		// client = null for global validator
		if (client != null)
		{
			m_AD_Client_ID = client.getAD_Client_ID();
			if (log.isLoggable(Level.INFO))
				log.info(client.toString());
		}

		engine.addModelChange(MDMSVersion.Table_Name, this);
		engine.addModelChange(MDMSContent.Table_Name, this);
		engine.addModelChange(MDMSAssociation.Table_Name, this);
		engine.addModelChange(MAttributeInstance.Table_Name, this);
	}

	@Override
	public int getAD_Client_ID()
	{
		return m_AD_Client_ID;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception
	{
		/*
		 * Before save event of attributeInstance table Set indexed flag on
		 * change of attribute value
		 */
		if (MAttributeInstance.Table_Name.equals(po.get_TableName()) && type == TYPE_AFTER_CHANGE)
		{
			MAttributeInstance attributeInstance = (MAttributeInstance) po;
			if (po.is_ValueChanged(MAttributeInstance.COLUMNNAME_Value)
				|| po.is_ValueChanged(MAttributeInstance.COLUMNNAME_ValueDate)
				|| po.is_ValueChanged(MAttributeInstance.COLUMNNAME_ValueNumber))
			{
				int contentID = DB.getSQLValue(po.get_TrxName(), DMSConstant.SQL_CONTENT_FROM_ASI, attributeInstance.getM_AttributeSetInstance_ID());
				if (contentID > 0)
				{
					doReIndexInAllVersions((MDMSContent) MTable.get(po.getCtx(), MDMSContent.Table_ID).getPO(contentID, po.get_TrxName()));
				}
			}
		}
		else if (MDMSContent.Table_Name.equals(po.get_TableName()) && type == TYPE_AFTER_CHANGE)
		{
			if (po.is_ValueChanged(MDMSContent.COLUMNNAME_Name)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_IsActive)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_ParentURL)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_Description)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_DMS_ContentType_ID)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_M_AttributeSetInstance_ID))
			{
				MDMSContent content = (MDMSContent) po;
				doReIndexInAllVersions(content);
			}
		}
		else if ((MDMSVersion.Table_Name.equals(po.get_TableName()) && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE)))
		{
			/*
			 * Create Indexing after commit transaction.
			 */
			final MDMSVersion version = (MDMSVersion) po;
			final MDMSContent content = (MDMSContent) version.getDMS_Content();

			if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && !version.isIndexed())
			{
				Trx trx = Trx.get(version.get_TrxName(), false);
				if (trx != null)
				{
					trx.addTrxEventListener(new TrxEventListener() {

						@Override
						public void afterCommit(Trx trx, boolean success)
						{
							if (success)
							{
								MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), false, null);

								// Delete index for version wise and create same
								String deleteIndexQuery = DMSConstant.DMS_VERSION_ID + ":" + version.getDMS_Version_ID();
								DMSSearchUtils.doIndexingInServer(content, association, version, deleteIndexQuery);

								/***
								 * Get linkable association and do indexing for same
								 */
								List<MDMSAssociation> associationList = MDMSAssociation.getLinkableAssociationFromContent(version.getDMS_Content_ID(), false);
								for (MDMSAssociation linkAssociation : associationList)
								{
									I_DMS_Content linkContent = linkAssociation.getDMS_Content();
									I_DMS_Version linkVersion = MDMSVersion.getLatestVersion(linkContent, false);

									// Delete index for linkable association and Create indexing
									deleteIndexQuery = DMSConstant.DMS_ASSOCIATION_ID + ":" + linkAssociation.getDMS_Association_ID();
									DMSSearchUtils.doIndexingInServer(linkContent, linkAssociation, linkVersion, deleteIndexQuery);
								}
							}
						}


						@Override
						public void afterRollback(Trx trx, boolean success)
						{
						}

						@Override
						public void afterClose(Trx trx)
						{
						}
					});
				}
			}
		}
		else if (MDMSAssociation.Table_Name.equals(po.get_TableName()) && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE))
		{
			// Index creation for any changes in linkable association
			MDMSAssociation association = (MDMSAssociation) po;
			if (MDMSAssociationType.isLink(association)
				&& association.getDMS_Content().isActive()
				&& (type == TYPE_AFTER_NEW || association.is_ValueChanged(MDMSAssociation.COLUMNNAME_IsActive)))
			{
				MDMSContent linkContent = (MDMSContent) association.getDMS_Content();
				//
				String deleteIndexQuery = DMSConstant.DMS_ASSOCIATION_ID + ":" + association.getDMS_Association_ID();
				DMSSearchUtils.doIndexingInServer(linkContent, association, MDMSVersion.getLatestVersion(linkContent), deleteIndexQuery);
			}
		}

		return null;
	} // modelChange

	@Override
	public String docValidate(PO po, int timing)
	{
		return null;
	}

	public void doReIndexInAllVersions(MDMSContent content)
	{
		for (MDMSVersion version : MDMSVersion.getVersionHistory(content))
		{
			// if indexed false then need to fire model validator event for value change
			if (!version.isIndexed())
			{
				version.setIsIndexed(true);
				version.save();
			}

			version.setIsIndexed(false);
			version.save(content.get_TrxName());
		}
	} // doReIndexInAllVersions

}
