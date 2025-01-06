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

package com.logilite.dms.util;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.compiere.model.MTable;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSContentType;
import com.logilite.dms.model.MDMSVersion;

/**
 * Util for DMS related operations.
 * <ul>
 * Operation Like
 * <li>Create Directory
 * <li>Create File
 * <li>File Store and Thumbnail
 * <li>Paste Physical Copied Folder / Content
 * <li>Paste Copy FileContent / DirContent
 * <li>Paste Cut Content
 * <li>Rename Content / Directory
 * <li>Create Link
 * <li>Create Index for Linkable Content
 * <li>Delete Content [ AS In Active ]
 * <li>Delete Content with Physical Document
 * <li>etc...
 * </ul>
 * 
 * @author Sachin Bhimani
 */
public class DMSOprUtils
{

	public static void updateContentTypeAndAttribute(DMS dms, int contentID, String contentType, Map<String, String> attributeMap)
	{
		String trxName = Trx.createTrxName("DMSUpdateAttrs_");
		Trx trx = Trx.get(trxName, true);

		try
		{
			int contentTypeID = 0;
			int asiID = 0;

			MDMSContent content = (MDMSContent) MTable.get(Env.getCtx(), MDMSContent.Table_ID).getPO(contentID, trx.getTrxName());
			if (!Util.isEmpty(contentType, true))
			{
				contentTypeID = MDMSContentType.getContentTypeIDFromName(contentType, dms.AD_Client_ID);
				MDMSContentType cType = (MDMSContentType) MTable.get(Env.getCtx(), MDMSContentType.Table_ID).getPO(contentTypeID, trx.getTrxName());
				if (attributeMap != null && !attributeMap.isEmpty())
					asiID = Utils.createOrUpdateASI(attributeMap, content.getM_AttributeSetInstance_ID(), cType.getM_AttributeSet_ID(), trx.getTrxName());
			}

			content.setDMS_ContentType_ID(contentTypeID);
			content.setM_AttributeSetInstance_ID(asiID);
			content.saveEx();
		}
		catch (Exception e)
		{
			trx.rollback();
			throw new AdempiereException("Error while updating attributes : " + e.getLocalizedMessage(), e);
		}
		finally
		{
			if (trx != null)
			{
				trx.commit();
				trx.close();
			}
		}
	}

	/**
	 * Check Clipboard/Copied Document already exists in same position for Copy to CreateLink
	 * operation
	 * 
	 * @param  currContent   - Current DMS Content
	 * @param  copiedContent - Copied Content
	 * @param  isDir         - is Directory
	 * @return               True if Document exists in same level
	 */
	public static boolean isDocumentPresent(MDMSContent currContent, MDMSContent copiedContent, boolean isDir, String trxName)
	{
		String sql = "	SELECT COUNT(DMS_Content_ID) FROM DMS_Association WHERE DMS_Content_ID=? AND DMS_Content_Related_ID "
						+ (((currContent == null || currContent.getDMS_Content_ID() <= 0) && !isDir) ? "IS NULL" : " = " + currContent.getDMS_Content_ID());

		return DB.getSQLValue(trxName, sql.toString(), copiedContent.getDMS_Content_ID()) > 0 ? true : false;
	} // isDocumentPresent

	public static void setContentAndAssociation(MDMSContent content, MDMSAssociation association, boolean isActive, String trxName)
	{
		if (association != null)
		{
			association.setIsActive(isActive);
			association.saveEx(trxName);
		}

		if (content != null)
		{
			content.setIsActive(isActive);
			content.saveEx(trxName);

			// Update Version too
			DB.executeUpdate(	"UPDATE DMS_Version SET IsActive = ? WHERE DMS_Content_ID = ? ",
								new Object[] { (isActive ? "Y" : "N"), content.getDMS_Content_ID() }, true, trxName);
		}
	} // setContentAndAssociationInActive

	/**
	 * Delete Content With Physical Document
	 * 
	 * @param  dms
	 * @param  content
	 */
	public static void deleteContentWithPhysicalDocument(DMS dms, MDMSContent content)
	{
		for (MDMSVersion version : content.getAllVersions())
		{
			try
			{
				File document = dms.getFileFromStorage(version);
				if (document != null && document.exists())
					document.delete();

				String thumbnailsURL = dms.getThumbnailURL(version, null);
				if (!Util.isEmpty(thumbnailsURL, true))
				{
					File thumbnails = new File(thumbnailsURL);
					if (thumbnails != null && thumbnails.exists())
						FileUtils.deleteDirectory(thumbnails.getParentFile());
				}
			}
			catch (Exception e)
			{
				DMS.log.log(Level.WARNING, "Document or Thumbnail deletion related issue, Error: " + e.getLocalizedMessage(), e);
			}
		}

		int contentID = content.getDMS_Content_ID();

		if (content != null && MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()))
		{
			// Delete existing index
			dms.getIndexSearcher().deleteIndexByField(DMSConstant.DMS_CONTENT_ID, "" + contentID);
		}

