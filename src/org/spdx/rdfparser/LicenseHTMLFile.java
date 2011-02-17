/**
 * Copyright (c) 2011 Source Auditor Inc.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spdx.rdfparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains a formatted HTML file for a given license.  Specific
 * formatting information is contained in this file.
 * @author Gary O'Neall
 *
 */
public class LicenseHTMLFile {
	static final String HTML_BEFORE_TITLE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\" \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n"+
			"<html xmlns=\"http://www.w3.org/1999/xhtml\"\n"+
			"	xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"+
			"	xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"+
			"	xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"+
			"	xmlns:dc=\"http://purl.org/dc/terms/\"\n"+
			"	xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n"+
			"	xmlns:spdx=\"http://spdx.org/spec#\">\n"+
			"<head>\n"+
			"	<title></title>\n"+
			"	<link rel=\"stylesheet\" href=\"screen.css\" media=\"screen\" type=\"text/css\" />\n"+
			"</head>\n"+
			"<body typeof=\"spdx:License\">\n"+
			"	<h1 property=\"dc:title\">";
	static final String AFTER_TITLE = "</h1>\n\n<p>Back to the <a href=\"";
	static final String AFTER_TOC_REFERENCE = "\">License List</a></p>\n\n<h2 id=\"information\">Information</h2>\n<dl>\n"+
			"			<dt>Full name</dt>\n"+
			"			<dd property=\"rdfs:label\">";
	static final String AFTER_LICENSE_NAME = "</dd>\n\n<dt>Short identifier</dt>\n<dd><code>";
	static final String AFTER_SHORT_ID = "</dd>\n";
	static final String WEBURL = "[WEBURL]";
	static final String SITE = "[SITE]";
	static final String OTHER_WEB_PAGE_FIRST = "<dt>Other web pages for this license</dt>\n"+
			"		<dd>\n"+
			"			<ul>\n";
	static final String OTHER_WEB_PAGE_ROW = 
			"				<li><a href=\""+WEBURL+"\" rel=\"owl:sameAs\">"+SITE+"</a></li>\n";
	static final String OTHER_WEB_PAGE_END = "</ul>\n</dd>\n</dl>\n";
	static final String BEFORE_NOTES = "</dl>\n";
	static final String NOTES = "[NOTES]";
	static final String NOTES_HTML = "<h2 id=\"notes\">Notes</h2>\n\n<p>"+NOTES+"</p>";
	static final String LICENSE_TEXT = "[LICENSE_TEXT]";
	static final String TEXT_HTML = "<h2 id=\"licenseText\">Text</h2>\n\n<div property=\"spdx:LicenseText\" class=\"license-text\">\n"+
							LICENSE_TEXT+"\n</div>\n";
	static final String END_HTML = "</body>\n</html>";

	static final Pattern SITE_PATTERN = Pattern.compile("http://(.*)\\.(.*)(\\.org|\\.com|\\.net|\\.info)");
	
	private SPDXLicense license;
	public LicenseHTMLFile(SPDXLicense license) {
		this.license = license;
	}
	public LicenseHTMLFile() {
		this.license = null;
	}
	
	/**
	 * @return the license
	 */
	public SPDXLicense getLicense() {
		return license;
	}

	/**
	 * @param license the license to set
	 */
	public void setLicense(SPDXLicense license) {
		this.license = license;
	}

	public void writeToFile(File htmlFile, String tableOfContentsReference) throws IOException {
		FileOutputStream stream = null;
		OutputStreamWriter writer = null;
		if (!htmlFile.exists()) {
			if (!htmlFile.createNewFile()) {
				throw(new IOException("Can not create new file "+htmlFile.getName()));
			}
		}
		try {
			stream = new FileOutputStream(htmlFile);
			writer = new OutputStreamWriter(stream);
			writer.write(HTML_BEFORE_TITLE);
			writer.write(escapeHTML(license.getName()));
			writer.write(AFTER_TITLE);
			writer.write(tableOfContentsReference);
			writer.write(AFTER_TOC_REFERENCE);
			writer.write(escapeHTML(license.getName()));
			writer.write(AFTER_LICENSE_NAME);
			writer.write(escapeHTML(license.getId()));
			writer.write(AFTER_SHORT_ID);
			if (license.getSourceUrl() != null) {
				writer.write(OTHER_WEB_PAGE_FIRST);
				String[] sourceUrls = license.getSourceUrl().split("\n");
				for (int i = 0; i < sourceUrls.length; i++) {
					String url = sourceUrls[i].trim();
					String site = getSiteFromUrl(url);
					String urlRow = OTHER_WEB_PAGE_ROW.replace(WEBURL, url);
					urlRow = urlRow.replace(SITE, site);
					writer.write(urlRow);
				}
				writer.write(OTHER_WEB_PAGE_END);
			}
			writer.write(BEFORE_NOTES);
			if (license.getNotes()!= null && !license.getNotes().isEmpty()) {
				writer.write(NOTES_HTML.replace(NOTES, escapeHTML(license.getNotes())));
			}
			writer.write(TEXT_HTML.replace(LICENSE_TEXT, escapeHTML(license.getText())));
			writer.write(END_HTML);
		} catch (FileNotFoundException e) {
			throw(e);
		} finally {
			if (writer != null) {
				writer.close();
			}
			if (stream != null) {
				stream.close();
			}
		}
	}
	private String getSiteFromUrl(String url) {
		Matcher matcher = SITE_PATTERN.matcher(url);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return url;
		}
	}
	private String escapeHTML(String text) {
		return text.replace("\n", "<br/>\n");
	}
}
