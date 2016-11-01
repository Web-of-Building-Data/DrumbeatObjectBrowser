package fi.aalto.drumbeat.object_browser.data_handlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NsIterator;

/*
* 
Jyrki Oraskari, Aalto University, 2016 

This research has partly been carried out at Aalto University in DRUMBEAT 
“Web-Enabled Construction Lifecycle” (2014-2017) —funded by Tekes, 
Aalto University, and the participating companies.

The MIT License (MIT)
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/


/**
 * @author joraskur
 *
 */
public class DrumbeatRESTDataHandler {
	final private NameSpaceHandler name_spaces_handler;
	// URL->Model
	static private final Map<String, Model> cache_for_huge_models = new HashMap<String, Model>();
	static private long cache_born_time = 0;
	
	public DrumbeatRESTDataHandler(NameSpaceHandler name_spaces_handler) {
		this.name_spaces_handler=name_spaces_handler;
	}


	/**
	 * @param url
	 * @return
	 */
	public Model getmodel_fromCache(String url) {
		Model model;
		if (cache_born_time - System.currentTimeMillis() > 60000 * 60) {
			cache_for_huge_models.clear();
			cache_born_time = System.currentTimeMillis();
		}
		Model cm = cache_for_huge_models.get(url);
		if (cm != null) {
			model = cm;
		} else {
			model = getModel(url);
			if (model == null)
				return null;
			if (model.size() > 500) {
				// Only one
				cache_for_huge_models.clear();
				cache_born_time = System.currentTimeMillis();
				cache_for_huge_models.put(url, model);

			} else {
				if (cache_for_huge_models.size() < 200)
					cache_for_huge_models.put(url, model);
			}
		}
		return model;
	}

	/**
	 * @param url
	 * @return
	 */
	public Model getModel(String url) {
		String json = getResponse(url);
		if (json == null) {
			System.err.println(" No response from th server!");
			return null;
		}
		InputStream is = new ByteArrayInputStream(json.getBytes());

		Model model = ModelFactory.createDefaultModel();// createOntologyModel(OntModelSpec.RDFS_MEM_TRANS_INF);

		try {
			model.read(is, null, "JSON-LD");

		} catch (Exception e) {
			// Non-REST fetches like OWL ontology file URLs cause this
			System.out.println("Ivalid JSON-LD message.  URL was:\n"+url);
			return null;
		}

		
		
		if (name_spaces_handler.getName_spaces() != null) {
			
			NsIterator listNameSpaces = model.listNameSpaces();
			while (listNameSpaces.hasNext())
			{
			String ns = listNameSpaces.next();
				String ns_abr = name_spaces_handler.getName_spaces().get(ns);
				if (ns_abr == null) {
					ns_abr = name_spaces_handler.name_namespace(ns);
					name_spaces_handler.getName_spaces().put(ns, ns_abr);						
				}
			}
		}
		return model;
	}

	/**
	 * @param url
	 * @return
	 */
	private String getResponse(String url) {

		int timeout = Integer.MAX_VALUE;

		HttpURLConnection c = null;
		try {
			URL u = new URL(url);
			c = (HttpURLConnection) u.openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("Content-length", "0");
			c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			c.setRequestProperty("Accept", "application/ld+json");
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.setConnectTimeout(timeout);
			c.setReadTimeout(timeout);

			c.connect();
			int status = c.getResponseCode();

			switch (status) {
			case 200:
			case 201:
				BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				return sb.toString();
			}

		} catch (MalformedURLException ex) {

		} catch (IOException ex) {
		} finally {
			if (c != null) {
				try {
					c.disconnect();
				} catch (Exception ex) {
				}
			}
		}
		return null;
	}

}