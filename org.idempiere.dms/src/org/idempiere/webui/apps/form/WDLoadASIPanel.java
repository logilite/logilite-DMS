package org.idempiere.webui.apps.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Column;
import org.adempiere.webui.component.Columns;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.editor.WAccountEditor;
import org.adempiere.webui.editor.WAssignmentEditor;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WDatetimeEditor;
import org.adempiere.webui.editor.WEditor;
import org.adempiere.webui.editor.WImageEditor;
import org.adempiere.webui.editor.WLocatorEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WPaymentEditor;
import org.adempiere.webui.editor.WSearchEditor;
import org.adempiere.webui.editor.WTableDirEditor;
import org.adempiere.webui.editor.WTimeEditor;
import org.adempiere.webui.editor.WYesNoEditor;
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
import org.idempiere.model.MDMSContentType;
import org.zkoss.zk.ui.Component;

public class WDLoadASIPanel extends Panel
{

	/**
	 * 
	 */
	private static final long	serialVersionUID			= -9141937878893779910L;

	private static CLogger		log							= CLogger.getCLogger(WDLoadASIPanel.class);

	private int					DMS_ContentType_ID			= 0;
	private int					m_M_AttributeSetInstance_ID	= 0;
	private int					m_WindowNo					= 0;
	private int					M_AttributeSet_ID			= 0;

	private Grid				attributeGrid				= new Grid();
	private Columns				columns						= new Columns();
	private Column				column						= new Column();
	private Rows				rows						= new Rows();

	MAttributeSet				mAttributeSet				= null;

	private boolean				m_changed					= false;

	/** List of Editors */
	private ArrayList<WEditor>	m_editors					= new ArrayList<WEditor>();

	public WDLoadASIPanel(int DMS_ContentType_ID, int m_M_AttributeSetInstance_ID)
	{
		m_WindowNo = SessionManager.getAppDesktop().registerWindow(this);
		this.DMS_ContentType_ID = DMS_ContentType_ID;
		this.m_M_AttributeSetInstance_ID = m_M_AttributeSetInstance_ID;
		initPanel();
	}

	public void initPanel()
	{
		this.setHeight("100%");
		this.setWidth("100%");
		this.appendChild(attributeGrid);

		column.setWidth("30%");
		columns.appendChild(column);

		column = new Column();
		column.setWidth("70%");
		columns.appendChild(column);

		attributeGrid.appendChild(columns);
		attributeGrid.appendChild(rows);

		initAttribute();
	}

