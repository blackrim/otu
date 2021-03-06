package jade.tree;

import java.util.*;

import opentree.otu.GeneralUtils;

public class JadeNode {
    
    public static final double MIN_BRANCHLENGTH = 0.0000000000000000000001;

    /*
	 * common associations
	 */
	private double BL; //branch lengths
	private double distance_to_tip;
	private double distance_from_tip; // distance from the root to a tip (not set automatically)
	//private int number;
	private String name;
	private JadeNode parent;
	private ArrayList<JadeNode> children;
//	private ArrayList<NodeObject> assoc; // @note might need to make this a HashMap<String, Object> or TreeMap<String,Object>
	private HashMap<String, Object> assoc;
	
	/*
	 * constructors
	 */
	public JadeNode() {
		init();
	}
	
	public JadeNode(JadeNode parent) {
		init();
		this.parent = parent;
	}
	
	public JadeNode(double BL, String name, JadeNode parent) {
		init();
		this.BL = BL;
		this.name = name;
		this.parent = parent;
	}

	/**
	 * called by constructors
	 */
	private void init() {
		this.BL = 0.0;
		this.distance_to_tip = 0.0;
		this.distance_from_tip = 0.0;
		//this.number = 0;
		this.name = "";
		this.parent = null;
		this.children = new ArrayList<JadeNode> ();
//		this.assoc = new ArrayList<NodeObject>();
		this.assoc = new HashMap<String, Object>();
	}
	
    // ===== node iterators

    public enum NodeOrder {PREORDER, POSTORDER};

    private void addDescendants(JadeNode n, List<JadeNode> children, NodeOrder order) {

        if (order == NodeOrder.PREORDER) {
            for (JadeNode c : n.children){
                addDescendants(c, children, order);
            }
            children.add(n);

        } else if (order == NodeOrder.POSTORDER) {
            children.add(n);

            for (JadeNode c : n.children){
                addDescendants(c, children, order);
            }
        }
    }
    
    public Iterable<JadeNode> getDescendants(NodeOrder order) {
        
        LinkedList<JadeNode> nodes = new LinkedList<JadeNode>();
        addDescendants(this, nodes, order);
        return nodes;
    }
    
    public Iterable<JadeNode> getDescendantLeaves(NodeOrder order) {
        LinkedList<JadeNode> leaves = new LinkedList<JadeNode>();
    	for (JadeNode descendant : getDescendants(order)) {
    		if (descendant.isExternal()) {
    			leaves.add(descendant);
    		}
    	}
    	return leaves;
    }
    
    public Iterable<JadeNode> getDescendantLeaves() {
    	return getDescendantLeaves(NodeOrder.PREORDER);
    }
    
    public Iterable<JadeNode> getDescendants() {
    	return getDescendants(NodeOrder.PREORDER);
    }
    
    /* ---------------------------- end node iterators --------------------------------*/
    
    /*
     * other public methods
     */
    
	public JadeNode [] getChildrenArr() {return (JadeNode[])this.children.toArray();}
	
	public ArrayList<JadeNode> getChildren() {return this.children;}
	
	public boolean isExternal() {return (this.children.size() < 1);}
	
	public boolean isInternal() {return (this.children.size() > 0);}
	
	public boolean isTheRoot() {return (this.parent == null);}
	
	public boolean hasParent() {return (this.parent != null);}
	
	public void setParent(JadeNode p) {this.parent = p;}
	
	//public int getNumber() {return this.number;}
	
	//public void setNumber(int n) {this.number = n;}
	
	public double getBL() {return this.BL;}
	
	public void setBL(double b) {this.BL = b;}
	
	public boolean hasChild(JadeNode test) {return this.children.contains(test);}
	
