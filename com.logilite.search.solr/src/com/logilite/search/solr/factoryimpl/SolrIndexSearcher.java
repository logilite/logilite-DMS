/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP * This program is free software;
 * you can redistribute it and/or modify it * under the terms version 2 of the
 * GNU General Public License as published * by the Free Software Foundation.
 * This program is distributed in the hope * that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. * See the GNU General Public License for
 * more details. * You should have received a copy of the GNU General Public
 * License along * with this program; if not, write to the Free Software
 * Foundation, Inc., * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA. *
 *****************************************************************************/

package com.logilite.search.solr.factoryimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.compiere.util.CLogger;

import com.idempiere.model.MIndexingConfig;
import com.logilite.search.factory.IIndexSearcher;

public class SolrIndexSearcher implements IIndexSearcher
{

	public static CLogger	log				= CLogger.getCLogger(SolrIndexSearcher.class);

	@SuppressWarnings("deprecation")
	private HttpSolrServer	server			= null;
	private MIndexingConfig	indexingConfig	= null;

	@SuppressWarnings("deprecation")
	@Override
	public void init(MIndexingConfig indexingConfig)
	{

		try
		{
			this.indexingConfig = indexingConfig;
			PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(
					SchemeRegistryFactory.createDefault());
			cxMgr.setMaxTotal(100);
			cxMgr.setDefaultMaxPerRoute(20);

			DefaultHttpClient httpclient = new DefaultHttpClient(cxMgr);
			httpclient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
			httpclient.getCredentialsProvider().setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(indexingConfig.getUserName(), indexingConfig.getPassword()));

			server = new HttpSolrServer(indexingConfig.getIndexServerUrl(), httpclient);
			server.setRequestWriter(new BinaryRequestWriter());
			server.setAllowCompression(true);

			server.ping();

		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server is not started ", e);
			throw new AdempiereException("Solr server is not started: " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Fail to initialize solr Server OR Invalid Username or Password ", e);
			throw new AdempiereException("Fail to initialize solr Server OR Invalid Username or Password ");
		}
	}

	@Override
	public List<Integer> searchIndex(String query)
	{
		try
		{
			if (server.ping() == null)
				init(indexingConfig);
		}
		catch (SolrServerException | IOException e)
		{
			log.log(Level.SEVERE, "Fail to ping solr Server ", e);
			throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
		}

		SolrQuery solrQuery = new SolrQuery();
		QueryResponse response = new QueryResponse();
		SolrDocumentList documentList = null;
		List<Integer> dmsContentList = new ArrayList<Integer>();

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
				ListIterator<SolrDocument> iterator = documentList.listIterator();

				while (iterator.hasNext())
				{
					current++;

					SolrDocument document = iterator.next();

					Map<String, Collection<Object>> searchedContent = document.getFieldValuesMap();

					Iterator<String> fields = document.getFieldNames().iterator();

					while (fields.hasNext())
					{
						String field = fields.next();

						if (field.equalsIgnoreCase("DMS_Content_ID"))
						{
							Collection<Object> values = searchedContent.get(field);
							Iterator<Object> value = values.iterator();

							while (value.hasNext())
							{
								Long obj = (Long) value.next();

								if (field.equalsIgnoreCase("DMS_Content_ID"))
								{
									DMS_Content_ID = obj.intValue();
								}
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
			//throw new AdempiereException("Searching content failure:" + e);
		}

		return dmsContentList;
	}

	@Override
	public void indexContent(Map<String, Object> indexValue)
	{
		try
		{
			try
			{
				if (server.ping() == null)
					init(indexingConfig);
			}
			catch (SolrServerException | IOException e)
			{
				log.log(Level.SEVERE, "Fail to ping solr Server ", e);
				throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
			}

			SolrInputDocument document = new SolrInputDocument();

			for (Entry<String, Object> row : indexValue.entrySet())
			{
				if (row.getKey() != null && row.getValue() != null)
				{
					document.addField(row.getKey(), row.getValue());
				}
			}
			server.add(document);
			server.commit();
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Indexing failure: ", e);
			throw new AdempiereException("Indexing failure: " + e.getLocalizedMessage());
		}
	}

	@Override
	public void deleteIndex(int id)
	{
		try
		{
			try
			{
				if (server.ping() == null)
					init(indexingConfig);
			}
			catch (SolrServerException | IOException e)
			{
				log.log(Level.SEVERE, "Fail to ping solr Server ", e);
				throw new AdempiereException("Fail to ping solr Server: " + e.getLocalizedMessage());
			}

			server.deleteByQuery("DMS_Content_ID:" + id);
			server.commit();
		}
		catch (SolrServerException e)
		{
			log.log(Level.SEVERE, "Solr server connection failure: ", e);
			throw new AdempiereException("Solr server connection failure:  " + e.getLocalizedMessage());
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Solr Document delete failure: ", e);
			throw new AdempiereException("Solr Document delete failure:  " + e.getLocalizedMessage());
		}

	}

	private class PreemptiveAuthInterceptor implements HttpRequestInterceptor
	{

		@SuppressWarnings("deprecation")
		public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
		{
			AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

			// If no auth scheme avaialble yet, try to initialize it
			// preemptively
			if (authState.getAuthScheme() == null)
			{
				CredentialsProvider credsProvider = (CredentialsProvider) context
						.getAttribute(ClientContext.CREDS_PROVIDER);
				HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
				Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost
						.getPort()));
				if (creds == null)
					throw new HttpException("No credentials for preemptive authentication");
				authState.setAuthScheme(new BasicScheme());
				authState.setCredentials(creds);
			}

		}

	}

	@SuppressWarnings("null")
	@Override
	public String buildSolrSearchQuery(HashMap<String, List<Object>> params)
	{
		StringBuffer query = new StringBuffer();

		for (Entry<String, List<Object>> row : params.entrySet())
		{
			String key = row.getKey();
			List<Object> value = row.getValue();

			if (value.size() == 2)
			{
				if (value.get(1).equals("*"))
					query.append(" AND ").append(key + ":[\"" + value.get(0) + "\" TO " + value.get(1) + " ]");
				else
					query.append(" AND ").append(key + ":[\"" + value.get(0) + "\" TO \"" + value.get(1) + "\" ]");
			}
			else
			{
				query.append(" AND ").append(key + ":\"" + value.get(0) + "\"");
			}
		}

		if (query.length() > 0)
			query.delete(0, 5);
		else
			query.append("*:*");

		return query.toString();
	}

}
