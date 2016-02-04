package org.idempiere.dms.storage;

import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IContentManagerProvider;

public class DefaultContentManagerFactory implements IContentManagerProvider
{

	@Override
	public IContentManager get(String key)
	{
		if (key.equalsIgnoreCase("application/pdf"))
		{
			return new ContentManager();
		}
		else if (key.startsWith("imeges/"))
		{
			return new ContentManager();
		}
		return null;
	}

}
