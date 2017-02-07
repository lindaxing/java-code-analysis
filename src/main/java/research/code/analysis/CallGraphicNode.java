package research.code.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.IMethodBinding;

import research.code.analysis.CallGraphic.Edge;

public class CallGraphicNode {
	final IMethodBinding method;
	
	private Set<Edge> froms = new HashSet<>();
	private Set<Edge> tos = new HashSet<>();
	
	public CallGraphicNode(IMethodBinding method) {
		this.method = method;
	}
	
	public void addFrom(Edge edge){
		froms.add(edge);
	}
	
	public void addTo(Edge edge){
		tos.add(edge);
	}

	public Set<Edge> getFroms() {
		return froms;
	}

	public Set<Edge> getTos() {
		return tos;
	}
	
}
