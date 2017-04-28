/*
 * Sonar ESQL Plugin
 * Copyright (C) 2013-2017 Thomas Pohl and EXXETA AG
 * http://www.exxeta.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exxeta.iss.sonar.esql.check;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.sonar.check.Rule;

import com.exxeta.iss.sonar.esql.api.tree.expression.CallExpressionTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CallStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateFunctionStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateModuleStatementTree;
import com.exxeta.iss.sonar.esql.api.tree.statement.CreateProcedureStatementTree;
import com.exxeta.iss.sonar.esql.api.visitors.DoubleDispatchVisitorCheck;
import com.exxeta.iss.sonar.esql.api.visitors.IssueLocation;
import com.exxeta.iss.sonar.esql.api.visitors.PreciseIssue;

@Rule(key = "UnusedRoutine")
public class UnusedRoutineCheck extends DoubleDispatchVisitorCheck {

	private static final String MESSAGE = "Remove the unused %s \"%s\".";

	private Set<String> calledProcedures = new HashSet<>();
	private Hashtable<String, CreateProcedureStatementTree> declaredProcedures = new Hashtable<>();
	private Set<String> calledFunctions = new HashSet<>();
	private Hashtable<String, CreateFunctionStatementTree> declaredFunctions = new Hashtable<>();

	@Override
	public void visitCreateModuleStatement(CreateModuleStatementTree tree) {
		calledFunctions.clear();
		calledProcedures.clear();
		declaredFunctions.clear();
		declaredProcedures.clear();
		super.visitCreateModuleStatement(tree);
		for (String function : calledFunctions) {
			declaredFunctions.remove(function);
		}
		for (String procedure : calledProcedures) {
			declaredProcedures.remove(procedure);
		}
		for (String function : declaredFunctions.keySet()) {
			if (!function.equalsIgnoreCase("Main")) {
				addIssue(new PreciseIssue(this, new IssueLocation(declaredFunctions.get(function),
						declaredFunctions.get(function), String.format(MESSAGE, "function", function))));
			}
		}
		for (String procedure : declaredProcedures.keySet()) {
			addIssue(new PreciseIssue(this, new IssueLocation(declaredProcedures.get(procedure),
					declaredProcedures.get(procedure), String.format(MESSAGE, "procedure", procedure))));
		}
	}

	@Override
	public void visitCreateProcedureStatement(CreateProcedureStatementTree tree) {
		declaredProcedures.put(tree.identifier().text(), tree);
		super.visitCreateProcedureStatement(tree);
	}

	@Override
	public void visitCreateFunctionStatement(CreateFunctionStatementTree tree) {
		declaredFunctions.put(tree.identifier().text(), tree);
		super.visitCreateFunctionStatement(tree);
	}

	@Override
	public void visitCallExpression(CallExpressionTree tree) {
		if (tree.functionName() != null) {
			calledFunctions.add(tree.functionName().pathElement().name().text());
		}
		super.visitCallExpression(tree);
	}

	@Override
	public void visitCallStatement(CallStatementTree tree) {
		if (tree.routineName() != null) {
			calledProcedures.add(tree.routineName().text());
		}
		super.visitCallStatement(tree);
	}
}