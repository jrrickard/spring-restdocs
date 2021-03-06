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

package org.springframework.restdocs.mockmvc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.restdocs.curl.CurlDocumentation.curlRequest;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.maskLinks;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.replacePattern;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.snippet.Attributes.attributes;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.restdocs.test.SnippetMatchers.httpRequest;
import static org.springframework.restdocs.test.SnippetMatchers.httpResponse;
import static org.springframework.restdocs.test.SnippetMatchers.snippet;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.RestDocumentation;
import org.springframework.restdocs.hypermedia.Link;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationIntegrationTests.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Integration tests for Spring REST Docs
 * 
 * @author Andy Wilkinson
 * @author Dewet Diener
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = TestConfiguration.class)
public class MockMvcRestDocumentationIntegrationTests {

	@Rule
	public RestDocumentation restDocumentation = new RestDocumentation(
			"build/generated-snippets");

	@Autowired
	private WebApplicationContext context;

	@Before
	public void deleteSnippets() {
		FileSystemUtils.deleteRecursively(new File("build/generated-snippets"));
	}

	@After
	public void clearOutputDirSystemProperty() {
		System.clearProperty("org.springframework.restdocs.outputDir");
	}

	@Test
	public void basicSnippetGeneration() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(document("basic"));
		assertExpectedSnippetFilesExist(new File("build/generated-snippets/basic"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc");
	}

	@Test
	public void linksSnippet() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("links",
						links(linkWithRel("rel").description("The description"))));

		assertExpectedSnippetFilesExist(new File("build/generated-snippets/links"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc",
				"links.adoc");
	}

