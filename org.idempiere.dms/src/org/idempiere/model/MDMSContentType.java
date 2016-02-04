package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMSContentType extends X_DMS_ContentType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7974749939408476322L;

	public MDMSContentType(Properties ctx, int DMS_ContentType_ID,
			String trxName) {
		super(ctx, DMS_ContentType_ID, trxName);
	}

	public MDMSContentType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
