package org.idempiere.dms.storage;

import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.IThumbnailProviderFactory;

public class DefaultThumbnailProviderFactory implements IThumbnailProviderFactory
{

	@Override
	public IThumbnailProvider get(int AD_Client_ID)
	{
		if (Env.getAD_Client_ID(Env.getCtx()) == AD_Client_ID)
			return new ThumbnailProvider();
		else
			return null;
	}
}
