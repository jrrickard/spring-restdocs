/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.restdocs.templates;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link TemplateResourceResolver}.
 * 
 * @author Andy Wilkinson
 */
public class StandardTemplateResourceResolverTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final TemplateResourceResolver resolver = new StandardTemplateResourceResolver();

	private final TestClassLoader classLoader = new TestClassLoader();

	@Test
	public void customSnippetResolution() throws Exception {
		this.classLoader.addResource(
				"org/springframework/restdocs/templates/test.snippet", getClass()
						.getResource("test.snippet"));
		Resource snippet = doWithThreadContextClassLoader(this.classLoader,
				new Callable<Resource>() {

					@Override
					public Resource call() {
						return StandardTemplateResourceResolverTests.this.resolver
								.resolveTemplateResource("test");
					}

				});

		assertThat(snippet.getURL(), is(equalTo(getClass().getResource("test.snippet"))));
	}

	@Test
	public void fallsBackToDefaultSnippet() throws Exception {
		this.classLoader.addResource(
				"org/springframework/restdocs/templates/default-test.snippet", getClass()
						.getResource("test.snippet"));
		Resource snippet = doWithThreadContextClassLoader(this.classLoader,
				new Callable<Resource>() {

					@Override
					public Resource call() {
						return StandardTemplateResourceResolverTests.this.resolver
								.resolveTemplateResource("test");
					}

				});

		assertThat(snippet.getURL(), is(equalTo(getClass().getResource("test.snippet"))));
	}

	@Test
	public void failsIfCustomAndDefaultSnippetDoNotExist() throws Exception {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(equalTo("Template named 'test' could not be resolved"));
		doWithThreadContextClassLoader(this.classLoader, new Callable<Resource>() {

			@Override
			public Resource call() {
				return StandardTemplateResourceResolverTests.this.resolver
						.resolveTemplateResource("test");
			}

		});
	}

	private <T> T doWithThreadContextClassLoader(ClassLoader classLoader,
			Callable<T> action) throws Exception {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			return action.call();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	private static class TestClassLoader extends ClassLoader {

		private Map<String, URL> resources = new HashMap<>();

		private void addResource(String name, URL url) {
			this.resources.put(name, url);
		}

		@Override
		public URL getResource(String name) {
			return this.resources.get(name);
		}

	}

}
