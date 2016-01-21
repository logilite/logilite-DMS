package org.idempiere.dms.storage;

import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IContentManagerProvider;

public class RelationalContentManagerProvider implements IContentManagerProvider
{
	@Override
	public IContentManager get(String key)
	{
		if(key.equalsIgnoreCase(ContentManager.key))
			return new ContentManager();
		else
			return null;
	}
}
