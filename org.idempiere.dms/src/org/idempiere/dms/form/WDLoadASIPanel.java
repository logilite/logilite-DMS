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

package org.idempiere.dms.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.ClientInfo;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WebEditorFactory;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.GridField;
import org.compiere.model.GridFieldVO;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.idempiere.dms.constant.DMSConstant;
import org.idempiere.model.MDMSContentType;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.Clients;

public class WDLoadASIPanel extends Panel
{

	/**
	 * 
	 */
	private static final long serialVersionUID  = -9141937878893779910L;

	private static CLogger    log               = CLogger.getCLogger(WDLoadASIPanel.class);

	private int               asiID             = 0;
	private int               m_WindowNo        = 0;
	private int               M_AttributeSet_ID = 0;

	private Grid              attributeGrid     = new Grid();
	private Rows              rows              = new Rows();
	private Label             lblAttribute      = new Label();

	private MAttributeSet     mAttributeSet     = null;

	private boolean           m_changed         = false;

	private MDMSContentType   contentType;

	/** List of Editors */
	public ArrayList<WEditor> m_editors         = new ArrayList<WEditor>();

	private int               windowNo          = 0;
	private int               tabNo             = 0;

	/**
	 * Constructor
	 * 
	 * @param DMS_ContentType_ID
	 * @param M_AttributeSetInstance_ID
	 */
	public WDLoadASIPanel(int DMS_ContentType_ID, int M_AttributeSetInstance_ID, int windowNo, int tabNo)
	{
		m_WindowNo = SessionManager.getAppDesktop().registerWindow(this);
		asiID = M_AttributeSetInstance_ID;

		contentType = new MDMSContentType(Env.getCtx(), DMS_ContentType_ID, null);

		this.windowNo = windowNo;
		this.tabNo = tabNo;

		initPanel();
	}

	public void initPanel()
	{
		this.appendChild(attributeGrid);

		if (!ClientInfo.isMobile())
		{
			this.setHeight("65%");
			this.setWidth("100%");
		}

		attributeGrid.appendChild(rows);

		if (contentType != null && !Util.isEmpty(contentType.getName(), true))
		{
			Row row = rows.newRow();
			lblAttribute.setText(DMSConstant.MSG_CONTENT_TYPE + ": " + contentType.getName());
			lblAttribute.setStyle("font-weight: bold;");
			row.appendCellChild(lblAttribute, 2);
		}

		initAttribute();
	}

	public void initAttribute()
	{
		M_AttributeSet_ID = contentType.getM_AttributeSet_ID();
		mAttributeSet = new MAttributeSet(Env.getCtx(), M_AttributeSet_ID, null);

		try
		{
			MAttribute[] attributes = mAttributeSet.getMAttributes(false);

			for (int i = 0; i < attributes.length; i++)
			{
				addAttributeLine(rows, attributes[i]);
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Attribute line adding failure: " + e);
			throw new AdempiereException("Attribute line adding failure: " + e.getLocalizedMessage());
		}
	} // initAttribute

	private void addAttributeLine(Rows rows, MAttribute attribute)
	{
		WEditor editor = null;
		//
		if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			editor = WebEditorFactory.getEditor(getListTypeGridField(attribute), true);
		}
		else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
		{
			editor = WebEditorFactory.getEditor(getNumberGridField(attribute), true);
		}
		else if (MAttribute.ATTRIBUTEVALUETYPE_Reference.equals(attribute.getAttributeValueType()))
		{
			editor = WebEditorFactory.getEditor(getReferenceGridField(attribute), true);
		}
		else
		// Text Field
		{
			editor = WebEditorFactory.getEditor(getStringGridField(attribute), true);
		}

		if (editor != null)
		{
			Row row = rows.newRow();
			row.setSclass("SB-Grid-field");

			Label label = editor.getLabel();
			if (label.getValue() == null || label.getValue().trim().length() < 1)
				label.setValue(attribute.getName());

			if (attribute.isMandatory())
			{
				label.setValue(label.getValue());
			}

			row.appendCellChild(label);

			editor.setMandatory(attribute.isMandatory());
			editor.fillHorizontal();

			if (asiID > 0)
			{
				setEditorAttribute(attribute, editor);
			}

			Component fieldEditor = editor.getComponent();
			row.appendCellChild(fieldEditor, 2);

			// Set attribute default value from the context
			String valueAttribute = Env.getContext(Env.getCtx(), windowNo, tabNo, attribute.getName(), true);
			if (!Util.isEmpty(valueAttribute, true))
			{
				editor.setValue(valueAttribute);
			}

			m_editors.add(editor);
		}
	} // addAttributeLine

