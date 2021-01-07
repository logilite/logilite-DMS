package org.idempiere.model;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.util.DMSFactoryUtils;
import org.idempiere.dms.util.DMSSearchUtils;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

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
		if (MAttributeInstance.Table_Name.equals(po.get_TableName())
			&& type == TYPE_BEFORE_CHANGE
			&& (po.is_ValueChanged(MAttributeInstance.COLUMNNAME_Value)
				|| po.is_ValueChanged(MAttributeInstance.COLUMNNAME_ValueTimeStamp)
				|| po.is_ValueChanged(MAttributeInstance.COLUMNNAME_ValueNumber)
				|| po.is_ValueChanged(MAttributeInstance.COLUMNNAME_ValueInt)))
		{
			MAttributeInstance attributeInstance = (MAttributeInstance) po;

			int dmsContentID = DB.getSQLValue(po.get_TrxName(), "SELECT DMS_Content_ID FROM DMS_Content WHERE M_AttributeSetInstance_ID = ? ", attributeInstance
																																								.getM_AttributeSetInstance_ID());

			if (dmsContentID > 0)
			{
				MDMSContent content = new MDMSContent(po.getCtx(), dmsContentID, po.get_TrxName());
				content.setIsIndexed(false);
				content.saveEx();
			}
		}

		/*
		 * Before Save event Set isIndexed flag false for eligible for create
		 * index again
		 */
		if (MDMSContent.Table_Name.equals(po.get_TableName())
			&& type == TYPE_BEFORE_CHANGE
			&& (po.is_ValueChanged(MDMSContent.COLUMNNAME_Name)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_ParentURL)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_Description)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_IsActive)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_DMS_ContentType_ID)
				|| po.is_ValueChanged(MDMSContent.COLUMNNAME_M_AttributeSetInstance_ID)))
		{
			MDMSContent dmsContent = (MDMSContent) po;
			dmsContent.setIsIndexed(false);
		}

		/*
		 * After Save event Create Indexing after commit transaction.
		 */
		if (MDMSContent.Table_Name.equals(po.get_TableName()) && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE))
		{
			final MDMSContent content = (MDMSContent) po;

			if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && !content.isIndexed())
			{
				Trx trx = Trx.get(content.get_TrxName(), false);
				if (trx != null)
				{
					trx.addTrxEventListener(new TrxEventListener() {

						@Override
						public void afterCommit(Trx trx, boolean success)
						{
							MDMSAssociation association = MDMSAssociation.getAssociationFromContent(content.getDMS_Content_ID(), null);

							IIndexSearcher indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

							if (indexSeracher == null)
							{
								throw new AdempiereException("Index Server not found");
							}

							IFileStorageProvider fsProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);
							if (fsProvider == null)
								throw new AdempiereException("Storage provider is not define on clientInfo.");

							IContentManager contentManager = DMSFactoryUtils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));
							if (contentManager == null)
								throw new AdempiereException("Content manager is not found.");

							// Delete existing index
							indexSeracher.deleteIndexByField(DMSConstant.DMS_CONTENT_ID, "" + content.getDMS_Content_ID());

							// Create index
							File file = fsProvider.getFile(contentManager.getPathByValue(content));
							Map<String, Object> solrValue = DMSSearchUtils.createIndexMap(content, association, file);
							indexSeracher.indexContent(solrValue);

							// Update the value of IsIndexed flag in Content
							if (!content.isIndexed())
							{
								DB.executeUpdate("UPDATE DMS_Content SET IsIndexed = 'Y' WHERE DMS_Content_ID = ? ", content.get_ID(), null);
							}

							if (content.isSyncIndexForLinkableDocs())
							{
								// Create index of Linkable docs is exists
								int[] linkAssociationIDs = DB.getIDsEx(	null, DMSConstant.SQL_LINK_ASSOCIATIONS_FROM_RELATED_TO_CONTENT,
																		MDMSAssociationType.VERSION_ID, content.getDMS_Content_ID(),
																		content.getDMS_Content_ID(), MDMSAssociationType.VERSION_ID);

								for (int linkAssociationID : linkAssociationIDs)
								{
									MDMSAssociation associationLink = new MDMSAssociation(Env.getCtx(), linkAssociationID, null);

									solrValue = DMSSearchUtils.createIndexMap(content, associationLink, file);

									indexSeracher.indexContent(solrValue);
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
			// For index changes of deleting linkable content.
			MDMSAssociation association = (MDMSAssociation) po;
			if (MDMSAssociationType.isLink(association)
				&& association.getDMS_Content().isActive()
				&& association.is_ValueChanged(MDMSAssociation.COLUMNNAME_IsActive)
				&& !association.isActive())
			{
				MDMSContent linkContent = (MDMSContent) association.getDMS_Content();
				linkContent.setIsIndexed(false);
				linkContent.setSyncIndexForLinkableDocs(true);
				linkContent.saveEx();
			}
		}

		return null;
	} // modelChange

	@Override
	public String docValidate(PO po, int timing)
	{
		return null;
	}

}
