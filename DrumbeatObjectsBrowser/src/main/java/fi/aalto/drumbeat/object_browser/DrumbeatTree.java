package fi.aalto.drumbeat.object_browser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import com.vaadin.server.ExternalResource;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import fi.aalto.drumbeat.object_browser.data_handlers.DrumbeatRESTDataHandler;
import fi.aalto.drumbeat.object_browser.data_handlers.EventBusCommunication;
import fi.aalto.drumbeat.object_browser.data_handlers.NameSpaceHandler;
import fi.aalto.drumbeat.object_browser.events.MainNodeUpdateEvent;
import fi.aalto.drumbeat.object_browser.events.NameSpaceDataChangedEvent;
import fi.aalto.drumbeat.object_browser.vo.DrumbeatNode;
import fi.aalto.drumbeat.object_browser.vo.DrumbeatProperty;
import fi.aalto.drumbeat.object_browser.vo.DrumbeatTuple;


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
public class DrumbeatTree {
	private final EventBusCommunication communication = EventBusCommunication.getInstance();
	private final String app_url;
	private final String url;
	private final TreeTable tree = new TreeTable("");
	private final VerticalLayout layout = new VerticalLayout();
	private Optional<DrumbeatNode> main_node = Optional.empty();

	private final Set<String> url_set = new HashSet<String>();
	
	private final Set<DrumbeatNode> node_tasklist = new HashSet<DrumbeatNode>();

	private final List<String> object_value_properties = Arrays.asList("hasDateTime,hasReal,hasInteger".split(","));
	private final List<String> objects_handled_automativally = Arrays.asList("globalId,name,description".split(","));
	private final NameSpaceHandler name_spaces_handler;
	
	private final DrumbeatListHandler list_handler=new DrumbeatListHandler(this);

	public DrumbeatTree(String app_url, String url, NameSpaceHandler name_spaces_handler) {
		this.app_url = app_url;
		this.url = url;
		this.name_spaces_handler = name_spaces_handler;
		communication.register(this);
	}

	public TreeTable getTree_presentation() {
		return tree;
	}



	/**
	 * 
	 */
	public void initialize() {
		// propertyID: "Object Browser" referenced in the code!
		tree.addContainerProperty("Object Browser", HorizontalLayout.class, "");
		tree.setWidth("100%");
		layout.setStyleName("drumbeat_white");
		layout.addComponent(tree);
		createObjectBrowserTree(url);
	}

	/**
	 * @return
	 */
	public VerticalLayout getLayout() {
		return layout;
	}

	/**
	 * @return
	 */
	public Optional<DrumbeatNode> getMain_node() {
		return main_node;
	}