	private GridField getReferenceGridField(MAttribute attribute)
	{
		return getGridField(attribute, attribute.getName(), attribute.getAD_Reference_ID(), attribute.getAD_Reference_Value_ID());
	} // getReferenceGridField

	private GridField getStringGridField(MAttribute attribute)
	{
		return getGridField(attribute, attribute.getName(), DisplayType.String, 0);
	} // getStringGridField

	private GridField getNumberGridField(MAttribute attribute)
	{
		return getGridField(attribute, attribute.getName(), DisplayType.Number, 0);
	} // getNumberGridField

	private GridField getListTypeGridField(MAttribute attribute)
	{
		GridField gridField = getGridField(attribute, "M_AttributeValue_ID", DisplayType.TableDir, 0);

		gridField.getVO().ValidationCode = "M_AttributeValue.M_Attribute_ID=" + attribute.get_ID();
		gridField.getVO().lookupInfo.ValidationCode = gridField.getVO().ValidationCode;
		gridField.getVO().lookupInfo.IsValidated = true;
		gridField = new GridField(GridFieldVO.createParameter(gridField.getVO()));
		return gridField;
	} // getListTypeGridField

	private GridField getGridField(MAttribute attribute, String columnName, int Reference_ID, int Reference_Value_ID)
	{
		String attribName = Msg.translate(Env.getCtx(), attribute.get_Translation(MAttribute.COLUMNNAME_Name));
		GridFieldVO vo = GridFieldVO
						.createParameter(Env.getCtx(), m_WindowNo, AEnv
										.getADWindowID(m_WindowNo), 0, 0, columnName, attribName, Reference_ID, Reference_Value_ID, false, false);
		String desc = attribute.get_Translation(MAttribute.COLUMNNAME_Description);
		vo.Description = desc != null ? desc : "";

		return new GridField(vo);
	} // getGridField

