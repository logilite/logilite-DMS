package com.logilite.dms.test.form;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.component.Borderlayout;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import com.logilite.dms.component.DMSContentBox;
import com.logilite.dms.component.DMSGalleryBox;
import com.logilite.dms.constant.DMSConstant;

/**
 * DMS component sample form
 * Purpose: For developer learning how to implement easily
 */
public class DMSComponentSampleForm extends ADForm
{
	private static final long		serialVersionUID	= 1L;
	private static final CLogger	log					= CLogger.getCLogger(DMSComponentSampleForm.class);

	Borderlayout					borderLayout		= new Borderlayout();

	DMSGalleryBox					galleryBox;

	@Override
	protected void initForm()
	{
		this.setWidth("100%");
		this.setHeight("100%");
		this.setSizable(true);
		this.setClosable(true);
		this.setMaximizable(true);
		this.appendChild(borderLayout);

		Center center = new Center();

		borderLayout.setWidth("100%");
		borderLayout.setHeight("100%");
		borderLayout.appendChild(center);

		Grid grid = new Grid();
		Rows rows = new Rows();
		grid.appendChild(rows);

		Div scrollableDiv = new Div();
		scrollableDiv.setStyle("overflow-y: auto; height: 100%; width: 100%;");
		scrollableDiv.appendChild(grid);

		center.appendChild(scrollableDiv);

		// Gallery Box Description
		String galleryBoxDesc = "<h1> 1) Gallery Box [ DMSGalleryBox ]</h1>"
								+ "		<p>The <strong>DMSGalleryBox</strong> is a versatile component designed to preview content in a gallery format with added customization options.</p>"
								+ "     <h2>Features</h2>"
								+ "			<ul class=\"features\">"
								+ "				<li>Displays content in a gallery view with tooltips or detailed information when hovering over items.</li>"
								+ "				<li>Includes an <strong>Upload File</strong> button for uploading content. This can be enabled or disabled based on preference (default is disabled).</li>"
								+ "				<li>Allows rendering and filtering of content using various configuration fields.</li>"
								+ "			</ul>"
								+ "    <h2>Configuration Fields</h2>"
								+ "			<ul class=\"config-fields\">"
								+ "				<li><strong>WindowNo:</strong> Used for parsing context if required.</li>"
								+ "				<li><strong>Table ID:</strong> Specifies the table to fetch content from.</li>"
								+ "				<li><strong>Record ID:</strong> Refers to a specific record in the selected table.</li>"
								+ "				<li><strong>DocExpWindow:</strong> Acts as the Document Explorer Window (root explorer form).</li>"
								+ "				<li><strong>Content Type ID:</strong> Fetches and uploads content based on the specified type. The upload dialog auto-selects the specified <code>contentType</code>.</li>"
								+ "				<li><strong>QueryParams:</strong> Allows preparation of custom queries for advanced filtering of content.</li>"
								+ "				<li><strong>Upload:</strong> Enables or disables the <strong>Upload File</strong> button.</li>"
								+ "				<li><strong>getContent_ID():</strong> Retrieves the latest <code>Content_ID</code> when a new content item is uploaded using the upload button.</li>"
								+ "				<li><strong>renderViewer():</strong> Once all the applicable fields are configured, this method loads and renders the content.</li>"
								+ "			</ul>"
								+ "    		<p>This configuration flexibility ensures the <strong>DMSGalleryBox</strong> component can adapt to various use cases while maintaining simplicity in implementation.</p>";

		Html descGallery = new Html(galleryBoxDesc);
		rows.newRow().appendCellChild(descGallery, 4);

		//
		galleryBox = new DMSGalleryBox(this, 0, "");
		galleryBox.enableUploadButton(true);
		galleryBox.setTableID(114);
		galleryBox.setRecordID(100);
		galleryBox.setWindowNo(getWindowNo());
		galleryBox.addEventListener(DMSConstant.EVENT_ON_UPLOAD_COMPLETE, this);
		galleryBox.renderViewer();

		rows.newRow().appendCellChild(galleryBox, 4);

		/*
		 * Content Box Label and Description
		 */
		String contentBoxDesc = " <h1> 2) Content Box [ DMSContentBox ]</h1>"
								+ "    <p>The <strong>DMSContentBox</strong> is a versatile component designed to preview content in a thumbnail format with added customization options.</p>"
								+ "    <h2>Features</h2>"
								+ "    <ul class=\"features\">"
								+ "        <li>Displays content in a thumbnail view with tooltips or detailed information when hovering over items.</li>"
								+ "        <li>Allows rendering content using various configuration fields.</li>"
								+ "    </ul>"
								+ "    <h2>Configuration Fields</h2>"
								+ "    <ul class=\"config-fields\">"
								+ "        <li><strong>Table ID:</strong> Specifies the table to fetch content from.</li>"
								+ "        <li><strong>Record ID:</strong> Refers to a specific record in the selected table.</li>"
								+ "        <li><strong>getContent_ID():</strong> Retrieves the latest <code>Content_ID</code> when a new content item is uploaded using the upload button.</li>"
								+ "        <li><strong>renderViewer():</strong> Once all the applicable fields are configured, this method loads and renders the content.</li>"
								+ "        <li><strong>openContentViewer():</strong> Attempts to retrieve selected documents from the DMS storage and convert them into a format (PDF) for preview. If a suitable content editor is available, the document is displayed in an explorer tab for previewing.</li>"
								+ "    </ul>"
								+ "    <p>This configuration flexibility ensures the <strong>DMSContentBox</strong> component can adapt to various use cases while maintaining simplicity in implementation.</p>";

		Html descContent = new Html(contentBoxDesc);
		rows.newRow().appendCellChild(descContent, 4);

		// Fetch user-specific records
		Map<Integer, List<Map.Entry<Integer, Integer>>> records = getSampleContents();
		if (!records.isEmpty())
		{
			for (Map.Entry<Integer, List<Map.Entry<Integer, Integer>>> contentEntry : records.entrySet())
			{
				List<Map.Entry<Integer, Integer>> recordList = contentEntry.getValue();
				for (int i = 0; i < recordList.size(); i++)
				{
					Map.Entry<Integer, Integer> record = recordList.get(i);
					Row contentRow = rows.newRow();
					// For Preview Content Box Component
					DMSContentBox contentBox = new DMSContentBox("");
					contentRow.appendCellChild(contentBox, 4);
					//
					contentBox.setTableID(record.getKey());
					contentBox.setRecordID(record.getValue());
					contentBox.setContent_ID(contentEntry.getKey());
					contentBox.renderViewer();
				}
			}
		}
	} // initForm

