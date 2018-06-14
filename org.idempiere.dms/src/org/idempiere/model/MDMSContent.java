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

public class MDMSContent extends X_DMS_Content
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6250555517481249806L;
	
	private String seqNo = null;

	public MDMSContent(Properties ctx, int DMS_Content_ID, String trxName)
	{
		super(ctx, DMS_Content_ID, trxName);
	}

	public MDMSContent(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}
	
	public String getSeqNo()
	{
		return seqNo;
	}

	public void setSeqNo(String seqNo)
	{
		this.seqNo = seqNo;
	}
}
