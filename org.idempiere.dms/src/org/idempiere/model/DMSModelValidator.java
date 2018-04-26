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

		if (MDMSContent.Table_Name.equals(po.get_TableName()) && type == TYPE_BEFORE_CHANGE
				&& po.is_ValueChanged("IsActive"))
		{
			MDMSContent dmsContent = (MDMSContent)po;
			
			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?", dmsContent.getDMS_Content_ID());
			
			MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);
			
			Map<String, Object> solrValue = Utils.createIndexMap(dmsContent, DMSAssociation);
			IIndexSearcher indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

			if (indexSeracher == null)
			{
				throw new AdempiereException("Index Server not found");
			}
			
			indexSeracher.deleteIndex(dmsContent.getDMS_Content_ID());
			indexSeracher.indexContent(solrValue);
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
