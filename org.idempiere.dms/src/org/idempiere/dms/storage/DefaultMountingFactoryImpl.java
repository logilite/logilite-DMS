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

import org.compiere.model.MSysConfig;
import org.compiere.model.PO;
import org.idempiere.dms.factories.IMounting;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.X_DMS_Content;

public class DefaultMountingFactoryImpl implements IMounting
{
	private final static String	DMS_MOUNTING_BASE	= MSysConfig.getValue("DMS_MOUNTING_BASE", "Attachment");
	private String				fileSeprator		= Utils.getStorageProviderFileSeparator();

	@Override
	public String getMountingStrategy(String Table_Name, int Record_ID)
	{
		return fileSeprator + DMS_MOUNTING_BASE + fileSeprator + Table_Name + fileSeprator + Record_ID;
	}

	@Override
	public String getMountingStrategy(PO po)
	{
		return fileSeprator + DMS_MOUNTING_BASE + fileSeprator + po.get_TableName() + fileSeprator + po.get_ID();
	}

	@Override
	public String getMountingStrategy(String path)
	{
		return fileSeprator + DMS_MOUNTING_BASE + fileSeprator + path;
	}

	@Override
	public String getMountingStrategy(X_DMS_Content DMSContent)
	{
		return null;
	}
}