	public boolean addChild(JadeNode c) {
		if (this.hasChild(c) == false) {
			this.children.add(c);
			c.setParent(this);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeChild(JadeNode c) {
		if (this.hasChild(c)) {
			this.children.remove(c);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @return the c-th child or throw IndexOutOfBoundsException.
	 */
	public JadeNode getChild(int c) throws IndexOutOfBoundsException {
		return this.children.get(c);
	}
	
	public void setName(String s) {this.name = s;}
	
	public String getName() {return this.name;}
	
	/**
	 * @param bl should be true to include branch lengths
	 * @return string with newick representation of the subtree rooted at this node
	 */
	public String getNewick(boolean bl) {
		StringBuffer ret = new StringBuffer("");
		for (int i = 0; i < this.getChildCount(); i++) {
			if (i == 0) {
				ret.append("(");
			}
			ret.append(this.getChild(i).getNewick(bl));
			if (bl) {
			    double branchLength = this.getChild(i).getBL();
			    if (branchLength == 0)
			        branchLength = MIN_BRANCHLENGTH;

			    ret.append(":".concat(String.valueOf(branchLength)));
			}
			if (i == this.getChildCount()-1) {
				ret.append(")");
			} else {
				ret.append(",");
			}
		}
		if (this.name != null) {
			ret.append(GeneralUtils.cleanName(this.name));
		}
		return ret.toString();
	}
	
	/**
	 * Get JSON output NOT FOR THE ARGUS viewer. DEPRECATED for ARGUS. Use the ArgusRepresentationConverter instead.
	 * 
	 * 
	 * @param bl should be true to include branch lengths
	 * @return string with JSON representation of the subtree rooted at this node
	 */
	public String getJSON(boolean bl) { 		
		StringBuffer ret = new StringBuffer("{");
		if (this.name != null) {
			ret.append(" \"name\": \"" + this.getName() + "\"");
		} else {
			ret.append(" \"name\": \"\"");
		}
		if (this.getObject("nodeid") != null) {
			ret.append("\n, \"nodeid\": \"" + this.getObject("nodeid") + "\""); // note: what is the difference between this version and the capitalized one below?
		}
		for (int i = 0; i < this.getChildCount(); i++) {
			if (i == 0) {
				ret.append("\n, \"children\": [\n");
			}
			ret.append(this.getChild(i).getJSON(bl));
			if (i == this.getChildCount() - 1) {
				ret.append("]\n");
			} else {
				ret.append(",\n");
			}
		}
		Object hc = this.getObject("haschild");
		if (hc != null) {
			int nc = (Integer)this.getObject("numchild");
			if(nc > this.getChildCount()){
				ret.append(", \"notcomplete\": 1");
			}
		}
		if (bl) {
			ret.append(", \"branch_length\": " + this.getBL());
		}
		Object sup = this.getObject("supporting_sources");
		if (sup != null) {
//			ret.append(", \"supportedBy\": ");
//			JSONExporter.writeStringArrayAsJSON(ret, (String []) sup);
			int sz = 0;
			for (int i=0;i<((String []) sup).length;i++){
				if ((((String []) sup)[i]).equals("taxonomy"))
					continue;
				else
					sz++;
			}
			ret.append(", \"size\": "+sz);
		}
		if(this.getObject("nodeId") != null){
			ret.append(", \"id\":"+this.getObject("nodeId")); // note: what is the difference between the capitalized version and the uncapitalized one above?
		}
		Object ig = this.getObject("ingroup");
		if(ig != null){
			ret.append(", \"ingroup\": 1");
		}
		ret.append("}");
		return ret.toString();
	}

	/**
	 * @return Returns all of the tips in the subtree rooted at `this`
	 */
	public ArrayList<JadeNode> getTips() {
		ArrayList<JadeNode> children = new ArrayList<JadeNode>();
		Stack<JadeNode> nodes = new Stack<JadeNode>();
		nodes.push(this);
		while (nodes.isEmpty() == false) {
			JadeNode jt = nodes.pop();
			for (int i = 0; i < jt.getChildCount(); i++) {
				nodes.push(jt.getChild(i));
			}
			if (jt.isExternal()) {
				children.add(jt);
			}
		}
		return children;
	}
	
	/**
	 * @return Returns all of the tips in the subtree rooted at `this`
	 */
	public int getTipCount() {
		int count = 0;
		Stack<JadeNode> nodes = new Stack<JadeNode>();
		nodes.push(this);
		while (nodes.isEmpty() == false) {
			JadeNode jt = nodes.pop();
			for (int i = 0; i < jt.getChildCount(); i++) {
				nodes.push(jt.getChild(i));
			}
			if (jt.isExternal()) {
				count += 1;
			}
		}
		return count;
	}
	
	/**
	 * @return Returns the maximum number of edges between `this` and a tip
	 *		that is a descendant of `this`
	 */
	public int getNodeMaxDepth() {
		int maxnodedepth = 0;
		ArrayList<JadeNode> tips = this.getTips();
		for (int i = 0; i < tips.size(); i++) {
			JadeNode curnode = tips.get(i);
			int tnodedepth = 0;
			while (curnode != this) {
				tnodedepth += 1;
				curnode = curnode.getParent();
			}
			if (tnodedepth > maxnodedepth) {
				maxnodedepth = tnodedepth;
			}
		}
		return maxnodedepth;
	}
	
	public JadeNode getParent() {return this.parent;}
	
	public int getChildCount() {return this.children.size();}
	
	public double getDistanceFromTip() {return this.distance_from_tip;}
	
	public void setDistanceFromTip(double inh) {this.distance_from_tip = inh;}
	
	public double getDistanceToTip() {return this.distance_to_tip;}
	
	public void setDistanceToTip(double inh) {this.distance_to_tip = inh;}
	
	/*
	 * Adds or a replace a mapping of key->obj for this node
	 * @note this in an example of an O(N) routine that would be constant time
	 * 		or O(log(N)) if we change the assoc datatype.
	 * @param key
	 * @param obj Object to be stored
	 *
	public void assocObject(String key, Object obj) {
		// @todo This is written as if there could be multiple elements in assoc that
		//		have the same key. I don't think this is true. (we should probably
		//		return on finding a match, rather than set a test=true flag).
		boolean test = false;
		for (int i = 0; i < this.assoc.size(); i++) {
			if (this.assoc.get(i).getName().compareTo(key) == 0) {
				test = true;
				this.assoc.get(i).setObject(obj);
			}
		}
		if (test == false) {
			NodeObject no = new NodeObject(key, obj);
			this.assoc.add(no);
		}
	}
	
	/*
	 * @return Object associated with this node and key through a previous call
	 *		to assocObject, or null
	 * @note this in an example of an O(N) routine that would be constant time
	 * 		or O(log(N)) if we change the assoc datatype.
	 * @param key
	 *
	public Object getObject(String key) {
		// @todo This is written as if there could be multiple elements in assoc that
		//		have the same key. I don't think this is true. (we should probably
		//		return on finding a match, rather than continuing to walk through the list).
		Object a = null;
		for (int i = 0; i < this.assoc.size(); i++) {
			if (this.assoc.get(i).getName().compareTo(key) == 0) {
				a = this.assoc.get(i).getObject();
			}
		}
		return a;
	} */
	
	/**
	 * Adds a mapping of key->obj for this tree. Unlike the JavaNode version,
	 *	this method does NOT guard against multiple keys being added. Note
	 *	that only the last object associated with a key will be accessible via
	 *	getObject
	 * @param key
	 * @param obj Object to be stored
	 */
	public void assocObject(String key, Object obj) {
		assoc.put(key, obj);
	}

	/**
	 * Returns the object associated with the last call of assocObject with this key
	 * @param key
	 */
	public Object getObject(String key) {
		return assoc.get(key);
	}
	
	/**
	 * Just checks if there is an object associated (by the assocObject method) with this key
	 * @param key
	 * @return
	 */
	public boolean hasAssocObject(String key) {
		return assoc.containsKey(key);
	}
	
	/**
	 * Return the HashMap of metadata associated with the tree
	 * @todo need to check
	 * @todo we should probably have a boolean flag to indicate whether or not the tree should be treated as rooted
	 */
	public HashMap<String,Object> getAssoc() {
		return assoc;
	}
}
