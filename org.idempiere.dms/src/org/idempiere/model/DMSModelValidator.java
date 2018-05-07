package org.idempiere.model;

import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;
import org.idempiere.dms.factories.Utils;

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

	}

	@Override
	public int getAD_Client_ID()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception
	{

		/*
		 * Before Save event Set isIndexed flag false for eligible for create
		 * index again
		 */
		if (MDMSContent.Table_Name.equals(po.get_TableName()) && type == TYPE_BEFORE_CHANGE
				&& (po.is_ValueChanged(MDMSContent.COLUMNNAME_Name)
						|| po.is_ValueChanged(MDMSContent.COLUMNNAME_ParentURL)
						|| po.is_ValueChanged(MDMSContent.COLUMNNAME_Description)
						|| po.is_ValueChanged(MDMSContent.COLUMNNAME_IsActive)))
		{
			MDMSContent dmsContent = (MDMSContent) po;
			dmsContent.setIsIndexed(false);
		}

		/*
		 * After Save event Create Indexing after commit transaction.
		 */
		if (MDMSContent.Table_Name.equals(po.get_TableName()) && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE))
		{
			final MDMSContent dmsContent = (MDMSContent) po;

			String contentType = dmsContent.getContentBaseType();
			final boolean isIndexed = dmsContent.isIndexed();

			if (MDMSContent.CONTENTBASETYPE_Content.equals(contentType) && !isIndexed)
			{
				Trx trx = Trx.get(dmsContent.get_TrxName(), false);
				if (trx != null)
				{
					trx.addTrxEventListener(new TrxEventListener() {

						@Override
						public void afterCommit(Trx trx, boolean success)
						{
							int DMS_Association_ID = DB.getSQLValue(null,
									"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
									dmsContent.getDMS_Content_ID());

							MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID,
									null);

							try
							{
								Map<String, Object> solrValue = Utils.createIndexMap(dmsContent, DMSAssociation);
								IIndexSearcher indexSeracher = ServiceUtils
										.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

								if (indexSeracher == null)
								{
									throw new AdempiereException("Index Server not found");
								}

								// Delete and Create Index
								indexSeracher.deleteIndex(dmsContent.getDMS_Content_ID());
								indexSeracher.indexContent(solrValue);
							}
							catch (Exception e)
							{
								log.log(Level.SEVERE, "RE-Indexing of Content Failure :", e);
								// throw new AdempiereException("RE-Indexing of
								// Content Failure :" + e);
							}

							// Update the value of IsIndexed flag in dmsContent

							if (!isIndexed)
							{
								DB.executeUpdate("UPDATE DMS_Content SET IsIndexed = 'Y' WHERE DMS_Content_ID = ? ",
										dmsContent.get_ID(), null);
							}

						}

						@Override
						public void afterRollback(Trx trx, boolean success)
						{
							// TODO Auto-generated method stub

						}

						@Override
						public void afterClose(Trx trx)
						{
							// TODO Auto-generated method stub

						}

					});

				}
			}
		}

		return null;
	}

	@Override
	public String docValidate(PO po, int timing)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
