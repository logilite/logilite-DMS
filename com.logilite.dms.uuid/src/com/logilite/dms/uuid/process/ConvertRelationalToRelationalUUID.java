package com.logilite.dms.uuid.process;

import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

/**
 * Process for converting relational DMS data structure to relational UUID structure
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ConvertRelationalToRelationalUUID extends SvrProcess
{

	@Override
	protected void prepare()
	{

	}

	@Override
	protected String doIt() throws Exception
	{

		String sql = "select  DISTINCT AD_table_ID from dms_association WHERE AD_table_ID >0;";
		int query = DB.getSQLValue(null, sql, getParameter());
		String tables;
		return null;
		// for()
		// {
		// DMS dms;
		// dms = new DMS(getAD_Client_ID());
		// dms.initMountingStrategy(tables);
		//
		// {
		// MDMSContent content = new MDMSContent(getCtx(), null, sql);
		// List<MDMSVersion> versions = content.getAllVersions();
		// for (MDMSVersion ver :versions)
		// {
		// String uu=ver.getDMS_Version_UU();
		// dms.renameContent(uu, content);
		// }
		//
		// }
		// }

	}
}
