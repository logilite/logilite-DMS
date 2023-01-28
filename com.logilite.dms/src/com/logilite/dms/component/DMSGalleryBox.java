/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.logilite.dms.component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.event.DialogEvents;
import org.adempiere.webui.event.ValueChangeEvent;
import org.adempiere.webui.theme.ThemeManager;
import org.compiere.model.MTable;
import org.compiere.util.Env;
import org.compiere.util.Util;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Components;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;

import com.logilite.dms.DMS;
import com.logilite.dms.constant.DMSConstant;
import com.logilite.dms.factories.IContentTypeAccess;
import com.logilite.dms.factories.IDMSUploadContent;
import com.logilite.dms.form.WDMSPanel;
import com.logilite.dms.model.I_DMS_Association;
import com.logilite.dms.model.I_DMS_Version;
import com.logilite.dms.model.MDMSContent;
import com.logilite.dms.util.DMSFactoryUtils;
import com.logilite.dms.util.DMSPermissionUtils;
import com.logilite.dms.util.DMSSearchUtils;

/**
 * DMS gallery Component with upload button and gallery icon viewer
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 * @author Parth Ambani @ Logilite Technologies
 */
public class DMSGalleryBox extends Hbox implements EventListener<Event>
{
	private static final long				serialVersionUID	= -6126859364277605068L;

	public Grid								gridGalleryViewer	= GridFactory.newGridLayout();
	private Button							btnUpload			= new Button();

	private Component						parentComponent;
	private MDMSContent						content;

	private DMS								dms;

	private int								windowNo			= 0;
	private int								tableID				= 0;
	private int								recordID			= 0;
	private int								contentID			= 0;
	private int								contentTypeID		= 0;

	public String							title				= "DMS";

	private boolean							isDocExpWindow		= false;
	private HashMap<String, List<Object>>	queryParamas;

	//
	public DMSGalleryBox()
	{
		init();

		Div divUpload = new Div();
		divUpload.setStyle("padding-top: 5px;");
		divUpload.appendChild(btnUpload);

		this.appendChild(divUpload);
		this.appendChild(gridGalleryViewer);
		this.setHeight((DMSConstant.CONTENT_GALLERY_ICON_WIDTH + 15) + "px");
		gridGalleryViewer.setHeight((DMSConstant.CONTENT_GALLERY_ICON_WIDTH + 10) + "px");
		btnUpload.setWidth("65px");
		btnUpload.setHeight("50px");
	} // DMSGalleryBox

	public DMSGalleryBox(Component form, int contentTypeID, String title)
	{
		this();
		//
		this.parentComponent = form;
		this.contentTypeID = contentTypeID;
		if (!Util.isEmpty(title, true))
		{
			this.title = title;
		}
	} // DMSGalleryBox

	private void init()
	{
		if (ThemeManager.isUseFontIconForImage())
			btnUpload.setIconSclass("z-icon-FileImport");
		else
			btnUpload.setImage(ThemeManager.getThemeResource("images/FileImport16.png"));

		enableUploadButton(false);
		btnUpload.addEventListener(Events.ON_CLICK, this);
	} // init

	/**
	 * Render Content Viewer
	 */
	public void renderViewer()
	{
		renderViewer(queryParamas);
	} // renderViewer

