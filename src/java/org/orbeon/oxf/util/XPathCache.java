/**
 *  Copyright (C) 2004 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.util;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.log4j.Logger;
import org.dom4j.DocumentHelper;
import org.dom4j.XPath;
import org.orbeon.oxf.cache.Cache;
import org.orbeon.oxf.cache.InternalCacheKey;
import org.orbeon.oxf.cache.ObjectCache;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.xml.XPathCacheStandaloneContext;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.xpath.StandaloneContext;
import org.orbeon.saxon.xpath.Variable;
import org.orbeon.saxon.xpath.XPathEvaluator;
import org.orbeon.saxon.xpath.XPathExpression;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Use the object cache to cache XPath expressions. Those are costly to parse.
 */
public class XPathCache {
    private static final Logger logger = LoggerFactory.createLogger(XPathCache.class);
    private static final boolean doCache = true;

    public static XPath createCacheXPath(PipelineContext context, String xpathExpression) {
        if (doCache) {
            Long validity = new Long(0);
            Cache cache = ObjectCache.instance();
            InternalCacheKey cacheKey = new InternalCacheKey("XPath Expression", xpathExpression);
            XPath xpath = (XPath) cache.findValid(context, cacheKey, validity);
            if (xpath == null) {
                xpath = DocumentHelper.createXPath(xpathExpression);
                cache.add(context, cacheKey, validity, xpath);
            }
            return xpath;
        } else {
            return DocumentHelper.createXPath(xpathExpression);
        }
    }


//    public static XPathExpression createCacheXPath20
//            (PipelineContext context, DocumentWrapper documentWrapper, NodeInfo nodeInfo,
//             String xpathExpression, Map prefixToURIMap, Map variableToValueMap, FunctionLibrary functionLibrary) {
//        try {
//            // Create Saxon XPath Evaluator
//            xpathEvaluator.setSource(documentWrapper);
//            StandaloneContext standaloneContext = (StandaloneContext) xpathEvaluator.getStaticContext();
//
//            // Declare namespaces
//            if (prefixToURIMap != null) {
//                for (Iterator i = prefixToURIMap.keySet().iterator(); i.hasNext();) {
//                    String prefix = (String) i.next();
//                    standaloneContext.declareNamespace(prefix, (String) prefixToURIMap.get(prefix));
//                }
//            }
//
//            // Declare variables
//            if (variableToValueMap != null) {
//                for (Iterator i = variableToValueMap.keySet().iterator(); i.hasNext();) {
//                    String repeatId = (String) i.next();
//                    Integer index = (Integer) variableToValueMap.get(repeatId);
//                    standaloneContext.declareVariable(repeatId, index);
//                }
//            }
//
//            // Add function library
//            if (functionLibrary != null) {
//                ((FunctionLibraryList) standaloneContext.getFunctionLibrary()).libraryList.add(0, functionLibrary);
//            }
//
//            XPathExpression exp = xpathEvaluator.createExpression(xpathExpression);
//            // Context node
//            if (nodeInfo != null) {
//                //somehow we need to set it both to the evaluator and the expression
//                xpathEvaluator.setContextNode(nodeInfo);
//                exp.setContextNode(nodeInfo);
//            }
//
//            return exp;
//        } catch (XPathException e) {
//            throw new OXFException(e);
//        }
//    }

    public static PooledXPathExpression getXPathExpression(PipelineContext pipelineContext,
                                                           NodeInfo contextNode,
                                                           String xpathExpression) {
        return getXPathExpression(pipelineContext, contextNode, xpathExpression, null, null, null);
    }

    public static PooledXPathExpression getXPathExpression(PipelineContext pipelineContext,
                                                           NodeInfo contextNode,
                                                           String xpathExpression,
                                                           Map prefixToURIMap) {
        return getXPathExpression(pipelineContext, contextNode, xpathExpression, prefixToURIMap, null, null);
    }

    public static PooledXPathExpression getXPathExpression(PipelineContext pipelineContext,
                                                           NodeInfo contextNode,
                                                           String xpathExpression,
                                                           Map prefixToURIMap,
                                                           Map variableToValueMap) {
        return getXPathExpression(pipelineContext, contextNode, xpathExpression, prefixToURIMap, variableToValueMap, null);
    }


