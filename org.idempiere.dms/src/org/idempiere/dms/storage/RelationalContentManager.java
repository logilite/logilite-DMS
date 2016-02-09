package org.idempiere.dms.storage;

import org.compiere.util.CLogger;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;

public class RelationalContentManager implements IContentManager
{
	public static final String	KEY	= "Relational";

	public static CLogger		log	= CLogger.getCLogger(RelationalContentManager.class);

	@Override
	public String getPath(I_DMS_Content content)
	{
		if (content != null)
		{
			if (content.getParentURL() != null)
				return content.getParentURL() + Utils.getStorageProviderFileSeparator() + content.getName();
			else
				return Utils.getStorageProviderFileSeparator() + content.getName();
		}
		else
			return null;
	}

}
