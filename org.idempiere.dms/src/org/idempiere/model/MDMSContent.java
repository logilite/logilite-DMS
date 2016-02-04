package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMSContent extends X_DMS_Content {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6250555517481249806L;

	public MDMSContent(Properties ctx, int DMS_Content_ID, String trxName) {
		super(ctx, DMS_Content_ID, trxName);
	}

	public MDMSContent(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