    public static PooledXPathExpression getXPathExpression(PipelineContext pipelineContext,
                                                           NodeInfo contextNode,
                                                           String xpathExpression,
                                                           Map prefixToURIMap,
                                                           Map variableToValueMap,
                                                           FunctionLibrary functionLibrary) {
        try {
            Long validity = new Long(0);
            Cache cache = ObjectCache.instance();
            String cacheKeyString = xpathExpression;
            if (variableToValueMap != null)
                for (Iterator i = variableToValueMap.keySet().iterator(); i.hasNext();) {
                    cacheKeyString = cacheKeyString + (String) i.next();
                }
            if (functionLibrary != null)
                cacheKeyString = cacheKeyString + functionLibrary.hashCode();

            InternalCacheKey cacheKey = new InternalCacheKey("XPath Expression2", cacheKeyString);
            ObjectPool pool = (ObjectPool) cache.findValid(pipelineContext, cacheKey, validity);
            if (pool == null) {
                pool = createXPathPool(contextNode, xpathExpression, prefixToURIMap, variableToValueMap, functionLibrary);
                cache.add(pipelineContext, cacheKey, validity, pool);
            }
            Object o = pool.borrowObject();
            PooledXPathExpression expr = (PooledXPathExpression) o;
            expr.setContextNode(contextNode);

            if (variableToValueMap != null) {
                for (Iterator i = variableToValueMap.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    expr.setVariable(name, variableToValueMap.get(name));
                }
            }
            return expr;
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }


    private static ObjectPool createXPathPool(NodeInfo contextNode,
                                              String xpathExpression,
                                              Map prefixToURIMap,
                                              Map variableToValueMap,
                                              FunctionLibrary functionLibrary) {
        try {
            SoftReferenceObjectPool pool = new SoftReferenceObjectPool();

            pool.setFactory(new CachedPoolableObjetFactory(pool, contextNode, xpathExpression,
                    prefixToURIMap, variableToValueMap, functionLibrary));

            return pool;
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }


    private static class CachedPoolableObjetFactory implements PoolableObjectFactory {
        private final NodeInfo contextNode;
        private final String xpathExpression;
        private final Map prefixToURIMap;
        private final Map variableToValueMap;
        private final FunctionLibrary functionLibrary;
        private final ObjectPool pool;

        public CachedPoolableObjetFactory(ObjectPool pool,
                                          NodeInfo contextNode,
                                          String xpathExpression,
                                          Map prefixToURIMap,
                                          Map variableToValueMap,
                                          FunctionLibrary functionLibrary) {
            this.pool = pool;
            this.contextNode = contextNode;
            this.xpathExpression = xpathExpression;
            this.prefixToURIMap = prefixToURIMap;
            this.variableToValueMap = variableToValueMap;
            this.functionLibrary = functionLibrary;
        }

        public void activateObject(Object o) throws Exception {
        }

        public void destroyObject(Object o) throws Exception {
            if (o instanceof PooledXPathExpression) {
                PooledXPathExpression xp = (PooledXPathExpression) o;
                xp.destroy();
            } else
                throw new OXFException(o.toString() + " is not a PooledXPathExpression");
        }

        public Object makeObject() throws Exception {
            if (logger.isDebugEnabled())
                logger.debug("makeObject(" + xpathExpression + ")");

            // Create Saxon XPath Evaluator
            XPathEvaluator evaluator = new XPathEvaluator(contextNode.getDocumentRoot());
            StandaloneContext origContext = (StandaloneContext) evaluator.getStaticContext();

            // HACK: Workaround Saxon bug: need to allocate enough Slots to accomodate all variables
            int numberVariables = 1;  // at least 1 if LetExpression is used
            int index = 0;
            while(index != -1) {
                index = xpathExpression.indexOf("$", index+1);
                numberVariables++;
            }
            origContext.setBaseURI(contextNode.getDocumentRoot().getBaseURI());
            StandaloneContext standaloneContext = new XPathCacheStandaloneContext(origContext, numberVariables);
            evaluator.setStaticContext(standaloneContext);


            // Declare namespaces
            if (prefixToURIMap != null) {
                for (Iterator i = prefixToURIMap.keySet().iterator(); i.hasNext();) {
                    String prefix = (String) i.next();
                    standaloneContext.declareNamespace(prefix, (String) prefixToURIMap.get(prefix));
                }
            }

            // Declare variables
            Map variables = new HashMap();
            if (variableToValueMap != null) {
                for (Iterator i = variableToValueMap.keySet().iterator(); i.hasNext();) {
                    String name = (String) i.next();
                    Object value = variableToValueMap.get(name);
                    Variable var = standaloneContext.declareVariable(name, value);
                    variables.put(name, var);
                }
            }
            
            // Add function library
            if (functionLibrary != null) {
                ((FunctionLibraryList) standaloneContext.getFunctionLibrary()).libraryList.add(0, functionLibrary);
            }

            XPathExpression exp = evaluator.createExpression(xpathExpression);
            return new PooledXPathExpression(exp, pool, standaloneContext, variables);
        }

        public void passivateObject(Object o) throws Exception {
        }

        public boolean validateObject(Object o) {
            return true;
        }
    }


}
