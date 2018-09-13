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

package org.idempiere.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;

public class MDMSMimeType extends X_DMS_MimeType
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3814987450064904684L;

	public MDMSMimeType(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	public MDMSMimeType(Properties ctx, int DMS_MimeType_ID, String trxName)
	{
		super(ctx, DMS_MimeType_ID, trxName);
	}

	public boolean beforeSave(boolean newRecord)
	{
		int count = DB.getSQLValue(null, "SELECT count(*) FROM DMS_MimeType WHERE UPPER(MimeType) = UPPER('" + getMimeType()
				+ "') OR UPPER(FileExtension) = UPPER ('" + getFileExtension() + "')");
		if (count != 0 && getDMS_MimeType_ID() == 0)
		{
			throw new AdempiereException("MimeType and File Extension must be unique.");
		}
		return true;
	}
}
