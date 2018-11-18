package com.sbelei.kafka.pharsers;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class MyParseTreeVisitor implements ParseTreeVisitor {

	@Override
	public Object visit(ParseTree tree) {
		System.out.println("TREE:" + tree.getText());
		return null;
	}

	@Override
	public Object visitChildren(RuleNode node) {
		if (node.getChildCount() > 0) {
			System.out.println("NODE:" + node.getText());
			try {
				node.accept(this);
			} catch (Throwable e) {
				return null;
			}
		} else {
			System.out.println("NODE:" + node.getText());
		}
		return null;
	}

	@Override
	public Object visitTerminal(TerminalNode node) {
		System.out.println("TERM NODE:" + node.getText());
		return null;
	}

	@Override
	public Object visitErrorNode(ErrorNode node) {
		System.out.println("ERR NODE:" + node.getText());
		return null;
	}

}
