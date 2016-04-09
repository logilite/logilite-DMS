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

package com.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MIndexingConfig extends X_LTX_Indexing_Conf
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 396330494436535133L;

	public MIndexingConfig(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MIndexingConfig(Properties ctx, int LTX_Indexing_Conf_ID, String trxName)
	{
		super(ctx, LTX_Indexing_Conf_ID, trxName);
	}

	public String getIndexServerUrl()
	{
		String url = getURL();
		String core = getLTX_Indexing_Core();

		if (url.substring(url.length() - 1).equals("/"))
			return url + core;
		else
			return url + "/" + core;
	}

}