package com.logilite.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.util.DMSOprUtils;
import org.idempiere.dms.util.Utils;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSAssociationType;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.MDMSContentType;
import org.idempiere.model.MDMSVersion;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ArchiveDMS implements IArchiveStore
{

	private static final CLogger			log								= CLogger.getCLogger(ArchiveDMS.class);

	private static final String				DMS_ARCHIVE_CONTENT_TYPE		= "ArchiveDocument";
	private static final String				DMS_ATTRIBUTE_BUSINESS_PARTNER	= "C_BPartner_ID";
	private static final String				DMS_ATTRIBUTE_DOCUMENT_TYPE		= "C_DocType_ID";
	private static final String				DMS_ATTRIBUTE_SET_NAME			= "Archive Document";
	private static final String				DMS_ATTRIBUTE_DOCUMENT_STATUS	= "DocStatus";
	private static final String				DMS_ATTRIBUTE_PROCESS			= "AD_Process_ID";
	private static final String				DMS_ATTRIBUTE_CREATED_DATE		= "Created";

	private static CCache<String, Integer>	cache_AttributeName				= new CCache<String, Integer>("cache_AttributeName", 5);
	private static CCache<String, Integer>	cache_AttributeSetName			= new CCache<String, Integer>("cache_AttributeSetName", 5);
	private static CCache<String, Integer>	cache_AttributeValue			= new CCache<String, Integer>("cache_AttributeValue", 5);

	private DMS								dms;

	/**
	 * Constructor
	 */
	public ArchiveDMS()
	{
		super();
		dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));
	}

	@Override
	public byte[] loadLOBData(MArchive archive, MStorageProvider prov)
	{
		byte[] data = archive.getByteData();
		if (data == null)
			return null;

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		try
		{
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document document = builder.parse(new ByteArrayInputStream(data));
			final NodeList entries = document.getElementsByTagName("archive");
			if (entries.getLength() != 1)
			{
				log.severe("no archive entry found");
				return null;
			}

			final Node archiveNode = entries.item(0);
			NodeList list = archiveNode.getChildNodes();
			if (list.getLength() > 0)
			{
				Node dmsContentNode = list.item(0);
				if (dmsContentNode != null)
				{
					String dmsContentID = dmsContentNode.getFirstChild().getNodeValue();
					if (!Util.isEmpty(dmsContentID))
					{
						int contentID = 0;
						try
						{
							contentID = Integer.parseInt(dmsContentID);
						}
						catch (Exception e)
						{
							return null;
						}

						MDMSContent mdmsContent = new MDMSContent(Env.getCtx(), contentID, null);
						File file = dms.getFileFromStorage(MDMSVersion.getLatestVersion(mdmsContent));
						if (file.exists())
						{
							// read files into byte[]
							final byte[] dataEntry = new byte[(int) file.length()];
							try
							{
								final FileInputStream fileInputStream = new FileInputStream(file);
								fileInputStream.read(dataEntry);
								fileInputStream.close();
							}
							catch (FileNotFoundException e)
							{
								log.log(Level.SEVERE, "File Not Found. " + e.getLocalizedMessage(), e);
							}
							catch (IOException e)
							{
								log.log(Level.SEVERE, "Error Reading The File. " + e.getLocalizedMessage(), e);
							}
							return dataEntry;
						}
						else
						{
							log.log(Level.SEVERE, "File does not exists on: " + file.getAbsolutePath());
							return null;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void save(MArchive archive, MStorageProvider prov, byte[] inflatedData)
	{
		try
		{
			int cTypeID = MDMSContentType.getContentTypeIDFromName(DMS_ARCHIVE_CONTENT_TYPE, 0);

			Integer tableID = archive.getAD_Table_ID();
			String tableName = MTable.getTableName(Env.getCtx(), tableID);
			int recordID = archive.getRecord_ID();

			MDMSContent mountingParent = null;
			if (Util.isEmpty(tableName) || recordID <= 0)
			{
				if (tableName == null)
					tableName = "";
				// Generate Mounting Parent
				dms.initMountingStrategy(tableName);
				dms.initiateMountingContent(Utils.getDMSMountingArchiveBase(dms.AD_Client_ID), tableName, recordID, tableID);
				mountingParent = dms.getMountingStrategy().getMountingParentForArchive();
			}
			else
			{
				// Generate Mounting Parent
				dms.initMountingStrategy(tableName);
				dms.initiateMountingContent(tableName, recordID, tableID);
				mountingParent = dms.getRootMountingContent(tableID, recordID);
			}
			// Generate File
			File file = generateFile(archive, inflatedData);

			// trx creation
			String trxName = Trx.createTrxName("DMSArchive_");
			Trx trx = Trx.get(trxName, true);

			try
			{
				// Create DMS Content
				int contentID = dms.createDMSContent(	file.getName(), MDMSContent.CONTENTBASETYPE_Content, dms.getPathFromContentManager(mountingParent), null,
														file, cTypeID, 0, true, trxName);

				// Create Attributes
				MDMSContent content = new MDMSContent(Env.getCtx(), contentID, trxName);
				addAttributes(tableID, recordID, content, archive);

				// Create DMS Association
				int contentRelatedID = 0;
				if (mountingParent != null)
					contentRelatedID = mountingParent.getDMS_Content_ID();
				int associationID = dms.createAssociation(contentID, contentRelatedID, recordID, tableID, MDMSAssociationType.PARENT_ID, trxName);

				// Create DMS Version record
				MDMSVersion version = dms.createVersion(file.getName(), contentID, 0, file, trxName);

				// File write on Storage provider and Generate Thumbnail Image
				DMSOprUtils.writeFileOnStorageAndThumnail(dms, file, version);

				archive.setByteData(generateEntry(content, new MDMSAssociation(Env.getCtx(), associationID, trxName)));

				trx.commit();
			}
			catch (Exception e)
			{
				if (trx != null)
					trx.rollback();
				log.log(Level.SEVERE, "Upload Content Failure :", e);
				throw new AdempiereException("Upload Content Failure :" + e);
			}
			finally
			{
				// Close transaction - "DMSArchive_"
				if (trx != null)
					trx.close();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	} // save

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
	} // generateEntry

	public File generateFile(MArchive archive, byte[] inflatedData) throws Exception
	{
		String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
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
			archiveName = archiveName.replace(DMSConstant.FILE_SEPARATOR, "_");
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
	} // generateFile

	public void addAttributes(int tableID, int recordID, MDMSContent dmsContent, MArchive archive)
	{
		if (dmsContent != null)
		{
			PO po = null;
			if (tableID > 0 && recordID > 0)
				po = MTable.get(Env.getCtx(), tableID).getPO(recordID, null);

			int attributeSetID = getAttributeSetID(DMS_ATTRIBUTE_SET_NAME);
			int bpartnerAttributeID = getAttributeID(DMS_ATTRIBUTE_BUSINESS_PARTNER);
			int docStatusAttributeID = getAttributeID(DMS_ATTRIBUTE_DOCUMENT_STATUS);
			int docTypeAttributeID = getAttributeID(DMS_ATTRIBUTE_DOCUMENT_TYPE);
			int createdAttributeID = getAttributeID(DMS_ATTRIBUTE_CREATED_DATE);
			int processAttributeID = getAttributeID(DMS_ATTRIBUTE_PROCESS);

			int bPartnerValue = 0;
			String docStatusValue = "";
			int docTypeValue = 0;

			if (po != null)
			{
				if (bpartnerAttributeID >= 0)
					bPartnerValue = po.get_ValueAsInt(DMS_ATTRIBUTE_BUSINESS_PARTNER);

				if (docStatusAttributeID >= 0)
				{
					docStatusValue = po.get_ValueAsString(DMS_ATTRIBUTE_DOCUMENT_STATUS);
				}

				if (docTypeAttributeID >= 0)
					docTypeValue = po.get_ValueAsInt(DMS_ATTRIBUTE_DOCUMENT_TYPE);
			}

			MAttributeSetInstance asi = new MAttributeSetInstance(Env.getCtx(), 0, attributeSetID, null);
			asi.save();

			MAttributeInstance attributeInstance = null;
			if (bPartnerValue > 0)
			{
				attributeInstance = new MAttributeInstance(Env.getCtx(), bpartnerAttributeID, asi.get_ID(), bPartnerValue, null);
				attributeInstance.save();
			}

			if (docTypeValue > 0)
			{
				attributeInstance = new MAttributeInstance(Env.getCtx(), docTypeAttributeID, asi.get_ID(), docTypeValue, null);
				attributeInstance.save();
			}

			if (!Util.isEmpty(docStatusValue))
			{
				attributeInstance = new MAttributeInstance(Env.getCtx(), docStatusAttributeID, asi.get_ID(), docStatusValue, null);
				attributeInstance.save();
			}

			if (createdAttributeID > 0)
			{
				attributeInstance = new MAttributeInstance(Env.getCtx(), createdAttributeID, asi.get_ID(), archive.getCreated(), null);
				attributeInstance.save();
			}

			if (processAttributeID > 0 && archive.getAD_Process_ID() > 0)
			{
				attributeInstance = new MAttributeInstance(Env.getCtx(), processAttributeID, asi.get_ID(), archive.getAD_Process_ID(), null);
				attributeInstance.save();
			}

			dmsContent.setM_AttributeSetInstance_ID(asi.get_ID());
			dmsContent.save();
		}
	} // addAttributes

	@Override
	public boolean deleteArchive(MArchive archive, MStorageProvider prov)
	{
		return true;
	}

	public int getAttributeID(String AttributeName)
	{
		Integer attributeID = cache_AttributeName.get(AttributeName);
		if (attributeID == null || attributeID <= 0)
		{
			String sql = "SELECT M_Attribute_ID FROM M_Attribute WHERE IsActive='Y' AND Name = ? AND AD_Client_ID = 0";
			attributeID = DB.getSQLValue(null, sql, AttributeName);
			if (attributeID > 0)
				cache_AttributeName.put(AttributeName, attributeID);
		}
		return attributeID;
	} // getAttributeID

	public int getAttributeSetID(String AttributeSetName)
	{
		Integer attributeSetID = cache_AttributeSetName.get(AttributeSetName);
		if (attributeSetID == null || attributeSetID <= 0)
		{
			String sql = "SELECT M_AttributeSet_ID FROM M_AttributeSet WHERE IsActive='Y' AND Name = ? AND AD_Client_ID = 0";
			attributeSetID = DB.getSQLValue(null, sql, AttributeSetName);
			if (attributeSetID > 0)
				cache_AttributeSetName.put(AttributeSetName, attributeSetID);
		}
		return attributeSetID;
	} // getAttributeSetID

	public int getAttributeValueID(String AttributeValue)
	{
		Integer attributeValueID = cache_AttributeValue.get(AttributeValue);
		if (attributeValueID == null || attributeValueID <= 0)
		{
			String sql = "SELECT M_AttributeValue_ID FROM M_AttributeValue WHERE IsActive='Y' AND Value = ? AND AD_Client_ID = 0";
			attributeValueID = DB.getSQLValue(null, sql, AttributeValue);
			if (attributeValueID > 0)
				cache_AttributeValue.put(AttributeValue, attributeValueID);
		}
		return attributeValueID;
	} // getAttributeValueID
}
