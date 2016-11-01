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
		this.drumbeatTree=drumbeatTree;
		tree=drumbeatTree.getTree_presentation();
	}

	public Set<DrumbeatNode> getList_begins() {
		return list_begins;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	private final Set<DrumbeatNode> list_begins = new HashSet<DrumbeatNode>();

	/**
	 * handle hasNext property chains to find listEnds
	 */
	public void CompressLiteralList() {
		Set<DrumbeatNode> list_begins_copy = new HashSet<DrumbeatNode>();
		list_begins_copy.addAll(list_begins); // Avoid concurrent modifications

		for (DrumbeatNode n : list_begins_copy) {
			recursively_handle_hasNext(n, 0);
		}
		list_begins.clear();
		doListEnds();
	}


	
	/**
	 * @param node
	 * @param counter
	 */
	private void recursively_handle_hasNext(DrumbeatNode node, int counter) {
		if (counter > 20)
			return;
		if (node.getType() != null)
			if (node.getType().endsWith("_EmptyList")) {
				list_ends.add(node);
				return;
			}
		if (tree.hasChildren(node))
			for (Object o1 : tree.getChildren(node)) {
				DrumbeatProperty p = (DrumbeatProperty) o1;
				if (p.toString().endsWith("hasNext")) {
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							drumbeatTree.handleNode(n, true);
							recursively_handle_hasNext(n, counter + 1);
						}
				}
			}
	}


	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	private final Set<DrumbeatNode> list_ends = new HashSet<DrumbeatNode>();

	/**
	 * handle listEnds
	 */
	private void doListEnds(){
		Set<DrumbeatNode> list_ends_copy = new HashSet<DrumbeatNode>();
		list_ends_copy.addAll(list_ends);
		for (DrumbeatNode n : list_ends_copy) {
			Stack<String> path = new Stack<String>();
			if (recursive_fetch_parents(n, path)) {
				String tuple = "(";
				while (!path.isEmpty()) {
					tuple += path.pop() + ",";
				}
				tuple = tuple.substring(0, tuple.length() - 1) + ")";				
				recursive_remove_and_set(n, n.getParent(), tuple);
			}
		}
		list_ends.clear();
	}
	
	
	
	/**
	 * @param node
	 * @param path
	 * @return
	 */
	private boolean recursive_fetch_parents(DrumbeatNode node, Stack<String> path) {
		if (!node.getType().endsWith("List"))
			return true;

		if (tree.hasChildren(node))
			for (Object o1 : tree.getChildren(node)) {
				DrumbeatProperty p = (DrumbeatProperty) o1;
				if (p.toString().endsWith("hasValue")) {
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							drumbeatTree.handleNode(n, true);
						}
					if (tree.hasChildren(p))
						for (Object o2 : tree.getChildren(p)) {
							DrumbeatNode n = (DrumbeatNode) o2;
							if (!n.hasURL())
								path.push(n.toString());
							else
								return false;
						}
				}
			}
			if (node.getParent() != null && node.getParent().getParent() != null) {
				DrumbeatNode p = node.getParent().getParent();
				return recursive_fetch_parents(p, path);
			}
		return true;

	}
	

	/**
	 * @param node
	 * @param lastproperty
	 * @param tuple
	 */
	private void recursive_remove_and_set(DrumbeatNode node, DrumbeatProperty lastproperty, String tuple) {
		if (!node.getType().endsWith("List")) {
			DrumbeatTuple dt = new DrumbeatTuple(tuple);
			drumbeatTree.addTreeStringNode(dt);
			tree.setParent(dt, lastproperty);
			tree.setChildrenAllowed(dt, false);
			return;
		}

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
				DrumbeatNode p = node.getParent().getParent();
				recursive_remove_and_set(p, node.getParent(), tuple);
			}

	}


	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////


}
