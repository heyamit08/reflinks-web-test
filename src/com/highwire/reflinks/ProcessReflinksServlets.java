package com.highwire.reflinks;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;





/**
 * Servlet implementation class ProcessReflinksServlets
 */

public class ProcessReflinksServlets extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	private void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		{
			
			response.setContentType("text/plain");
			PrintWriter out = response.getWriter();
			
			
			/**
			 * 1 - get all the request parameters and store in some data
			 * structure 2 - based on passed parameter (evaluate) which method
			 * to invoke (arXiv, DOI, DateRange specific. 3 - Call appropriate
			 * method based on type of request.
			 */

			Map<String, List<String>> parsedParameterList = parseQueryString(request);
			/*Set<Map.Entry<String, List<String>>> entrySet = parsedParameterList
					.entrySet();

			Iterator<Map.Entry<String, List<String>>> itr = entrySet.iterator();

			while (itr.hasNext()) {
				Map.Entry<String, List<String>> entry = itr.next();
				System.out.println("Param :: " + entry.getKey() + " value :: "
						+ Arrays.asList(entry.getValue()));

			}*/
			
			/**
			 * 2 : Switch case
			 */
			
			
			
			if(parsedParameterList.get("arXiv")!=null)
			{
				/**
				 * Process "/arXiv/{arXiv-id}" ,
				 * "/arXiv/{arXiv-id-1},{arXiv-id-2},...,{arXiv-id-N}"
				 */
				Document doc = processArXivList(parsedParameterList
						.get("arXiv"));

				DOMSource domSource = new DOMSource(doc);
				StringWriter writer = new StringWriter();
				StreamResult result = new StreamResult(writer);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer;
				try {
					transformer = tf.newTransformer();
					transformer.transform(domSource, result);
				} catch (TransformerConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				out.println(writer.toString());
			}
			if(parsedParameterList.get("doi")!=null)
			{
				/**
				 * Need to implement the logic for handling these cases.
				 */
			}
			

			
			out.close();

		}

	}

	private Map<String, List<String>> parseQueryString(
			HttpServletRequest request) {

		Map<String, List<String>> parameterList = new HashMap<String, List<String>>();

		String pathInfo = request.getPathInfo();
		if(pathInfo!=null)
		{
			StringTokenizer pathInfoTokenizer = new StringTokenizer(pathInfo, "/");
			while (pathInfoTokenizer.hasMoreTokens()) {
				String dataToSplit = pathInfoTokenizer.nextToken();
				StringTokenizer dataToSplitTokenizer = new StringTokenizer(
						dataToSplit, ",");
				List<String> tokenList = new ArrayList<String>();
				while (dataToSplitTokenizer.hasMoreElements()) {
					String token = dataToSplitTokenizer.nextToken();
					tokenList.add(token);
				}
				parameterList.put("arXiv", tokenList);
			}
		}

		Enumeration<String> requestParameter = request.getParameterNames();
		while (requestParameter.hasMoreElements()) {
			String param = requestParameter.nextElement();
			parameterList.put(param,
					Arrays.asList(request.getParameterValues(param)));
		}
		return parameterList;

	}
	
	private Document processArXivList(List<String> list)
	{
		
		List<ArXivResponse> results = new ArrayList<ArXivResponse>();
		try {
			Connection conn =  DabaBaseAccessObject.getConnection();
			String linkids = "'"+StringUtils.join(list, "','")+"'";
			String sqlQuery = "select doi.resource_key, doi.link_id as doi_id, arxiv.link_id as arxiv_id,j.journal_title  from (select * from citation_link " 
				 + " where link_type_id =3 ) as doi join "  
				 + " (select * from citation_link "
				 + " where link_type_id =9 ) as arxiv on doi.resource_key = arxiv.resource_key " 
				+ " join citation c on c.resource_key = doi.resource_key " 
				 + "join journal j on j.journal_key = c.journal_key " 
				 + " where arxiv.link_id in (" + linkids + ")";
			
			System.out.println("sqlQuery :: " + sqlQuery);
			Statement stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(sqlQuery);		
			
			while(resultSet.next())
			{
				ArXivResponse arXivResponse = new ArXivResponse();
				
				arXivResponse.setArXivId(resultSet.getString("arxiv_id"));
				arXivResponse.setDoiId(resultSet.getString("doi_id"));
				arXivResponse.setJournalTitle(resultSet.getString("journal_title"));
				results.add(arXivResponse);
			}			
			
		} catch (SQLException e) {
			
			e.printStackTrace();
		}
		
		return generateResponseXML(results);
		
	}
	
	private Document generateResponseXML(List<ArXivResponse> arXivResponseList)
 {

		Document doc = null;
		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();

			Element preprintElement = doc.createElement("preprint");
			doc.appendChild(preprintElement);

			Attr attr = doc.createAttribute("identifier");
			attr.setValue("To be decided");
			preprintElement.setAttributeNode(attr);

			attr = doc.createAttribute("version");
			attr.setValue("0.1");
			preprintElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns");
			attr.setValue("http://arxiv.org/doi_feed");
			preprintElement.setAttributeNode(attr);

			attr = doc.createAttribute("xmlns:xsi");
			attr.setValue("http://www.w3.org/2001/XMLSchema-instance");
			preprintElement.setAttributeNode(attr);

			attr = doc.createAttribute("xsi:schemaLocation");
			attr.setValue("http://arxiv.org/doi_feed http://arxiv.org/schemas/doi_feed.xsd");
			preprintElement.setAttributeNode(attr);

			Element dateElement = doc.createElement("date");
			preprintElement.appendChild(dateElement);

			attr = doc.createAttribute("year");
			attr.setValue("2013");
			dateElement.setAttributeNode(attr);

			attr = doc.createAttribute("month");
			attr.setValue("06");
			dateElement.setAttributeNode(attr);

			attr = doc.createAttribute("day");
			attr.setValue("25");
			dateElement.setAttributeNode(attr);

			Iterator<ArXivResponse> arXivResponseListItr = arXivResponseList
					.iterator();
			while (arXivResponseListItr.hasNext()) {
				ArXivResponse arXivResponse = arXivResponseListItr.next();

				Element articleElement = doc.createElement("article");

				attr = doc.createAttribute("preprint_id");
				attr.setValue(arXivResponse.getArXivId());
				articleElement.setAttributeNode(attr);

				attr = doc.createAttribute("doi");
				attr.setValue(arXivResponse.getDoiId());
				articleElement.setAttributeNode(attr);

				attr = doc.createAttribute("journal_ref");
				attr.setValue(arXivResponse.getJournalTitle());
				articleElement.setAttributeNode(attr);

				preprintElement.appendChild(articleElement);

			}

		} catch (ParserConfigurationException e) {

			e.printStackTrace();
			return null;
		}

		return doc;

	}
	
	
	
	

}
