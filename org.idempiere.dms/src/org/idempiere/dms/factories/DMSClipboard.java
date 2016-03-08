package org.idempiere.dms.factories;

import java.util.HashMap;
import java.util.Map;

import org.compiere.model.MSession;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;

public class DMSClipboard
{
	private static CLogger						log							= CLogger.getCLogger(DMSClipboard.class);

	public static Map<Integer, I_DMS_Content>	storeSessionIDandContentID	= new HashMap<Integer, I_DMS_Content>();

	public static void putCopyDMSContent(MDMSContent content)
	{

		MSession mSession = MSession.get(Env.getCtx(), false);

		if (content != null && mSession != null)
		{
			storeSessionIDandContentID.put(mSession.getAD_Session_ID(), content);
		}
	}

	public static MDMSContent getCopyDMSContent()
	{
		MSession mSession = MSession.get(Env.getCtx(), false);

		if (mSession != null)
		{
			return (MDMSContent) storeSessionIDandContentID.get(mSession.getAD_Session_ID());
		}
		return null;
	}

	public static void removeSessionId(int ad_Session_ID)
	{
		storeSessionIDandContentID.remove(ad_Session_ID);
		
	}
}
