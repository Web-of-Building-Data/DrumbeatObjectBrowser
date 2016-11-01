package fi.aalto.drumbeat.object_browser.data_handlers;

import java.util.SortedMap;
import java.util.TreeMap;

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
public class NameSpaceHandler {
	private int ns_count = 1;
	// URI->abreviation
	private final SortedMap<String, String> name_spaces = new TreeMap<String, String>();
	
	/**
	 * @param URI
	 * @return
	 */
	public String name_namespace(String URI) {
		String ns = "";
		try {
			String[] parts = URI.split("/");
			for (String txt : parts) {
				if (txt.length() > 0) {
					ns = txt.replace("#", "");
					ns = ns.replace("_", "");
					char c = ns.charAt(0);
					if (Character.isDigit(c))
						ns = "ns" + ns;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "ns_" + ns_count++;
		}
		return ns;
	}

	/**
	 * @return
	 */
	public SortedMap<String, String> getName_spaces() {
		return name_spaces;
	}

	
}
