/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Web application lifecycle listener.
 *
 * @author Chhavi kumar.b
 */
public class ContextListener implements ServletContextListener {

    Logger logger = null;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        logger.info("Context Initialized.");
        System.out.println("Context Initializing...");
        String configPath = System.getProperty("verifyface.props");
        if (StringUtils.isBlank(configPath)) {
//            configPath = "D:\\Neurotec_Biometric_10_0_SDK_Trial\\config\\api.properties";
            configPath = "D:\\Neurotec_Biometric_10_0_SDK_Trial\\config\\api.properties";
        }
        System.out.println("configPath before override*********" + configPath);
//        configPath = "E:\\verifyface\\config\\api.properties";
        System.out.println("configPath*********" + configPath);
        if (configPath != null && configPath.length() > 0) {
            ConfigUtil configUtil = new ConfigUtil();
            boolean isInit = configUtil.init(configPath);
            System.out.println("================Init props =>"+isInit);
            if (isInit) {
                System.out.println("================Init success");
                System.setProperty("props.file.path", configPath);
                PropertyConfigurator.configureAndWatch(ConfigUtil.getProperty("api.log4j.path", "/apps/oh-policy-mgr/config/log4j.properties"), Long.valueOf(ConfigUtil.getProperty("api.ump.log4j.ideal.timeout", "6000")));
                logger = Logger.getLogger(ContextListener.class);
                logger.info("Configurations loaded.");
                System.out.println("Configurations loaded.");
            } else {
                System.out.println("Configpath => "+configPath);
                System.out.println("Configurations were not loaded.SO we are going to shut down the tomcat.");
                try {
                    Thread.sleep(30000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        } else {
            logger.info("Configuration path is empty or coming null");
            System.out.println("Configuration path is empty or coming null");
        }
        System.out.println("Last Line..........");

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Application Context is destroyed.");
        System.out.println("Application Context is destroyed.");
        
    }
}
