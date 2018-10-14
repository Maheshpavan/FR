/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author Balaji.s
 */
public class ConfigUtil {

    private static final Logger logger = Logger.getLogger(ConfigUtil.class);
    private static Properties m_props = new Properties();

    public String m_strPropsPath = "";
    public Connection m_conn = null;
    public boolean isStopthread = false;
    int m_nCurrentMaxVersion = 0;
    String m_strParamSetName = null;
    String m_strModuleName = null;
    int m_nPropLoadTime = 600000;
    String m_strConfigParamSetQuery = null;
    String m_strSysActiveConfigQuery = null;
    String m_strConfigSetversionQuery = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

    public boolean init(String fileLoc) {
        if (logger.isInfoEnabled()) {
            logger.info("Inside ConfigUtil Init method");
        }
        Statement st = null;
        ResultSet rs = null;
        try {
            m_props = Utilities.loadFile(fileLoc);
            if (m_props != null) {
                System.out.println("Loaded Properties from File :: " + m_props);
                m_strModuleName = m_props.getProperty("ump.param.modulename", "ump-test").trim();
                if (logger.isInfoEnabled()) {
                    logger.info("ConfigUtil init() : Module names : " + m_strModuleName);
                }
            // code added to get the queries file path
                //String strQueriesFilePath = m_props.getProperty("ump.queries.file.path");
                //FileInputStream fin1 = new FileInputStream(strQueriesFilePath);
                //if(logger.isInfoEnabled()){
                //logger.info("strQueriesFilePath : " + strQueriesFilePath);
                // }
                //m_props.load(fin1);
                //.close();

                //Property for loading Active Config based on state from table : sys_active_config
                m_strSysActiveConfigQuery = m_props.getProperty("ump.select.sys_active_config.state");

                //Property for loading Config Params based on moduleId and Active Set name
                m_strConfigParamSetQuery = m_props.getProperty("ump.select.config_param_sets.state");

                //Property for loading Config params based on version check
                m_strConfigSetversionQuery = m_props.getProperty("ump.select.config_param_sets.version");
                DBConnection dbconnection = null;
                dbconnection = DBConnection.getInstance();
                m_conn = dbconnection.getConnection();
                if (m_conn == null) {
                    logger.error("ConfigUtil init() failed, connection failed....", new Exception());
                    return false;
                } else {
                    if (m_strSysActiveConfigQuery != null) {
                        st = m_conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        rs = st.executeQuery(m_strSysActiveConfigQuery);
                        if (rs.next()) {
                            m_strParamSetName = rs.getString(1);
                        }
                        if (logger.isInfoEnabled()) {
                            logger.info("ConfigUtil init() Active Pram Set name : " + m_strParamSetName);
                        }
                    } else {
                        logger.error("unable to load the 'ump.select.sys_active_config.state' property from properties file");
                    }
                    if (m_strConfigSetversionQuery != null) {
                        String strConfigSetversionQuery = m_strConfigSetversionQuery.replaceAll("\\$\\(currentmaxversion\\)", "" + m_nCurrentMaxVersion);
                        rs = st.executeQuery(strConfigSetversionQuery);
                        if (rs.next()) {
                            m_nCurrentMaxVersion = rs.getInt(1);
                        }
                    } else {
                        logger.error("unable to load the 'ump.select.config_param_sets.version' property from properties file");
                    }
                }
            } else {
                if (logger.isInfoEnabled()) {
                    logger.info("Property file load fail .. [path = " + fileLoc + "]");
                }
                return false;
            }

        } catch (Exception e) {
            logger.error(Utilities.getStackTrace(e));
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
            } catch (Exception ex) {
                logger.error(Utilities.getStackTrace(ex));
            }
            try {
                if (st != null) {
                    st.close();
                    st = null;
                }
            } catch (Exception ex) {
                logger.error(Utilities.getStackTrace(ex));

            }
        }
        //Loading Configurations based on Active set
        loadProperties();
//        new LoadPropertiesRefreshThread(this).start();
        return true;
    }

    public static Properties getProperties() {
//        m_props = attchLocalProps(m_props);
        return m_props;
    }

    public static String getProperty(String key, String defaultValue) {
        String val = m_props.getProperty(key, defaultValue);
//        if (logger.isDebugEnabled()) {
//            logger.debug("gettig property :::  key=" + key + " :: default value=" + defaultValue + " :: returning value=" + val);
//        }

//        val = getLocalVals(key, val);
        return val;
    }

    public static String getProperty(String key) {
        String val = m_props.getProperty(key);
//        if (logger.isDebugEnabled()) {
//            logger.debug("gettig property :::  key=" + key + " :: returning value=" + val);
//        }
//        val = getLocalVals(key, val);
        return val;

    }

    public static String getLocalVals(String key, String val) {
        switch (key) {
            case "creditmgr.props.file.path":
                val = "D:\\\\SVN\\\\OPENHOUSE\\\\UMP\\\\source\\\\trunk\\\\main\\\\ump-policy-manager\\\\config\\\\umppolicymgr.properties";
                break;
            case "qhandlers.classpath":
                val = "E:/qmanager/messagingGW2.0/handlers/queue/";
                break;
            case "qhandlers.jars":
                val = "E:/qmanager/messagingGW2.0/libs/";
                break;
            case "api.ump.log4j.path":
                val = "D:/SVN/OPENHOUSE/UMP/source/trunk/main/api-ump/config/api-ump-log4j.properties";
                break;
            case "api.ump.log4j.ideal.timeout":
                val = "6000";
                break;
        }
        return val;
    }

    public static Properties attchLocalProps(Properties props) {
        props.put("creditmgr.props.file.path", "D:\\\\SVN\\\\OPENHOUSE\\\\UMP\\\\source\\\\trunk\\\\main\\\\ump-policy-manager\\\\config\\\\umppolicymgr.properties");
        props.put("qhandlers.classpath", "E:/qmanager/messagingGW2.0/handlers/queue/");
        props.put("qhandlers.jars", "E:/qmanager/messagingGW2.0/libs/");
        props.put("api.ump.log4j.path", "D:/SVN/OPENHOUSE/UMP/source/trunk/main/api-ump/config/api-ump-log4j.properties");
        props.put("api.ump.log4j.ideal.timeout", "6000");
        return props;
    }

    public Properties loadProperties() {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (m_strConfigParamSetQuery != null) {
                if (m_conn != null) {
                    String[] moduleNameArray = m_strModuleName.split(",");
                    for (int i = 0; i < moduleNameArray.length; i++) {
                        String strModuleName = moduleNameArray[i];
                        if (logger.isInfoEnabled()) {
                            logger.info("ConfigUtil loadProperties() : Module Name : " + strModuleName);
                        }

                        m_strConfigParamSetQuery = m_strConfigParamSetQuery.replaceAll("!!modulenames!!", "(?)");

                        ps = m_conn.prepareStatement(m_strConfigParamSetQuery);
                        ps.setString(1, m_strParamSetName);
                        ps.setString(2, strModuleName);

                        rs = ps.executeQuery();
                        while (rs.next()) {
                            m_props.setProperty(rs.getString("param_name"), rs.getString("param_value"));
                            if (logger.isInfoEnabled()) {
                                logger.info("ConfigUtil loadProperties() : Property : " + rs.getString("param_name") + " = " + rs.getString("param_value"));
                                System.out.println("ConfigUtil loadProperties() : Property : " + rs.getString("param_name") + " = " + rs.getString("param_value"));
                            }
                        }
                    }
                    return m_props;
                }
            } else {
                logger.error("unable to load the 'select.sys_active_config.flag' property from properties file");
            }
        } catch (Exception e) {
            logger.error(Utilities.getStackTrace(e));
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                    ps = null;
                }
            } catch (Exception e) {
                logger.error(Utilities.getStackTrace(e));
            }
            try {
                if (rs != null) {
                    rs.close();
                    rs = null;
                }
            } catch (Exception e) {
                logger.error(Utilities.getStackTrace(e));
            }
        }
        return null;
    }

    public boolean deinit() {
        if (m_conn != null) {
            try {
                m_conn.close();
            } catch (Exception e) {
                logger.error(Utilities.getStackTrace(e));
            }
        }
        isStopthread = true;
        return true;
    }

    class LoadPropertiesRefreshThread extends Thread {

        long lTimeInMillis = System.currentTimeMillis();

        LoadPropertiesRefreshThread(ConfigUtil obj) {
            // m_objLoadPropsUtil = obj;
        }

        int currentMaxVersion = 0;

        public void run() {
            DBConnection dbconnection = null;
            while (!isStopthread) {
                Statement st = null;
                ResultSet rs = null;
                try {
                    long lDiff = System.currentTimeMillis() - lTimeInMillis;
                    m_nPropLoadTime = Utilities.toInt(m_props.getProperty("ump.param.reloadtime"), m_nPropLoadTime);
                    if (m_conn == null || m_conn.isClosed()) {

                        dbconnection = DBConnection.getInstance();
                        m_conn = dbconnection.getConnection();
                    }
                    if ((lDiff >= m_nPropLoadTime) && (m_conn != null)) {
                        if (m_strSysActiveConfigQuery != null) {
                            st = m_conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                            rs = st.executeQuery(m_strSysActiveConfigQuery);
                            if (rs.next()) {
                                m_strParamSetName = rs.getString(1);
                            }
                        } else {
                            logger.error("unable to load the 'ump.select.sys_active_config.state' property from properties file");
                        }
                        if (m_strConfigSetversionQuery != null) {
                            m_strConfigSetversionQuery = m_strConfigSetversionQuery.replaceAll("\\$\\(currentmaxversion\\)", "" + m_nCurrentMaxVersion);
                            rs = st.executeQuery(m_strConfigSetversionQuery);
                            if (rs.next()) {
                                currentMaxVersion = rs.getInt(1);
                            }
                            if (currentMaxVersion > m_nCurrentMaxVersion) {
                                m_nCurrentMaxVersion = currentMaxVersion;
                                loadProperties();
                                if (logger.isInfoEnabled()) {
                                    logger.info("Latest Properties are Loaded");
                                }
                            }

                            lTimeInMillis = System.currentTimeMillis();

                        } else {
                            logger.error("unable to load the 'ump.select.config_param_sets.version' property from properties file");
                        }
                    }
                    sleep(m_nPropLoadTime);
                    //sleep(1000);
                } catch (Exception e) {
                    logger.error(Utilities.getStackTrace(e));
                } finally {
                    if (dbconnection != null) {
                        dbconnection.closeConnection(rs, st, m_conn);
                    }
                }

            }
        }
    }

    public static final void setProperties(Properties prop, boolean flag) {
        if (flag) {
            if (m_props != null) {
                m_props.putAll(prop);
            } else {
                m_props = prop;
            }
        } else {
            m_props = prop;
        }

    }

    public static String prepareUMPReponse(String strCode, String strDesc, String strTid) {
        return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();
    }

}
