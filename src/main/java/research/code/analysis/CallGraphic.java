package research.code.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class CallGraphic {
	
	private Set<CallGraphicNode> allNodes = new HashSet<>();
		
	/**
	 * @param start
	 * @param m
	 * @return toNode
	 */
	public CallGraphicNode addEdge(CallGraphicNode start,MethodInvocation m){
		CallGraphicNode toNode = findNode(m.resolveMethodBinding());
		boolean hasExists = true;
		if(toNode == null){
			hasExists = false;
			toNode = new CallGraphicNode(m.resolveMethodBinding());
			allNodes.add(toNode);
		}
		
		Edge edge = new Edge();
		edge.node = start;
		edge.lineNum = m.getStartPosition();
		toNode.addFrom(edge);
		
		edge = new Edge();
		edge.node = toNode;
		edge.lineNum = m.getStartPosition();
		
		start.addTo(edge);
		
		if(hasExists){
			return null;
		}
		return toNode;
	}
	
	private CallGraphicNode findNode(IMethodBinding m){
		for (CallGraphicNode node : allNodes) {
			if(m.equals( node.method ) ){
				return node;
			}
		}
		return null;
	}
	
	public static class Edge {
		private CallGraphicNode node;
		private int lineNum;

		public int getLineNum() {
			return lineNum;
		}
		public void setLineNum(int lineNum) {
			this.lineNum = lineNum;
		}
		@Override
		public int hashCode() {
			return node.hashCode() << 31 + lineNum;
		}
		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Edge)){
				return false;
			}
			return node.equals(((Edge)obj).node) && lineNum == ((Edge)obj).lineNum;
		}
	}
	
}
