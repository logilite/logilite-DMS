package org.idempiere.dms.storage;

import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.IThumbnailProviderFactory;

public class DefaultThumbnailProviderFactory implements IThumbnailProviderFactory
{

	@Override
	public IThumbnailProvider get(int AD_Client_ID)
	{
		return new ThumbnailProvider();
	}
}
