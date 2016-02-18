package org.idempiere.webui.apps.form;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Window;
import org.compiere.model.MImage;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.componenet.DMSVersionViewer;
import org.idempiere.dms.factories.IThumbnailProvider;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Components;
import org.zkoss.zul.Cell;

public class WDMSVersion extends Window
{

	/**
	 * 
	 */
	private static final long			serialVersionUID		= -3613076228042516782L;

	public static CLogger				log						= CLogger.getCLogger(WDMSVersion.class);
	public static DMSVersionViewer[]	cstmComponent			= null;
	public static boolean				isSelected[];

	private I_DMS_Content[]				dmsContent				= null;
	private IThumbnailProvider			thumbnailProvider		= null;

	private Grid						gridView				= GridFactory.newGridLayout();

	private static final String			SQL_FETCH_VERSION_LIST	= "SELECT DISTINCT DMS_Content_ID FROM DMS_Association a WHERE DMS_Content_Related_ID= ? "
																		+ " AND a.DMS_AssociationType_ID = (SELECT DMS_AssociationType_ID FROM DMS_AssociationType "
																		+ " WHERE NAME='Version') UNION SELECT DMS_Content_ID FROM DMS_Content WHERE DMS_Content_ID = ?"
																		+ " order by DMS_Content_ID";

	public WDMSVersion(MDMSContent mDMSContent)
	{
		thumbnailProvider = Utils.getThumbnailProvider(Env.getAD_Client_ID(Env.getCtx()));

		if (thumbnailProvider == null)
			throw new AdempiereException("Thumbnail provider is not found.");

		try
		{
			renderDMSVersion(mDMSContent);
			init();
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "DMS Version fetching failure :" + e.getLocalizedMessage());
			throw new AdempiereException("DMS Version fetching failure :" + e.getLocalizedMessage());
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
		int size = 0;

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		Components.removeAllChildren(gridView);

		Rows rows = new Rows();
		Row row = new Row();

		Cell cell = null;

		MImage mImage = null;
		AImage image = null;

		MDMSAssociation dmsAssociation = null;

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
				rs.beforeFirst();
				rs.last();
				size = rs.getRow();
				rs.beforeFirst();
			}

			if (size == 0)
			{
				log.log(Level.SEVERE, "No Versions available.");
				throw new AdempiereException("No Versions available.");
			}
			dmsContent = new I_DMS_Content[size];

			while (rs.next())
			{
				dmsContent[i++] = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Root content fetching failure: ", e.getLocalizedMessage());
			throw new AdempiereException("Root content fetching failure: " + e.getLocalizedMessage());
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		cstmComponent = new DMSVersionViewer[dmsContent.length];
		isSelected = new boolean[dmsContent.length];

		for (i = 0; i < cstmComponent.length; i++)
		{
			thumbFile = thumbnailProvider.getFile(dmsContent[i], "150");
			if (thumbFile == null)
			{
				mImage = Utils.getMimetypeThumbnail(dmsContent[i].getDMS_MimeType_ID());
				imgByteData = mImage.getData();

				if (imgByteData != null)
					image = new AImage(dmsContent[i].getName(), imgByteData);
			}
			else
			{
				image = new AImage(thumbFile);
			}

			cstmComponent[i] = new DMSVersionViewer(dmsContent[i], image, i);

			gridView.setSizedByContent(true);
			gridView.setZclass("none");
			cstmComponent[i].setDheight(130);
			cstmComponent[i].setDwidth(130);

			cell = new Cell();
			cell.setWidth(row.getWidth());
			cell.appendChild(cstmComponent[i]);

			row.setStyle("display:flex; flex-direction: row; flex-wrap: wrap;");
			row.setZclass("none");
			row.appendCellChild(cell);
			rows.appendChild(row);
			row.appendChild(cstmComponent[i]);
		}
		gridView.appendChild(rows);
	}
}