	@Test
	public void pathParametersSnippet() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("{foo}", "/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("links", pathParameters(parameterWithName("foo")
						.description("The description"))));

		assertExpectedSnippetFilesExist(new File("build/generated-snippets/links"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc",
				"path-parameters.adoc");
	}

	@Test
	public void requestParametersSnippet() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("/").param("foo", "bar").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("links", requestParameters(parameterWithName("foo")
						.description("The description"))));

		assertExpectedSnippetFilesExist(new File("build/generated-snippets/links"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc",
				"request-parameters.adoc");
	}

	@Test
	public void requestFieldsSnippet() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(
				get("/").param("foo", "bar").content("{\"a\":\"alpha\"}")
						.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("links",
						requestFields(fieldWithPath("a").description("The description"))));

		assertExpectedSnippetFilesExist(new File("build/generated-snippets/links"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc",
				"request-fields.adoc");
	}

	@Test
	public void responseFieldsSnippet() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("/").param("foo", "bar").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document(
						"links",
						responseFields(
								fieldWithPath("a").description("The description"),
								fieldWithPath("links").description(
										"Links to other resources"))));

		assertExpectedSnippetFilesExist(new File("build/generated-snippets/links"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc",
				"response-fields.adoc");
	}

	@Test
	public void parameterizedOutputDirectory() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andDo(document("{method-name}"));
		assertExpectedSnippetFilesExist(new File(
				"build/generated-snippets/parameterized-output-directory"),
				"http-request.adoc", "http-response.adoc", "curl-request.adoc");
	}

	@Test
	public void multiStep() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation))
				.alwaysDo(document("{method-name}-{step}")).build();

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());
		assertExpectedSnippetFilesExist(
				new File("build/generated-snippets/multi-step-1/"), "http-request.adoc",
				"http-response.adoc", "curl-request.adoc");

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());
		assertExpectedSnippetFilesExist(
				new File("build/generated-snippets/multi-step-2/"), "http-request.adoc",
				"http-response.adoc", "curl-request.adoc");

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk());
		assertExpectedSnippetFilesExist(
				new File("build/generated-snippets/multi-step-3/"), "http-request.adoc",
				"http-response.adoc", "curl-request.adoc");
	}

	@Test
	public void preprocessedRequest() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		Pattern pattern = Pattern.compile("(\"alpha\")");

		mockMvc.perform(
				get("/").header("a", "alpha").header("b", "bravo")
						.contentType(MediaType.APPLICATION_JSON)
						.accept(MediaType.APPLICATION_JSON).content("{\"a\":\"alpha\"}"))
				.andExpect(status().isOk())
				.andDo(document("original-request"))
				.andDo(document(
						"preprocessed-request",
						preprocessRequest(prettyPrint(), removeHeaders("a"),
								replacePattern(pattern, "\"<<beta>>\""))));

		assertThat(
				new File("build/generated-snippets/original-request/http-request.adoc"),
				is(snippet().withContents(
						httpRequest(RequestMethod.GET, "/").header("Host", "localhost")
								.header("a", "alpha").header("b", "bravo")
								.header("Content-Type", "application/json")
								.header("Accept", MediaType.APPLICATION_JSON_VALUE)
								.header("Content-Length", "13")
								.content("{\"a\":\"alpha\"}"))));
		String prettyPrinted = String.format("{%n  \"a\" : \"<<beta>>\"%n}");
		assertThat(
				new File(
						"build/generated-snippets/preprocessed-request/http-request.adoc"),
				is(snippet()
						.withContents(
								httpRequest(RequestMethod.GET, "/")
										.header("Host", "localhost")
										.header("b", "bravo")
										.header("Content-Type", "application/json")
										.header("Accept",
												MediaType.APPLICATION_JSON_VALUE)
										.header("Content-Length",
												Integer.toString(prettyPrinted.getBytes().length))
										.content(prettyPrinted))));
	}

	@Test
	public void preprocessedResponse() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		Pattern pattern = Pattern.compile("(\"alpha\")");

		mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("original-response"))
				.andDo(document(
						"preprocessed-response",
						preprocessResponse(prettyPrint(), maskLinks(),
								removeHeaders("a"),
								replacePattern(pattern, "\"<<beta>>\""))));

		assertThat(
				new File("build/generated-snippets/original-response/http-response.adoc"),
				is(snippet().withContents(
						httpResponse(HttpStatus.OK)
								.header("a", "alpha")
								.header("Content-Type", "application/json")
								.content(
										"{\"a\":\"alpha\",\"links\":[{\"rel\":\"rel\","
												+ "\"href\":\"href\"}]}"))));
		assertThat(
				new File(
						"build/generated-snippets/preprocessed-response/http-response.adoc"),
				is(snippet().withContents(
						httpResponse(HttpStatus.OK).header("Content-Type",
								"application/json").content(
								String.format("{%n  \"a\" : \"<<beta>>\",%n  \"links\" :"
										+ " [ {%n    \"rel\" : \"rel\",%n    \"href\" :"
										+ " \"...\"%n  } ]%n}")))));
	}

	@Test
	public void customSnippetTemplate() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)).build();

		ClassLoader classLoader = new URLClassLoader(new URL[] { new File(
				"src/test/resources/custom-snippet-templates").toURI().toURL() },
				getClass().getClassLoader());
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andDo(document("custom-snippet-template"));
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
		assertThat(new File(
				"build/generated-snippets/custom-snippet-template/curl-request.adoc"),
				is(snippet().withContents(equalTo("Custom curl request"))));

		mockMvc.perform(get("/")).andDo(
				document(
						"index",
						curlRequest(attributes(key("title").value(
								"Access the index using curl")))));
	}

	private void assertExpectedSnippetFilesExist(File directory, String... snippets) {
		for (String snippet : snippets) {
			assertTrue(new File(directory, snippet).isFile());
		}
	}

	@Configuration
	@EnableWebMvc
	static class TestConfiguration extends WebMvcConfigurerAdapter {

		@Bean
		public TestController testController() {
			return new TestController();
		}

	}

	@RestController
	static class TestController {

		@RequestMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
		public ResponseEntity<Map<String, Object>> foo() {
			Map<String, Object> response = new HashMap<>();
			response.put("a", "alpha");
			response.put("links", Arrays.asList(new Link("rel", "href")));
			HttpHeaders headers = new HttpHeaders();
			headers.add("a", "alpha");
			return new ResponseEntity<Map<String, Object>>(response, headers,
					HttpStatus.OK);
		}

	}

}
