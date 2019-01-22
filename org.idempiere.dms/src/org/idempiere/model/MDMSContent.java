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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.dms.factories.Utils;

public class MDMSContent extends X_DMS_Content
{

	/**
	 * 
	 */
	private static final long	serialVersionUID		= -6250555517481249806L;

	public static final String	SQL_FETCH_VERSION_LIST	= "SELECT DISTINCT DMS_Content_ID, seqno FROM DMS_Association a "
																+ " WHERE DMS_Content_Related_ID = ? AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType WHERE NAME='Version') "
																+ " UNION SELECT DMS_Content_ID, null FROM DMS_Content WHERE DMS_Content_ID = ? AND ContentBaseType <> 'DIR' ORDER BY DMS_Content_ID DESC";

	private String				seqNo					= null;

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

	public static List<I_DMS_Content> getVersionHistory(MDMSContent DMS_Content)
	{
		List<I_DMS_Content> contentList = new ArrayList<I_DMS_Content>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			MDMSAssociation dmsAssociation = Utils.getAssociationFromContent(DMS_Content.getDMS_Content_ID(), null);

			pstmt = DB.prepareStatement(SQL_FETCH_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE, null);
			pstmt.setInt(1, dmsAssociation.getDMS_Content_Related_ID());
			pstmt.setInt(2, dmsAssociation.getDMS_Content_Related_ID());

			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					MDMSContent content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
					// Set version number
					content.setSeqNo(rs.getString("seqno"));
					contentList.add(content);
				}
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Version list fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		return contentList;
	} // getVersionHistory

}
