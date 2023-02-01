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

package com.logilite.dms.factories;

import java.util.HashMap;
import java.util.Map;

import org.compiere.model.MSession;
import org.compiere.util.Env;

import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSContent;

public class DMSClipboard
{

	public static Map<Integer, I_DMS_Content>	sessionClip	= new HashMap<Integer, I_DMS_Content>();
	public static DMSClip						dmsClip		= new DMSClip();

	public static void put(MDMSContent content, boolean isCopy)
	{

		MSession mSession = MSession.get(Env.getCtx(), false);

		if (content != null && mSession != null)
		{
			sessionClip.put(mSession.getAD_Session_ID(), content);
			dmsClip.setCopy(isCopy);
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

	public static boolean getIsCopy()
	{
		MSession mSession = MSession.get(Env.getCtx(), false);

		if (mSession != null)
		{
			return dmsClip.isCopy();
		}
		return false;
	}

}
