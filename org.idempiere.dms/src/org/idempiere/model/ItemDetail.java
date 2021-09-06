package org.idempiere.model;

import java.sql.Timestamp;

import org.compiere.model.MUser;
import org.compiere.util.Env;
import org.idempiere.dms.constant.DMSConstant;

public class ItemDetail
{
	private I_DMS_Version		version;
	private I_DMS_Association	association;
	private I_DMS_Content		content;
	private String				name;
	private String				contentType;
	private String				size;
	private Timestamp			updated;
	private String				fileType;
	private String				modifiedBy;

	public ItemDetail(I_DMS_Version version, I_DMS_Association association)
	{
		this.version = version;
		this.association = association;
		this.content = version.getDMS_Content();
		this.name = getContentName(content, version.getSeqNo());
		if (this.content.getDMS_ContentType_ID() > 0)
			this.contentType = this.content.getDMS_ContentType().getName();
		this.size = version.getDMS_FileSize();
		this.updated = version.getUpdated();
		this.fileType = this.content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory) ? DMSConstant.MSG_FILE_FOLDER
																										: this.content.getDMS_MimeType().getName();
		this.modifiedBy = MUser.get(Env.getCtx(), version.getUpdatedBy()).getName();
	}

	public I_DMS_Version getVersion()
	{
		return version;
	}

	public void setVersion(I_DMS_Version version)
	{
		this.version = version;
	}

	public I_DMS_Association getAssociation()
	{
		return association;
	}

	public void setAssociation(I_DMS_Association association)
	{
		this.association = association;
	}

	public I_DMS_Content getContent()
	{
		return content;
	}

	public void setContent(I_DMS_Content content)
	{
		this.content = content;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getContentType()
	{
		return contentType;
	}

	public void setContentType(String contentType)
	{
		this.contentType = contentType;
	}

	public String getSize()
	{
		return size;
	}

	public void setSize(String size)
	{
		this.size = size;
	}

	public Timestamp getUpdated()
	{
		return updated;
	}

	public void setUpdated(Timestamp updated)
	{
		this.updated = updated;
	}

	public String getFileType()
	{
		return fileType;
	}

	public void setFileType(String fileType)
	{
		this.fileType = fileType;
	}

	public String getModifiedBy()
	{
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy)
	{
		this.modifiedBy = modifiedBy;
	}

	private String getContentName(I_DMS_Content content, int version)
	{
		String name = content.getName();

		if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && version > 0)
			name = name + " - V" + version;

		return name;
	}

}
