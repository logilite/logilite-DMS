package org.idempiere.dms.storage;

import org.compiere.util.CLogger;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.model.I_DMS_Content;

public class ContentManager implements IContentManager
{
	public static String KEY = "Relational";
	
	public static CLogger	log	= CLogger.getCLogger(ContentManager.class);

	@Override
	public String getPath(I_DMS_Content content)
	{
		return content.getParentURL(); 
	}

}
