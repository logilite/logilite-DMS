package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMS_MimeType extends X_DMS_MimeType {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3814987450064904684L;

	public MDMS_MimeType(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MDMS_MimeType(Properties ctx, int DMS_MimeType_ID, String trxName) {
		super(ctx, DMS_MimeType_ID, trxName);
	}

}
