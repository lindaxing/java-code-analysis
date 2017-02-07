package com.ai.code.analysis;

import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;

import com.ai.code.analysis.CallTree.TreeVisitor;

public class RefCallTreeNode extends CallTreeNode {
	
	private CallTreeNode node;
	private CallTreeNode parent;
	
	public RefCallTreeNode(CallTreeNode node) {
		this.node = node;
	}

	@Override
	public IMethodBinding getMethod() {
		return node.getMethod();
	}

	@Override
	public void setMethod(IMethodBinding method) {
		node.setMethod(method);
	}

	@Override
	public List<CallTreeNode> getSubs() {
		return node.getSubs();
	}

	@Override
	public void setSubs(List<CallTreeNode> subs) {
		node.setSubs(subs);
	}

	@Override
	public CallTreeNode getParent() {
		return this.parent;
	}

	@Override
	public void setParent(CallTreeNode parent) {
		this.parent = parent;
	}

	@Override
	public void accept(TreeVisitor tv) {
		if(tv.visit(this)){
			getSubs().forEach((x)->x.accept(tv));
		}
	}

	@Override
	public void mark() {
		node.mark();
	}

	@Override
	public void unmark() {
		node.unmark();
	}

	@Override
	public boolean isMark() {
		return node.isMark();
	}
	
}
