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

package org.idempiere.dms.factories;

import java.util.HashMap;
import java.util.Map;

import org.compiere.model.MSession;
import org.compiere.util.Env;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;

public class DMSClipboard
{

	public static Map<Integer, I_DMS_Content>	sessionClip		= new HashMap<Integer, I_DMS_Content>();

	public static boolean						isViewerCopy	= false;

	public static void put(MDMSContent content)
	{

		MSession mSession = MSession.get(Env.getCtx(), false);

		if (content != null && mSession != null)
		{
			sessionClip.put(mSession.getAD_Session_ID(), content);
		}
	}

	public static MDMSContent get()
	{
		MSession mSession = MSession.get(Env.getCtx(), false);

		if (mSession != null)
		{
			return (MDMSContent) sessionClip.get(mSession.getAD_Session_ID());
		}
		return null;
	}

	public static void removeSessionId(int ad_Session_ID)
	{
		sessionClip.remove(ad_Session_ID);

	}

	public static boolean getIsViewerCopy()
	{
		return isViewerCopy;
	}

	public static void setViewerCopy(boolean isViewerCopy)
	{
		DMSClipboard.isViewerCopy = isViewerCopy;
	}

}
