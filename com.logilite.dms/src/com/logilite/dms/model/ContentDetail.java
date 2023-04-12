package com.logilite.dms.model;

import java.sql.Timestamp;

import org.compiere.model.MUser;

import com.logilite.dms.constant.DMSConstant;

/**
 * Details of the content, version and association to easy for usage like Icon viewer, sorting,
 * etc...
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ContentDetail
{
	private I_DMS_Version		version;
	private I_DMS_Association	association;

	private String				name;
	private String				fileType;
	private String				modifiedBy;
	private String				tooltipText;
	private String				size		= "";
	private String				contentType	= "";

	/**
	 * Constructor
	 * 
	 * @param version
	 * @param association
	 */
	public ContentDetail(I_DMS_Version version, I_DMS_Association association)
	{
		this.version = version;
		this.association = association;

		I_DMS_Content content = version.getDMS_Content();
		name = getContentName(content, version.getSeqNo());
		modifiedBy = MUser.getNameOfUser(version.getUpdatedBy());
		tooltipText = getTooltipTextMsg();

		if (content.getDMS_ContentType_ID() > 0)
		{
			contentType = content.getDMS_ContentType().getName();
		}

		if (MDMSContent.CONTENTBASETYPE_Directory.equals(content.getContentBaseType()))
		{
			fileType = DMSConstant.MSG_FILE_FOLDER;
		}
		else
		{
			size = version.getDMS_FileSize();
			fileType = content.getDMS_MimeType().getName();
		}
	} // ContentDetail

	public String getName()
	{
		return name;
	}

	public I_DMS_Version getVersion()
	{
		return version;
	}

	public I_DMS_Association getAssociation()
	{
		return association;
	}

	public I_DMS_Content getContent()
	{
		return version.getDMS_Content();
	}

	public String getContentTypeName()
	{
		return contentType;
	}

	public String getSize()
	{
		return size;
	}

	public Timestamp getUpdated()
	{
		return version.getUpdated();
	}

	public String getFileType()
	{
		return fileType;
	}

	public String getModifiedByName()
	{
		return modifiedBy;
	}

	public String getTooltipText()
	{
		return tooltipText;
	}

	public boolean isLink()
	{
		return MDMSAssociationType.isLink(association);
	}

	/**
	 * Get content name with versioning if applicable
	 * 
	 * @param  content
	 * @param  version
	 * @return
	 */
	private String getContentName(I_DMS_Content content, int version)
	{
		String name = content.getName();

		if (MDMSContent.CONTENTBASETYPE_Content.equals(content.getContentBaseType()) && version > 0)
			name = name + " - V" + version;

		return name;
	} // getContentName

	/**
	 * Tooltip text building
	 * 
	 * @return
	 */
	private String getTooltipTextMsg()
	{
		StringBuffer sb = new StringBuffer(((MDMSContent) version.getDMS_Content()).getToolTipTextMsg());

		if (MDMSContent.CONTENTBASETYPE_Content.equals(version.getDMS_Content().getContentBaseType()) && version.getDMS_FileSize() != null)
			sb.append("\nFile Size: " + version.getDMS_FileSize());

		if (association.getDMS_AssociationType_ID() > 0)
			sb.append("\nAssociation as " + association.getDMS_AssociationType().getName());

		sb.append("\nVersion ID: " + version.getDMS_Version_ID());
		sb.append("\nContent ID: " + version.getDMS_Content_ID());
		return sb.toString();
	} // getTooltipTextMsg

}
