/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.remoting.http.jetty;

import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
//import org.mortbay.jetty.Server;
//import org.mortbay.jetty.nio.SelectChannelConnector;
//import org.mortbay.jetty.servlet.Context;
//import org.mortbay.jetty.servlet.ServletHandler;
//import org.mortbay.jetty.servlet.ServletHolder;
//import org.mortbay.log.Log;
//import org.mortbay.log.StdErrLog;
//import org.mortbay.thread.QueuedThreadPool;
import com.alibaba.dubbo.remoting.http.support.AbstractHttpServer;

public class JettyHttpServer extends AbstractHttpServer {

	private static final Logger logger = LoggerFactory.getLogger(JettyHttpServer.class);

	private Server server;

	private URL url;

	public JettyHttpServer(URL url, final HttpHandler handler) {
		super(url, handler);

		// modified by lishen
		this.url = url;
		// TODO we should leave this setting to slf4j
		Log.setLog(new StdErrLog());
		Log.getLog().setDebugEnabled(false);

		DispatcherServlet.addHttpHandler(url.getPort(), handler);

		int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setDaemon(true);
		threadPool.setMaxThreads(threads);
		threadPool.setMinThreads(threads);

		// SelectChannelConnector connector = new SelectChannelConnector();
		// if (! url.isAnyHost() && NetUtils.isValidLocalHost(url.getHost())) {
		// connector.setHost(url.getHost());
		// }
		// connector.setPort(url.getPort());

		InetSocketAddress addr = new InetSocketAddress(url.getPort());
		if (!url.isAnyHost() && NetUtils.isValidLocalHost(url.getHost())) {
			addr = new InetSocketAddress(url.getHost(), url.getPort());
		} else {
			addr = new InetSocketAddress(url.getPort());
		}
		server = new Server(addr);
		// server.setThreadPool(threadPool);
		// server.addConnector(connector);

		//
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		//
		ServletHolder servletHolder = new ServletHolder(DispatcherServlet.class);
		servletHolder.setInitOrder(2);
		context.addServlet(servletHolder, "/*");
		//

		ServletManager.getInstance().addServletContext(url.getPort(), context.getServletContext());

		try {
			server.start();
		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed to start jetty server on " + url.getAddress() + ", cause: " + e.getMessage(), e);
		}
	}

	public void close() {
		super.close();

		// modified by lishen
		ServletManager.getInstance().removeServletContext(url.getPort());

		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

}