	public void initAttribute()
	{

		MDMSContentType contentType = new MDMSContentType(Env.getCtx(), DMS_ContentType_ID, null);

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
	}

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
			editor = WebEditorFactory.getEditor(getGridField(attribute), true);
		}
		else
		// Text Field
		{
			editor = WebEditorFactory.getEditor(getStringGridField(attribute), true);
		}

		if (editor != null)
		{
			Row row = rows.newRow();

			Label label = editor.getLabel();
			if (label.getValue() == null || label.getValue().trim().length() < 1)
				label.setValue(attribute.getName());

			row.appendChild(label.rightAlign());

			editor.setMandatory(attribute.isMandatory());
			editor.fillHorizontal();

			if (m_M_AttributeSetInstance_ID > 0)
			{
				setEditorAttribute(attribute, editor);
			}

			Component fieldEditor = editor.getComponent();
			row.appendChild(fieldEditor);

			m_editors.add(editor);
		}
	}

	private GridField getGridField(MAttribute attribute)
	{
		GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), m_WindowNo, AEnv.getADWindowID(m_WindowNo), 0, 0,
				attribute.getName(), Msg.translate(Env.getCtx(), attribute.get_Translation("Name")),
				attribute.getAD_Reference_ID(), attribute.getAD_Reference_Value_ID(), false, false);
		String desc = attribute.get_Translation("Description");
		vo.Description = desc != null ? desc : "";
		GridField gridField = new GridField(vo);
		return gridField;
	}

	private GridField getStringGridField(MAttribute attribute)
	{
		GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), m_WindowNo, AEnv.getADWindowID(m_WindowNo), 0, 0,
				attribute.getName(), Msg.translate(Env.getCtx(), attribute.get_Translation("Name")),
				DisplayType.String, 0, false, false);
		String desc = attribute.get_Translation("Description");
		vo.Description = desc != null ? desc : "";
		GridField gridField = new GridField(vo);
		return gridField;
	}

	private GridField getNumberGridField(MAttribute attribute)
	{
		GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), m_WindowNo, AEnv.getADWindowID(m_WindowNo), 0, 0,
				attribute.getName(), Msg.translate(Env.getCtx(), attribute.get_Translation("Name")),
				DisplayType.Number, 0, false, false);
		String desc = attribute.get_Translation("Description");
		vo.Description = desc != null ? desc : "";
		GridField gridField = new GridField(vo);
		return gridField;
	}

	private GridField getListTypeGridField(MAttribute attribute)
	{
		GridFieldVO vo = GridFieldVO.createParameter(Env.getCtx(), m_WindowNo, AEnv.getADWindowID(m_WindowNo), 0, 0,
				"M_AttributeValue_ID", Msg.translate(Env.getCtx(), "M_AttributeValue_ID"), DisplayType.TableDir, 0,
				false, false);
		vo.ValidationCode = "M_AttributeValue.M_Attribute_ID=" + attribute.get_ID();
		vo.lookupInfo.ValidationCode = vo.ValidationCode;
		vo.lookupInfo.IsValidated = true;
		vo = GridFieldVO.createParameter(vo);
		String desc = attribute.get_Translation("Description");
		vo.Description = desc != null ? desc : "";
		GridField gridField = new GridField(vo);
		return gridField;
	}

	private void setEditorAttribute(MAttribute attribute, WEditor editor)
	{
		MAttributeInstance instance = attribute.getMAttributeInstance(m_M_AttributeSetInstance_ID);
		if (instance != null)
		{
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
			{
				if (instance.getM_AttributeValue_ID() > 0)
					editor.setValue(instance.getM_AttributeValue_ID());
			}
			else
			{
				if (editor instanceof WDateEditor || editor instanceof WTimeEditor || editor instanceof WDatetimeEditor)
				{
					if (instance.getValueTimeStamp() != null)
						editor.setValue(instance.getValueTimeStamp());
				}
				else if (editor instanceof WImageEditor || editor instanceof WAssignmentEditor
						|| editor instanceof WLocatorEditor || editor instanceof WPaymentEditor
						|| editor instanceof WTableDirEditor || editor instanceof WSearchEditor
						|| editor instanceof WAccountEditor)
				{
					if (instance.getValueInt() > 0)
						editor.setValue(instance.getValueInt() > 0);
				}
				else if (editor instanceof WNumberEditor)
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
	}

	public int saveAttributes()
	{
		String mandatory = "";

		MAttributeSetInstance m_masi = new MAttributeSetInstance(Env.getCtx(), m_M_AttributeSetInstance_ID,
				M_AttributeSet_ID, null);
		m_masi.saveEx();

		m_M_AttributeSetInstance_ID = m_masi.getM_AttributeSetInstance_ID();

		MAttribute[] attributes = mAttributeSet.getMAttributes(false);
		for (int i = 0; i < attributes.length; i++)
		{
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attributes[i].getAttributeValueType()))
			{
				WEditor editor = (WEditor) m_editors.get(i);
				Object item = editor.getValue();
				MAttributeValue value = (item != null && Integer.valueOf(String.valueOf(item)) > 0) ? new MAttributeValue(
						Env.getCtx(), Integer.valueOf(String.valueOf(item)), null) : null;
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && value == null)
					mandatory += " - " + attributes[i].getName();
				attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attributes[i].getAttributeValueType()))
			{
				WEditor editor = (WEditor) m_editors.get(i);
				BigDecimal value = (BigDecimal) editor.getValue();
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && value == null)
					mandatory += " - " + attributes[i].getName();
				// setMAttributeInstance doesn't work without decimal point
				if (value != null && value.scale() == 0)
					value = value.setScale(1, BigDecimal.ROUND_HALF_UP);
				attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Reference.equals(attributes[i].getAttributeValueType()))
			{
				WEditor editor = m_editors.get(i);
				if (editor instanceof WYesNoEditor)
				{
					String value = (boolean) editor.getValue() ? "Y" : "N";
					attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
				}
				else if (editor instanceof WDateEditor || editor instanceof WDatetimeEditor
						|| editor instanceof WTimeEditor)
				{
					Timestamp value = (Timestamp) editor.getValue();
					if (attributes[i].isMandatory() && value == null)
						mandatory += " - " + attributes[i].getName();
					attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
				}
				else if (editor instanceof WNumberEditor)
				{
					Object value = editor.getValue();
					if (attributes[i].isMandatory() && value == null)
						mandatory += " - " + attributes[i].getName();
					if (value instanceof Number)
						attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, ((Number) value).intValue());
					else
						attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, (BigDecimal) value);
				}
				else if (editor instanceof WImageEditor || editor instanceof WAssignmentEditor
						|| editor instanceof WLocatorEditor || editor instanceof WSearchEditor
						|| editor instanceof WPaymentEditor || editor instanceof WTableDirEditor
						|| editor instanceof WAccountEditor)
				{
					Integer value = (Integer) editor.getValue();
					if (attributes[i].isMandatory() && value == null)
						mandatory += " - " + attributes[i].getName();
					attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID,
							value == null ? 0 : value.intValue());
				}
				else
				{
					String value = String.valueOf(editor.getValue());
					if (attributes[i].isMandatory() && value == null)
						mandatory += " - " + attributes[i].getName();
					attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
				}
			}
			else
			{
				WEditor editor = m_editors.get(i);
				String value = String.valueOf(editor.getValue());
				if (log.isLoggable(Level.FINE))
					log.fine(attributes[i].getName() + "=" + value);
				if (attributes[i].isMandatory() && (value == null || value.length() == 0))
					mandatory += " - " + attributes[i].getName();
				attributes[i].setMAttributeInstance(m_M_AttributeSetInstance_ID, value);
			}
			m_changed = true;
		} // for all attributes

		// Save Model
		if (m_changed)
		{
			m_masi.setM_AttributeSet_ID(M_AttributeSet_ID);
			m_masi.setDescription();
			m_masi.save();
		}
		m_M_AttributeSetInstance_ID = m_masi.getM_AttributeSetInstance_ID();
		return m_M_AttributeSetInstance_ID;
	}
}
