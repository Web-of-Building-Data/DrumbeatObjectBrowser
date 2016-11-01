package fi.aalto.drumbeat.object_browser.vo;

import java.util.Optional;

import org.apache.jena.rdf.model.RDFNode;

import fi.aalto.drumbeat.object_browser.data_handlers.NameSpaceHandler;

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
public class DrumbeatNode {
	private boolean isOWLClassNode = false;
	private boolean isShownAlready = false;

	private Optional<RDFNode> jena_node = Optional.empty();

	private Optional<String> owlClassType = Optional.empty();
	private Optional<String> connected_name = Optional.empty();
	private DrumbeatProperty parent;

	private final NameSpaceHandler name_spaces_handler;

	/**
	 * @param jena_node
	 * @param name_spaces_handler
	 */
	public DrumbeatNode(RDFNode jena_node, NameSpaceHandler name_spaces_handler) {
		super();
		this.jena_node = Optional.of(jena_node);
		this.name_spaces_handler = name_spaces_handler;
		toString(); // activate first time for table
	}

	/**
	 * @return
	 */
	public boolean containsTTL() {
		if (jena_node.isPresent()) {
			if (!jena_node.get().isLiteral())
				return jena_node.get().asResource().getURI().contains(".ttl");
			else
				return false;
		}
		return false;
	}

	/**
	 * @return
	 */
	public boolean isRepetition() {
		return isShownAlready;
	}

	/**
	 * @param isRepetition
	 */
	public void setRepetition(boolean isRepetition) {
		this.isShownAlready = isRepetition;
	}

	/**
	 * @return
	 */
	public boolean isType() {
		return isOWLClassNode;
	}

	/**
	 * @param isType
	 */
	public void setType(boolean isType) {
		this.isOWLClassNode = isType;
	}

	/**
	 * @return
	 */
	public String getURI() {
		if (jena_node.isPresent()) {
			if (!jena_node.get().isLiteral())
				return jena_node.get().asResource().getURI();
			else
				return jena_node.get().asLiteral().getLexicalForm();
		}
		return "";
	}

	public boolean hasURL() {
		if (jena_node.isPresent())
			return jena_node.get().isURIResource();
		else
			return false;
	}

	/**
	 * @return
	 */
	public boolean isDrumbeatBlankNode() {
		if (jena_node.isPresent()) {
			if (!jena_node.get().isLiteral())
				return jena_node.get().asResource().getURI().contains("BLANK/_LINE");
			else
				return false;
		}
		return false;
	}

	/**
	 * @return
	 */
	public String getType() {
		if (owlClassType.isPresent())
			return owlClassType.get();
		else
			return "";
	}

	/**
	 * @param type
	 */
	public void setType(String type) {
		this.owlClassType = Optional.of(type);
	}

	public String getName() {
		if (connected_name.isPresent())
			return connected_name.get();
		else
			return "";
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.connected_name = Optional.of(name);
	}

	/**
	 * @return
	 */
	private String name_title() {
		String ret = "";
		if (connected_name != null) {
			ret += " '" + connected_name + "'";
		}
		return ret;

	}

	/**
	 * @return
	 */
	private String type_title() {
		String ret = "";
		if (owlClassType != null) {
			ret += owlClassType;
		}
		if (ret.length() > 0) {
			return " (" + ret + ")";
		} else
			return "";

	}

	/**
	 * @return
	 */
	public DrumbeatProperty getParent() {
		return parent;
	}

	/**
	 * @param parent
	 */
	public void setParent(DrumbeatProperty parent) {
		this.parent = parent;
	}

	/**
	 * @param uri
	 * @return
	 */
	private String getNameSpace(String uri) {

		int inx = uri.lastIndexOf("/");
		if (inx > 0) {
			if (inx != uri.length() - 1)
				return uri.substring(0, inx + 1);
		}
		return uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (jena_node.isPresent()) {
			if (jena_node.get().isLiteral())
				return "'" + jena_node.get().asLiteral().getLexicalForm() + "'";
			else {
				if (name_spaces_handler.getName_spaces() != null) {
					String uri = jena_node.get().asResource().getURI();
					String ns = getNameSpace(uri);// jena_node.asResource().getNameSpace();
					if (ns != null) {
						String ns_abr = name_spaces_handler.getName_spaces().get(ns);
						if (ns_abr == null) {
							ns_abr = name_spaces_handler.name_namespace(ns);
							name_spaces_handler.getName_spaces().put(ns, ns_abr);
						}
						ns = uri.replace(ns, "");
						String type_title = type_title();
						if (ns_abr.contains("BLANK")) {

							if (type_title.length() > 0) {

								return type_title;
							} else {
								return "--->";
							}
						}
						return ns_abr + ":" + ns + type_title + name_title();
					} else
						return uri + type_title() + name_title();
				} else
					return jena_node.get().asResource().getURI() + type_title() + name_title();

			}
		}
		return "";
	}

}
