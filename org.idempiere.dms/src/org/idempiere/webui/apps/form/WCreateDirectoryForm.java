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

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.compiere.util.CLogger;
import org.idempiere.dms.DMS;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.dms.factories.Utils;
import org.idempiere.model.I_DMS_Content;
import org.idempiere.model.MDMSContent;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Separator;
import org.zkoss.zul.South;

public class WCreateDirectoryForm extends Window implements EventListener<Event>
{

	/**
	 * 
	 */
	private static final long		serialVersionUID	= 4397569198011705268L;
	private static final CLogger	log					= CLogger.getCLogger(WCreateDirectoryForm.class);

	private DMS						dms;
	private MDMSContent				mDMSContent			= null;

	private Borderlayout			mainLayout			= new Borderlayout();
	private Panel					parameterPanel		= new Panel();
	private Label					lblDir				= new Label(DMSConstant.MSG_DIRECTORY_NAME);
	private ConfirmPanel			confirmPanel		= new ConfirmPanel(true, false, false, false, false, false);
	private Textbox					txtboxDirectory		= new Textbox();

	private int						tableID				= 0;
	private int						recordID			= 0;

	/**
	 * Constructor initialize
	 * 
	 * @param dms
	 * @param DMSContent
	 */
	public WCreateDirectoryForm(DMS dms, I_DMS_Content DMSContent, int tableID, int recordID)
	{
		this.dms = dms;

		try
		{
			this.mDMSContent = (MDMSContent) DMSContent;
			this.tableID = tableID;
			this.recordID = recordID;

			init();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render Component Problem", e);
			throw new AdempiereException("Render Component Problem : " + e);
		}
	}

	/**
	 * initialize components
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception
	{
		this.setHeight("150px");
		this.setWidth("500px");
		this.setTitle(DMSConstant.MSG_CREATE_DIRECTORY);
		mainLayout.setParent(this);
		mainLayout.setHflex("1");
		mainLayout.setVflex("1");

		lblDir.setValue(DMSConstant.MSG_ENTER_DIRETORY_NAME + ": ");
		lblDir.setStyle("padding-left: 5px");
		txtboxDirectory.setWidth("300px");
		txtboxDirectory.setFocus(true);
		txtboxDirectory.addEventListener(Events.ON_OK, this);

		North north = new North();
		north.setParent(mainLayout);
		mainLayout.appendChild(north);
		north.appendChild(parameterPanel);

		Hbox hbox = new Hbox();
		hbox.setAlign("center");
		hbox.setPack("start");
		hbox.appendChild(lblDir);
		hbox.appendChild(txtboxDirectory);

		parameterPanel.setStyle("padding: 5px");
		parameterPanel.appendChild(hbox);
		Separator separator = new Separator();
		separator.setOrient("horizontal");
		separator.setBar(true);
		separator.setStyle("padding-top: 40px");
		parameterPanel.appendChild(separator);

		South south = new South();
		south.setSclass("dialog-footer");
		south.setParent(mainLayout);
		mainLayout.appendChild(south);
		south.appendChild(confirmPanel);

		confirmPanel.getButton(ConfirmPanel.A_OK).setImageContent(Utils.getImage("Ok24.png"));
		confirmPanel.getButton(ConfirmPanel.A_CANCEL).setImageContent(Utils.getImage("Cancel24.png"));
		confirmPanel.addActionListener(Events.ON_CLICK, this);

		AEnv.showCenterScreen(this);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.zkoss.zk.ui.event.EventListener#onEvent(org.zkoss.zk.ui.event.Event)
	 */
	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getTarget().getId().equals(ConfirmPanel.A_CANCEL))
		{
			this.detach();
		}
		else if (event.getTarget().getId().equals(ConfirmPanel.A_OK) || Events.ON_OK.equals(event.getName()))
		{
			try
			{
				dms.createDirectory(txtboxDirectory.getValue(), mDMSContent, tableID, recordID, true, null);
			}
			catch (AdempiereException e)
			{
				throw new WrongValueException(txtboxDirectory, e.getLocalizedMessage(), e);
			}
			this.detach();
		}
	} // onEvent
}