package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MDMS_Status extends X_DMS_Status {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2585968698762732410L;

	public MDMS_Status(Properties ctx, int DMS_Status_ID, String trxName) {
		super(ctx, DMS_Status_ID, trxName);
	}

	public MDMS_Status(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