	/**
	 * @param url
	 */
	private void createObjectBrowserTree(String url) {
		tree.clear();
		tree.setHeight("900px");
		List<DrumbeatNode> url_node_set = handleURL(url);
		if (url_node_set.size() > 0)
			main_node = Optional.of(url_node_set.get(0));
		fetchNewNodes(url_node_set);

		tree.addExpandListener(new Tree.ExpandListener() {
			private static final long serialVersionUID = 278278278;

			public void nodeExpand(ExpandEvent event) {
				if (event.getItemId() instanceof DrumbeatNode) {
					DrumbeatNode node = (DrumbeatNode) event.getItemId();
					if (!node.isRepetition()) {
						handleNode(node, false);

						if (tree.hasChildren(node))
							for (Object o : tree.getChildren(node))
								if (tree.getChildren(o).size() < 7)
									tree.setCollapsed(o, false);

					}
				}
			}

		});

		tree.setCellStyleGenerator(new Table.CellStyleGenerator() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {

				if (propertyId == null) {
					// Styling for row
					if (itemId instanceof DrumbeatNode) {
						if (((DrumbeatNode) itemId).isRepetition())
							return "gray";

						if (((DrumbeatNode) itemId).hasURL())
							return "bold";
						else
							return "literal";
					} else
						return null;
				}
				return null;
			}
		});

	}

	/**
	 * @param url
	 * @return
	 */
	private List<DrumbeatNode> handleURL(String url) {

		List<DrumbeatNode> list_of_new_nodes = new ArrayList<DrumbeatNode>();

		if (url.contains(".ttl"))
			return list_of_new_nodes;

		DrumbeatRESTDataHandler data = new DrumbeatRESTDataHandler(name_spaces_handler);
		Model model = data.getmodel_fromCache(url);
		if (model == null)
			return list_of_new_nodes;

		StmtIterator statement_iterator = model.listStatements();
		while (statement_iterator.hasNext()) {
			Statement stmt = statement_iterator.nextStatement();
			RDFNode s = stmt.getSubject();
			DrumbeatNode d_s = url_node_map.get(s.asResource().getURI());
			if (d_s == null) {
				d_s = new DrumbeatNode(s, name_spaces_handler);
				list_of_new_nodes.add(d_s);
				url_node_map.put(s.asResource().getURI(), d_s);
				addTreeNode(d_s);
			}
		}
		communication.post(new NameSpaceDataChangedEvent());
		return list_of_new_nodes;
	}

	/**
	 * @param property
	 */
	private void addTreePropertyNode(DrumbeatProperty property) {
		HorizontalLayout layout = new HorizontalLayout();
		Label label = new Label(property.toString());
		layout.addComponent(label);
		tree.addItem(new Object[] { layout }, property);
	}

	/**
	 * @param dt
	 */
	public void addTreeStringNode(DrumbeatTuple dt) {
		HorizontalLayout layout = new HorizontalLayout();
		Label label = new Label(dt.toString());
		layout.addComponent(label);
		tree.addItem(new Object[] { layout }, dt);
	}

	/**
	 * @param node
	 */
	private void addTreeNode(DrumbeatNode node) {
		HorizontalLayout layout = createTreeNodeLayout(node);
		tree.addItem(new Object[] { layout }, node);
	}

	/**
	 * @param node
	 */
	@SuppressWarnings("unchecked")
	private void updateTreeNode(DrumbeatNode node) {
		HorizontalLayout layout = createTreeNodeLayout(node);
		tree.getContainerProperty(node, "Object Browser").setValue(layout);
	}

	/**
	 * @param node
	 * @return
	 */
	private HorizontalLayout createTreeNodeLayout(DrumbeatNode node) {
		HorizontalLayout layout = new HorizontalLayout();
		if (node.hasURL()) {
			Link link = new Link(node.toString(), new ExternalResource(app_url + "?url=" + node.getURI()));
			layout.addComponent(link);
		} else {
			Label label = new Label(node.toString());
			layout.addComponent(label);
		}
		return layout;
	}

	/**
	 * @param node
	 * @param d_p
	 * @param o
	 * @return
	 */
	private DrumbeatNode add_leaf(DrumbeatNode node, DrumbeatProperty d_p, RDFNode o) {
		DrumbeatNode ret = null;
		DrumbeatNode d_o = new DrumbeatNode(o, name_spaces_handler);
		updateNextOpenings(d_o, d_p);

		if (d_o.hasURL())
			ret = d_o;

		addTreeNode(d_o);
		if (!d_o.hasURL())
			tree.setChildrenAllowed(d_o, false);

		if (d_o.hasURL())
			if (!url_set.add(d_o.getURI()))
				d_o.setRepetition(true);
		tree.setParent(d_o, d_p);
		d_o.setParent(d_p);
		return ret;
	}

	final private Map<String, DrumbeatNode> url_node_map = new HashMap<String, DrumbeatNode>();
	final private Map<String, DrumbeatProperty> id_property_map = new HashMap<String, DrumbeatProperty>();

	/**
	 * @param nodes
	 * @return
	 */
	private List<DrumbeatNode> fetchNewNodes(List<DrumbeatNode> nodes) {
		List<DrumbeatNode> new_nodes = new ArrayList<DrumbeatNode>();
		for (DrumbeatNode node : nodes) {
			new_nodes.addAll(handleNode(node, false));
			tree.setCollapsed(node, false);
			if (tree.hasChildren(node))
				for (Object o : tree.getChildren(node))
					if (tree.getChildren(o).size() < 7)
						tree.setCollapsed(o, false);
		}
		return new_nodes;
	}

	/**
	 * @param node
	 * @param property
	 * @param o
	 * @return
	 */
	private DrumbeatNode add_leaf(DrumbeatNode node, Resource property, RDFNode o) {
		DrumbeatNode ret = null;
		DrumbeatNode d_o = new DrumbeatNode(o, name_spaces_handler);

		if (d_o.hasURL())
			ret = d_o;

		addTreeNode(d_o);

		String property_id = node.hashCode() + "::" + property.getURI();
		DrumbeatProperty d_p = id_property_map.get(property_id);
		if (d_p == null) {
			d_p = new DrumbeatProperty(property, node);
			id_property_map.put(property_id, d_p);
			addTreePropertyNode(d_p);

			tree.setParent(d_p, node);
		}
		if (!d_o.hasURL())
			tree.setChildrenAllowed(d_o, false);

		if (property.getLocalName().equals("type"))
			tree.setChildrenAllowed(d_o, false);
		else {
			if (d_o.hasURL())
				if (!url_set.add(d_o.getURI()))
					d_o.setRepetition(true);
		}
		tree.setParent(d_o, d_p);
		d_o.setParent(d_p);

		updateNextOpenings(d_o, d_p);
		return ret;
	}

	/**
	 * @param d_o
	 * @param d_p
	 */
	private void updateNextOpenings(DrumbeatNode d_o, DrumbeatProperty d_p) {
		if (objects_handled_automativally.contains(d_p.toString()))
			node_tasklist.add(d_o);

		if (d_p.toString().endsWith("Type"))
			node_tasklist.add(d_o);
		if (d_p.toString().endsWith("Identifier"))
			node_tasklist.add(d_o);
	}

	private Set<DrumbeatNode> handled_nodes = new HashSet<DrumbeatNode>();

	/**
	 * @param node
	 * @param list_handling
	 * @return
	 */
	public List<DrumbeatNode> handleNode(DrumbeatNode node, boolean list_handling) {
		List<DrumbeatNode> list_of_new_nodes = new ArrayList<DrumbeatNode>();
		if (!handled_nodes.add(node) || !node.hasURL())
			return list_of_new_nodes;
		String url = node.getURI();

		if (node.containsTTL())
			return list_of_new_nodes;
		DrumbeatRESTDataHandler data = new DrumbeatRESTDataHandler(name_spaces_handler);
		Model model = data.getmodel_fromCache(url);

		if (model == null) {
			// OWL-references do not have any leaves
			tree.setChildrenAllowed(node, false);
			return list_of_new_nodes;
		}

		StmtIterator statement_iterator = model.listStatements();
		List<Statement> statements = new ArrayList<Statement>();
		Statement type_stmt = null;

		while (statement_iterator.hasNext()) {
			Statement stmt = statement_iterator.nextStatement();

			if (stmt.getPredicate().getLocalName().equals("type"))
				type_stmt = stmt;
			else if (stmt.getPredicate().getLocalName().equals("tag_IfcElement"))
				;
			else if (stmt.getPredicate().getLocalName().contains("ownerHistory"))
				;
			else if (stmt.getPredicate().getLocalName().equals("hasString")) {
				tree.removeItem(node);
				try {
					if (node.getParent().toString().equals("name")) {
						String name = stmt.getObject().asLiteral().getLexicalForm();
						node.getParent().getParent().setName(name);
					}
				} catch (Exception e) {

				}
				updateTreeNode(node.getParent().getParent());
				handleStatement(node.getParent().getParent(), node.getParent(), stmt, list_of_new_nodes);

				return list_of_new_nodes;
			} else if (object_value_properties.contains(stmt.getPredicate().getLocalName())) {
				tree.removeItem(node);

				handleStatement(node.getParent().getParent(), node.getParent(), stmt, list_of_new_nodes);

				return list_of_new_nodes;
			} else if (stmt.getPredicate().getLocalName().equals("hasNext") && !list_handling) {
				list_handler.getList_begins().add(node);
				statements.add(stmt);
			} else
				statements.add(stmt);
		}

		if (type_stmt != null)

		{
			node.setType(type_stmt.getObject().asResource().getLocalName());
			updateTreeNode(node);
			if (node.getParent() != null)
				tree.setParent(node, node.getParent());
			tree.setCollapsed(node, false);
		}

		for (Statement stmt : statements) {
			handleStatement(node, stmt, list_of_new_nodes);

		}
		if (!list_handling) {
			communication.post(new NameSpaceDataChangedEvent());
			doUpdateCycle();
		}
		return list_of_new_nodes;
	}

	/**
	 * @param node
	 * @param stmt
	 * @param list_of_new_nodes
	 */
	private void handleStatement(DrumbeatNode node, Statement stmt, List<DrumbeatNode> list_of_new_nodes) {
		DrumbeatNode new_node = add_leaf(node, stmt.getPredicate(), stmt.getObject());
		if (new_node != null) {
			list_of_new_nodes.add(new_node);
		}
	}

	/**
	 * @param node
	 * @param property
	 * @param stmt
	 * @param list_of_new_nodes
	 */
	private void handleStatement(DrumbeatNode node, DrumbeatProperty property, Statement stmt,
			List<DrumbeatNode> list_of_new_nodes) {
		DrumbeatNode new_node = add_leaf(node, property, stmt.getObject());
		if (new_node != null) {
			list_of_new_nodes.add(new_node);
		}
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 
	 */
	private void doNodeTasklist() {
		Set<DrumbeatNode> handle_next_copy = new HashSet<DrumbeatNode>();
		handle_next_copy.addAll(node_tasklist); // Avoid concurrent modifications

		for (DrumbeatNode n : handle_next_copy) {
			handleNode(n, false);
		}
		node_tasklist.clear();
	}

	/**
	 * 
	 */
	private void doUpdateCycle() {
		if (main_node.isPresent())
			communication.post(new MainNodeUpdateEvent(main_node.get()));
		list_handler.CompressLiteralList();
		doNodeTasklist();
	}

}
