package com.logilite.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.IArchiveStore;
import org.compiere.model.MArchive;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MStorageProvider;
import org.compiere.model.MTable;
import org.compiere.model.PO;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.zkoss.util.media.AMedia;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;
import com.sun.corba.se.impl.orbutil.graph.Node;

public class ArchiveDMS implements IArchiveStore
{
	private IFileStorageProvider			fileStorgProvider				= null;
	private IContentManager					contentManager					= null;
	private IIndexSearcher					indexSeracher					= null;
	private IThumbnailGenerator				thumbnailGenerator				= null;
	private String							DMS_ARCHIVE_CONTENT_TYPE		= "ArchiveDocument";
	private String							DMS_ATTRIBUTE_BUSINESS_PARTNER	= "C_BPartner_ID";
	private String							DMS_ATTRIBUTE_DOCUMENT_TYPE		= "C_DocType_ID";
	private String							DMS_ATTRIBUTE_SET_NAME			= "Archive Document";
	private String							DMS_ATTRIBUTE_DOCUMENT_STATUS	= "DocStatus";
	private static CCache<String, Integer>	cTypeCache						= new CCache<String, Integer>(
			"ArchiveCache", 100);
	private static final CLogger			log								= CLogger.getCLogger(ArchiveDMS.class);

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

			if (Util.isEmpty(tableName))
				tableName = "";
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

			// Create Attributes
			addAttributes(tableID, recordID, dmsContent);

			// Create DMS Association
			MDMSAssociation dmsAssociation = new MDMSAssociation(Env.getCtx(), 0, archive.get_TrxName());
			dmsAssociation.setDMS_Content_ID(dmsContent.getDMS_Content_ID());
			dmsAssociation.setAD_Table_ID(tableID);
			dmsAssociation.setRecord_ID(recordID);
			dmsAssociation.setDMS_AssociationType_ID(MDMSAssociationType.getVersionType(true));
			if (mountingParent != null)
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

			archive.setByteData(generateEntry(dmsContent, dmsAssociation));
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public byte[] generateEntry(MDMSContent dmsContent, MDMSAssociation dmsAssociation)
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try
		{
			// create xml entry
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document document = builder.newDocument();
			final Element root = document.createElement("archive");
			document.appendChild(root);
			document.setXmlStandalone(true);
			Element content = document.createElement("DMS_Content_ID");
			content.appendChild(document.createTextNode(String.valueOf(dmsContent.get_ID())));
			root.appendChild(content);
			
			Element association = document.createElement("DMS_Association_ID");
			association.appendChild(document.createTextNode(String.valueOf(dmsAssociation.get_ID())));
			root.appendChild(association);
			
			final Source source = new DOMSource(document);
			final ByteArrayOutputStream bos = new ByteArrayOutputStream();
			final Result result = new StreamResult(bos);
			final Transformer xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(source, result);
			final byte[] xmlData = bos.toByteArray();
			return xmlData;
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getMessage());
			return "Error Message".getBytes();
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
			if (file != null)
			{
				fileOuputStream.flush();
				fileOuputStream.close();
			}
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

	public void addAttributes(int tableID, int recordID, MDMSContent dmsContent)
	{
		if (tableID > 0 && recordID > 0 && dmsContent != null)
		{
			PO po = MTable.get(Env.getCtx(), tableID).getPO(recordID, null);
			boolean attributeFlag = false;

			int attributeSetID = getAttributeSetID(DMS_ATTRIBUTE_SET_NAME);
			int bpartnerAttributeID = getAttributeID(DMS_ATTRIBUTE_BUSINESS_PARTNER);
			int docStatusAttributeID = getAttributeID(DMS_ATTRIBUTE_DOCUMENT_STATUS);
			int docTypeAttributeID = getAttributeID(DMS_ATTRIBUTE_DOCUMENT_TYPE);

			int bPartnerValue = 0;
			String docStatusValue = "";
			int docTypeValue = 0;

			if (bpartnerAttributeID >= 0)
			{
				bPartnerValue = po.get_ValueAsInt(DMS_ATTRIBUTE_BUSINESS_PARTNER);
				if (bPartnerValue > 0)
					attributeFlag = true;
			}

			if (docStatusAttributeID >= 0)
			{
				docStatusValue = po.get_ValueAsString(DMS_ATTRIBUTE_DOCUMENT_STATUS);
				if (!Util.isEmpty(docStatusValue))
					attributeFlag = true;
			}

			if (docTypeAttributeID >= 0)
			{
				docTypeValue = po.get_ValueAsInt(DMS_ATTRIBUTE_DOCUMENT_TYPE);
				if (docTypeValue > 0)
					attributeFlag = true;
			}

			if (attributeFlag)
			{
				MAttributeSetInstance asi = new MAttributeSetInstance(Env.getCtx(), 0, attributeSetID, null);
				asi.save();

				MAttributeInstance attributeInstance = null;
				if (bPartnerValue > 0)
				{
					attributeInstance = new MAttributeInstance(Env.getCtx(), bpartnerAttributeID, asi.get_ID(),
							bPartnerValue, null);
					attributeInstance.save();
				}

				if (docTypeValue > 0)
				{
					attributeInstance = new MAttributeInstance(Env.getCtx(), docTypeAttributeID, asi.get_ID(),
							docTypeValue, null);
					attributeInstance.save();
				}

				dmsContent.setM_AttributeSetInstance_ID(asi.get_ID());
				dmsContent.save();
			}
		}
	}

	@Override
	public boolean deleteArchive(MArchive archive, MStorageProvider prov)
	{
		return true;
	}

	public int getAttributeID(String AttributeName)
	{
		Integer attributeID = cTypeCache.get(AttributeName);
		if (attributeID == null || attributeID <= 0)
		{
			String sql = "SELECT M_Attribute_ID FROM M_Attribute WHERE IsActive='Y' AND Name = ? AND AD_Client_ID = 0";
			attributeID = DB.getSQLValue(null, sql, AttributeName);
			if (attributeID > 0)
				cTypeCache.put(AttributeName, attributeID);
		}
		return attributeID;
	}

	public int getAttributeSetID(String AttributeSetName)
	{
		Integer attributeSetID = cTypeCache.get(AttributeSetName);
		if (attributeSetID == null || attributeSetID <= 0)
		{
			String sql = "SELECT M_AttributeSet_ID FROM M_AttributeSet WHERE IsActive='Y' AND Name = ? AND AD_Client_ID = 0";
			attributeSetID = DB.getSQLValue(null, sql, AttributeSetName);
			if (attributeSetID > 0)
				cTypeCache.put(AttributeSetName, attributeSetID);
		}
		return attributeSetID;
	}

}
