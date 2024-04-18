package com.logilite.dms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.adempiere.base.Core;
import org.adempiere.base.IResourceFinder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.LayoutUtils;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Tab;
import org.adempiere.webui.component.Tabbox;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.Tabpanels;
import org.adempiere.webui.component.Tabs;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.Dialog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.Zip;
import org.compiere.model.MImage;
import org.compiere.model.MSysConfig;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.zkoss.image.AImage;
import org.zkoss.io.Files;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.impl.LabelImageElement;

import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.form.WDocumentViewer;
import com.logilite.dms.model.I_DMS_Content;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSAssociation;
import com.logilite.dms.model.MDMSAssociationType;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.model.MDMSMimeType;
import com.logilite.dms.model.MDMSVersion;
import com.logilite.dms.util.DMSConvertToPDFUtils;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSSearchUtils;

/**
 * @author Sachin
 */
public class DMS_ZK_Util
{

	static CLogger log = CLogger.getCLogger(DMS_ZK_Util.class);

	/**
	 * @return string for setupload
	 */
	public static String getUploadSetting()
	{
		StringBuilder uploadSetting = new StringBuilder("true,native");
		int size = MSysConfig.getIntValue(DMSConstant.DMS_ZK_MAX_UPLOAD_SIZE, 0);
		if (size > 0)
		{
			uploadSetting.append(",maxsize=").append(size);
		}
		return uploadSetting.toString();
	} // getUploadSetting

	public static void downloadDocument(DMS dms, I_DMS_Version version) throws FileNotFoundException, IOException
	{
		File document = dms.getFileFromStorage(version);

		if (document.exists())
			downloadDocument(document, (MDMSContent) version.getDMS_Content());
		else
			Dialog.warn(0, "Document is not available.");
	} // downloadDocument

	public static void downloadDocument(File document, I_DMS_Content content) throws FileNotFoundException, IOException
	{
		byte[] bytesArray = new byte[(int) document.length()];
		FileInputStream fis = new FileInputStream(document);
		fis.read(bytesArray);
		fis.close();
		AMedia media = new AMedia(content.getName(), content.getDMS_MimeType().getMimeType(), "application/octet-stream", bytesArray);
		Filedownload.save(media);
	} // downloadDocument

	public static void downloadFile(String filename, String format, String ctype, File destZipFile) throws FileNotFoundException
	{
		AMedia media = new AMedia(filename, format, ctype, new FileInputStream(destZipFile.getAbsolutePath()));
		Filedownload.save(media);
	} // downloadFile

