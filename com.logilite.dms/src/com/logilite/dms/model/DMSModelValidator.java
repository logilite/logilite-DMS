package com.logilite.dms.model;

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
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.exception.DMSContentExistException;
import com.logilite.dms.factories.IPermissionManager;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSPermissionUtils;
import com.logilite.dms.util.DMSSearchUtils;

public class DMSModelValidator implements ModelValidator
{
	private int				m_AD_Client_ID;
	private static CLogger	log	= CLogger.getCLogger(DMSModelValidator.class);

	@Override
	public void initialize(ModelValidationEngine engine, MClient client)
	{
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
		engine.addModelChange(MDMSPermission.Table_Name, this);
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
		 * attributeInstance table Set indexed flag on change of attribute value
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
		else if (MDMSContent.Table_Name.equals(po.get_TableName()))
		{
			MDMSContent content = (MDMSContent) po;

			if (type == TYPE_AFTER_CHANGE)
			{
				if (po.is_ValueChanged(MDMSContent.COLUMNNAME_Name)
					|| po.is_ValueChanged(MDMSContent.COLUMNNAME_IsActive)
					|| po.is_ValueChanged(MDMSContent.COLUMNNAME_ParentURL)
					|| po.is_ValueChanged(MDMSContent.COLUMNNAME_Description)
					|| po.is_ValueChanged(MDMSContent.COLUMNNAME_DMS_ContentType_ID)
					|| po.is_ValueChanged(MDMSContent.COLUMNNAME_M_AttributeSetInstance_ID))
				{
					doReIndexInAllVersions(content);
				}
			}
		}
		else if ((MDMSVersion.Table_Name.equals(po.get_TableName()) && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE)))
		{
			/*
			 * Create Indexing after commit transaction.
			 */
			final MDMSVersion version = (MDMSVersion) po;
			final MDMSContent content = (MDMSContent) version.getDMS_Content();

			Trx trx = Trx.get(version.get_TrxName(), false);
			if (trx != null)
			{
				trx.addTrxEventListener(new TrxEventListener() {

					@Override
					public void afterCommit(Trx trx, boolean success)
					{
						if (success)
						{
							// Prevent to fire multiple time event for the same document indexing
							int isIndexed = DB.getSQLValue(null, " SELECT "
																	+ " CASE WHEN isIndexed ='N' THEN 1 ELSE 0 END "
																	+ " FROM DMS_Version WHERE DMS_version_ID=? ", version.getDMS_Version_ID());

							if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && !version.isIndexed() && isIndexed > 0)
							{
								MDMSAssociation association = MDMSAssociation.getParentAssociationFromContent(content.getDMS_Content_ID(), false, null);

								// Delete index for version wise and create same
								DMSSearchUtils.doIndexingInServer(	content, association, version,
																	DMSConstant.DMS_VERSION_ID, "" + version.getDMS_Version_ID());

								/***
								 * Get linkable association and do indexing for same
								 */
								List<MDMSAssociation> associationList = MDMSAssociation.getLinkableAssociationFromContent(version.getDMS_Content_ID(), false);
								for (MDMSAssociation linkAssociation : associationList)
								{
									I_DMS_Content linkContent = linkAssociation.getDMS_Content();
									I_DMS_Version linkVersion = MDMSVersion.getLatestVersion(linkContent, false);

									// Delete index for linkable association and Create indexing
									DMSSearchUtils.doIndexingInServer(	linkContent, linkAssociation, linkVersion,
																		DMSConstant.DMS_ASSOCIATION_ID, "" + linkAssociation.getDMS_Association_ID());
								}
							}
						}
					} // afterCommit

					@Override
					public void afterRollback(Trx trx, boolean success)
					{
					}

					@Override
					public void afterClose(Trx trx)
					{
						if (DMSPermissionUtils.isPermissionAllowed())
						{
							if (content.get_TrxName().equals(trx.getTrxName()))
							{
								DMSPermissionUtils.createContentPermission(content.getDMS_Content_ID());
							}
						}
					}
				});
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
				DMSSearchUtils.doIndexingInServer(	linkContent, association, MDMSVersion.getLatestVersion(linkContent),
													DMSConstant.DMS_ASSOCIATION_ID, "" + association.getDMS_Association_ID());
			}
		}
		else if (MDMSPermission.Table_Name.equals(po.get_TableName()) && type == TYPE_AFTER_NEW)
		{
			MDMSPermission permission = (MDMSPermission) po;

			/*
			 * Create a navigation permission if User1 grant the access of leaf directory or content
			 * to User2 then parent hierarchy should be navigable to top to bottom for User2.
			 * -----
			 * For example: A_Dir > B_Dir > C_Dir
			 * --> If User1 grant the permission to User2 for C_Dir then for User2 also navigable to
			 * C_Dir via using A_Dir & B_Dir
			 * --> So Here we creating A_Dir & B_Dir Navigation permission
			 */

			// Avoid to create navigation permission if current user and permissible user is
			// same. because he has already access to reach there...!!!
			if (Env.getAD_User_ID(Env.getCtx()) != permission.getAD_User_ID())
			{
				IPermissionManager permissionManager = DMSFactoryUtils.getPermissionFactory();
				if (permissionManager != null)
				{
					permissionManager.createNavigationPermission(permission);
				}
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