	/**
	 * Render Content Viewer based on given query params
	 * 
	 * @param queryFilterParamas
	 */
	public void renderViewer(HashMap<String, List<Object>> queryFilterParamas)
	{
		//
		Components.removeAllChildren(gridGalleryViewer);
		//
		dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));
		//
		if (tableID > 0 && recordID > 0)
		{
			String table = MTable.getTableName(Env.getCtx(), tableID);
			dms.initMountingStrategy(table);
			dms.initiateMountingContent(table, recordID, tableID);
			content = dms.getRootMountingContent(tableID, recordID);
		}

		HashMap<I_DMS_Version, I_DMS_Association> contentsMap = null;

		if (contentTypeID > 0 || (queryFilterParamas != null && queryFilterParamas.size() > 0))
		{
			if (queryFilterParamas == null)
				queryFilterParamas = new HashMap<String, List<Object>>();
			queryFilterParamas.putAll(getQueryParams());
			contentsMap = dms.renderSearchedContent(queryFilterParamas, content, tableID, recordID);
		}
		else
			contentsMap = dms.getDMSContentsWithAssociation(content, dms.AD_Client_ID, DMSConstant.DOCUMENT_VIEW_NON_DELETED_VALUE);

		// Content Type wise access restriction
		IContentTypeAccess contentTypeAccess = DMSFactoryUtils.getContentTypeAccessFactory();
		HashMap<I_DMS_Version, I_DMS_Association> contentsMapCTFiltered = contentTypeAccess.getFilteredContentList(contentsMap);

		// Permission wise access restriction
		HashMap<I_DMS_Version, I_DMS_Association> mapPerFiltered;
		if (DMSPermissionUtils.isPermissionAllowed())
		{
			mapPerFiltered = dms.getPermissionManager().getFilteredVersionList(contentsMapCTFiltered);
		}
		else
		{
			mapPerFiltered = contentsMapCTFiltered;
		}

		String[] eventsList = new String[] { Events.ON_CLICK, Events.ON_SELECT };
		DefaultComponentIconViewerGallery viewerComponent = (DefaultComponentIconViewerGallery) DMSFactoryUtils.getDMSComponentViewer(DMSConstant.ICON_VIEW_GALLERY);
		viewerComponent.init(	dms, mapPerFiltered, gridGalleryViewer, DMSConstant.CONTENT_GALLERY_ICON_WIDTH, DMSConstant.CONTENT_GALLERY_ICON_HEIGHT, this,
								eventsList);

		gridGalleryViewer.setVisible(!mapPerFiltered.isEmpty());
	} // renderViewer

	public HashMap<String, List<Object>> getQueryParams()
	{
		if (contentTypeID <= 0)
			return null;

		HashMap<String, List<Object>> params = new LinkedHashMap<String, List<Object>>();
		DMSSearchUtils.setSearchParams(DMSConstant.CONTENTTYPE, contentTypeID, null, params);
		DMSSearchUtils.setSearchParams(DMSConstant.SHOW_INACTIVE, false, null, params);

		if (tableID > 0)
			DMSSearchUtils.setSearchParams(DMSConstant.AD_TABLE_ID, dms.validTableID(tableID), null, params);
		if (recordID > 0)
			DMSSearchUtils.setSearchParams(DMSConstant.RECORD_ID, dms.validRecordID(recordID), null, params);

		return params;
	} // getQueryParamas

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (btnUpload.equals(event.getTarget()))
		{
			if (contentTypeID > 0)
				Env.setContext(Env.getCtx(), windowNo, 0, MDMSContent.COLUMNNAME_DMS_ContentType_ID, contentTypeID);

			//
			DMS dms = new DMS(Env.getAD_Client_ID(Env.getCtx()));

			dms.initMountingStrategy(MTable.getTableName(Env.getCtx(), tableID));
			dms.initiateMountingContent(MTable.getTableName(Env.getCtx(), tableID), recordID, tableID);
			MDMSContent currDMSContent = dms.getRootMountingContent(tableID, recordID);

			//
			IDMSUploadContent uploadContent = DMSFactoryUtils.getUploadContenFactory(dms, currDMSContent, false, tableID, recordID, windowNo, 0);
			((Component) uploadContent).addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception
				{
					Integer[] contentIDs = uploadContent.getUploadedDocContentIDs();
					if (contentIDs.length > 0)
						contentID = contentIDs[0];

					if (parentComponent != null && contentID > 0)
						Events.sendEvent(new Event(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, parentComponent));
				}
			});
		}
		else if (event.getTarget() instanceof Cell)
		{
			Window window = new Window();
			window.setTitle(title);
			window.setSclass("popup-dialog");
			window.setBorder("normal");
			window.setClosable(true);
			window.setSizable(true);
			window.setHeight("70%");
			window.setWidth("70%");
			window.setMaximizable(true);

			//
			WDMSPanel docDMSPanel;
			if (recordID > 0 && tableID > 0)
			{
				docDMSPanel = new WDMSPanel(tableID, recordID, windowNo, 0);
			}
			else
			{
				docDMSPanel = new WDMSPanel(windowNo, 0);
			}

			if (contentTypeID > 0)
			{
				docDMSPanel.getContentTypeComp().setValue(contentTypeID);
				ValueChangeEvent changeEvent = new ValueChangeEvent(docDMSPanel.getContentTypeComp(), docDMSPanel.getContentTypeComp().getColumnName(),
																	null, contentTypeID);
				docDMSPanel.valueChange(changeEvent);
				docDMSPanel.setDocExplorerWindow(true);
				docDMSPanel.searchContents(true);
			}
			else
			{
				docDMSPanel.renderViewer();
			}

			window.appendChild(docDMSPanel);
			window.addEventListener(DialogEvents.ON_WINDOW_CLOSE, new EventListener<Event>() {

				@Override
				public void onEvent(Event event) throws Exception
				{
					HashMap<I_DMS_Version, I_DMS_Association> contents = docDMSPanel.getDMS().getDMSContentsWithAssociation(docDMSPanel.getCurrDMSContent(),
																															docDMSPanel.getDMS().AD_Client_ID,
																															true);
					if (contents.isEmpty())
					{
						contentID = 0;
						if (parentComponent != null)
							Events.sendEvent(new Event(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, parentComponent));
					}
					else
					{
						renderViewer();
					}
				}
			});

			AEnv.showCenterScreen(window);
		}
	} // onEvent

	public void enableUploadButton(boolean isEnable)
	{
		btnUpload.setEnabled(isEnable);
	} // enableUploadButton

	public boolean isEditable()
	{
		return btnUpload.isEnabled();
	} // isEditable

	public void setRecordID(int recordID)
	{
		this.recordID = recordID;
	} // setRecordID

	public void setTableID(int tableID)
	{
		this.tableID = tableID;
	} // setTableID

	public void setWindowNo(int windowNo)
	{
		this.windowNo = windowNo;
	} // setWindowNo

	public void setcontentType_ID(Integer contentTypeID)
	{
		this.contentTypeID = contentTypeID;
	} // setcontentType_ID

	public void setContent_ID(Integer contentID)
	{
		this.contentID = contentID;
	} // setContent_ID

	public BigDecimal getContent_ID()
	{
		return BigDecimal.valueOf(contentID);
	} // getContent_ID

	public boolean isDocExplorerWindow()
	{
		return isDocExpWindow;
	}

	public void setDocExplorerWindow(boolean isDocExplorerWindow)
	{
		this.isDocExpWindow = isDocExplorerWindow;
	}

	public void applyFilter(HashMap<String, List<Object>> queryParamas)
	{
		this.queryParamas = queryParamas;
	}

	public Component getParentComponent()
	{
		return parentComponent;
	}

}
