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

package org.idempiere.dms.storage;

import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.model.I_DMS_Content;

public class RelationalContentManager implements IContentManager
{
	public static final String	KEY	= "REL";

	public static CLogger		log	= CLogger.getCLogger(RelationalContentManager.class);

	@Override
	public String getPath(I_DMS_Content content)
	{
		String path = "";

		if (content != null && content.getDMS_Content_ID() > 0)
		{
			if (!Util.isEmpty(content.getParentURL(), true))
				path = content.getParentURL() + DMSConstant.FILE_SEPARATOR + content.getName();
			else if (!Util.isEmpty(content.getName(), true))
				path = DMSConstant.FILE_SEPARATOR + content.getName();
		}

		return path;
	}

}
