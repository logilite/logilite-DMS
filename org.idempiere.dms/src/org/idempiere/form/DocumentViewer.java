package org.idempiere.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.TimeUtil;


public abstract class DocumentViewer extends ADForm
{
	
	private static final long	serialVersionUID	= -650927969633409950L;
	//** Window No *//*
	public int					m_WindowNo			= 0;
	//** The Document Archives *//*

	//public MSMC_Document[]		msmc_Documents		= new MSMC_Document[0];
	//** Archive Index *//*
	public int					m_index				= 0;
	//** Table direct *//*
	public int					m_AD_Table_ID		= 0;
	//** Record direct *//*
	public int					m_Record_ID			= 0;

	public static CLogger		log					= CLogger.getCLogger(DocumentViewer.class);

	public KeyNamePair[] getDocumentTypeData()
	{
		String sql = "SELECT SMC_Document_Type_ID, name FROM SMC_Document_Type";
		return DB.getKeyNamePairs(sql, true);
	}

	public KeyNamePair[] getDocumentCategoryData()
	{
		String sql = "SELECT SMC_Document_Category_ID, name FROM SMC_Document_Category";
		return DB.getKeyNamePairs(sql, true);
	}

	public int cmd_query(KeyNamePair docType, KeyNamePair docCategory, Timestamp createdFrom, Timestamp createdTo)
	{
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT SMC_Document_ID, AD_Client_ID, AD_Org_ID, Created, CreatedBy,");
		sql.append("Updated, UpdatedBy, IsActive, Description, Notes, Not_Valid_Before,");
		sql.append("Not_Valid_After,SMC_Document_Category_ID,SMC_Document_Type_ID,Status, ");
		sql.append("Name, pdfurl FROM SMC_Document");// WHERE AD_Client_ID=" + Env.getAD_Client_ID(Env.getCtx()));

		if (docType != null && docType.getKey() > 0)
			sql.append(" AND SMC_Document_Type_ID=").append(docType.getKey());

		if (docCategory != null && docCategory.getKey() > 0)
			sql.append(" AND SMC_Document_Category_ID=").append(docCategory.getKey());

		if (createdFrom != null)
			sql.append(" AND Created>=").append(DB.TO_DATE(createdFrom, true));

		if (createdTo != null)
			sql.append(" AND Created<").append(DB.TO_DATE(TimeUtil.addDays(createdTo, 1), true));

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		//ArrayList<MSMC_Document> list = new ArrayList<MSMC_Document>();

		try
		{
			pstmt = DB.prepareStatement(sql.toString(), null);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
		//		list.add(new MSMC_Document(Env.getCtx(), rs, null));
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql.toString(), e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		/*if (list.size() == 0)
			log.fine(sql.toString());
		else if (log.isLoggable(Level.FINER))
			log.finer(sql.toString());

		msmc_Documents = new MSMC_Document[list.size()];

		for(int i=0;i<list.size();i++)
			msmc_Documents[i]=list.get(i);
		if (log.isLoggable(Level.INFO))
			log.info("Length=" + msmc_Documents.length);
		*/
		return  0;//list.size();

	}
}
