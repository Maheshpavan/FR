/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author chhavikumar.b
 */
public class Utilities {

    private static final Logger logger = Logger.getLogger(Utilities.class);
//    private static Gson gson = new Gson();
    public static SimpleDateFormat sdfYYYYDDMM = new SimpleDateFormat("yyyy-MM-dd");

    public static final ThreadLocal<SimpleDateFormat> sdfYYYYDDMMThreadLocal = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }

    };

    public static String getDate(String strDate) {
        return sdfYYYYDDMMThreadLocal.get().format(new java.util.Date(strDate));
    }

    public static String getDate(Date date) {
        return sdfYYYYDDMMThreadLocal.get().format(date);
    }

    public static Properties loadFile(String fileName) {
        InputStream is = null;
        Properties p = null;
        File f = new File(fileName);
        try {
            is = new FileInputStream(fileName);
            if (logger.isDebugEnabled()) {
                logger.debug("Util :: loadFile(..) :: file successfully loaded from filesystem :: fileName = " + fileName);
            }

        } catch (FileNotFoundException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Util :: loadFile(..) :: File can't load using FileInputStream hence trying to load from classpath :: fileName = "
                        + fileName);
            }
            try {
//                is = ClassLoader.getSystemResourceAsStream(fileName);
                is = Utilities.class.getClassLoader().getResourceAsStream(fileName);
                if (logger.isDebugEnabled()) {
                    logger.debug("Util :: loadFile(..) :: file successfully loaded from classpath :: fileName = " + fileName);
                }

            } catch (Exception e1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Util :: loadFile(..) :: can't load file from classpath also :: :: fileName = " + fileName
                            + " Ex: ", e);
                }

            }
        }

        if (is != null) {
            p = new Properties();
            try {
                p.load(is);
                //System.out.println("Util :: loadFile(..) :: File Successfully loaded to Properties :: fileName = " + fileName);
            } catch (IOException ex) {
                logger.error("Util :: loadFile(..) :: :: fileName = " + fileName + " Ex: ", ex);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        logger.error(getStackTrace(e));
                    }
                }
            }
        }
        if (p == null) {

            throw new RuntimeException("loading properties file failed ... File Name" + fileName);

        }
        return p;
    }

    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }

    public static String getDateFromStringRiyadh(String dateInString) {
        try {
            if (StringUtils.isNotBlank(dateInString)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat dateFormat = new SimpleDateFormat("E,dd MMM yyyy");
                // String dateInString = "2016-12-21 08:37:37.253";
                Date newdate = sdf.parse(dateInString);
                return dateFormat.format(newdate);
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static String getDateFromString(String dateInString) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("E,dd MMM yyyy");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            // String dateInString = "2016-12-21 08:37:37.253";
            Date newdate = sdf.parse(dateInString);
            return dateFormat.format(newdate);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static int toInt(String strValue, int nDefValue) {
        try {
            if (strValue != null && strValue.length() > 0) {
                return Integer.parseInt(strValue);
            }
        } catch (Exception e) {
            logger.error("Exception in toInt().Ex:", e);
        }
        return nDefValue;
    }

    public static String prepareReponse(String strCode, String strDesc, String strTid) {
        return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();
    }

    public static String prepareReponseByData(String strCode, String strDesc, String strTid, String key, String data) {
        return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\",\"" + key + "\":").append(data).append("}}").toString();
    }

    public static byte[] prepareReponseByte(String strCode, String strDesc, String strTid) {
        try {
            return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\"}}").toString().getBytes("UTF-8");

        } catch (Exception e) {
        }
        return null;
    }

    public static String prepareReponse(String strCode, String strDesc, String strTid, int nUserId) {
        if (nUserId > 0) {
            return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\",\"userId\":\"").append(nUserId + "").append("\"}}").toString();
        } else {
            return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();
        }
    }

    public static String prepareReponsetoken(String strCode, String strDesc, String strTid) {
        return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"token\":\"").append(strTid).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();
    }

    public static String prepareReponsetoken(String strCode, String strDesc, String strTid, int nUserId) {
        if (nUserId > 0) {
            return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"token\":\"").append(strTid).append("\",\"transid\":\"").append(strTid).append("\",\"userId\":\"").append(nUserId + "").append("\"}}").toString();
        } else {
            return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"description\":\"").append(strDesc).append("\",\"token\":\"").append(strTid).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();

        }
    }

    public static String prepareIDReponse(String strCode, int id, String strTid) {
        return new StringBuilder("{\"response\":{\"code\":\"").append(strCode).append("\",\"id\":\"").append(id).append("\",\"transid\":\"").append(strTid).append("\"}}").toString();
    }

//    public static <T extends Object> T fromJson(String jsonString, Class<T> objectClass) throws JsonSyntaxException {
//        return gson.fromJson(jsonString, objectClass);
//    }
    private static final int RANDOM_STRING_LENGTH = 10;
    private static final String CHAR_LIST
            = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    public static String generateRandomString() {
        StringBuffer randStr = new StringBuffer();
        for (int i = 0; i < RANDOM_STRING_LENGTH; i++) {
            int number = getRandomNumber();
            char ch = CHAR_LIST.charAt(number);
            randStr.append(ch);
        }
        return randStr.toString();
    }

    private static int getRandomNumber() {
        int randomInt = 0;
        Random randomGenerator = new Random();
        randomInt = randomGenerator.nextInt(CHAR_LIST.length());
        if (randomInt - 1 == -1) {
            return randomInt;
        } else {
            return randomInt - 1;
        }
    }

    public static JSONArray convertResultSetIntoJSON(ResultSet resultSet) throws Exception {
        JSONArray jsonArray = new JSONArray();
        while (resultSet.next()) {
            int total_rows = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase();
                Object columnValue = resultSet.getObject(i + 1);
                // if value in DB is null, then we set it to default value
//                if (columnValue == null) {
//                    columnValue = "null";
//                }
                if (columnValue instanceof Timestamp) {
                    columnValue = columnValue + "";
                }
                /*
                 Next if block is a hack. In case when in db we have values like price and price1 there's a bug in jdbc - 
                 both this names are getting stored as price in ResulSet. Therefore when we store second column value,
                 we overwrite original value of price. To avoid that, i simply add 1 to be consistent with DB.
                 */
                if (obj.has(columnName)) {
                    columnName += "1";
                }
                obj.put(columnName, columnValue);
            }
            jsonArray.put(obj);
        }

        return jsonArray;
    }

    public static String nullToEmpty(String string) {
        return string == null ? "" : string;
    }

    public static long getDifferenceDays(Date d1, Date d2) {
        try {
            long diff = d2.getTime() - d1.getTime();
            return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
        }
        return -1;
    }

    public static double calculateMonthlyPayment(int loanAmount, int termInYears, double interestRate) {

        // Convert interest rate into a decimal
        // eg. 6.5% = 0.065
        interestRate /= 100.0;

        // Monthly interest rate 
        // is the yearly rate divided by 12
        double monthlyRate = interestRate / 12.0;

        // The length of the term in months 
        // is the number of years times 12
        int termInMonths = termInYears * 12;

        // Calculate the monthly payment
        // Typically this formula is provided so 
        // we won't go into the details
        // The Math.pow() method is used calculate values raised to a power
        double monthlyPayment
                = (loanAmount * monthlyRate)
                / (1 - Math.pow(1 + monthlyRate, -termInMonths));

        return monthlyPayment;
    }

}
