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

package com.logilite.search.solr.factoryimpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.compiere.util.CLogger;
import org.compiere.util.Util;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.idempiere.model.MIndexingConfig;
import com.logilite.search.factory.IIndexSearcher;
import com.logilite.search.factory.ServiceUtils;

@SuppressWarnings("deprecation")
public class SolrIndexSearcher implements IIndexSearcher
{

	public static CLogger		log						= CLogger.getCLogger(SolrIndexSearcher.class);

	public static final String	SOLR_FIELDTYPE_TLONGS	= "tlongs";

	public static final String	SOLR_FIELD_DESCRIPTION	= "description";

	private HttpSolrServer		server					= null;
	private MIndexingConfig		indexingConfig			= null;

	public HashSet <String>		fieldSet				= new HashSet <String>();
	public HashSet <String>		fieldTypeSet			= new HashSet <String>();

	@Override
	public void init(MIndexingConfig indexingConfig)
	{
		try
		{
			this.indexingConfig = indexingConfig;
			PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
			cxMgr.setMaxTotal(100);
			cxMgr.setDefaultMaxPerRoute(20);

			DefaultHttpClient httpclient = new DefaultHttpClient(cxMgr);
			httpclient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(indexingConfig.getUserName(), indexingConfig.getPassword()));

			server = new HttpSolrServer(indexingConfig.getIndexServerUrl(), httpclient);
			server.setRequestWriter(new BinaryRequestWriter());
			server.setAllowCompression(true);
			server.ping();

			try
			{
				/*
				 * Build Schema Fields Type set
				 */
				buildSolrSchemaFieldsTypeSet();

				// Create Fields Type in schema if not exists
				createOrCheckFieldsTypeInSolrSchema(SOLR_FIELDTYPE_TLONGS);

				/*
				 * Build Schema Fields set
				 */
				buildSolrSchemaFieldsSet();

				// Create Fields in schema if not exists
				createOrCheckFieldsInSolrSchema(ServiceUtils.DMS_CONTENT_ID);
				createOrCheckFieldsInSolrSchema(ServiceUtils.FILE_CONTENT);
				createOrCheckFieldsInSolrSchema(SOLR_FIELD_DESCRIPTION);
			}
			catch (Exception e)
			{
				log.log(Level.SEVERE, "Issue while build/create fieldtype/field in solr schema: ", e.getLocalizedMessage());
				throw new AdempiereException("Issue while build/create fieldtype/field in solr schema: " + e.getLocalizedMessage(), e);
			}
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started: ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage(), e);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to initialize solr Server OR Invalid Username or Password ", e);
			throw new AdempiereException("Fail to initialize solr Server OR Invalid Username or Password.", e);
		}
	} // init

	/**
	 * Build Solr schema for Fields
	 * 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void buildSolrSchemaFieldsSet() throws SolrServerException, IOException
	{
		final SchemaRequest.Fields fieldsSchemaRequest = new SchemaRequest.Fields();
		SchemaResponse.FieldsResponse fieldsResponse1 = fieldsSchemaRequest.process(server);
		List <Map <String, Object>> fields = fieldsResponse1.getFields();
		for (Map <String, Object> map : fields)
		{
			fieldSet.add((String) map.get("name"));
		}
	} // buildSolrSchemaFieldsMap

	/**
	 * Build Solr schema for FieldType
	 * 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void buildSolrSchemaFieldsTypeSet() throws SolrServerException, IOException
	{
		SchemaRequest.FieldTypes fTypes = new SchemaRequest.FieldTypes();
		SchemaResponse.FieldTypesResponse ftRes = fTypes.process(server);

		List <FieldTypeRepresentation> fieldTypes = ftRes.getFieldTypes();
		for (FieldTypeRepresentation ftRepr : fieldTypes)
		{
			Map <String, Object> ftAttrib = ftRepr.getAttributes();
			fieldTypeSet.add(ftAttrib.get("name").toString());
		}
	} // buildSolrSchemaFieldsTypeSet

	/**
	 * Create FieldType if not exists in solr schema
	 * 
	 * @param fieldTypeName
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void createOrCheckFieldsTypeInSolrSchema(String fieldTypeName) throws SolrServerException, IOException
	{
		Map <String, Object> mapAttribute = new HashMap <String, Object>();
		if (!fieldTypeSet.contains(fieldTypeName) && SOLR_FIELDTYPE_TLONGS.equals(fieldTypeName))
		{
			mapAttribute.put("name", SOLR_FIELDTYPE_TLONGS);
			mapAttribute.put("class", "solr.TrieLongField");
			mapAttribute.put("precisionStep", "8");
			mapAttribute.put("multiValued", "true");
			mapAttribute.put("positionIncrementGap", "0");
		}

		if (mapAttribute.size() > 0)
		{
			FieldTypeDefinition ftDefinition = new FieldTypeDefinition();
			ftDefinition.setAttributes(mapAttribute);

			SchemaRequest.AddFieldType ft = new SchemaRequest.AddFieldType(ftDefinition);
			ft.process(server);

			//
			buildSolrSchemaFieldsTypeSet();
		}
	} // createOrCheckFieldsTypeInSolrSchema

	/**
	 * Create Field if not exists in solr schema
	 * 
	 * @param fieldName
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public void createOrCheckFieldsInSolrSchema(String fieldName) throws SolrServerException, IOException
	{
		Map <String, Object> mapAttribute = new HashMap <String, Object>();
		if (fieldName.equalsIgnoreCase(ServiceUtils.FILE_CONTENT) && !fieldSet.contains(ServiceUtils.FILE_CONTENT))
		{
			mapAttribute.put("name", ServiceUtils.FILE_CONTENT);
			mapAttribute.put("type", "text_general");
			mapAttribute.put("indexed", true);
			mapAttribute.put("stored", false);
			mapAttribute.put("multiValued", true);
		}
		else if (fieldName.equalsIgnoreCase(ServiceUtils.DMS_CONTENT_ID) && !fieldSet.contains(ServiceUtils.DMS_CONTENT_ID))
		{
			mapAttribute.put("name", ServiceUtils.DMS_CONTENT_ID);
			mapAttribute.put("type", SOLR_FIELDTYPE_TLONGS);
		}
		else if (fieldName.equalsIgnoreCase(SOLR_FIELD_DESCRIPTION) && !fieldSet.contains(SOLR_FIELD_DESCRIPTION))
		{
			mapAttribute.put("name", SOLR_FIELD_DESCRIPTION);
			mapAttribute.put("type", "text_general");
		}

		if (mapAttribute.size() > 0)
		{
			SchemaRequest.AddField schemaRequest = new SchemaRequest.AddField(mapAttribute);
			schemaRequest.process(server);

			//
			buildSolrSchemaFieldsSet();
		}
	} // createOrCheckFieldsInSolrSchema

	/**
	 * Check Server is Up and running
	 * 
	 * @throws AdempiereException
	 */
	public void checkServerIsUp() throws AdempiereException
	{
		try
		{
			if (server.ping() == null)
				init(indexingConfig);
		}
		catch (SolrServerException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage(), e);
		}
	} // checkServerIsUp

	@Override
	public List <Integer> searchIndex(String query)
	{
		checkServerIsUp();

		SolrQuery solrQuery = new SolrQuery();
		QueryResponse response = new QueryResponse();
		SolrDocumentList documentList = null;
		List <Integer> dmsContentList = new ArrayList <Integer>();

		long numbFound = 0;
		int current = 0;
		int DMS_Content_ID = 0;

		try
		{
			solrQuery.setQuery(query);
			response = server.query(solrQuery);
			documentList = response.getResults();
			numbFound = documentList.getNumFound();

			while (current < numbFound)
			{
				ListIterator <SolrDocument> iterator = documentList.listIterator();

				while (iterator.hasNext())
				{
					current++;

					SolrDocument document = iterator.next();
					Map <String, Collection <Object>> searchedContent = document.getFieldValuesMap();
					Iterator <String> fields = document.getFieldNames().iterator();
					while (fields.hasNext())
					{
						String field = fields.next();

						if (field.equalsIgnoreCase(ServiceUtils.DMS_CONTENT_ID))
						{
							Collection <Object> values = searchedContent.get(field);
							Iterator <Object> value = values.iterator();

							while (value.hasNext())
							{
								Long obj = (Long) value.next();
								DMS_Content_ID = obj.intValue();
							}
						}
					}
					dmsContentList.add(DMS_Content_ID);
				}
				solrQuery.setStart(current);
				response = server.query(solrQuery);
				documentList = response.getResults();
				numbFound = documentList.getNumFound();
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Searching content failure:", e);
		}
		return dmsContentList;
	}

	@Override
	public void indexContent(Map <String, Object> indexValue)
	{
		indexContent(indexValue, null);
	}

	@Override
	public void indexContent(Map <String, Object> indexValue, File file)
	{
		checkServerIsUp();

		try
		{
			String content = (String) indexValue.get(ServiceUtils.FILE_CONTENT);
			SolrInputDocument document = new SolrInputDocument();

			for (Entry <String, Object> row : indexValue.entrySet())
			{
				if (row.getKey() != null && row.getValue() != null)
				{
					document.addField(row.getKey(), row.getValue());
				}
			}

			// Inside document content searching
			if (ServiceUtils.isAllowDocumentContentSearch() && Util.isEmpty(content) && file != null)
			{
				// Read content from file
				content = processDocument(file);

				document.addField(ServiceUtils.FILE_CONTENT, content);
			}

			server.add(document);
			server.commit();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to create Indexing: ", e);
			throw new AdempiereException("Fail to create Indexing: " + e.getLocalizedMessage(), e);
		}
	} // indexContent

	@Override
	public void deleteIndex(int content_ID)
	{
		checkServerIsUp();

		try
		{
			server.deleteByQuery("DMS_Content_ID:" + content_ID);
			server.commit();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure: ", e);
			throw new AdempiereException("Solr server connection failure: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr Document delete failure: ", e);
			throw new AdempiereException("Solr Document delete failure: " + e.getLocalizedMessage(), e);
		}
	} // deleteIndex

	@Override
	public String buildSolrSearchQuery(HashMap <String, List <Object>> params)
	{
		StringBuffer query = new StringBuffer();

		for (Entry <String, List <Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List <Object> value = row.getValue();

			if (value.size() == 2)
			{
				if (value.get(0) instanceof String || value.get(1) instanceof String)
				{
					query.append(" AND (").append(key + ":[" + value.get(0) + " TO " + value.get(1) + " ])");
				} // Handle condition when two boolean value passed.
				else if (value.get(0) instanceof Boolean || value.get(1) instanceof Boolean)
				{
					query.append(" AND (").append(key + ":" + value.get(0) + " OR ").append(key + ":" + value.get(1) + ")");
				}
				else if (value.get(1).equals("*"))
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ])");
				else
					query.append(" AND (").append(key + ":[\"" + value.get(0) + "\" TO \"" + value.get(1) + "\" ])");
			}
			else
			{
				if (value.get(0) instanceof String)
					query.append(" AND (").append(key + ":*" + value.get(0) + "*)");
				else
					query.append(" AND (").append(key + ":\"" + value.get(0) + "\")");
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);
		else
			query.append("*:*");

		return query.toString();
	} // buildSolrSearchQuery

	@Override
	public String getColumnValue(String query, String columnName)
	{
		checkServerIsUp();

		String content = "";
		try
		{
			SolrQuery solrQuery = new SolrQuery();
			QueryResponse response = new QueryResponse();

			solrQuery.setQuery(query);
			response = server.query(solrQuery);
			SolrDocumentList documentList = response.getResults();
			ListIterator <SolrDocument> iterator = documentList.listIterator();
			while (iterator.hasNext())
			{
				SolrDocument document = iterator.next();
				Map <String, Collection <Object>> searchedContent = document.getFieldValuesMap();
				Iterator <String> fields = document.getFieldNames().iterator();
				while (fields.hasNext())
				{
					String field = fields.next();
					if (field.equalsIgnoreCase(columnName))
					{
						Collection <Object> values = searchedContent.get(field);
						Iterator <Object> value = values.iterator();

						while (value.hasNext())
						{
							String obj = (String) value.next();
							content = obj.toString();
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Searching content failure:", e);
		}

		return content;
	} // getColumnValue

	/**
	 * @param file
	 * @return
	 */
	private String processDocument(File file)
	{
		try
		{
			Metadata metadata = new Metadata();
			ContentHandler handler = new BodyContentHandler(-1);
			TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
			AutoDetectParser parser = new AutoDetectParser(tikaConfig);
			TikaInputStream stream = TikaInputStream.get(new FileInputStream(file));

			parser.parse(stream, handler, metadata, new ParseContext());
			stream.close();

			return handler.toString();
		}
		catch (FileNotFoundException e)
		{
			log.log(Level.SEVERE, "File Not Found: ", e);
			throw new AdempiereException("File Not Found: " + e.getLocalizedMessage(), e);
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to read file: ", e);
			throw new AdempiereException("Fail to read file: " + e.getLocalizedMessage(), e);
		}
		catch (SAXException e)
		{
			log.log(Level.SEVERE, "Fail to XML parser: ", e);
			throw new AdempiereException("Fail to XML parser: " + e.getLocalizedMessage(), e);
		}
		catch (TikaException e)
		{
			log.log(Level.SEVERE, "Fail to parse file content: ", e);
			throw new AdempiereException("Fail to parse file content: " + e.getLocalizedMessage(), e);
		}
	} // processDocument

	/**
	 * Class PreemptiveAuthInterceptor
	 */
	private class PreemptiveAuthInterceptor implements HttpRequestInterceptor
	{
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
		{
			AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

			// If no auth scheme available yet, try to initialize it preemptively
			if (authState.getAuthScheme() == null)
			{
				CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
				Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
				if (creds == null)
					throw new HttpException("No credentials for preemptive authentication");
				authState.setAuthScheme(new BasicScheme());
				authState.setCredentials(creds);
			}
		} // process
	} // PreemptiveAuthInterceptor
}
