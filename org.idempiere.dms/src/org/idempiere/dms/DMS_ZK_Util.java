package org.idempiere.dms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.theme.ThemeManager;
import org.adempiere.webui.window.FDialog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.compiere.model.MImage;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
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

/**
 * @author Sachin
 */
public class DMS_ZK_Util
{

	static CLogger log = CLogger.getCLogger(DMS_ZK_Util.class);

	public static void downloadDocument(DMS dms, MDMSContent content) throws FileNotFoundException, IOException
	{
		File document = dms.getFileFromStorage(content);

		if (document.exists())
			downloadDocument(document, content);
		else
			FDialog.warn(0, "Document is not available.");
	} // downloadDocument

	public static void downloadDocument(File document, MDMSContent content) throws FileNotFoundException, IOException
	{
		byte[] bytesArray = new byte[(int) document.length()];
		FileInputStream fis = new FileInputStream(document);
		fis.read(bytesArray);
		fis.close();
		AMedia media = new AMedia(content.getName(), content.getDMS_MimeType().getMimeType(), "application/octet-stream", bytesArray);
		Filedownload.save(media);
	} // downloadDocument

	/**
	 * Get AImage for the Content
	 * 
	 * @param  dms
	 * @param  content
	 * @param  thumbImgSize
	 * @return              {@link AImage}
	 */
	public static AImage getThumbImageForContent(DMS dms, I_DMS_Content content, String thumbImgSize)
	{
		File thumbFile = dms.getThumbnailFile(content, thumbImgSize);
		AImage image = null;
		try
		{
			if (thumbFile == null)
			{
				MImage mImage = null;
				if (content.getContentBaseType().equals(MDMSContent.CONTENTBASETYPE_Directory))
					mImage = dms.getDirThumbnail();
				else
					mImage = dms.getMimetypeThumbnail(content.getDMS_MimeType_ID());

				byte[] imgByteData = mImage.getData();
				if (imgByteData != null)
					image = new AImage(content.getName(), imgByteData);
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
		if (!ThemeManager.isUseFontIconForImage())
			return;

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

	private static void loadCSSFile(String cssFileURL, String cssStyleID )
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
	}

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

}
