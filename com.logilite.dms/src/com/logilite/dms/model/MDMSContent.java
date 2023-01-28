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

package com.logilite.dms.model;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MTable;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

import com.logilite.dms.constant.DMSConstant;

public class MDMSContent extends X_DMS_Content
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -6250555517481249806L;

	protected static CLogger	log					= CLogger.getCLogger(MDMSContent.class);

	//
	public MDMSContent(Properties ctx, int DMS_Content_ID, String trxName)
	{
		super(ctx, DMS_Content_ID, trxName);
	}

	public MDMSContent(Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}

	/**
	 * Create DMS Content
	 * 
	 * @param  name
	 * @param  contentBaseType
	 * @param  parentURL
	 * @param  isMounting
	 * @return                 DMS_Content_ID
	 */
	public static int create(String name, String contentBaseType, String parentURL, boolean isMounting)
	{
		return create(name, contentBaseType, parentURL, isMounting, null);
	} // create

	/**
	 * Create DMS Content
	 * 
	 * @param  name
	 * @param  contentBaseType
	 * @param  parentURL
	 * @param  isMounting
	 * @param  trxName
	 * @return                 DMS_Content_ID
	 */
	public static int create(String name, String contentBaseType, String parentURL, boolean isMounting, String trxName)
	{
		return create(name, contentBaseType, parentURL, null, null, 0, 0, isMounting, trxName);
	} // create

	/**
	 * Create DMS Content
	 * 
	 * @param  name
	 * @param  contentBaseType
	 * @param  parentURL
	 * @param  desc
	 * @param  media
	 * @param  contentTypeID
	 * @param  asiID
	 * @param  isMounting
	 * @param  trxName
	 * @return                 DMS_Content_ID
	 */
	public static int create(	String name, String contentBaseType, String parentURL, String desc, File file, int contentTypeID, int asiID, boolean isMounting,
								String trxName)
	{
		MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(0, trxName);
		content.setName(name);
		content.setDescription(desc);
		content.setParentURL(parentURL);
		content.setIsMounting(isMounting);
		content.setContentBaseType(contentBaseType);
		content.setDMS_MimeType_ID(MDMSMimeType.getMimeTypeID(file));
		if (asiID > 0)
			content.setM_AttributeSetInstance_ID(asiID);
		if (contentTypeID > 0)
			content.setDMS_ContentType_ID(contentTypeID);
		content.saveEx();

		log.log(Level.INFO, "New DMS_Content_ID = " + content.getDMS_Content_ID() + " for " + name + " as (" + contentBaseType + ") at " + parentURL);

		return content.getDMS_Content_ID();
	} // create

	/**
	 * Get DMS content from Content UU
	 * 
	 * @param  DMS_Content_UU
	 * @return                {@link I_DMS_Content}
	 */
	public static I_DMS_Content getContent(String DMS_Content_UU)
	{
		if (Util.isEmpty(DMS_Content_UU, true))
			return null;

		return (I_DMS_Content) new Query(Env.getCtx(), MDMSContent.Table_Name, " DMS_Content_UU ILIKE ? ", null).setParameters(DMS_Content_UU).first();
	} // getContent

	/**
	 * Get Content - AssociationType Value
	 * 
	 * @param  DMS_Content_ID
	 * @return                AssociationType Value
	 */
	public static String getContentAssociationType(int DMS_Content_ID)
	{
		String contentType = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_CONTENT_TYPE, null);
			pstmt.setInt(1, DMS_Content_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				contentType = rs.getString(1);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the content type for dms content: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return contentType;
	} // getContentAssociationType

	public String getToolTipTextMsg()
	{
		StringBuffer msg = new StringBuffer(getName());
		if (getDMS_ContentType_ID() > 0)
		{
			msg.append("\nContent Type: ").append(getDMS_ContentType().getName());
		}

		msg.append("\nItem Type: ");
		if (getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			msg.append(DMSConstant.DIRECTORY);
		}
		else
		{
			msg.append(getDMS_MimeType().getName());
		}

		msg.append("\nParent URL: ").append(getParentURL() == null ? "" : getParentURL());
		msg.append("\nCreated: ").append(DMSConstant.SDF.format(new Date(getCreated().getTime())));
		msg.append("\nUpdated: ").append(DMSConstant.SDF.format(new Date(getUpdated().getTime())));

		return msg.toString();
	} // getToolTipTextMsg

	/**
	 * get DMS_Content_Related_ID from DMS_content
	 * 
	 * @param  DMS_Content
	 * @return
	 */
	public int getDMS_Content_Related_ID()
	{
		if (getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
		{
			return getDMS_Content_ID();
		}
		else
		{
			MDMSAssociation DMSAssociation = MDMSAssociation.getParentAssociationFromContent(getDMS_Content_ID(), isActive(), null);

			if (DMSAssociation.getDMS_Content_Related_ID() > 0)
			{
				MDMSContent Content = new MDMSContent(Env.getCtx(), DMSAssociation.getDMS_Content_Related_ID(), null);
				if (Content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
					return DMSAssociation.getDMS_Content_ID();
				else
					return DMSAssociation.getDMS_Content_Related_ID();
			}
			else
			{
				return DMSAssociation.getDMS_Content_ID();
			}
		}
	} // getDMS_Content_Related_ID

	/**
	 * Get linkable Association and its Content related references
	 * 
	 * @param  isActiveonly
	 * @return              {@code Map<I_DMS_Association, I_DMS_Content>}
	 */
	public HashMap<I_DMS_Association, I_DMS_Content> getLinkableAssociationWithContentRelated(boolean isActiveonly)
	{
		HashMap<I_DMS_Association, I_DMS_Content> map = new LinkedHashMap<I_DMS_Association, I_DMS_Content>();

		String sql = "SELECT DMS_Association_ID, DMS_Content_Related_ID 	 FROM DMS_Association "
						+ " WHERE IsActive = ? AND DMS_Content_ID = ? AND DMS_AssociationType_ID = ? ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setString(1, isActiveonly ? "Y" : "N");
			pstmt.setInt(2, getDMS_Content_ID());
			pstmt.setInt(3, MDMSAssociationType.LINK_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				int contentRelatedID = rs.getInt("DMS_Content_Related_ID");
				MDMSAssociation linkableAssociation = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
				MDMSContent contentRelated = (contentRelatedID > 0 ? new MDMSContent(Env.getCtx(), contentRelatedID, null) : null);

				map.put(linkableAssociation, contentRelated);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Error while fetching list of linkable documents for content " + getName());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return map;
	} // getLinkableAssociationWithContentRelated

	public List<MDMSContent> getVersionRelatedContentList()
	{
		List<MDMSContent> dmsContent = new ArrayList<MDMSContent>();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_ANOTHER_VERSION_IDS, null);
			pstmt.setInt(1, getDMS_Content_ID());
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(rs.getInt(1), null);
				dmsContent.add(content);
			}
		}
		catch (SQLException e)
		{
			throw new AdempiereException("Something went wrong while fetching the content type for dms content: " + e, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return dmsContent;
	} // getVersionRelatedContentList

	public List<MDMSVersion> getAllVersions()
	{
		Query query = new Query(Env.getCtx(), MDMSVersion.Table_Name, "DMS_Content_ID = ?", get_TrxName());
		query.setClient_ID();
		query.setOnlyActiveRecords(true);
		query.setParameters(getDMS_Content_ID());
		query.setOrderBy(MDMSVersion.COLUMNNAME_SeqNo);
		List<MDMSVersion> versions = query.list();
		return versions;
	}

}
