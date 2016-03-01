package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.component.ZkCssHelper;
import org.adempiere.webui.window.FDialog;
import org.compiere.model.MImage;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.componenet.DMSViewerComponent;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.util.media.AMedia;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

public class WDMSVersion extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long				serialVersionUID		= -3613076228042516782L;

	public static CLogger					log						= CLogger.getCLogger(WDMSVersion.class);

	private ArrayList<DMSViewerComponent>	viewerComponents		= null;

	private IThumbnailProvider				thumbnailProvider		= null;
	private IFileStorageProvider			fileStorgProvider		= null;
	private IContentManager					contentManager			= null;

	private static DMSViewerComponent		prevComponent			= null;

	private Grid							gridView				= GridFactory.newGridLayout();

	private static final String				SQL_FETCH_VERSION_LIST	= "SELECT DISTINCT DMS_Content_ID FROM DMS_Association a WHERE DMS_Content_Related_ID= ? "
																			+ " AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType "
																			+ " WHERE NAME='Version') UNION SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ?"
																			+ " AND ContentBaseType <> 'DIR' order by DMS_Content_ID";

	public WDMSVersion(MDMSContent mDMSContent)
	{
		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()));

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		try
		{
			renderDMSVersion(mDMSContent);
			init();

			this.addEventListener(Events.ON_CLICK, this);
			this.addEventListener(Events.ON_DOUBLE_CLICK, this);
			this.addEventListener(Events.ON_CLOSE, this);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "DMS Version fetching failure :" + e);
			throw new AdempiereException("DMS Version fetching failure :" + e);
		}

	}

	private void init()
	{
		this.setHeight("38%");
		this.setWidth("44%");
		this.setTitle("DMS Version List");
		this.setClosable(true);
		this.appendChild(gridView);

		gridView.setStyle("overflow: auto; position:relative;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		AEnv.showCenterScreen(this);
	}

	public void renderDMSVersion(MDMSContent DMS_Content) throws IOException
	{
		byte[] imgByteData = null;
		File thumbFile = null;
		int i = 0;

		Components.removeAllChildren(gridView);

		Rows rows = new Rows();
		Row row = new Row();

		Cell cell = null;

		MImage mImage = null;
		AImage image = null;

		List<I_DMS_Content> dmsContent = getVersionHistory(DMS_Content);
		viewerComponents = new ArrayList<DMSViewerComponent>();

		for (i = 0; i < dmsContent.size(); i++)
		{
			thumbFile = thumbnailProvider.getFile(dmsContent.get(i), "150");
			if (thumbFile == null)
			{
				mImage = Utils.getMimetypeThumbnail(dmsContent.get(i).getDMS_MimeType_ID());
				imgByteData = mImage.getData();

				if (imgByteData != null)
					image = new AImage(dmsContent.get(i).getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			DMSViewerComponent viewerComponent = new DMSViewerComponent(dmsContent.get(i), image);

			viewerComponent.setDheight(WDMSPanel.COMPONENT_HEIGHT);
			viewerComponent.setDwidth(WDMSPanel.COMPONENT_WIDTH);

			viewerComponent.addEventListener(Events.ON_DOUBLE_CLICK, this);
			viewerComponent.addEventListener(Events.ON_CLICK, this);
			viewerComponent.addEventListener(Events.ON_RIGHT_CLICK, this);

			viewerComponents.add(viewerComponent);

			gridView.setSizedByContent(true);
			gridView.setZclass("none");

			cell = new Cell();
			cell.setWidth(row.getWidth());
			cell.appendChild(viewerComponent);

			row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap;");
			row.setZclass("none");
			row.appendCellChild(cell);
			rows.appendChild(row);
			row.appendChild(viewerComponent);
		}
		gridView.appendChild(rows);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		log.info(event.getName());
		
		event.getTarget();
		
		if (Events.ON_DOUBLE_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			downloadSelectedComponent(DMSViewerComp);
		}
		else if (Events.ON_CLICK.equals(event.getName())
				&& event.getTarget().getClass().equals(DMSViewerComponent.class))
		{
			DMSViewerComponent DMSViewerComp = (DMSViewerComponent) event.getTarget();
			currentCompSelection(DMSViewerComp);
		}
		else if (Events.ON_CLICK.equals(event.getName()) && event.getTarget().getClass().equals(WDMSVersion.class))
		{
			if (prevComponent != null)
			{
				ZkCssHelper.appendStyle(prevComponent.getfLabel(),
						"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");
			}
		}
	}

	private List<I_DMS_Content> getVersionHistory(MDMSContent DMS_Content)
	{
		List<I_DMS_Content> dmsContent = new ArrayList<I_DMS_Content>();
		MDMSAssociation dmsAssociation = null;

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?",
					DMS_Content.getDMS_Content_ID());

			dmsAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			pstmt = DB.prepareStatement(SQL_FETCH_VERSION_LIST, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE, null);

			pstmt.setInt(1, dmsAssociation.getDMS_Content_Related_ID());
			pstmt.setInt(2, dmsAssociation.getDMS_Content_Related_ID());

			rs = pstmt.executeQuery();

			if (rs != null)
			{
				while (rs.next())
				{
					dmsContent.add(new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null));
				}
			}

			if (dmsContent.size() == 0)
			{
				throw new AdempiereException("No versions are available.");
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Version list fetching failure: ", e);
			throw new AdempiereException("Version list fetching failure: " + e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}
		return dmsContent;
	}

	private void currentCompSelection(DMSViewerComponent DMSViewerComp)
	{
		if (prevComponent != null)
		{
			ZkCssHelper.appendStyle(prevComponent.getfLabel(),
					"background-color:#ffffff; box-shadow: 7px 7px 7px #ffffff");
		}

		for (DMSViewerComponent viewerComponent : viewerComponents)
		{
			if (viewerComponent.getDMSContent().getDMS_Content_ID() == DMSViewerComp.getDMSContent()
					.getDMS_Content_ID())
			{
				ZkCssHelper.appendStyle(DMSViewerComp.getfLabel(),
						"background-color:#99cbff; box-shadow: 7px 7px 7px #888888");

				prevComponent = viewerComponent;
				break;
			}
		}
	}

	private void downloadSelectedComponent(DMSViewerComponent DMSViewerComp) throws FileNotFoundException
	{
		File document = fileStorgProvider.getFile(contentManager.getPath(DMSViewerComp.getDMSContent()));

		if (document.exists())
		{
			AMedia media = new AMedia(document, "application/octet-stream", null);
			Filedownload.save(media);
		}
		else
		{
			FDialog.warn(0, "Docuement is not available to download.");
		}
	}
}
