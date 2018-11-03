/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dao;

import com.beans.MatchedResultsBean;
import com.common.ConfigUtil;
import com.common.DBConnection;
import com.common.Utilities;
import com.beans.User;
import com.common.LoadNativeLibraries;
import com.common.ResponseCodes;
import static com.common.ResponseCodes.ServiceErrorCodes.SUCCESS;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFace;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NMatchingSpeed;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.io.NFile;
import com.neurotec.licensing.NLicense;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author pavankumar.g
 */
public class UserDAO {

    DBConnection dbconnection = null;

    static NBiometricClient biometricClient = null;
    static NSubject probeSubject = null;
//    static NBiometricTask enrollTask = null;
    static boolean isInitDone = false;
    String url = "";
    String FILE_FOLDER_SEPERATOR = "^";

    public UserDAO() {
        System.out.println("in UserDAO()");
        if (!isInitDone) {
            dbconnection = DBConnection.getInstance();
            try {
//                final String components = "Biometrics.FaceExtraction,Biometrics.FaceMatching";
//                System.setProperty("jna.library.path", "D:\\faceverification\\Neurotec_Biometric_10_0_SDK_Trial_2018-03-22\\Neurotec_Biometric_10_0_SDK_Trial\\Bin\\Win64_x64");
//                System.setProperty("jna.library.path", "D:\\faceverification\\Neurotec_Biometric_10_0_SDK_Trial_2018-03-22\\Neurotec_Biometric_10_0_SDK_Trial\\Lib\\Win64_x64");
//                LibraryManager.initLibraryPath();
//                if (!NLicense.obtainComponents("/local", 5000, components)) {
//                    System.err.format("Could not obtain licenses for components: %s%n", components);
//                    System.exit(-1);
//                }
                url = ConfigUtil.getProperty("url", "http://localhost:8081/VerifyFace");
                FILE_FOLDER_SEPERATOR = ConfigUtil.getProperty("file.folder.seperator", "^");
//                loadTemplates();
//                WorkerThread workerThread = new WorkerThread();
                MyThread thread = new MyThread("LoadTemplates");
                thread.start();
                isInitDone = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static {
        try {
            System.out.println("Executing static block UserDAO class");
            LoadNativeLibraries.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static Logger logger = Logger.getLogger(UserDAO.class);

    public int addUserDetails(List<User> listUser) throws SQLException, Exception {
        String insertQuery = ConfigUtil.getProperty("store.customer.data.query", "INSERT INTO verifyface.users ( NAME, age, mobile, email, address, file_name, image_link, created_by,uuid,person_sequence_num,status,description,folder_name)	VALUES	(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE updated_on=now()");
        String insertORUpdateQuery = ConfigUtil.getProperty("store.customer.data.onduplicate.update.query", "INSERT INTO verifyface.users ( NAME, age, mobile, email, address, file_name, image_link, created_by,uuid,person_sequence_num,status,description,folder_name)	VALUES	(?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE status=1,description='',updated_on=now()");
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Connection objConn = null;
        String description = "";
        int nDBStatus = 1;
        int nStatus = 0;
        int nTemplateStatus = -1;

        for (User user : listUser) {
            String templateFolderName = user.getTemplateFolderName();
            if (StringUtils.isBlank(templateFolderName)) {
                templateFolderName = user.getFolderName();
            }

            nTemplateStatus = createTemplate(user.getImageLocation(), user.getUuid(), templateFolderName);
            try {
                if (nTemplateStatus != 0) {
                    nDBStatus = 2;
                } else {
                    insertQuery = insertORUpdateQuery;
                }
                if (nTemplateStatus == -2) {
                    description = "bad sharpness";
                } else if (nTemplateStatus == -3) {
                    description = "bad contrast";
                } else if (nTemplateStatus == -4) {
                    description = "too many obects found";
                }
                objConn = dbconnection.getConnection();
                if (objConn != null) {
                    pstmt = objConn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                    pstmt.setString(1, user.getName());
                    pstmt.setInt(2, user.getAge());
                    pstmt.setString(3, user.getMobile());
                    pstmt.setString(4, user.getEmail());
                    pstmt.setString(5, user.getAddress());
                    pstmt.setString(6, user.getImageName());
                    pstmt.setString(7, user.getImageLink());
                    pstmt.setString(8, user.getCreatedBy());
                    pstmt.setString(9, user.getUuid());
                    pstmt.setString(10, user.getPersonSeqNumber());
                    pstmt.setInt(11, nDBStatus);
                    pstmt.setString(12, description);
                    pstmt.setString(13, user.getFolderName());
                    nStatus = pstmt.executeUpdate();
                } else {
                    logger.error("addCustomerDetails(): connection object is null");
                }

            } catch (SQLException sqle) {
                logger.error("addUserDetails() : Got SQLException " + Utilities.getStackTrace(sqle));
                throw new SQLException(sqle.getMessage());
            } catch (Exception e) {
                logger.error("addUserDetails() Got Exception : " + Utilities.getStackTrace(e));
                throw new Exception(e.getMessage());
            } finally {
                if (objConn != null) {
                    dbconnection.closeConnection(rs, pstmt, objConn);
                }
                try {
                    if (nStatus <= 0) {
                        File newFile = new File(user.getImageLocation());
                        newFile.deleteOnExit();
                    }
                } catch (Exception e) {

                }
            }
        }
        if (nTemplateStatus != 0) {
            return nTemplateStatus;
        } else {
            return nStatus;
        }
    }

    public String getUserDetails(int min, int max, String sortBy, String orderBy, String search, String transId) {
        JSONObject response = new JSONObject();
        String query = ConfigUtil.getProperty("get.users.list", "select * from users where status=1 ");
        JSONArray userDetailsArray = new JSONArray();
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Connection objConn = null;
        try {

            if (search != null && search.trim().length() > 0) {
                query = query + " and name like '%" + search + "%'";
            }

            if (sortBy != null && sortBy.trim().length() > 0) {
                query = query + " ORDER BY " + sortBy + " " + orderBy;
            }

            if (min >= 0 && max > 0) {
                query = query + " LIMIT " + min + "," + max;
            }

            System.out.println("query => " + query);
            response.put("code", "0");
            response.put("transid", transId);
            objConn = dbconnection.getConnection();
            pstmt = objConn.prepareStatement(query);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                JSONObject object = new JSONObject();
                object.put("id", rs.getInt("id"));
                object.put("name", Utilities.nullToEmpty(rs.getString("name")));
                object.put("age", rs.getInt("age"));
                object.put("mobile", Utilities.nullToEmpty(rs.getString("mobile")));
                object.put("email", Utilities.nullToEmpty(rs.getString("email")));
                object.put("image", url + rs.getString("image_link"));
                object.put("address", Utilities.nullToEmpty(rs.getString("address")));
                object.put("personSeqNumber", Utilities.nullToEmpty(rs.getString("person_sequence_num")));
                object.put("createdOn", Utilities.nullToEmpty(rs.getString("created_on")));
                object.put("createdBy", Utilities.nullToEmpty(rs.getString("created_by")));
                userDetailsArray.put(object);
            }
            response.put("response", userDetailsArray);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (objConn != null) {
                dbconnection.closeConnection(rs, pstmt, objConn);
            }
        }

        return response.toString();
    }

    public int updateUser(String imageName, String folderName) {
        String query = ConfigUtil.getProperty("update.user", "update users set folder_name=? where status=1 and uuid=?");
        PreparedStatement pstmt = null;
        Connection objConn = null;
        ResultSet rs = null;
        int nResult = -1;
        String currentFolderName = "";

        try {
            objConn = dbconnection.getConnection();

            pstmt = objConn.prepareStatement(ConfigUtil.getProperty("get.user.by.uuid", "select * from users where status=1 and uuid=?"));
            pstmt.setString(1, imageName.trim());
            rs = pstmt.executeQuery();
            if (rs.next()) {
                currentFolderName = rs.getString("folder_name");
                if (StringUtils.isBlank(currentFolderName)) {
                    return 0;
                }
            } else {
                return 0;
            }

            if (!folderName.equalsIgnoreCase(currentFolderName)) {
                System.out.println("query => " + query);
                pstmt = objConn.prepareStatement(query);
                pstmt.setString(1, folderName.trim());
                pstmt.setString(2, imageName.trim());
                nResult = pstmt.executeUpdate();
                String templatesPath = ConfigUtil.getProperty("templates.path", "E:\\xampp\\htdocs\\fr\\public\\templates\\");
                File file = new File(templatesPath + File.separator + currentFolderName + File.separator + imageName);
                if (file.renameTo(new File(templatesPath + File.separator + folderName + File.separator + imageName))) {
                    // if file copied successfully then delete the original file
                    file.delete();
                    logger.debug("File moved successfully from folder => " + currentFolderName + " , to folder => " + folderName);
                } else {
                    logger.debug("Failed to move the file from folder => " + currentFolderName + " , to folder => " + folderName);
                }
            } else {
                return 1;
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate")) {
                nResult = -2;
            }
            e.printStackTrace();
        } finally {
            if (objConn != null) {
                dbconnection.closeConnection(rs, pstmt, objConn);
            }
        }

        return nResult;
    }

    public int deleteUser(String imageName, String folderName) {
        String query = ConfigUtil.getProperty("update.user", "update users set status=2 where status=1 and uuid=? and folder_name=?");
        PreparedStatement pstmt = null;
        Connection objConn = null;
        int nResult = -1;
        try {

            System.out.println("query => " + query);
            objConn = dbconnection.getConnection();
            pstmt = objConn.prepareStatement(query);
            pstmt.setString(1, imageName.trim());
            pstmt.setString(2, folderName.trim());
            nResult = pstmt.executeUpdate();
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate")) {
                nResult = -2;
            }
            e.printStackTrace();
        } finally {
            if (objConn != null) {
                dbconnection.closeConnection(null, pstmt, objConn);
            }
        }

        return nResult;
    }

    public JSONObject getUserDetailsByUUID(String uuid, int score, String folderName) {
        String query = ConfigUtil.getProperty("get.user.by.uuid", "select * from users where status=1 and uuid=?");
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        Connection objConn = null;
        JSONObject response = null;
        try {
            objConn = dbconnection.getConnection();
            if (StringUtils.isBlank(folderName)) {
                pstmt = objConn.prepareStatement(query);
                pstmt.setString(1, uuid);
            } else {
                pstmt = objConn.prepareStatement(query + " and folder_name=?");
                pstmt.setString(1, uuid);
                pstmt.setString(2, folderName);
            }

            rs = pstmt.executeQuery();
            if (rs.next()) {
                response = new JSONObject();
                response.put("id", rs.getInt("id"));
                response.put("name", Utilities.nullToEmpty(rs.getString("name")));
                response.put("age", rs.getInt("age"));
                response.put("mobile", Utilities.nullToEmpty(rs.getString("mobile")));
                response.put("email", Utilities.nullToEmpty(rs.getString("email")));
                response.put("image", url + rs.getString("image_link"));
                response.put("address", Utilities.nullToEmpty(rs.getString("address")));
                response.put("personSeqNumber", Utilities.nullToEmpty(rs.getString("person_sequence_num")));
                response.put("score", Utilities.nullToEmpty(score + ""));
                response.put("createdOn", rs.getString("created_on"));
                response.put("createdBy", rs.getString("created_by"));
//                response.put("folderName", uuid.split("\\^").length >= 2 ? uuid.split("\\^")[1] : "others");
                response.put("folderName", rs.getString("folder_name"));
                return response;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (objConn != null) {
                dbconnection.closeConnection(rs, pstmt, objConn);
            }
        }

        return response;
    }

    private int createTemplate(String srcImage, String uuid, String folderName) {
        logger.debug("=======>creating template for " + srcImage);

        NSubject probeSubject = null;
        NBiometricClient biometricClient1 = null;
        NBiometricTask enrollTask = null;
        int nStatus = 0;
        try {
            biometricClient1 = new NBiometricClient();
            final String additionalComponents = "Biometrics.FaceSegmentsDetection";
            boolean isAdditionalComponentActivated = NLicense.isComponentActivated(additionalComponents);
            biometricClient1.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
            biometricClient1.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
            biometricClient1.setFacesDetectBaseFeaturePoints(isAdditionalComponentActivated);
            biometricClient1.setFacesDetermineGender(isAdditionalComponentActivated);
            biometricClient1.setFacesDetermineAge(isAdditionalComponentActivated);
            biometricClient1.setFacesDetectProperties(isAdditionalComponentActivated);
            biometricClient1.setFacesRecognizeExpression(isAdditionalComponentActivated);
            biometricClient1.setFacesMaximalRoll((Float) 120.0f);
            biometricClient1.setFacesMaximalYaw((Float) 90.0f);
            probeSubject = createSubject(srcImage, uuid, false);

            NBiometricStatus status = biometricClient1.createTemplate(probeSubject);
            enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
//            for (NFace nface : probeSubject.getFaces()) {
//                for (NLAttributes attribute : nface.getObjects()) {
//                    System.out.println("face:");
//                    System.out.format("\tlocation = (%d, %d), width = %d, height = %d\n", attribute.getBoundingRect().getBounds().x, attribute.getBoundingRect().getBounds().y,
//                            attribute.getBoundingRect().width, attribute.getBoundingRect().height);
//
////                    System.out.println("age con******" + attribute.getGenderConfidence());
//                    if ((attribute.getGenderConfidence() > 0)) {
//                        System.out.println("\tfound Gender:");
//                        System.out.println("Gender:" + attribute.getGender());
//                    }
//
//                    if ((attribute.getAge() > 0)) {
//                        System.out.println("\tfound age:");
//                        System.out.println("age:" + attribute.getAge());
//                    }
//
//                    if ((attribute.getRightEyeCenter().confidence > 0) || (attribute.getLeftEyeCenter().confidence > 0)) {
//                        System.out.println("\tfound eyes:");
//                        if (attribute.getRightEyeCenter().confidence > 0) {
//                            System.out.format("\t\tRight: location = (%d, %d), confidence = %d%n", attribute.getRightEyeCenter().x, attribute.getRightEyeCenter().y,
//                                    attribute.getRightEyeCenter().confidence);
//                        }
//                        if (attribute.getLeftEyeCenter().confidence > 0) {
//                            System.out.format("\t\tLeft: location = (%d, %d), confidence = %d%n", attribute.getLeftEyeCenter().x, attribute.getLeftEyeCenter().y,
//                                    attribute.getLeftEyeCenter().confidence);
//                        }
//                    }
//                    if (isAdditionalComponentActivated) {
//                        if (attribute.getNoseTip().confidence > 0) {
//                            System.out.println("\tfound nose:");
//                            System.out.format("\t\tlocation = (%d, %d), confidence = %d%n", attribute.getNoseTip().x, attribute.getNoseTip().y, attribute.getNoseTip().confidence);
//                        }
//                        if (attribute.getMouthCenter().confidence > 0) {
//                            System.out.println("\tfound mouth:");
//                            System.out.printf("\t\tlocation = (%d, %d), confidence = %d%n", attribute.getMouthCenter().x, attribute.getMouthCenter().y, attribute.getMouthCenter().confidence);
//                        }
//                    }
//                }
//            }
            String templatesPath = ConfigUtil.getProperty("templates.path", "E:\\xampp\\htdocs\\fr\\public\\templates\\");
            if (status == NBiometricStatus.BAD_SHARPNESS) {
                System.out.format("Failed to create probe template for : %s . Status: %s.\n", uuid, status);
                return -2;
            } else if (status == NBiometricStatus.BAD_CONTRAST) {
                System.out.format("Failed to create probe template for : %s . Status: %s.\n", uuid, status);
                return -3;
            } else if (status == NBiometricStatus.TOO_MANY_OBJECTS) {
                System.out.format("Failed to create probe template for : %s . Status: %s.\n", uuid, status);
                return -4;
            } else if (status != NBiometricStatus.OK) {
                System.out.format("Failed to create probe template for : %s . Status: %s.\n", uuid, status);
                return -1;
            } else {
                try {
                    File file = new File(templatesPath + folderName);
                    if (!file.isDirectory()) {
                        file.mkdir();
                    }
                } catch (Exception e) {
                }
                //template saving to the folder
                NFile.writeAllBytes(templatesPath + folderName + File.separator + uuid, probeSubject.getTemplate().save());
            }
            enrollTask.getSubjects().add(probeSubject);
            long lStart = System.currentTimeMillis();
            biometricClient.performTask(enrollTask);
            logger.debug("[enroll][createTemplate][performTask] adding template to cache : " + (System.currentTimeMillis() - lStart) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (probeSubject != null) {
                    probeSubject.dispose();
                    probeSubject = null;
                }
                if (biometricClient1 != null) {
                    biometricClient1.dispose();
                    biometricClient1 = null;
                }
                if (enrollTask != null) {
                    enrollTask.dispose();
                    enrollTask = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return nStatus;
    }

    private void loadTemplates() {
        ExecutorService executor = null;
        long lStartTime = System.currentTimeMillis();
        NBiometricTask enrollTask = null;
        try {
            String srcLocation = ConfigUtil.getProperty("FILES_DIR", "D:\\api\\images\\");
            File file = new File(srcLocation);
            if (file.isDirectory()) {
                biometricClient = new NBiometricClient();
                final String additionalComponents = "Biometrics.FaceSegmentsDetection";
                boolean isAdditionalComponentActivated = NLicense.isComponentActivated(additionalComponents);
                biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                biometricClient.setFacesDetectBaseFeaturePoints(isAdditionalComponentActivated);
                biometricClient.setFacesDetermineGender(isAdditionalComponentActivated);
                biometricClient.setFacesDetermineAge(isAdditionalComponentActivated);
                biometricClient.setFacesDetectProperties(isAdditionalComponentActivated);
                biometricClient.setFacesRecognizeExpression(isAdditionalComponentActivated);
                biometricClient.setFacesMaximalRoll((Float) 120.0f);
                biometricClient.setFacesMaximalYaw((Float) 90.0f);

                enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
                executor = Executors.newFixedThreadPool(10);//creating a pool of 5 threads  
                for (int i = 0; i < file.list().length; i++) {
                    logger.debug("[loadTemplates()] " + file.listFiles()[i].getName().split("\\.")[0]);
//                    enrollTask.getSubjects().add(createSubject(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0], String.format("GallerySubject_%d", i)));
                    Runnable worker = new WorkerThread(file.listFiles()[i], i);
                    executor.execute(worker);//calling execute method of ExecutorService  
                }
                biometricClient.performTask(enrollTask);

            }
//            if (enrollTask.getStatus() != NBiometricStatus.OK) {
//                System.out.format("Enrollment was unsuccessful. Status: %s.\n", enrollTask.getStatus());
//                if (enrollTask.getError() != null) {
//                    throw enrollTask.getError();
//                }
//                System.exit(-1);
//            }
            System.out.println("Templates loaded successfully , Total =>" + file.list().length);
            System.out.println("Total time to load templates => " + (System.currentTimeMillis() - lStartTime) / 1000 + " Sec's");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (executor != null) {
                    executor.shutdown();
                    while (!executor.isTerminated()) {
                    }
                }
                if (enrollTask != null) {
                    enrollTask.dispose();
                    enrollTask = null;
                }
            } catch (Exception e) {
            }
        }
    }

    public String verifyMatchedInFromGallery(String sourceImage, String name, String transId, int nMatchingThreshhold, String folderNames, int noOfResults) {
        JSONArray response = new JSONArray();
        ArrayList<MatchedResultsBean> results = null;

        try {
            results = (ArrayList<MatchedResultsBean>) checkMatchedImages(sourceImage, name, nMatchingThreshhold);
            if (results != null) {

                for (MatchedResultsBean result : results) {
                    try {
                        if (StringUtils.isBlank(folderNames)) {
                            JSONObject object = getUserDetailsByUUID(result.getId(), result.getScore(), null);
                            if (object != null) {
                                response.put(object);
                            }
                        } else {
//                            String id = result.getId();
//                            String folderName = id.split("\\^").length >= 2 ? id.split("\\^")[1] : "others";
                            for (String folder : folderNames.split(",")) {
                                JSONObject object = getUserDetailsByUUID(result.getId(), result.getScore(), folder);
                                if (object != null) {
                                    response.put(object);
                                }
                            }

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (response.length() == noOfResults) {
                        break;
                    }
                }
            }

            try {
                File verifyImage = new File(sourceImage);
                if (verifyImage.exists()) {
                    boolean bVal = verifyImage.delete();
                    if (bVal) {
                        logger.debug(" Verify Image has successfully deleted");
                    } else {
                        logger.debug(" Verify Image has failed to deleted");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().equalsIgnoreCase("BAD_SHARPNESS")) {
                return Utilities.prepareReponseByData(ResponseCodes.ServiceErrorCodes.BAD_SHARPNESS.getCode(), ResponseCodes.ServiceErrorCodes.BAD_SHARPNESS.DESC(), transId, "response", response.toString());
            }
        } finally {
            try {
                if (results != null) {
                    results = null;
                }
            } catch (Exception e) {
            }
        }

        if (response.length() > 0) {
            return Utilities.prepareReponseByData(SUCCESS.getCode(), SUCCESS.DESC(), transId, "response", response.toString());
        } else {
            return Utilities.prepareReponse(ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.getCode(), ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.DESC(), transId);
        }
    }

    private List<MatchedResultsBean> checkMatchedImages(String sourceImage, String name, int nMachingThreshhold) throws Exception {
        int nThreashHold = Integer.parseInt(ConfigUtil.getProperty("verify.image.threashold", "44"));
        biometricClient.setMatchingThreshold(nMachingThreshhold);
        NSubject.MatchingResultCollection results = null;
        biometricClient.setFacesMatchingSpeed(NMatchingSpeed.LOW);
//        biometricClient.setFacesQualityThreshold((byte) 0);
        List<MatchedResultsBean> resultList = null;
        MatchedResultsBean resultsBean = null;

        NSubject nSubject = createSubject(sourceImage, name, true);
        long lStart = System.currentTimeMillis();

        try {
//            for (NSubject subject : subjects) {

            NBiometricStatus status = biometricClient.identify(nSubject);
            System.out.println("subjects=====> " + nSubject.getRelatedSubjects().size());
//            for (NSubject subject : subjects) {
            logger.debug("[identify] total timetaken for identify from neuro " + (System.currentTimeMillis() - lStart) + " ms");
//                NBiometricStatus status = biometricClient.identify(subject);
            if (status == NBiometricStatus.OK) {
                resultList = new ArrayList<>();
                for (NMatchingResult result : nSubject.getMatchingResults()) {
                    resultsBean = new MatchedResultsBean(result.getScore(), result.getId());
                    resultList.add(resultsBean);
//                    System.out.format("Matched with ID: '%s' with score %d\n", result.getId(), result.getScore());
                }
                Collections.sort(resultList, MatchedResultsBean.Score);
                return resultList;
            } else if (status == NBiometricStatus.MATCH_NOT_FOUND) {
                System.out.format("Match not found");
            } else if (status == NBiometricStatus.BAD_SHARPNESS) {
                System.out.format("Match not found");
                throw new Exception("BAD_SHARPNESS");
            } else {
                System.out.format("Identification failed. Status: %s\n", status);
            }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (nSubject != null) {
                    nSubject.dispose();
                    nSubject = null;
                }

            } catch (Exception e) {
            }

        }
        return resultList;
    }

    private static NSubject createSubject(String fileName, String subjectId, boolean isMultipleFaces) {
        NSubject subject = new NSubject();
        subject.setId(subjectId);
        subject.setMultipleSubjects(isMultipleFaces);
        NFace face = new NFace();
        face.setFileName(fileName);
        subject.getFaces().add(face);
        return subject;
    }

    class MyThread extends Thread {

        public MyThread(String name) {
            super(name);
        }

//        @Override
        public void run_thread() {
            System.out.println("MyThread - START " + Thread.currentThread().getName());
            ExecutorService executor = null;
            long lStartTime = System.currentTimeMillis();
            NBiometricTask enrollTask = null;

            try {
                int nFaceQualityThreshold = Integer.parseInt(ConfigUtil.getProperty("faces.quality.threshold", "0"));
                int nFaceQualityThresholdEnable = Integer.parseInt(ConfigUtil.getProperty("enable.custom.faces.quality.threshold", "1"));
                String strTemplatesDirPath = ConfigUtil.getProperty("templates.path", "E:\\xampp\\htdocs\\fr\\public\\templates\\");
                String strTemplatesFolders = ConfigUtil.getProperty("verifyface.folder.names", "accused,person_missing,person_found,dead_bodies,victims,others,facebook");
                File file = null;

                for (String templateFolderName : strTemplatesFolders.split(",")) {
                    file = new File(strTemplatesDirPath + File.separator + templateFolderName);
                    System.out.println("Templatepath=>" + file.getAbsolutePath());
                    int nFailedCount = 0;
                    if (file.isDirectory()) {
                        if (biometricClient == null) {
                            biometricClient = new NBiometricClient();
                            final String additionalComponents = "Biometrics.FaceSegmentsDetection";
                            boolean isAdditionalComponentActivated = NLicense.isComponentActivated(additionalComponents);
                            biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectBaseFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetermineGender(isAdditionalComponentActivated);
                            biometricClient.setFacesDetermineAge(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectProperties(isAdditionalComponentActivated);
                            biometricClient.setFacesRecognizeExpression(isAdditionalComponentActivated);
                            biometricClient.setFacesMaximalRoll((Float) 120.0f);
                            biometricClient.setFacesMaximalYaw((Float) 90.0f);
                            if (nFaceQualityThresholdEnable == 1) {
                                biometricClient.setFacesQualityThreshold((byte) nFaceQualityThreshold);
                            }

                        }
//                    biometricClient.setFacesTemplateSize(NTemplateSize.SMALL);
                    }
                    enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
                    executor = Executors.newFixedThreadPool(Integer.parseInt(ConfigUtil.getProperty("worker.threads", "100")));//creating a pool of 5 threads  
                    System.out.println("************Total Images in folder => " + file.list().length);
                    for (int i = 0; i < file.list().length; i++) {
                        logger.debug("[loadTemplates()] " + file.listFiles()[i].getName().split("\\.")[0]);
                        NSubject subject = null;
                        NSubject tSubject = null;
                        NFace face = null;
                        try {
//                            long time = System.currentTimeMillis();
//                            //1-create templates , 2-Load Template
//                            subject = loadSubjectFromTemplate(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0]);
//                            System.out.println("timetaken for loadSubjectFromTemplate() => " + (System.currentTimeMillis() - time) + " ms");
//                            enrollTask.getSubjects().add(subject);
                            Runnable worker = new WorkerThread(file.listFiles()[i], i);
                            executor.execute(worker);//calling execute method of ExecutorService  
//                            if (i > 0 && i % 1000 == 0) {
//                                long lTime = System.currentTimeMillis();
//                                biometricClient.performTask(enrollTask);
//                                System.out.println("*****************Loaded images into memory===>" + i + " , timetaken for performtask() => " + (System.currentTimeMillis() - lTime) + " ms");
//                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (subject != null) {
                                    subject.dispose();
                                    subject = null;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
//                    System.out.println("enrollTask Sizee=>"+enrollTask.getSize());
                    biometricClient.performTask(enrollTask);
                    System.out.println("Failed count of folder " + templateFolderName + "=> " + nFailedCount);
                    System.out.println("Templates loaded successfully of folder " + templateFolderName + ", Total =>" + file.list().length);
                    System.out.println("Total time to load from folder " + templateFolderName + " templates => " + (System.currentTimeMillis() - lStartTime) / 1000 + " Sec's");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (executor != null) {
                        executor.shutdown();
                        while (!executor.isTerminated()) {
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            System.out.println("MyThread - END " + Thread.currentThread().getName());
        }
//        @Override

        @Override
        public void run() {
            System.out.println("MyThread - START " + Thread.currentThread().getName());
//            ExecutorService executor = null;
            long lStartTime = System.currentTimeMillis();

            try {
                int nFaceQualityThreshold = Integer.parseInt(ConfigUtil.getProperty("faces.quality.threshold", "0"));
                int nFaceQualityThresholdEnable = Integer.parseInt(ConfigUtil.getProperty("enable.custom.faces.quality.threshold", "1"));
                String strTemplatesDirPath = ConfigUtil.getProperty("templates.path", "E:\\xampp\\htdocs\\fr\\public\\templates\\");
                String strTemplatesFolders = ConfigUtil.getProperty("verifyface.folder.names", "accused,person_missing,person_found,dead_bodies,victims,others,facebook");
                File file = null;

                for (String templateFolderName : strTemplatesFolders.split(",")) {
                    file = new File(strTemplatesDirPath + File.separator + templateFolderName);
                    System.out.println("Templatepath=>" + file.getAbsolutePath());
                    int nFailedCount = 0;
                    if (file.isDirectory()) {
                        if (biometricClient == null) {
                            biometricClient = new NBiometricClient();
                            final String additionalComponents = "Biometrics.FaceSegmentsDetection";
                            boolean isAdditionalComponentActivated = NLicense.isComponentActivated(additionalComponents);
                            biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectBaseFeaturePoints(isAdditionalComponentActivated);
                            biometricClient.setFacesDetermineGender(isAdditionalComponentActivated);
                            biometricClient.setFacesDetermineAge(isAdditionalComponentActivated);
                            biometricClient.setFacesDetectProperties(isAdditionalComponentActivated);
                            biometricClient.setFacesRecognizeExpression(isAdditionalComponentActivated);
                            biometricClient.setFacesMaximalRoll((Float) 120.0f);
                            biometricClient.setFacesMaximalYaw((Float) 90.0f);
                            if (nFaceQualityThresholdEnable == 1) {
                                biometricClient.setFacesQualityThreshold((byte) nFaceQualityThreshold);
                            }

                        }
//                    biometricClient.setFacesTemplateSize(NTemplateSize.SMALL);
                    }
                    NBiometricTask enrollTask = null;
                    try {

                        enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
//                    executor = Executors.newFixedThreadPool(Integer.parseInt(ConfigUtil.getProperty("worker.threads", "10")));//creating a pool of 5 threads  
                        System.out.println("************Total Images in folder => " + file.list().length);
                        for (int i = 0; i < file.list().length; i++) {
                            logger.debug("[loadTemplates()] " + file.listFiles()[i].getName().split("\\.")[0]);
//                        NSubject subject = null;
                            NSubject tSubject = null;
                            NFace face = null;
                            try {
                                long time = System.currentTimeMillis();
                                //1-create templates , 2-Load Template
                                NSubject subject = loadSubjectFromTemplate(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0]);
//                            System.out.println("timetaken for loadSubjectFromTemplate() => " + (System.currentTimeMillis() - time) + " ms");
                                enrollTask.getSubjects().add(subject);
//                        Runnable worker = new WorkerThread(file.listFiles()[i], i);
//                        executor.execute(worker);//calling execute method of ExecutorService  
                                if (i > 0 && i % 1000 == 0) {
                                    long lTime = System.currentTimeMillis();
                                    biometricClient.performTask(enrollTask);
                                    System.out.println("*****************Loaded images into memory===>" + i + " , timetaken for performtask() => " + (System.currentTimeMillis() - lTime) + " ms");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                try {
//                                if (subject != null) {
//                                    subject.dispose();
//                                    subject = null;
//                                }
//                                if(enrollTask!=null) enrollTask.dispose();
                                } catch (Exception e) {
                                }
                            }
                        }
                        biometricClient.performTask(enrollTask);
                        System.out.println("Failed count of folder " + templateFolderName + "=> " + nFailedCount);
                        System.out.println("Templates loaded successfully of folder " + templateFolderName + ", Total =>" + file.list().length);
                        System.out.println("Total time to load from folder " + templateFolderName + " templates => " + (System.currentTimeMillis() - lStartTime) / 1000 + " Sec's");
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (enrollTask != null) {
                                enrollTask.dispose();
                                enrollTask = null;
                            }
                        } catch (Exception e) {
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
//                    if (executor != null) {
//                        executor.shutdown();
//                        while (!executor.isTerminated()) {
//                        }
//                    }
//                    if (enrollTask != null) {
//                        enrollTask.dispose();
//                    }
                } catch (Exception e) {
                }
            }

            System.out.println("MyThread - END " + Thread.currentThread().getName());
        }

        public void run1() {
            System.out.println("MyThread - START " + Thread.currentThread().getName());
//            ExecutorService executor = null;
            long lStartTime = System.currentTimeMillis();
            NBiometricTask enrollTask = null;

            try {
                int nCreateTemplate = Integer.parseInt(ConfigUtil.getProperty("create.or.load.templates", "1"));
                int nFaceQualityThreshold = Integer.parseInt(ConfigUtil.getProperty("faces.quality.threshold", "0"));
                int nFaceQualityThresholdEnable = Integer.parseInt(ConfigUtil.getProperty("enable.custom.faces.quality.threshold", "1"));
                String srcLocation = ConfigUtil.getProperty("FILES_DIR", "D:\\api\\images\\");
                if (nCreateTemplate == 2) {
                    srcLocation = ConfigUtil.getProperty("templates.images.path", "e:\\api\\templates\\");
                }
                String templatesPath = ConfigUtil.getProperty("templates.images.path", "e:\\api\\templates\\");
                File file = null;
                if (nCreateTemplate == 1 || nCreateTemplate == 3) {
                    file = new File(srcLocation);
                } else {
                    file = new File(templatesPath);
                }
                System.out.println("Location=>" + file.getAbsolutePath());
                System.out.println("Templatepath=>" + templatesPath);
                int nFailedCount = 0;
                if (file.isDirectory()) {
                    biometricClient = new NBiometricClient();
                    final String additionalComponents = "Biometrics.FaceSegmentsDetection";
                    boolean isAdditionalComponentActivated = NLicense.isComponentActivated(additionalComponents);
                    biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                    biometricClient.setFacesDetectAllFeaturePoints(isAdditionalComponentActivated);
                    biometricClient.setFacesDetectBaseFeaturePoints(isAdditionalComponentActivated);
                    biometricClient.setFacesDetermineGender(isAdditionalComponentActivated);
                    biometricClient.setFacesDetermineAge(isAdditionalComponentActivated);
                    biometricClient.setFacesDetectProperties(isAdditionalComponentActivated);
                    biometricClient.setFacesRecognizeExpression(isAdditionalComponentActivated);
                    biometricClient.setFacesMaximalRoll((Float) 120.0f);
                    biometricClient.setFacesMaximalYaw((Float) 90.0f);
                    if (nFaceQualityThresholdEnable == 1) {
                        biometricClient.setFacesQualityThreshold((byte) nFaceQualityThreshold);
                    }

//                    biometricClient.setFacesTemplateSize(NTemplateSize.SMALL);
                    if (nCreateTemplate != 3) {
                        enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
                    }
//                    executor = Executors.newFixedThreadPool(Integer.parseInt(ConfigUtil.getProperty("worker.threads", "10")));//creating a pool of 5 threads  
                    System.out.println("************Total Images in folder => " + file.list().length);
                    for (int i = 0; i < file.list().length; i++) {
                        logger.debug("[loadTemplates()] " + file.listFiles()[i].getName().split("\\.")[0]);
                        NSubject subject = null;
                        NSubject tSubject = null;
                        NFace face = null;
                        try {
                            //1-create templates , 2-Load Template
                            if (nCreateTemplate == 2) {
                                subject = loadSubjectFromTemplate(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0]);
                                enrollTask.getSubjects().add(subject);
                            } else if (nCreateTemplate == 3) { // only create template
                                try {
                                    tSubject = new NSubject();
                                    face = new NFace();
//                                subject = createSubject(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0], String.format("GallerySubject_%d", i));
                                    face.setFileName(file.listFiles()[i].getAbsolutePath());
                                    tSubject.getFaces().add(face);
                                    tSubject.setId(file.listFiles()[i].getName().split("\\.")[0]);

                                    NBiometricStatus nStatus = biometricClient.createTemplate(tSubject);
                                    if (nStatus == NBiometricStatus.OK) {
                                        NFile.writeAllBytes(templatesPath + file.listFiles()[i].getName().split("\\.")[0], tSubject.getTemplate().save());
                                    } else {
                                        nFailedCount++;
                                        System.out.println("Invalid image to load into cache , file=>" + file.listFiles()[i].getName() + " & status=> " + nStatus);
                                        logger.error("Invalid image to load into cache , file=>" + file.listFiles()[i].getName() + " & status=> " + nStatus);
                                    }
                                } catch (Exception e) {
                                    logger.error(" 3-Only CreateTemplate : " + e.getStackTrace());
                                } finally {
                                    try {
                                        if (face != null) {
                                            face.dispose();
                                        }
                                        if (tSubject != null) {
                                            tSubject.dispose();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            } else {
                                subject = createSubject(file.listFiles()[i].getAbsolutePath(), file.listFiles()[i].getName().split("\\.")[0], false);
                                NBiometricStatus nStatus = biometricClient.createTemplate(subject);
                                if (nStatus == NBiometricStatus.OK) {
                                    NFile.writeAllBytes(templatesPath + file.listFiles()[i].getName().split("\\.")[0], subject.getTemplate().save());
                                    enrollTask.getSubjects().add(subject);
                                } else {
                                    nFailedCount++;
                                    System.out.println("Invalid image to load into cache , file=>" + file.listFiles()[i].getName() + " & status=> " + nStatus);
                                    logger.error("Invalid image to load into cache , file=>" + file.listFiles()[i].getName() + " & status=> " + nStatus);
                                }
                            }

//                        Runnable worker = new WorkerThread(file.listFiles()[i], i);
//                        executor.execute(worker);//calling execute method of ExecutorService  
                            if (i > 0 && i % 100 == 0) {
                                if (nCreateTemplate != 3) {
                                    biometricClient.performTask(enrollTask);
                                }
                                System.out.println("*****************Loaded images into memory===>" + i);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (subject != null) {
                                    subject.dispose();
                                    subject = null;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (nCreateTemplate != 3) {
                        biometricClient.performTask(enrollTask);
                    }

                }
//            if (enrollTask.getStatus() != NBiometricStatus.OK) {
//                System.out.format("Enrollment was unsuccessful. Status: %s.\n", enrollTask.getStatus());
//                if (enrollTask.getError() != null) {
//                    throw enrollTask.getError();
//                }
//                System.exit(-1);
//            }
                System.out.println("Failed count => " + nFailedCount);
                System.out.println("Templates loaded successfully , Total =>" + file.list().length);
                System.out.println("Total time to load templates => " + (System.currentTimeMillis() - lStartTime) / 1000 + " Sec's");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
//                try {
//                    if (executor != null) {
//                        executor.shutdown();
//                        while (!executor.isTerminated()) {
//                        }
//                    }
//                } catch (Exception e) {
//                }
            }

            System.out.println("MyThread - END " + Thread.currentThread().getName());
        }

    }

    class WorkerThread implements Runnable {

        File file;
        int i = 0;

        public WorkerThread() {

        }

        public WorkerThread(File s, int iValue) {
            this.file = s;
            this.i = iValue;
        }

        public void run() {
//            System.out.println(Thread.currentThread().getName() + " (Start) message = " + file.getName());
            processmessage();//call processmessage method that sleeps the thread for 2 seconds  
//            System.out.println(Thread.currentThread().getName() + " (End)");//prints thread name  
        }

        private void processmessage() {
//            System.out.println("biometric object => "+biometricClient);
            NSubject subject = null;
            NBiometricTask enrollTask = null;
            try {
                //1-create templates , 2-Load Template
                enrollTask = biometricClient.createTask(EnumSet.of(NBiometricOperation.ENROLL), null);
                long time = System.currentTimeMillis();
                subject = loadSubjectFromTemplate(file.getAbsolutePath(), file.getName().split("\\.")[0]);
                enrollTask.getSubjects().add(subject);
//                System.out.println("timetaken for loadSubjectFromTemplate() => " + (System.currentTimeMillis() - time) + " ms");
                if (i > 0 && i % 1000 == 0) {
//                    System.out.println("enrollTask  size = >"+enrollTask.getSize());
//                    System.out.println("enrollTask  size = >"+enrollTask.getStatistics());
                    System.out.println("nBiometricObject=>" + biometricClient);
                    long lTime = 0l;
                    lTime = System.currentTimeMillis();
                    biometricClient.performTask(enrollTask);
                    System.out.println("*****************Loaded images into memory===>" + i + " , timetaken for performtask() => " + (System.currentTimeMillis() - lTime) + " ms");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    if (subject != null) {
                        subject.dispose();
                        subject = null;
                    }
                    if (enrollTask != null) {
                        enrollTask.dispose();
                        enrollTask = null;
                    }
                } catch (Exception e) {
                }
            }
        }
    }

    private static NSubject loadSubjectFromTemplate(String fileName, String subjectId) throws IOException {
        NSubject subject = new NSubject();
        subject.setTemplateBuffer(NFile.readAllBytes(fileName));
        subject.setId(subjectId);
        return subject;
    }
}
