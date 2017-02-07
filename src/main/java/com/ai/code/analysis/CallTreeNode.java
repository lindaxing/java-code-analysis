package com.ai.code.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;

import com.ai.code.analysis.CallTree.TreeVisitor;

public class CallTreeNode {
	private IMethodBinding method;
	private List<CallTreeNode> subs = new ArrayList<>();
	private CallTreeNode parent;
	private boolean mark = false;
	
	public void mark(){
		this.mark = true;
	}
	public void unmark(){
		this.mark = false;
	}
	
	public boolean isMark(){
		return mark;
	}
	
	public IMethodBinding getMethod() {
		return method;
	}
	public void setMethod(IMethodBinding method) {
		this.method = method;
	}
	public List<CallTreeNode> getSubs() {
		return subs;
	}
	public void setSubs(List<CallTreeNode> subs) {
		this.subs = subs;
	}
	public CallTreeNode getParent() {
		return parent;
	}
	public void setParent(CallTreeNode parent) {
		this.parent = parent;
	}
	
	public void accept(final TreeVisitor tv) {
		if(tv.visit(this)){
			subs.forEach((x)->x.accept(tv));
		}
	}
}
