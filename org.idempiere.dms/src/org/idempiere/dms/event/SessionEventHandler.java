package org.idempiere.dms.event;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MSession;
import org.compiere.model.PO;
import org.idempiere.dms.factories.DMSClipboard;
import org.osgi.service.event.Event;

public class SessionEventHandler extends AbstractEventHandler
{

	@Override
	protected void doHandleEvent(Event event)
	{
		PO po = getPO(event);
		MSession mSession = (MSession) po;

		if (event.getTopic().equals(IEventTopics.PO_AFTER_CHANGE) && po instanceof MSession
				&& ((MSession) po).isProcessed())
		{
			DMSClipboard.removeSessionId(mSession.getAD_Session_ID());
		}
	}

	@Override
	protected void initialize()
	{
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSession.Table_Name);
	}
}
