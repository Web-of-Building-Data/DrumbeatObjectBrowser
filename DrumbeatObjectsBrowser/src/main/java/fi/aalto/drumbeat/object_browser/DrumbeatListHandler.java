package fi.aalto.drumbeat.object_browser;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.vaadin.ui.TreeTable;

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

public class DrumbeatListHandler {

	private final DrumbeatTree drumbeatTree;
	public final TreeTable tree;

	public DrumbeatListHandler(DrumbeatTree drumbeatTree) {
		this.drumbeatTree = drumbeatTree;
		tree = drumbeatTree.getTree_presentation();
	}

	public Set<DrumbeatNode> getList_begins() {
		return list_begins;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	private final Set<DrumbeatNode> list_begins = new HashSet<DrumbeatNode>();

	/**
	 * handle hasNext property chains to find listEnds
	 */
	public void compressLists() {
		Set<DrumbeatNode> list_begins_copy = new HashSet<DrumbeatNode>();
		list_begins_copy.addAll(list_begins); // Avoid concurrent modifications

		for (DrumbeatNode list_start_node : list_begins_copy) {
			DrumbeatNode list_end_node = recursively_find_listEnd(list_start_node, 0);
			if (list_end_node != null) {
				doList(list_start_node, list_end_node);
			}
		}
		list_begins.clear();
	}

	/**
	 * @param node
	 * @param counter
	 */
	private DrumbeatNode recursively_find_listEnd(DrumbeatNode node, int counter) {
		if (counter > 20)
			return null;
		if (node.getType() != null)
			if (node.getType().endsWith("_EmptyList")) {
				return node;
			}
		if (tree.hasChildren(node))
			for (Object o1 : tree.getChildren(node)) {
				DrumbeatProperty p = (DrumbeatProperty) o1;
				if (p.toString().endsWith("hasNext")) {
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							drumbeatTree.handleNode(n, true);
							return recursively_find_listEnd(n, counter + 1);
						}
				}
			}
		return null;
	}

	/**
	 * handle listEnds
	 */
	private void doList(DrumbeatNode list_start_node, DrumbeatNode list_end_node) {

		Stack<String> literal_nodes_stack = new Stack<String>();
		recursively_get_literals(list_end_node, literal_nodes_stack);
		if (!literal_nodes_stack.isEmpty()) {
			String tuple = "(";
			while (!literal_nodes_stack.isEmpty()) {
				tuple += literal_nodes_stack.pop() + ",";
			}
			tuple = tuple.substring(0, tuple.length() - 1) + ")"; // removes
																	// last
																	// comma
			recursively_remove(list_end_node, list_end_node.getParent(), tuple);

			DrumbeatTuple dt = new DrumbeatTuple(tuple);
			drumbeatTree.addTreeStringNode(dt);

			if (list_start_node.getParent().getParent().isDrumbeatBlankNode())
			{
				tree.removeItem(list_start_node.getParent());
				tree.removeItem(list_start_node.getParent().getParent());
				tree.setParent(dt, list_start_node.getParent().getParent().getParent());
			}
			else
				tree.setParent(dt, list_start_node.getParent());
			tree.setChildrenAllowed(dt, false);

		}

	}

	/**
	 * @param node
	 * @param literal_stack
	 * @return
	 */
	private void recursively_get_literals(DrumbeatNode node, Stack<String> literal_stack) {
		if (!node.getType().endsWith("List"))
			return;

		if (tree.hasChildren(node))
			for (Object o1 : tree.getChildren(node)) {
				DrumbeatProperty p = (DrumbeatProperty) o1;
				if (p.toString().endsWith("hasValue")) {
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							drumbeatTree.handleNode(n, true); // fetch from the
																// net
						}

					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							if (!n.hasURL())
								literal_stack.push(n.toString());
						}
				}
			}

		if (node.getParent() != null && node.getParent().getParent() != null) {
			DrumbeatNode p = node.getParent().getParent();
			recursively_get_literals(p, literal_stack);
		}
	}

	/**
	 * @param node
	 * @param parent_property
	 * @param tuple
	 */
	private void recursively_remove(DrumbeatNode node, DrumbeatProperty parent_property, final String tuple) {
		if (!node.getType().endsWith("List"))
			return;

		// Removal of the nodes
		if (tree.hasChildren(node)) {
			Set<Object> children = new HashSet<Object>();
			children.addAll(tree.getChildren(node));
			for (Object o1 : children) {
				DrumbeatProperty p = (DrumbeatProperty) o1;
				if (p.toString().endsWith("hasValue")) {
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							tree.removeItem(n);
						}
				}
				tree.removeItem(p);
			}
		}
		tree.removeItem(node);

		if (node.getParent() != null && node.getParent().getParent() != null) {
			DrumbeatNode parent_node = node.getParent().getParent();
			DrumbeatProperty property = node.getParent();
			recursively_remove(parent_node, property, tuple);
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////

}