		int no = DB.executeUpdate("DELETE FROM DMS_Association 	WHERE DMS_Content_ID = ?", contentID, content.get_TrxName());
		DMS.log.log(Level.INFO, no + " association(s) deleted for Content #" + contentID);

		no = DB.executeUpdate("DELETE FROM DMS_Version			WHERE DMS_Content_ID = ?", contentID, content.get_TrxName());
		DMS.log.log(Level.INFO, no + " version(s) deleted for Content #" + contentID);

		no = DB.executeUpdate("DELETE FROM DMS_Content 			WHERE DMS_Content_ID = ?", contentID, content.get_TrxName());
		DMS.log.log(Level.INFO, no + " content deleted for Content #" + contentID);
	} // deleteContentWithPhysicalDocument

	/**
	 * Get the related contents of give content like versions, Linkable docs etc
	 * 
	 * @param  dmsContent
	 * @param  trxName
	 * @param  isActive
	 * @return            Map of related contents
	 */
	public static HashMap<I_DMS_Content, I_DMS_Association> getRelatedContents(MDMSContent dmsContent, boolean isActive, String trxName)
	{
		HashMap<I_DMS_Content, I_DMS_Association> map = new LinkedHashMap<I_DMS_Content, I_DMS_Association>();

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(DMSConstant.SQL_GET_RELATED_CONTENT, trxName);
			pstmt.setInt(1, dmsContent.getDMS_Content_ID());
			pstmt.setString(2, isActive ? "Y" : "N");
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				map.put(new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), trxName),
						new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), trxName));
			}
		}
		catch (SQLException e)
		{
			DMS.log.log(Level.SEVERE, "getRelatedContents fetching failure: ", e);
			throw new AdempiereException("getRelatedContents fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return map;
	} // getRelatedContents

	/**
	 * Get Files from storage for all version of content
	 * 
	 * @param  dms     DMS
	 * @param  content - Content
	 * @return         Files
	 */
	public static File[] getFileFromStorageAllVersion(DMS dms, I_DMS_Content content)
	{
		ArrayList<File> files = new ArrayList<File>();
		for (MDMSVersion version : MDMSVersion.getVersionHistory(content))
		{
			File file = dms.getFileFromStorage(version);
			if (file != null)
				files.add(file);
		}
		return (File[]) files.toArray();
	} // getFileFromStorageAllVersion

	/**
	 * Permanently delete the physical document, its entire hierarchical structure, any linked
	 * references to other locations, and the associated indexing data.
	 * 
	 * @param dms         DMS Object
	 * @param association Association
	 */
	public static void deletePhysicalContentsAndHierarchy(DMS dms, MDMSAssociation association)
	{
		if (association == null)
			return;

		int associationID = association.get_ID();
		int contentID = association.getDMS_Content_ID();
		MDMSContent content = (MDMSContent) association.getDMS_Content();

		String infoMessage = "Table ID #"	+ association.getAD_Table_ID() + ", Record ID #" + association.getRecord_ID() + ", Content #" + contentID
								+ ", Association #" + associationID + ", Type [ " + content.getContentBaseType() + " ], Path: " + content.getParentURL()
								+ ", [ " + content.getName() + " ]" + ((MDMSAssociationType.isLink(association)) ? ", [ Linkable ]" : " ");
		// Check Link
		if (MDMSAssociationType.isLink(association))
		{
			dms.getIndexSearcher().deleteIndexByField(DMSConstant.DMS_ASSOCIATION_ID, "" + associationID);
			MDMSAssociation.removeAssociation(association);
		}

		// Check Directory type Content and delete it's child
		else if (MDMSContent.CONTENTBASETYPE_Directory.equalsIgnoreCase(content.getContentBaseType()))
		{
			// Checks the directory to ensure no content remains.
			List<MDMSAssociation> childAssociations = MDMSAssociation.getChildAssociationFromContent(contentID, association.get_TrxName());
			for (MDMSAssociation childAssociation : childAssociations)
			{
				deletePhysicalContentsAndHierarchy(dms, childAssociation);
			}

			// Delete a physical Document and Content related things
			deleteContentWithPhysicalDocument(dms, content);
		}
		else
		{
			// Delete a physical Document and Content related things
			deleteContentWithPhysicalDocument(dms, content);
		}

		DMS.log.log(Level.WARNING, infoMessage);
	} // deletePhysicalContentsAndHierarchy
}
