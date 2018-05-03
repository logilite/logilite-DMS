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

package org.idempiere.webui.apps.form;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.GridFactory;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.dms.factories.IContentManager;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.FileStorageUtil;
import org.idempiere.model.IFileStorageProvider;
import org.idempiere.model.MDMSAssociation;
import org.idempiere.model.MDMSContent;
import org.idempiere.model.X_DMS_Content;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Cell;

import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

public class WRenameContent extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= -4440351217070536198L;
	private static CLogger			log					= CLogger.getCLogger(WRenameContent.class);

	private static final String		spFileSeprator		= Utils.getStorageProviderFileSeparator();

	private MDMSContent				DMSContent			= null;
	private MDMSContent				parent_Content 		= null;

	private Grid					gridView			= null;

	private Textbox					txtName				= null;
	private Textbox					txtDesc				= null;

	private Label					lblName				= null;
	private Label					lblDesc				= null;

	private ConfirmPanel			confirmpanel		= null;

	private Button					btnOk				= null;
	private Button					btnCancel			= null;

	private boolean					cancel				= false;

	private IContentManager			contentManager		= null;
	private IFileStorageProvider	fileStorgProvider	= null;
	private IIndexSearcher			indexSeracher		= null;

	private String					baseURL				= null;
	private String					renamedURL			= null;

	public WRenameContent(MDMSContent DMSContent)
	{
		this.DMSContent = DMSContent;

		fileStorgProvider = FileStorageUtil.get(Env.getAD_Client_ID(Env.getCtx()), false);

		if (fileStorgProvider == null)
			throw new AdempiereException("Storage provider is not define on clientInfo.");

		contentManager = Utils.getContentManager(Env.getAD_Client_ID(Env.getCtx()));

		if (contentManager == null)
			throw new AdempiereException("Content manager is not found.");

		indexSeracher = ServiceUtils.getIndexSearcher(Env.getAD_Client_ID(Env.getCtx()));

		if (indexSeracher == null)
		{
			throw new AdempiereException("Index Server not found");
		}

		init();
	}

	private void init()
	{
		gridView = GridFactory.newGridLayout();

		this.setHeight("32%");
		this.setWidth("30%");
		this.setTitle("Rename");
		this.appendChild(gridView);
		this.setClosable(true);
		this.addEventListener(Events.ON_OK, this);

		gridView.setStyle("position:relative;overflow:auto;");
		gridView.makeNoStrip();
		gridView.setOddRowSclass("even");
		gridView.setZclass("none");
		gridView.setWidth("100%");
		gridView.setHeight("100%");

		Columns columns = new Columns();
		Rows rows = new Rows();

		lblName = new Label("Please enter a new name for the item:");
		txtName = new Textbox();
		
		lblDesc = new Label("Description");
		txtDesc = new Textbox();
		txtDesc.setMultiline(true);
		txtDesc.setRows(2);
		

		parent_Content = new MDMSContent(Env.getCtx(), Utils.getDMS_Content_Related_ID(DMSContent), null);
		if (DMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
		{
			txtName.setValue(parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf(".")));
		}
		else
		{
			txtName.setText(DMSContent.getName());
		}

		txtDesc.setValue(DMSContent.getDescription());

		txtName.setFocus(true);
		txtName.setSelectionRange(0, txtName.getValue().length() - 1);
		txtName.setWidth("98%");
		txtDesc.setWidth("98%");

		confirmpanel = new ConfirmPanel();

		btnOk = confirmpanel.createButton(ConfirmPanel.A_OK);
		btnCancel = confirmpanel.createButton(ConfirmPanel.A_CANCEL);

		gridView.appendChild(columns);
		gridView.appendChild(rows);

		Column column = new Column();
		columns.appendChild(column);

		Row row = new Row();
		row.appendChild(lblName);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(txtName);
		rows.appendChild(row);

		row = new Row();
		row.appendChild(lblDesc);
		rows.appendChild(row);
		
		row = new Row();
		row.appendChild(txtDesc);
		rows.appendChild(row);

		row = new Row();
		Cell cell = new Cell();
		cell.setAlign("right");
		cell.appendChild(btnOk);
		cell.appendChild(btnCancel);
		row.appendChild(cell);
		rows.appendChild(row);

		btnOk.addEventListener(Events.ON_CLICK, this);
		btnCancel.addEventListener(Events.ON_CLICK, this);
		btnOk.setImageContent(Utils.getImage("Ok24.png"));
		btnCancel.setImageContent(Utils.getImage("Cancel24.png"));

		AEnv.showCenterScreen(this);
	}

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			cancel = true;
			this.detach();
		}
		if (event.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(event.getName()))
		{
			renameContent();
		}
	}

	private void ValidateName()
	{
		String fillMandatory = Msg.translate(Env.getCtx(), "FillMandatory");

		if (Util.isEmpty(txtName.getValue()))
		{
			throw new WrongValueException(txtName, fillMandatory);
		}
		else if (DMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
		{
			if (txtName.getValue().length() > Utils.filaNameLength)
				throw new WrongValueException(txtName, "Invalid Directory Name. Directory name less than 250 character");
			
			String fileSeprator = Utils.getStorageProviderFileSeparator();
			if (txtName.getValue().contains(fileSeprator))
				throw new WrongValueException(txtName, "Invalid Directory Name.");
		}
		else if (DMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Content))
		{
			String validationMsg = Utils.isValidFileName(txtName.getValue());			
			if(validationMsg != null){
				String validationResponse = Msg.translate(Env.getCtx(), validationMsg);
				throw new WrongValueException(txtName, validationResponse);
			}
		}
	}

	private void renameContent()
	{
		if(!txtDesc.getValue().equals(DMSContent.getDescription()))
		{
			DMSContent.setDescription(txtDesc.getValue());
			DMSContent.save();

			int DMS_Association_ID = DB.getSQLValue(null,
					"SELECT DMS_Association_ID FROM DMS_Association WHERE DMS_Content_ID = ?", DMSContent.getDMS_Content_ID());

			MDMSAssociation DMSAssociation = new MDMSAssociation(Env.getCtx(), DMS_Association_ID, null);

			Map<String, Object> solrValue = Utils.createIndexMap(DMSContent, DMSAssociation);
			indexSeracher.deleteIndex(DMSContent.getDMS_Content_ID());
			indexSeracher.indexContent(solrValue);
		}
		
			ValidateName();

			PreparedStatement pstmt = null;
			ResultSet rs = null;

			if (DMSContent.getContentBaseType().equals(X_DMS_Content.CONTENTBASETYPE_Directory))
			{
				if (!txtName.getValue().equalsIgnoreCase(parent_Content.getName())){
					baseURL = contentManager.getPath(DMSContent);

					File dirPath = new File(fileStorgProvider.getBaseDirectory(contentManager.getPath(DMSContent)));
					String newFileName = fileStorgProvider.getBaseDirectory(contentManager.getPath(DMSContent));
					newFileName = newFileName.substring(0, newFileName.lastIndexOf(spFileSeprator));

					File files[] = new File(newFileName).listFiles();

					if (newFileName.charAt(newFileName.length() - 1) == spFileSeprator.charAt(0))
						newFileName = newFileName + txtName.getValue();
					else
						newFileName = newFileName + spFileSeprator + txtName.getValue();

					File newFile = new File(newFileName);

					for (int i = 0; i < files.length; i++)
					{
						if (newFile.getName().equalsIgnoreCase(files[i].getName()))
						{
							throw new AdempiereException("Directory already exists.");
						}
					}

					if (!Util.isEmpty(DMSContent.getParentURL()))
						renamedURL = DMSContent.getParentURL() + spFileSeprator + txtName.getValue();
					else
						renamedURL = spFileSeprator + txtName.getValue();

					
					if(!dirPath.renameTo(newFile))
						throw new AdempiereException("Invalid File Name.");

					Utils.renameFolder(DMSContent, baseURL, renamedURL);
					DMSContent.setName(txtName.getValue());
					DMSContent.saveEx();
				}
				
			}
			else
			{
				if (!txtName.getValue().equalsIgnoreCase(
						parent_Content.getName().substring(0, parent_Content.getName().lastIndexOf("."))))
				{
					int DMS_Content_ID = Utils.getDMS_Content_Related_ID(DMSContent);
					MDMSContent content = null;
					MDMSAssociation association = null;
					try
					{
						pstmt = DB.prepareStatement(Utils.SQL_GET_RELATED_CONTENT, null);
						pstmt.setInt(1, DMS_Content_ID);
						pstmt.setInt(2, DMS_Content_ID);

						rs = pstmt.executeQuery();

						while (rs.next())
						{
							content = new MDMSContent(Env.getCtx(), rs.getInt("DMS_Content_ID"), null);
							association = new MDMSAssociation(Env.getCtx(), rs.getInt("DMS_Association_ID"), null);
							renameFile(content, association);
						}
					}
					catch (Exception e)
					{
						log.log(Level.SEVERE, "Rename content failure.", e);
						throw new AdempiereException("Rename content failure: " + e.getLocalizedMessage());
					}
					finally
					{
						DB.close(rs, pstmt);
						rs = null;
						pstmt = null;
					}
				}
			}
		this.detach();
	}

	/**
	 * @return true if dialog cancel by user
	 */
	public boolean isCancel()
	{
		return cancel;
	}

	private void renameFile(MDMSContent content, MDMSAssociation association)
	{
		String newPath = fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath();
		String fileExt = newPath.substring(newPath.lastIndexOf("."), newPath.length());
		newPath = newPath.substring(0, newPath.lastIndexOf(spFileSeprator));
		newPath = newPath + spFileSeprator + txtName.getValue() + fileExt;
		newPath = Utils.getUniqueFilename(newPath);

		File oldFile = new File(fileStorgProvider.getFile(contentManager.getPath(content)).getAbsolutePath());
		File newFile = new File(newPath);
		oldFile.renameTo(newFile);

		content.setName(newFile.getAbsolutePath().substring(newFile.getAbsolutePath().lastIndexOf(spFileSeprator) + 1,
				newFile.getAbsolutePath().length()));
		content.saveEx();

		try
		{
			Map<String, Object> solrValue = Utils.createIndexMap(content, association);
			indexSeracher.deleteIndex(content.getDMS_Content_ID());
			indexSeracher.indexContent(solrValue);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "RE-Indexing of Content Failure :", e);
			throw new AdempiereException("RE-Indexing of Content Failure :" + e);
		}
	}
}
