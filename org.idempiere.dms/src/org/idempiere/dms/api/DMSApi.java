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

package org.idempiere.dms.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.adempiere.exceptions.AdempiereException;
import org.apache.commons.io.FileUtils;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.WrongValueException;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

/**
 * DMS API SUPPORT
 * 
 * @author Sachin
 */
public class DMSApi implements I_DMS_Api
{

	private static final String	SQL_GET_ASSOCIATION_SEQ_NO	= "SELECT COALESCE(MAX(seqNo), 0) + 1  FROM DMS_Association WHERE DMS_Content_Related_ID = ? AND AD_Client_ID = ?";

	public IFileStorageProvider	thumbnailStorageProvider	= null;
	public IFileStorageProvider	fileStorageProvider			= null;
	public IThumbnailProvider	thumbnailProvider			= null;
	public IMountingStrategy	mountingStrategy			= null;
	public IContentManager		contentManager				= null;
	public IIndexSearcher		indexSeracher				= null;

	/**
	 * Constructor for initialize provider
	 */
	public DMSApi()
	{
		fileStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorageProvider == null)
			throw new AdempiereException("Storage provider is not found.");

		thumbnailStorageProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), true);

		if (thumbnailStorageProvider == null)
			throw new AdempiereException("Thumbnail Storage provider is not found.");

		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
			throw new AdempiereException("Index server is not found.");

		mountingStrategy = Utils.getMountingStrategy(null);
	}

	/*
	 * Adding files and File Version
	 */

	@Override
	public boolean addFile(File file)
	{
		return addFile(null, file);
	}

	@Override
	public boolean addFile(String dirPath, File file)
	{
		return addFile(dirPath, file, file.getName());
	}

	@Override
	public boolean addFile(String dirPath, File file, String fileName)
	{
		return addFile(dirPath, file, fileName, 0, 0);
	}

	@Override
	public boolean addFile(String dirPath, File file, String fileName, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, null, null, AD_Table_ID, Record_ID);
	}

	@Override
	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap)
	{
		return addFile(dirPath, file, fileName, contentType, attributeMap, 0, 0);
	}

	@Override
	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID)
	{
		return addFile(dirPath, file, fileName, contentType, attributeMap, AD_Table_ID, Record_ID, false);
	}

	// @Override
	// public boolean addFileVersion(String dirPath, File file)
	// {
	// return addFileVersion(dirPath, file, file.getName());
	// }
	//
	// @Override
	// public boolean addFileVersion(String dirPath, File file, String fileName)
	// {
	// return addFileVersion(dirPath, file, fileName, 0, 0);
	// }
	//
	// @Override
	// public boolean addFileVersion(String dirPath, File file, String fileName,
	// int AD_Table_ID, int Record_ID)
	// {
	// return addFile(dirPath, file, fileName, null, null, AD_Table_ID,
	// Record_ID, true);
	// }

	@Override
	public boolean addFileVersion(int DMS_Content_ID, File file)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * Create File, if directory not exists then create
	 * 
	 * @param dirPath
	 * @param file
	 * @param fileName
	 * @param contentType
	 * @param attributeMap
	 * @param AD_Table_ID
	 * @param Record_ID
	 * @param isVersion
	 * @return TRUE if success
	 */
	public boolean addFile(String dirPath, File file, String fileName, String contentType, Map<String, String> attributeMap, int AD_Table_ID, int Record_ID,
			boolean isVersion)
	{
		int asiID = 0;
		int seqNo = 0;
		int contentTypeID = 0;
		int DMS_Content_Related_ID = 0;
		int DMS_AssociationType_ID = 0;

		String desc = null;
		String name = null;
		String parentURL = null;

		Trx trx = null;
		AMedia media = null;
		MDMSContent addedContent = null;

		MDMSContent dirContent = mountingStrategy.getMountingParent(AD_Table_ID, Record_ID);

		if (!isVersion)
		{
			String validationResponse = Utils.isValidFileName(fileName);
			if (validationResponse != null)
				throw new WrongValueException(Msg.translate(Env.getCtx(), validationResponse));
		}

		try
		{
			media = new AMedia(file.getName(), null, null, FileUtils.readFileToByteArray(file));
		}
		catch (IOException e)
		{
			throw new AdempiereException("Issue while creating Media file.", e);
		}

		if (isVersion)
		{
			if (Utils.getMimeTypeID(media) != dirContent.getDMS_MimeType_ID())
				throw new WrongValueException("Mime type not matched, please upload same mime type version document.");
		}

		String trxName = Trx.createTrxName("AddFiles");
		trx = Trx.get(trxName, true);

		if (!Util.isEmpty(dirPath, true))
		{
			// TODO Need implement dir creation with recursive logic
			dirContent = Utils.createDirectory(dirPath, dirContent, AD_Table_ID, Record_ID, fileStorageProvider, contentManager, false, trx.getTrxName());
		}

		try
		{
			if (!isVersion)
			{
				String format = media.getFormat();
				if (format == null)
					throw new AdempiereException("Invalid File format");

				if (!fileName.endsWith(format))
					name = fileName + "." + format;
				else
					name = fileName;

				if (contentType != null)
				{
					// TODO Need to Test
					contentTypeID = DB.getSQLValue(null, "SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE Value = ? AND AD_Client_ID = ?", contentType,
							Env.getAD_Client_ID(Env.getCtx()));

					MDMSContentType cType = new MDMSContentType(Env.getCtx(), contentTypeID, trx.getTrxName());
					asiID = Utils.createASI(attributeMap, cType.getM_AttributeSet_ID(), trx.getTrxName());
				}
				parentURL = contentManager.getPath(dirContent);
			}
			else
			{
				name = dirContent.getName();
				parentURL = dirContent.getParentURL();
				asiID = dirContent.getM_AttributeSetInstance_ID();
				contentTypeID = dirContent.getDMS_ContentType_ID();
			}

			// Create Content
			int DMS_Content_ID = Utils.createDMSContent(name, name, MDMSContent.CONTENTBASETYPE_Content, parentURL, desc, media, contentTypeID, asiID, false,
					trx.getTrxName());
			addedContent = new MDMSContent(Env.getCtx(), DMS_Content_ID, trx.getTrxName());

			if (isVersion)
			{
				// TODO Need to check
				DMS_Content_Related_ID = Utils.getDMS_Content_Related_ID(dirContent);
				DMS_AssociationType_ID = MDMSAssociationType.getVersionType(false);

				seqNo = DB.getSQLValue(null, SQL_GET_ASSOCIATION_SEQ_NO, Utils.getDMS_Content_Related_ID(dirContent), Env.getAD_Client_ID(Env.getCtx()));
			}
			else
			{
				if (dirContent != null)
					DMS_Content_Related_ID = dirContent.getDMS_Content_ID();
				DMS_AssociationType_ID = MDMSAssociationType.getVersionType(true);

				// Display an error when trying to upload same name file
				String path = fileStorageProvider.getBaseDirectory(contentManager.getPath(addedContent));
				File fileCheck = new File(path);
				if (fileCheck.exists())
				{
					throw new WrongValueException(
							"File already exists, either rename or upload as a version. \n (Either same file name content exist in inActive mode)");
				}
			}

			// Create Association
			Utils.createAssociation(DMS_Content_ID, DMS_Content_Related_ID, Record_ID, AD_Table_ID, DMS_AssociationType_ID, seqNo, trx.getTrxName());

			// File write on Storage provider and create thumbnail
			Utils.writeFileOnStorageAndThumnail(fileStorageProvider, contentManager, media, addedContent);

			// Transaction commit
			trx.commit();
		}
		catch (Exception e)
		{
			if (trx != null)
				trx.rollback();
			throw new AdempiereException("Upload Content Failure :" + e);
		}
		finally
		{
			if (trx != null)
				trx.close();
		}

		return true;
	} // addFile

	/*
	 * Select Content
	 */

	@Override
	public MDMSContent[] selectContent(String dirPath)
	{
		return null;
	} // selectContent

	@Override
	public MDMSContent[] selectContent(String dirPath, int AD_Table_ID, int Record_ID)
	{
		return null;
	}

}
