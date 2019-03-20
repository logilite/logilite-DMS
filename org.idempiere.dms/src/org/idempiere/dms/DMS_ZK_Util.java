package org.idempiere.dms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.adempiere.base.Service;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Menupopup;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MImage;
import org.compiere.util.CCache;
import org.idempiere.dms.factories.IDMSViewer;
import org.idempiere.dms.factories.IDMSViewerFactory;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Menuitem;

/**
 * @author Sachin
 */
public class DMS_ZK_Util
{

	static CCache<String, IDMSViewer>	cache_thumbnailGenerator	= new CCache<String, IDMSViewer>("DMSViewer", 2);

	public static void downloadDocument(DMS dms, MDMSContent content) throws FileNotFoundException
	{
		File document = dms.getFileFromStorage(content);

		if (document.exists())
			downloadDocument(document);
		else
			FDialog.warn(0, "Document is not available.");
	} // downloadDocument

	public static void downloadDocument(File document) throws FileNotFoundException
	{
		AMedia media = new AMedia(document, "application/octet-stream", null);
		Filedownload.save(media);
	} // downloadDocument

	/**
	 * Factory call for DMS Component Viewer
	 * 
	 * @param toggleAction
	 * @return {@link IDMSViewer}
	 */
	public static IDMSViewer getDMSCompViewer(String toggleAction)
	{
		List<IDMSViewerFactory> factories = Service.locator().list(IDMSViewerFactory.class).getServices();
		for (IDMSViewerFactory factory : factories)
		{
			return factory.get(toggleAction);
		}

		return null;
	}

	/**
	 * Get AImage for the Content
	 * 
	 * @param dms
	 * @param content
	 * @param thumbImgSize
	 * @return {@link AImage}
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
			e.printStackTrace();
		}

		return image;
	} // getThumbImageForContent

	/**
	 * Create Cell and added child of Row
	 * 
	 * @param row
	 * @param rowSpan
	 * @param colSpan
	 * @param comp - Component
	 * @return {@link Cell}
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
	 * @param menuPopup
	 * @param itemName - Menu Item Name
	 * @param icon - Image Name
	 * @param listener
	 * @return {@link Menuitem}
	 */
	public static Menuitem createMenuItem(Menupopup menuPopup, String itemName, String icon, EventListener<? extends Event> listener)
	{
		Menuitem menuItem = new Menuitem(itemName);
		menuItem.setImageContent(Utils.getImage(icon));
		menuItem.addEventListener(Events.ON_CLICK, listener);

		menuPopup.appendChild(menuItem);
		return menuItem;
	} // createMenuItem

	/**
	 * Set Data for Button component
	 * 
	 * @param btn - Button Component
	 * @param icon - Image Name
	 * @param toolTipText
	 * @param listener
	 */
	public static void setButtonData(Button btn, String icon, String toolTipText, EventListener<? extends Event> listener)
	{
		btn.setTooltiptext(toolTipText);
		btn.setImageContent(Utils.getImage(icon));
		btn.addEventListener(Events.ON_CLICK, listener);

		ZkCssHelper.appendStyle(btn, "margin: 3px !important;");
	} // setButtonData

}
