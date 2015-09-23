package com.alibaba.dubbo.container.jetty;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

//import org.mortbay.jetty.Handler;
//import org.mortbay.jetty.Server;
//import org.mortbay.jetty.nio.SelectChannelConnector;
//import org.mortbay.jetty.servlet.FilterHolder;
//import org.mortbay.jetty.servlet.ServletHandler;
//import org.mortbay.jetty.servlet.ServletHolder;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.container.Container;
import com.alibaba.dubbo.container.page.PageServlet;
import com.alibaba.dubbo.container.page.ResourceFilter;

/**
 * JettyContainer. (SPI, Singleton, ThreadSafe)
 * 
 * @author william.liangf
 */
public class JettyContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(JettyContainer.class);

    public static final String JETTY_PORT = "dubbo.jetty.port";

    public static final String JETTY_DIRECTORY = "dubbo.jetty.directory";

    public static final String JETTY_PAGES = "dubbo.jetty.page";

    public static final int DEFAULT_JETTY_PORT = 8080;

    //SelectChannelConnector connector;

	private Server server;

    public void start() {
        String serverPort = ConfigUtils.getProperty(JETTY_PORT);
        int port;
        if (serverPort == null || serverPort.length() == 0) {
            port = DEFAULT_JETTY_PORT;
        } else {
            port = Integer.parseInt(serverPort);
        }
//        connector = new SelectChannelConnector();
//        connector.setPort(port);
        //
        ServletContextHandler context = new ServletContextHandler(  
                ServletContextHandler.SESSIONS);  
        context.setContextPath("/");  
        
        String resources = ConfigUtils.getProperty(JETTY_DIRECTORY);
        if (resources != null && resources.length() > 0) {
            FilterHolder resourceHolder =new FilterHolder(ResourceFilter.class);
            
            //, "/*", Handler.DEFAULT);
            resourceHolder.setInitParameter("resources", resources);
            
            context.addFilter(resourceHolder,"/*",EnumSet.allOf(DispatcherType.class));  
            
        }
       
        
        ServletHolder pageHolder = new ServletHolder(PageServlet.class);
        pageHolder.setInitParameter("pages", ConfigUtils.getProperty(JETTY_PAGES));
        pageHolder.setInitOrder(2);
        context.addServlet(pageHolder, "/*");
        
        server = new Server(port);
        server.setHandler(context);  
        
        try {
            server.start();
            server.join();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start jetty server on " + NetUtils.getLocalHost() + ":" + port + ", cause: " + e.getMessage(), e);
        }
    }

    public void stop() {
        try {
            if (server != null) {
            	server.destroy();
            	server = null;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

}