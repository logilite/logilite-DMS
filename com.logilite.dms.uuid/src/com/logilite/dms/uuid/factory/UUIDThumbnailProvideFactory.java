package com.logilite.dms.uuid.factory;

import org.compiere.model.MClientInfo;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.IThumbnailProviderFactory;

import com.logilite.dms.uuid.classes.UUIDContentManager;
import com.logilite.dms.uuid.storage.UUIDThumbnailProvider;

public class UUIDThumbnailProvideFactory implements IThumbnailProviderFactory
{
	@Override
	public IThumbnailProvider get(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID);
		String key = clientInfo.get_ValueAsString("DMS_ContentManagerType");
		
		if (UUIDContentManager.KEY.equals(key))
		{
			return new UUIDThumbnailProvider();
		}
		return null;
	}

}