	@Override
	public void onEvent(Event event) throws Exception
	{
		if (event.getName() == DMSConstant.EVENT_ON_UPLOAD_COMPLETE)
		{
			galleryBox.renderViewer();
		}
	} // onEvent

	private Map<Integer, List<Entry<Integer, Integer>>> getSampleContents()
	{
		Map<Integer, List<Map.Entry<Integer, Integer>>> records = new HashMap<>();

		String sql_For_Get_Contents = "SELECT 114 AS AD_Table_ID, u.AD_User_ID AS Record_ID, c.DMS_Content_ID "
										+ " FROM AD_User u "
										+ " INNER JOIN DMS_Content c ON (c.DMS_Content_ID IN (SELECT DISTINCT DMS_Content_ID FROM DMS_Association a 		"
										+ "													WHERE (a.AD_Table_ID = 114 AND a.Record_ID = u.AD_User_ID)) "
										+ "								AND c.ContentBaseType ='CNT' AND c.IsMounting ='N') "
										+ " WHERE AD_User_ID IN (100) ";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql_For_Get_Contents, null);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				int contentID = rs.getInt(DMSConstant.DMS_CONTENT_ID);
				int tableID = rs.getInt(DMSConstant.AD_TABLE_ID);
				int recordID = rs.getInt(DMSConstant.RECORD_ID);

				List<Map.Entry<Integer, Integer>> recordData = records.computeIfAbsent(contentID, k -> new ArrayList<>());
				recordData.add(new AbstractMap.SimpleEntry<>(tableID, recordID));
			}
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, "Content fetching failure :", e);
			throw new AdempiereException("Content fetching failure :" + e);
		}
		finally
		{
			DB.close(rs, pstmt);
		}
		return records;
	} // getSampleContents

}