	private void setEditorAttribute(MAttribute attribute, WEditor editor)
	{
		MAttributeInstance instance = attribute.getMAttributeInstance(asiID);
		if (instance != null)
		{
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
			{
				if (instance.getM_AttributeValue_ID() > 0)
					editor.setValue(instance.getM_AttributeValue_ID());
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
			{
				editor.setValue(instance.getValueNumber().doubleValue());
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_StringMax40.equals(attribute.getAttributeValueType()))
			{
				editor.setValue(instance.getValue());
			}
			else
			{
				int dt = editor.getGridField().getDisplayType();
				if (dt == DisplayType.Date || dt == DisplayType.DateTime || dt == DisplayType.Time)
				{
					if (instance.getValueTimeStamp() != null)
						editor.setValue(instance.getValueTimeStamp());
				}
				else if (dt == DisplayType.Image
							|| dt == DisplayType.Assignment
								|| dt == DisplayType.Locator
								|| dt == DisplayType.Payment
								|| dt == DisplayType.TableDir
								|| dt == DisplayType.Table
								|| dt == DisplayType.Search
								|| dt == DisplayType.Account)
				{
					if (instance.getValueInt() > 0)
						editor.setValue(instance.getValueInt());
				}
				else if (dt == DisplayType.Number)
				{
					editor.setValue(instance.getValueNumber().doubleValue());
				}
				else if (dt == DisplayType.Integer)
				{
					editor.setValue(instance.getValueInt());
				}
				else if (DisplayType.isNumeric(dt))
				{
					if (instance.getValueNumber() != null)
						editor.setValue(instance.getValueNumber());
				}
				else
				{
					if (instance.getValue() != null)
						editor.setValue(instance.getValue());
				}
			}
		}
	} // setEditorAttribute

	public int saveAttributes()
	{
		MAttributeSetInstance m_masi = new MAttributeSetInstance(Env.getCtx(), asiID, M_AttributeSet_ID, null);
		m_masi.saveEx();

		asiID = m_masi.getM_AttributeSetInstance_ID();

		MAttribute[] attributes = mAttributeSet.getMAttributes(false);
		for (int i = 0; i < attributes.length; i++)
		{
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attributes[i].getAttributeValueType()))
			{
				WEditor editor = (WEditor) m_editors.get(i);
				Object item = editor.getValue();
				MAttributeValue value = (item != null && Integer.valueOf(String.valueOf(item)) > 0) ? new MAttributeValue(Env.getCtx(),
								Integer.valueOf(String.valueOf(item)),
								null) : null;
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && value == null)
				{
					Clients.scrollIntoView(editor.getComponent());
					throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
				}

				attributes[i].setMAttributeInstance(asiID, value);
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attributes[i].getAttributeValueType()))
			{
				WEditor editor = (WEditor) m_editors.get(i);
				BigDecimal value = (BigDecimal) editor.getValue();
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && value == null)
				{
					Clients.scrollIntoView(editor.getComponent());
					throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
				} // setMAttributeInstance doesn't work without decimal point
				if (value != null && value.scale() == 0)
					value = value.setScale(1, BigDecimal.ROUND_HALF_UP);

				attributes[i].setMAttributeInstance(asiID, value);
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Reference.equals(attributes[i].getAttributeValueType()))
			{
				setEditorValue(attributes[i], m_editors.get(i));
			}
			else
			{
				WEditor editor = m_editors.get(i);
				String value = String.valueOf(editor.getValue());
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && (value == null || value.length() == 0))
					throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);

				attributes[i].setMAttributeInstance(asiID, value);
			}
			m_changed = true;
		} // for all attributes

		// Save Model
		if (m_changed)
		{
			m_masi.setM_AttributeSet_ID(M_AttributeSet_ID);
			// m_masi.setDescription();
			m_masi.save();
		}
		asiID = m_masi.getM_AttributeSetInstance_ID();
		return asiID;
	} // saveAttributes

	public void setEditableAttribute(boolean isEditable)
	{
		for (WEditor editor : m_editors)
		{
			editor.setReadWrite(isEditable);
		}
	}

	private void setEditorValue(MAttribute attributes, WEditor editor)
	{
		int displayType = editor.getGridField().getDisplayType();
		if (displayType == DisplayType.YesNo)
		{
			String value = (boolean) editor.getValue() ? "Y" : "N";
			attributes.setMAttributeInstance(asiID, value);
		}
		else if (displayType == DisplayType.Date || displayType == DisplayType.DateTime || displayType == DisplayType.Time)
		{
			Timestamp valueTimeStamp = (Timestamp) editor.getValue();
			if (attributes.isMandatory() && valueTimeStamp == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			attributes.setMAttributeInstance(asiID, valueTimeStamp);
		}
		else if (DisplayType.isNumeric(displayType))
		{
			Object value = editor.getValue();
			if (attributes.isMandatory() && value == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			if (displayType == DisplayType.Integer)
				attributes.setMAttributeInstance(asiID, value == null ? 0 : ((Number) value).intValue(), null);
			else
				attributes.setMAttributeInstance(asiID, (BigDecimal) value);
		}
		else if (displayType == DisplayType.Image
					|| displayType == DisplayType.Assignment
						|| displayType == DisplayType.Locator
						|| displayType == DisplayType.Payment
						|| displayType == DisplayType.TableDir
						|| displayType == DisplayType.Table
						|| displayType == DisplayType.Search
						|| displayType == DisplayType.Account)
		{
			Integer value = (Integer) editor.getValue();
			if (attributes.isMandatory() && value == null)
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			String valueLabel = null;
			if (displayType == DisplayType.TableDir
				|| displayType == DisplayType.Table
					|| displayType == DisplayType.Search
					|| displayType == DisplayType.Account)
			{
				valueLabel = editor.getDisplay();
			}

			attributes.setMAttributeInstance(asiID, value == null ? 0 : value.intValue(), valueLabel);
		}
		else
		{
			String value = null;
			if (editor.getValue() == null || Util.isEmpty(editor.getValue().toString(), true))
				value = null;
			else
				value = String.valueOf(editor.getValue());

			if (attributes.isMandatory() && Util.isEmpty(value))
			{
				Clients.scrollIntoView(editor.getComponent());
				throw new WrongValueException(editor.getComponent(), DMSConstant.MSG_FILL_MANDATORY);
			}

			attributes.setMAttributeInstance(asiID, value);
		}
	} // setEditorValue

}
