/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.GadgetException;
import org.apache.shindig.gadgets.http.HttpRequest;
import org.apache.shindig.gadgets.http.HttpResponse;
import org.apache.shindig.gadgets.parse.caja.CajaCssLexerParser;
import org.apache.shindig.gadgets.uri.ProxyUriManager;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.caja.lexer.ParseException;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Rewrite links to referenced content in a stylesheet
 */
public class CssRequestRewriter implements RequestRewriter {

  private static final Logger logger = Logger.getLogger(CssRequestRewriter.class.getName());

  private final CajaCssLexerParser cssParser;
  private final ProxyUriManager proxyUriManager;
  private final ContentRewriterFeature.Factory rewriterFeatureFactory;

  @Inject
  public CssRequestRewriter(CajaCssLexerParser cssParser,
      ProxyUriManager proxyUriManager, ContentRewriterFeature.Factory rewriterFeatureFactory) {
    this.cssParser = cssParser;
    this.proxyUriManager = proxyUriManager;
    this.rewriterFeatureFactory = rewriterFeatureFactory;
  }

  public boolean rewrite(HttpRequest request, HttpResponse original,
      MutableContent content) throws RewritingException {
    ContentRewriterFeature.Config config = rewriterFeatureFactory.get(request);
    if (!RewriterUtils.isCss(request, original)) {
      return false;
    }
    
    String css = content.getContent();
    StringWriter sw = new StringWriter((css.length() * 110) / 100);
    rewrite(new StringReader(css), request.getUri(),
        new UriMaker(proxyUriManager, config), sw, false);
    content.setContent(sw.toString());

    return true;
  }

  /**
   * Rewrite the given CSS content and optionally extract the import references.
   * @param content CSS content
   * @param source Uri of content
   * @param uriMaker a Uri Maker
   * @param writer Output
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Reader content, Uri source,
      UriMaker uriMaker, Writer writer, boolean extractImports) throws RewritingException {
    try {
      String original = IOUtils.toString(content);
      try {
        List<Object> stylesheet = cssParser.parse(original);
        List<String> stringList = rewrite(stylesheet, source, uriMaker, extractImports);
        // Serialize the stylesheet
        cssParser.serialize(stylesheet, writer);
        return stringList;
      } catch (GadgetException ge) {
        if (ge.getCause() instanceof ParseException) {
          logger.log(Level.WARNING,
              "Caja CSS parse failure: " + ge.getCause().getMessage() + " for " + source);
          writer.write(original);
          return Collections.emptyList();
        } else {
          throw new RewritingException(ge, ge.getHttpStatusCode());
        }
      }
    } catch (IOException ioe) {
      throw new RewritingException(ioe, HttpResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Rewrite the CSS content in a style DOM node.
   * @param styleNode Rewrite the CSS content of this node
   * @param source Uri of content
   * @param uriMaker a UriMaker
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public List<String> rewrite(Element styleNode, Uri source,
      UriMaker uriMaker, boolean extractImports) throws RewritingException {
    try {
      List<Object> stylesheet = cssParser.parse(styleNode.getTextContent());
      List<String> imports = rewrite(stylesheet, source, uriMaker, extractImports);
      // Write the rewritten CSS back into the element
      String content = cssParser.serialize(stylesheet);
      if (StringUtils.isEmpty(content) || StringUtils.isWhitespace(content)) {
        // Remove the owning node
        styleNode.getParentNode().removeChild(styleNode);
      } else {
        styleNode.setTextContent(content);
      }
      return imports;
    } catch (GadgetException ge) {
      if (ge.getCause() instanceof ParseException) {
        logger.log(Level.WARNING,
              "Caja CSS parse failure: " + ge.getCause().getMessage() + " for " + source);
        return Collections.emptyList();
      } else {
        throw new RewritingException(ge, ge.getHttpStatusCode());
      }
    }
  }

  /**
   * Rewrite the CSS DOM in place.
   * @param styleSheet To rewrite
   * @param source  Uri of content
   * @param uriMaker a UriMaker
   * @param extractImports If true remove the import statements from the output and return their
   *            referenced URIs.
   * @return Empty list of extracted import URIs.
   */
  public static List<String> rewrite(List<Object> styleSheet, final Uri source,
      final UriMaker uriMaker, final boolean extractImports) {
    final List<String> imports = Lists.newLinkedList();

    for (int i = styleSheet.size() - 1; i >= 0; i--) {
      if (styleSheet.get(i) instanceof CajaCssLexerParser.ImportDecl) {
        if (extractImports) {
          imports.add(0, ((CajaCssLexerParser.ImportDecl)styleSheet.get(i)).getUri());
          styleSheet.remove(i);
        } else {
          CajaCssLexerParser.ImportDecl importDecl = (CajaCssLexerParser.ImportDecl) styleSheet
              .get(i);
          importDecl.setUri(rewriteUri(uriMaker, importDecl.getUri(), source));
        }
      } else if (styleSheet.get(i) instanceof CajaCssLexerParser.UriDecl) {
        CajaCssLexerParser.UriDecl uriDecl = (CajaCssLexerParser.UriDecl) styleSheet
              .get(i);
          uriDecl.setUri(rewriteUri(uriMaker, uriDecl.getUri(), source));
      }
    }
    return imports;
  }
  
  private static String rewriteUri(UriMaker uriMaker, String input, Uri context) {
    Uri inboundUri = null;
    try {
      inboundUri = Uri.parse(input);
    } catch (IllegalArgumentException e) {
      // Don't rewrite at all.
      return input;
    }
    if (context != null) {
      inboundUri = context.resolve(inboundUri);
    }
    ProxyUriManager.ProxyUri proxyUri =
        new ProxyUriManager.ProxyUri(DomWalker.makeGadget(context), inboundUri);
    return uriMaker.make(proxyUri, context).toString();
  }
  
  public static UriMaker uriMaker(ProxyUriManager wrapped, ContentRewriterFeature.Config config) {
    return new UriMaker(wrapped, config);
  }
  
  public static class UriMaker {
    private final ProxyUriManager wrapped;
    private final ContentRewriterFeature.Config config;
    
    private UriMaker(ProxyUriManager wrapped, ContentRewriterFeature.Config config) {
      this.wrapped = wrapped;
      this.config = config;
    }
    
    public Uri make(ProxyUriManager.ProxyUri uri, Uri context) {
      if (config.shouldRewriteURL(uri.getResource().toString())) {
        List<ProxyUriManager.ProxyUri> puris = Lists.newArrayList(uri);
        List<Uri> returned = wrapped.make(puris, null);
        return returned.get(0);
      }
      return context.resolve(uri.getResource());
    }
  }
}

