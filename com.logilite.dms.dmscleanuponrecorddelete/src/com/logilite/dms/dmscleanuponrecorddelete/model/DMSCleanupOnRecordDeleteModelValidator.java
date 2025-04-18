package com.logilite.dms.dmscleanuponrecorddelete.model;

import java.sql.ResultSet;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MTable;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.util.DMSOprUtils;
import com.logilite.dms.util.Utils;

/**
 * Model Validator for DMS content delete on Record delete
 * 
 * @author Logilite Technology
 */
public class DMSCleanupOnRecordDeleteModelValidator implements ModelValidator
{
	private static CLogger	log				= CLogger.getCLogger(DMSCleanupOnRecordDeleteModelValidator.class);
	private int				m_AD_Client_ID	= 0;

	@Override
	public void initialize(ModelValidationEngine engine, MClient client)
	{
		if (client != null)
		{
			m_AD_Client_ID = client.getAD_Client_ID();
			if (log.isLoggable(Level.INFO))
				log.info(client.toString());
		}

		addModelChangesListener(engine);
	} // initialize

	/**
	 * Add model Listener in define Tables
	 * 
	 * @param engine Model Validation Engine
	 */
	private void addModelChangesListener(ModelValidationEngine engine)
	{
		/**
		 * Remove all tables related to `DMS_%`, `T_%`, 'I_%', and `AD_%` (except `AD_User`,
		 * `AD_PrintFormat`, and `AD_PrintFormat_Access`) along with columns containing multiple
		 * ParentLink references
		 */
		String sql = "SELECT * FROM DMS_AUTO_CLEANER_TABLES_V";
		try
		{
			ResultSet rs = DB.prepareStatement(sql, null).executeQuery();
			while (rs.next())
			{
				String tableName = rs.getString(1);
				engine.addModelChange((String) tableName, this);
			}
		}
		catch (Exception e)
		{
			throw new AdempiereException("Table not found");
		}
	} // addModelChangesListener

	@Override
	public int getAD_Client_ID()
	{
		return m_AD_Client_ID;
	} // getAD_Client_ID

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception
	{
		if (type == TYPE_AFTER_DELETE)
		{
			String parentURL = "%" + Utils.getDMSMountingBase(po.getAD_Client_ID()) + "%" + po.get_TableName();
			int associationID = DB.getSQLValue(	po.get_TrxName(), DMSConstant.SQL_GET_RECORD_MOUNTING_ASSOCIATION, po.getAD_Client_ID(), parentURL,
												po.get_IDOld(), po.get_Table_ID());

			if (associationID > 0)
			{
				DMS dms = new DMS(Env.getAD_Client_ID(Env.getCtx()), po.get_TableName());

				MDMSAssociation association = (MDMSAssociation) MTable	.get(Env.getCtx(), MDMSAssociation.Table_ID)
																		.getPO(associationID, po.get_TrxName());

				DMSOprUtils.deletePhysicalContentsAndHierarchy(dms, association);
			}
		}
		return null;
	} // modelChange

	@Override
	public String docValidate(PO po, int timing)
	{
		return null;
	} // docValidate
} // DMSCleanupOnRecordDeleteModelValidator
