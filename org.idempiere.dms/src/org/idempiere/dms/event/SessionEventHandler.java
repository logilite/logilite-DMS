/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

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
