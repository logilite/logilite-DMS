package com.logilite.dms.uuid.factory;

import com.logilite.dms.factories.IContentManager;
import com.logilite.dms.factories.IContentManagerProvider;
import com.logilite.dms.uuid.classes.UUIDContentManager;

/**
 * UUID content manager provider factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDContentManagerProviderFactory implements IContentManagerProvider
{

	@Override
	public IContentManager get(String key)
	{
		if (UUIDContentManager.KEY.equals(key))
			return new UUIDContentManager();

		return null;
	}

}
