package com.common;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.log4j.Logger;

/**
 *
 * @author pavankumar.g
 */
public class DBConnection {

    Logger logger = Logger.getLogger(DBConnection.class);
    //static AppLogger m_objAppLogger = new AppLogger(DBConnection.class);
    private static DBConnection dbconn;
    private Context initCtx;
    private Context envCtx;
    private DataSource ds;
  private final ReentrantReadWriteLock propertylock = new ReentrantReadWriteLock();
    // Private constructor
    public DBConnection() {
    }
    static ResourceBundle rs;

    public static synchronized DBConnection getInstance() {

        if (dbconn != null) {
            return dbconn;
        } else {
            dbconn = new DBConnection();
            dbconn.loadDataSource();
            return dbconn;
        }
    }

    private void loadDataSource() {
        try {
            try {
                initCtx = new InitialContext();
                envCtx = (Context) initCtx.lookup("java:/comp/env");
                ds = (DataSource) envCtx.lookup("jdbc/verifyface");
            } catch (Exception e) {
                logger.error("Problem loading datasource from local initialcontext", e);
            }
            if (ds == null) {
                throw new Exception("Data source not found in any Context");
            }
        } catch (Exception exp) {
            System.out.println("exp"+exp);
            exp.printStackTrace();
            logger.error("Cannot load DataSource=>" + exp);
        }
    }
    int getCount = 0;

    /**
     *
     * @return
     */
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
        } catch (Exception exp) {
            exp.printStackTrace();
            logger.error("Exception arised in DBConnection ::", exp);
        } finally {
            if (conn == null) {
                try {
                    dbconn = null;
                    getInstance();
                    conn = ds.getConnection();
                } catch (Exception e) {
                    logger.error("Exception arised in ReConnect to DB", e);
                }
            }
        }
        return conn;
    }
    
    public void closeConnection(Statement stmt, Connection conn) {    

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.debug("Exception closing stmt in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            stmt = null;
        }

        this.closeConnection(conn);
    }

    public void closeConnection(ResultSet rs, Statement stmt, Connection conn) {

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing rs in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            rs = null;
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing stmt in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            stmt = null;
        }

        this.closeConnection(conn);
    }

    public void closeConnection(ResultSet rs, CallableStatement stmt, Connection conn) {

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing rs in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            rs = null;
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing stmt in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            stmt = null;
        }

        this.closeConnection(conn);
    }

    public void closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn) {

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing rs in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            rs = null;
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.error("Exception closing stmt in closeConnection(ResultSet rs, PreparedStatement stmt, Connection conn)=>" + ex);
        } finally {
            stmt = null;
        }

        this.closeConnection(conn);
    }
    int closeConnection = 0;

    public void closeConnection(Connection conn) {
        //logger.debug(++closeConnection + ":Close Conn=>" + conn);
        try {
            if (conn != null) {
                if (!conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (Exception e) {
            logger.debug("Exception in closeConnection(Connection conn)=>" + e);
        } finally {
            conn = null;
        }
    }

    public void closeStatement(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.debug("Exception closing stmt in closeStatement(Statement stmt)=>" + ex);
        } finally {
            stmt = null;
        }
    }

    public void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            logger.debug("Exception closing stmt in closeResultSet(ResultSet rs)=>" + ex);
        } finally {
            rs = null;
        }
    }

    public void closeStatementAndResultSet(ResultSet rs, Statement stmt) {

        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception ex) {
            logger.debug("Exception closing rs in closeStatementAndResultSet(ResultSet rs, Statement stmt)=>" + ex);
        } finally {
            rs = null;
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception ex) {
            logger.debug("Exception closing stmt in closeStatementAndResultSet(ResultSet rs, Statement stmt)=>" + ex);
        } finally {
            stmt = null;
        }

    }

    public DataSource getDataSource() {
        return ds;
    }
}
