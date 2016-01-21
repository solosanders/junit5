/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.resolver;

import static java.util.stream.Collectors.toList;
import static org.junit.gen5.commons.util.ReflectionUtils.*;
import static org.junit.gen5.engine.discovery.PackageSelector.forPackageName;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.gen5.commons.util.Preconditions;
import org.junit.gen5.engine.DiscoverySelector;
import org.junit.gen5.engine.EngineDiscoveryRequest;
import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.discovery.ClassSelector;
import org.junit.gen5.engine.discovery.PackageSelector;
import org.junit.gen5.engine.junit5.descriptor.ClassTestDescriptor;
import org.junit.gen5.engine.junit5.descriptor.PackageTestDescriptor;

public class ClassResolver extends JUnit5TestResolver {
	public static ClassTestDescriptor descriptorForParentAndClass(TestDescriptor parent, Class<?> testClass) {
		String packageName = testClass.getPackage().getName();
		String fullQualifiedClassName = testClass.getCanonicalName();
		String className = fullQualifiedClassName.substring(packageName.length() + 1);
		String uniqueId = parent.getUniqueId() + "/[class:" + className + "]";

		if (parent.findByUniqueId(uniqueId).isPresent()) {
			return (ClassTestDescriptor) parent.findByUniqueId(uniqueId).get();
		}
		else {
			return new ClassTestDescriptor(uniqueId, testClass);
		}
	}

	@Override
	public void resolveAllFrom(TestDescriptor parent, EngineDiscoveryRequest discoveryRequest) {
		Preconditions.notNull(parent, "parent must not be null!");
		Preconditions.notNull(discoveryRequest, "discoveryRequest must not be null!");

		List<TestDescriptor> classDescriptors = new LinkedList<>();
		if (parent.isRoot()) {
			classDescriptors.addAll(resolveClassesFromSelectors(parent, discoveryRequest));
		}
		else if (parent instanceof PackageTestDescriptor) {
			String packageName = ((PackageTestDescriptor) parent).getPackageName();
			classDescriptors.addAll(resolveTopLevelClassesInPackage(packageName, parent, discoveryRequest));
		}

		for (TestDescriptor child : classDescriptors) {
			getTestResolverRegistry().notifyResolvers(child, discoveryRequest);
		}
	}

	private List<TestDescriptor> resolveClassesFromSelectors(TestDescriptor root,
			EngineDiscoveryRequest discoveryRequest) {
		// @formatter:off
        return discoveryRequest.getSelectorsByType(ClassSelector.class).stream()
                .map(classSelector -> fetchBySelector(classSelector, root))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
        // @formatter:on
	}

	@Override
	public Optional<TestDescriptor> fetchBySelector(DiscoverySelector selector, TestDescriptor root) {
		if (selector instanceof ClassSelector) {
			Class<?> testClass = ((ClassSelector) selector).getTestClass();
			if (isTopLevelTestClass(testClass)) {
				PackageSelector packageSelector = forPackageName(testClass.getPackage().getName());
				TestDescriptor parent = getTestResolverRegistry().fetchParent(packageSelector, root);
				return getTestDescriptor(parent, testClass);
			}
		}
		return Optional.empty();
	}

	private List<TestDescriptor> resolveTopLevelClassesInPackage(String packageName, TestDescriptor parent,
			EngineDiscoveryRequest discoveryRequest) {
		// @formatter:off
        return findAllClassesInPackageOnly(packageName, this::isTopLevelTestClass).stream()
                .map(testClass -> descriptorForParentAndClass(parent, testClass))
                .peek(parent::addChild)
                .collect(toList());
        // @formatter:on
	}

	private boolean isTopLevelTestClass(Class<?> candidate) {
		//please do not collapse into single return
		if (isAbstract(candidate))
			return false;
		if (candidate.isLocalClass())
			return false;
		if (candidate.isAnonymousClass())
			return false;
		return !candidate.isMemberClass() || isStatic(candidate);
	}

	private Optional<TestDescriptor> getTestDescriptor(TestDescriptor parent, Class<?> testClass) {
		TestDescriptor child = descriptorForParentAndClass(parent, testClass);
		parent.addChild(child);
		return Optional.of(child);
	}
}
