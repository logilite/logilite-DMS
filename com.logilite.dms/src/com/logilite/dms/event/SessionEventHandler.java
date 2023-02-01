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

package com.logilite.dms.event;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.MSession;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.osgi.service.event.Event;

import com.logilite.dms.DMS_ZK_Util;
import com.logilite.dms.factories.DMSClipboard;

public class SessionEventHandler extends AbstractEventHandler
{

	@Override
	protected void doHandleEvent(Event event)
	{
		PO po = getPO(event);
		MSession mSession = (MSession) po;
		String webSession = mSession.getWebSession();

		if (event.getTopic().equals(IEventTopics.PO_AFTER_CHANGE) && po instanceof MSession && ((MSession) po).isProcessed())
		{
			DMSClipboard.removeSessionId(mSession.getAD_Session_ID());
		}
		else if (SessionManager.isUserLoggedIn(Env.getCtx()) && (Util.isEmpty(webSession) || !(webSession.equals("Server") || webSession.equals("WebService"))))
		{
			/*
			 * When user login to the account then load the CSS file for DMS
			 */

			// Load DMS CSS file content and attach as style tag in Head tab
			DMS_ZK_Util.loadDMSThemeCSSFile();
			DMS_ZK_Util.loadDMSMobileCSSFile();
		}
	}

	@Override
	protected void initialize()
	{
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MSession.Table_Name);
	}
}