	/**
	 * Get AImage for the Content
	 * 
	 * @param  dms
	 * @param  version
	 * @param  thumbImgSize
	 * @return              {@link AImage}
	 */
	public static AImage getThumbImageForVersion(DMS dms, I_DMS_Version version, String thumbImgSize)
	{
		File thumbFile = dms.getThumbnailFile(version, thumbImgSize);
		AImage image = null;
		try
		{
			if (thumbFile == null)
			{
				MImage mImage = null;
				if (MDMSContent.CONTENTBASETYPE_Directory.equals(version.getDMS_Content().getContentBaseType()))
					mImage = dms.getDirThumbnail();
				else
					mImage = dms.getMimetypeThumbnail(version.getDMS_Content().getDMS_MimeType_ID());

				byte[] imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(version.getValue(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}
		}
		catch (IOException e)
		{
			throw new AdempiereException("ERROR: unable to get thumbnail image due to exception " + e, e);
		}

		return image;
	} // getThumbImageForContent

	/**
	 * Create Cell and added child of Row
	 * 
	 * @param  row
	 * @param  rowSpan
	 * @param  colSpan
	 * @param  comp    - Component
	 * @return         {@link Cell}
	 */
	public static Cell createCellUnderRow(Row row, int rowSpan, int colSpan, Component comp)
	{
		Cell cell = new Cell();
		cell.appendChild(comp);
		if (rowSpan > 0)
			cell.setRowspan(rowSpan);
		if (colSpan > 0)
			cell.setColspan(colSpan);
		row.appendChild(cell);

		return cell;
	} // createAndAppendCell

	/**
	 * Create MenuItem and append child of MenuPopup
	 * 
	 * @param  menuPopup
	 * @param  itemName  - Menu Item Name
	 * @param  icon      - Image Name
	 * @param  listener
	 * @return           {@link Menuitem}
	 */
	public static Menuitem createMenuItem(Menupopup menuPopup, String itemName, String icon, EventListener<? extends Event> listener)
	{
		Menuitem menuItem = new Menuitem(itemName);
		DMS_ZK_Util.setFontOrImageAsIcon(icon, menuItem);
		menuItem.addEventListener(Events.ON_CLICK, listener);
		menuPopup.appendChild(menuItem);
		return menuItem;
	} // createMenuItem

	/**
	 * Set Font or Image based icon to the component
	 * 
	 * @param iconName   - Icon Name
	 * @param imgElement - component which are implementing {@link LabelImageElement}
	 */
	public static void setFontOrImageAsIcon(String iconName, LabelImageElement imgElement)
	{
		if (ThemeManager.isUseFontIconForImage())
		{
			String iconSclass = "z-icon-" + iconName;
			imgElement.setIconSclass(iconSclass);
			LayoutUtils.addSclass("font-icon-toolbar-button", imgElement);
		}
		else
		{
			String size = Env.getContext(Env.getCtx(), "#ZK_Toolbar_Button_Size");
			String suffix = "24.png";
			if (!Util.isEmpty(size))
			{
				suffix = size + ".png";
			}
			imgElement.setImageContent(DMS_ZK_Util.getImage(iconName + suffix));
		}
	} // setFontOrImageAsIcon

	/**
	 * Set Data for Button component
	 * 
	 * @param btn         - Button Component
	 * @param icon        - Image Name
	 * @param toolTipText
	 * @param listener
	 */
	public static void setButtonData(Button btn, String icon, String toolTipText, EventListener<? extends Event> listener)
	{
		btn.setTooltiptext(toolTipText);
		DMS_ZK_Util.setFontOrImageAsIcon(icon, btn);
		btn.addEventListener(Events.ON_CLICK, listener);

		ZkCssHelper.appendStyle(btn, "margin: 3px !important;");
	} // setButtonData

	/**
	 * Load DMS CSS file and append its style in Head tag
	 */
	public static void loadDMSThemeCSSFile()
	{
		loadCSSFile("/css/dms.css", "ID_DMS_Style_Ref");
	} // loadDMSThemeCSSFile

	/**
	 * Load DMS CSS file and append its style in Head tag
	 */
	public static void loadDMSMobileCSSFile()
	{
		if (!ClientInfo.isMobile())
			return;

		loadCSSFile("/css/dms-mobile.css", "ID_DMS_Mobile_Style_Ref");
	} // loadDMSThemeCSSFile

	private static void loadCSSFile(String cssFileURL, String cssStyleID)
	{
		if (Util.isEmpty(cssFileURL, true))
			return;

		IResourceFinder rf = Core.getResourceFinder();
		URL url = rf.getResource(cssFileURL);
		try
		{
			InputStream in = url.openStream();
			String cssContents = IOUtils.toString(in, StandardCharsets.UTF_8.toString());
			cssContents = cssContents.replaceAll("[\\n\\t\\r]", " ");
			cssContents = cssContents.replaceAll("[\\\\]", "\\\\\\\\");
			cssContents = cssContents.replaceAll("[\\\"]", "\\\\\"");
			cssContents = cssContents.replaceAll("[\\\']", "\\\\\'");

			StringBuffer sb = new StringBuffer();
			sb.append(" var css  = '" + cssContents + "';");
			sb.append(	" var head = document.head || document.getElementsByTagName('head')[0]; 	"
						+ " if ($(head).find('Style[id=\"" + cssStyleID + "\"]').length <= 0)		"
						+ " { 																"
						+ " 	var style = document.createElement('style'); 				"
						+ " 	style.id = '" + cssStyleID + "'; 							"
						+ " 	style.type = 'text/css'; 									"
						+ " 	style.title = 'DMSStyle'; 									"
						+ " 	if 	(style.styleSheet) 										"
						+ "			{ style.styleSheet.cssText = css; } 					"
						+ "		else 														"
						+ "			{ style.appendChild(document.createTextNode(css)); }	"
						+ " 	head.appendChild(style); 									"
						+ " }																");
			Clients.evalJavaScript(sb.toString());
		}
		catch (IOException e)
		{
			throw new AdempiereException("Error: Unable to load dms.css file. " + e.getLocalizedMessage(), e);
		}
	} // loadCSSFile

	public static AImage getImage(String name)
	{
		IResourceFinder rf = null;
		URL url = null;
		AImage image = null;
		try
		{
			rf = Core.getResourceFinder();
			url = rf.getResource("/dmsimages/" + name);
			image = new AImage(url);
		}
		catch (IOException e)
		{
			log.log(Level.WARNING, name + " Icon not found");
		}
		return image;
	} // getImage

	/**
	 * Get Media from File
	 * 
	 * @param  file
	 * @return      {@link AMedia}
	 */
	public static AMedia getMediaFromFile(File file)
	{
		try
		{
			return new AMedia(file.getName(), null, null, FileUtils.readFileToByteArray(file));
		}
		catch (IOException e)
		{
			throw new AdempiereException("Issue while creating Media file.", e);
		}
	} // getMediaFromFile

	/**
	 * Zip the srcFolder into the destFileZipFile. All the folder subtree of the src folder is added
	 * to the destZipFile archive.
	 *
	 * @param srcFolder   File, the path of the srcFolder
	 * @param destZipFile File, the path of the destination zipFile. This file will be created or
	 *                    erased.
	 * @param includesdir
	 */
	public static void zipFolder(File srcFolder, File destZipFile, String includesdir)
	{
		Zip zipper = new Zip();
		zipper.setDestFile(destZipFile);
		zipper.setBasedir(srcFolder);
		zipper.setIncludes(includesdir.replace(" ", "*"));
		zipper.setUpdate(true);
		zipper.setCompress(true);
		zipper.setCaseSensitive(false);
		zipper.setFilesonly(false);
		zipper.setTaskName("zip");
		zipper.setTaskType("zip");
		zipper.setProject(new Project());
		zipper.setOwningTarget(new Target());
		zipper.execute();
	} // zipFolder

	/**
	 * Read storage file and write it to specified directory path
	 * 
	 * @param outputDirectory - To directory path
	 * @param inputFile       - Storage / Input file
	 */
	public static void readFileFromAndWriteToDir(String outputDirectory, File inputFile)
	{
		byte[] data = null;
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(inputFile);
			data = Files.readAll(fis);
		}
		catch (IOException e)
		{
			throw new AdempiereException("Error while reading file : " + inputFile.getName(), e);
		}
		finally
		{
			if (fis != null)
			{
				try
				{
					fis.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		//
		DMS_ZK_Util.writeBLOB(outputDirectory, data);
	} // readFileFromAndWriteToDir

	/**
	 * Write data to given path
	 * 
	 * @param  directoryPath - Path to write byte data
	 * @param  data          - Data of the file
	 * @return               True if successfully write else throws error
	 */
	public static boolean writeBLOB(String directoryPath, byte[] data)
	{
		FileOutputStream fos = null;
		try
		{
			File file = new File(directoryPath);

			String absolutePath = file.getAbsolutePath();
			String folderpath = absolutePath.substring(0, absolutePath.lastIndexOf(DMSConstant.FILE_SEPARATOR));

			new File(folderpath).mkdirs();

			if (file.exists())
			{
				file = new File(absolutePath);
			}

			fos = new FileOutputStream(file, true);
			fos.write(data);

			return true;
		}
		catch (Exception e)
		{
			throw new AdempiereException("Blob writing failure for directory path: " + directoryPath + ", Error: " + e.getLocalizedMessage());
		}
		finally
		{
			try
			{
				if (fos != null)
					fos.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	} // writeBLOB

	public static String doValidDirName(String directoryName)
	{
		return directoryName.replaceAll("[^a-zA-Z 0-9_()-]", "_");
	} // doValidDirName

	/**
	 * @param  editor - Editor component
	 * @param  dt     - DisplayType
	 * @return        removed special characters and indexable field name for ASI
	 */
	public static String getIndexibleColumnName(WEditor editor, int dt)
	{
		if (dt == DisplayType.Search || dt == DisplayType.Table || dt == DisplayType.List)
			return "ASI_" + DMSSearchUtils.getIndexFieldName(editor.getColumnName());
		else
			return "ASI_" + DMSSearchUtils.getIndexFieldName(editor.getLabel().getValue());
	} // getIndexibleColumnName

	/**
	 * Open Content Document for the Viewing
	 * 
	 * @param dms                     DMS
	 * @param windowNo                Window No
	 * @param tabNo                   Tab No
	 * @param tableID                 Table_ID
	 * @param recordID                Record_ID
	 * @param isMountingBaseStructure True - If its Mounting Based Structure
	 * @param isWindowAccess          Having Window Access
	 * @param tabs
	 * @param tabBox
	 * @param tabPanels
	 * @param component               Component of the Content
	 * @param version                 MDMSVersion
	 * @param selectedContent         MDMSContent
	 * @param selectedAssociation     MDMSAssociation
	 * @param panelClass              Class
	 */
	public static void openContentDocumentViewer(	DMS dms, int windowNo, int tabNo, int tableID, int recordID,
													boolean isMountingBaseStructure, boolean isWindowAccess,
													Tabs tabs, Tabbox tabBox, Tabpanels tabPanels, Component component,
													MDMSVersion version, MDMSContent selectedContent, MDMSAssociation selectedAssociation, Component panelClass)
	{
		MDMSMimeType mimeType = (MDMSMimeType) selectedContent.getDMS_MimeType();
		File documentToPreview = dms.getFileFromStorage(version);

		if (documentToPreview != null)
		{
			String name = selectedContent.getName();

			try
			{
				documentToPreview = DMSConvertToPDFUtils.convertDocToPDF(documentToPreview, mimeType);
			}
			catch (Exception e)
			{
				if (e.getCause() instanceof WorkbookNotFoundException)
				{
					// Do not throw error, some document having complex function used and
					// implemented libs not enough to handle that things.
				}
				else
				{
					String errorMsg = "Whoops! There was a problem previewing this document. \n Due to exception: " + e.getLocalizedMessage();
					log.log(Level.SEVERE, errorMsg, e);
					FDialog.warn(windowNo, errorMsg, "Document preview issue...");
				}
			}

			if (DMSFactoryUtils.getContentEditor(mimeType.getMimeType()) != null)
			{
				boolean isContentActive = (boolean) component.getAttribute(DMSConstant.COMP_ATTRIBUTE_ISACTIVE);

				Tab tabData = new Tab(name);
				tabData.setClass(isContentActive ? "SB-Active-Content" : "SB-InActive-Content");
				tabData.setClosable(true);
				tabs.appendChild(tabData);
				tabBox.setSelectedTab(tabData);

				//
				WDocumentViewer documentViewer = new WDocumentViewer(	dms, tabBox, documentToPreview, selectedContent, tableID, recordID, windowNo, tabNo,
																		component);
				Tabpanel tabPanel = documentViewer.initForm(isWindowAccess, isMountingBaseStructure, MDMSAssociationType.isLink(selectedAssociation));
				tabPanels.appendChild(tabPanel);
				documentViewer.getAttributePanel().addEventListener(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, (EventListener<?>) panelClass);
				documentViewer.getAttributePanel().addEventListener(DMSConstant.EVENT_ON_RENAME_COMPLETE, (EventListener<?>) panelClass);

				// panelClass.appendChild(tabBox);
			}
			else
			{
				FDialog.warn(windowNo, "Not able to preview for this content, Please download it...", "Document preview issue...");
				// downloadDocument(documentToPreview, selectedContent);
			}
		}
		else
		{
			FDialog.error(windowNo, panelClass, "ContentNotFoundInStorage", dms.getPathFromContentManager(version), "Content Not Found In the Storage");
		}
	} // openContentDocumentViewer

}
