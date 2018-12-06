package com.logilite.model;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.IArchiveStore;
import org.compiere.model.MArchive;
import org.compiere.model.MStorageProvider;
import org.compiere.model.MTable;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IMountingStrategy;
import org.idempiere.dms.factories.IThumbnailGenerator;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSMimeType;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.util.media.AMedia;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class ArchiveDMS implements IArchiveStore
{
	private IFileStorageProvider			fileStorgProvider			= null;
	private IContentManager					contentManager				= null;
	private IIndexSearcher					indexSeracher				= null;
	private IThumbnailGenerator				thumbnailGenerator			= null;
	private String							DMS_ARCHIVE_CONTENT_TYPE	= "ArchiveDocument";
	private static CCache<String, Integer>	cTypeCache					= new CCache<String, Integer>("ArchiveCache",
			100);
	private static final CLogger			log							= CLogger.getCLogger(ArchiveDMS.class);

	@Override
	public byte[] loadLOBData(MArchive archive, MStorageProvider prov)
	{
		return null;
	}

	@Override
	public void save(MArchive archive, MStorageProvider prov, byte[] inflatedData)
	{
		try
		{
			fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);
			if (fileStorgProvider == null)
				throw new AdempiereException("Storage provider is not define on clientInfo.");

			contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));
			if (contentManager == null)
				throw new AdempiereException("Content manager is not found.");

			indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));
			if (indexSeracher == null)
				throw new AdempiereException("Solr Index server is not found.");

			int cTypeID = getContentTypeID(DMS_ARCHIVE_CONTENT_TYPE, 0);

			Integer tableID = archive.getAD_Table_ID();
			String tableName = MTable.getTableName(Env.getCtx(), tableID);
			int recordID = archive.getRecord_ID();

			// Generate Mounting Parent
			Utils.initiateMountingContent(tableName, recordID, tableID);
			IMountingStrategy mountingStrategy = Utils.getMountingStrategy(tableName);
			MDMSContent mountingParent = mountingStrategy.getMountingParent(tableName, recordID);

			// Generate File
			File file = generateFile(archive, inflatedData);

			// Create DMS Content
			MDMSContent dmsContent = new MDMSContent(Env.getCtx(), 0, archive.get_TrxName());
			dmsContent.setName(file.getName());
			dmsContent.setValue(file.getName());
			dmsContent.setDMS_MimeType_ID(Utils.getMimeTypeID(new AMedia(file, "application/pdf", null)));
			dmsContent.setParentURL(contentManager.getPath(mountingParent));
			dmsContent.setContentBaseType(X_DMS_Content.CONTENTBASETYPE_Content);
			dmsContent.setIsMounting(true);
			dmsContent.setDMS_FileSize(Utils.readableFileSize(file.length()));
			if (cTypeID > 0)
				dmsContent.setDMS_ContentType_ID(cTypeID);
			dmsContent.saveEx();

			// Create DMS Association
			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), 0, archive.get_TrxName());
			dmsAssociation.setDMS_Content_ID(dmsContent.getDMS_Content_ID());
			dmsAssociation.setAD_Table_ID(tableID);
			dmsAssociation.setRecord_ID(recordID);
			dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType(true));
			dmsAssociation.setDMS_Content_Related_ID(mountingParent.getDMS_Content_ID());
			dmsAssociation.saveEx();

			// Upload file to DMS
			fileStorgProvider.writeBLOB(fileStorgProvider.getBaseDirectory(contentManager.getPath(dmsContent)),
					inflatedData, dmsContent);

			MDMSMimeType mimeType = new MDMSMimeType(Env.getCtx(), dmsContent.getDMS_MimeType_ID(),
					archive.get_TrxName());

			// Generate Thumbnail Image
			thumbnailGenerator = Utils.getThumbnailGenerator(mimeType.getMimeType());
			if (thumbnailGenerator != null)
				thumbnailGenerator.addThumbnail(dmsContent, file, null);

			try
			{
				Map<String, Object> solrValue = Utils.createIndexMap(dmsContent, dmsAssociation);
				indexSeracher.indexContent(solrValue);
			}
			catch (Exception e)
			{
				file = new File(fileStorgProvider.getBaseDirectory(contentManager.getPath(dmsContent)));
				if (file.exists())
					file.delete();
				throw new AdempiereException("Indexing of Content Failure :" + e);
			}

			archive.setByteData("Testing".getBytes());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public File generateFile(MArchive archive, byte[] inflatedData) throws Exception
	{
		String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
		String archiveName = archive.getName();
		int recordID = archive.getRecord_ID();
		if (Util.isEmpty(archiveName))
		{
			archiveName = "";
			if (recordID > 0)
				archiveName += recordID;
		}
		else
		{
			archiveName = archiveName.trim();
			archiveName = archiveName.replaceAll(" ", "");
			if (recordID > 0)
				archiveName = recordID + " " + archiveName;
		}

		String fileName = archiveName + "_" + timeStamp + ".pdf";
		File file = new File(fileName);
		FileOutputStream fileOuputStream = null;
		try
		{
			fileOuputStream = new FileOutputStream(file);
			fileOuputStream.write(inflatedData);
		}
		finally
		{
			fileOuputStream.flush();
			fileOuputStream.close();
		}
		return file;
	}

	public static int getContentTypeID(String contentType, int clientID)
	{
		Integer cTypeID = cTypeCache.get(contentType);
		if (cTypeID == null || cTypeID <= 0)
		{
			cTypeID = DB.getSQLValue(null,
					"SELECT DMS_ContentType_ID FROM DMS_ContentType WHERE IsActive='Y' AND Value = ? AND AD_Client_ID = ?",
					contentType, clientID);
			if (cTypeID > 0)
				cTypeCache.put(contentType, cTypeID);
		}
		return cTypeID;
	}

	@Override
	public boolean deleteArchive(MArchive archive, MStorageProvider prov)
	{
		return true;
	}

}
