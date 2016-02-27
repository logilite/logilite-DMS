package org.idempiere.dms.storage;

import org.compiere.model.MClientInfo;
import org.compiere.util.Env;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IContentManagerProvider;

public class DefaultContentManagerFactory implements IContentManagerProvider
{

	@Override
	public IContentManager get(int AD_Client_ID)
	{
		MClientInfo clientInfo = MClientInfo.get(Env.getCtx(), AD_Client_ID, null);
		String key = clientInfo.get_ValueAsString("DMS_ContentManagerType");

		if (key.equalsIgnoreCase(RelationalContentManager.KEY))
		{
			return new RelationalContentManager();
		}
		return null;
	}

}
