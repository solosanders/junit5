/*
 * Copyright 2015 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.task;

import static org.junit.gen5.commons.util.ReflectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

public class MethodTask<T> implements ExecutionTask {

	private final Class<T> target;
	private final T instance;
	private final Method method;


	MethodTask(Class<T> target, Method method, T instance) {
		this.target = target;
		this.method = method;
		this.instance = instance;
	}

	@Override
	public void execute() throws Exception {
		System.out.println("--> TASK: " + this.getClass().getSimpleName() + " - " + this.method.getName() + "()");

		invokeMethod(this.method, this.instance);
	}

	@Override
	public List<ExecutionTask> getChildren() {
		return null;
	}

	@Override
	public String toString() {
		return this.method.getName() + "()";
	}
}