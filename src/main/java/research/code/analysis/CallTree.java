package research.code.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class CallTree {

	private final CallTreeNode root ;

	public CallTree(){
		root = new CallTreeNode();
		root.setParent(null);
		root.setMethod(null);
	}
	
	public  CallTreeNode getRoot(){
		return root;
	}

	public void accept(TreeVisitor tv){
		root.getSubs().forEach((x)->x.accept(tv));
	}

	public static interface TreeVisitor{
		public boolean visit(CallTreeNode node);
		public boolean visit(RefCallTreeNode node);
	}
	@FunctionalInterface
	public static interface NodeSelector{
		public boolean filter(CallTreeNode node);
	}
	
	public List<CallTreeNode> findSameTreeNode(IMethodBinding method){
		List<CallTreeNode> nodes = new ArrayList<>();
		this.accept(new TreeVisitor() {
			@Override
			public boolean visit(RefCallTreeNode node) {
				return false;
			}
			@Override
			public boolean visit(CallTreeNode node) {
				if(node.getMethod().equals(method)){
					nodes.add(node);
					// skip subscribes node
					return false;
				}
				return true;
			}
		});
		return nodes;
	}
	
	public List<CallTreeNode> selectNodes(NodeSelector selector){
		List<CallTreeNode> nodes = new ArrayList<>();
		this.accept(new TreeVisitor(){
			@Override
			public boolean visit(CallTreeNode node) {
				if(selector.filter(node)){
					nodes.add(node);
				}
				return true;
			}
			@Override
			public boolean visit(RefCallTreeNode node) {
				return false;
			}
		});
		return nodes;
	}
	
	public void cleanSelect(){
		this.accept(new TreeVisitor(){
			@Override
			public boolean visit(CallTreeNode node) {
				 node.unmark();
				 return true;
			}

			@Override
			public boolean visit(RefCallTreeNode node) {
				return false;
			}
		});
	}
}
