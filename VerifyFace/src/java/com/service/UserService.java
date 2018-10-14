/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import com.common.ResponseCodes;
import static com.common.ResponseCodes.ServiceErrorCodes.GENERIC_ERROR;
import static com.common.ResponseCodes.ServiceErrorCodes.SUCCESS;
import com.common.Utilities;
import com.beans.User;
import static com.common.ResponseCodes.ServiceErrorCodes.BAD_CONTRAST;
import static com.common.ResponseCodes.ServiceErrorCodes.BAD_SHARPNESS;
import static com.common.ResponseCodes.ServiceErrorCodes.DUPLICATE_IMAGE;
import static com.common.ResponseCodes.ServiceErrorCodes.INVALID_INPUT_IMAGE;
import static com.common.ResponseCodes.ServiceErrorCodes.TOO_MANY_OBJECTS;
import com.dao.UserDAO;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author pavankumar.g
 */
public class UserService {

    private static final Logger logger = Logger.getLogger(UserService.class);
    private static UserDAO objUserDAO = null;

    public UserService() {
        try {
            System.out.println("in UserService()");
            if (objUserDAO == null) {
                objUserDAO = new UserDAO();
            }

        } catch (Exception e) {
            logger.error("Exception in UserService(),ex:" + e.getMessage(), e);
        }
    }

    public String addUserDetails(List<User> user, String strTid) {
        int isUpdated = 0;
        int nUserID = -1;
        ResponseCodes.ServiceErrorCodes errorCode = null;
        try {

            isUpdated = objUserDAO.addUserDetails(user);
            if (isUpdated > 0) {
                return Utilities.prepareReponse(SUCCESS.getCode(), SUCCESS.DESC(), strTid);
            } else if (isUpdated == -2) {
                return Utilities.prepareReponse(BAD_SHARPNESS.getCode(), BAD_SHARPNESS.DESC(), strTid);
            } else if (isUpdated == -3) {
                return Utilities.prepareReponse(BAD_CONTRAST.getCode(), BAD_CONTRAST.DESC(), strTid);
            } else if (isUpdated == -4) {
                return Utilities.prepareReponse(TOO_MANY_OBJECTS.getCode(), TOO_MANY_OBJECTS.DESC(), strTid);
            } else if (isUpdated == -1) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), strTid);
            } else {
                return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), strTid);
            }

        } catch (SQLException sqle) {
            if(sqle.getMessage()!=null && (sqle.getMessage().contains("uuid_uq_idx")||sqle.getMessage().contains("Duplicate") )){
                return Utilities.prepareReponse(DUPLICATE_IMAGE.getCode() + "", DUPLICATE_IMAGE.DESC(), strTid, nUserID);
            }
            logger.error(" Got SQLException addUserDetails() , ex:" + Utilities.getStackTrace(sqle));
            return Utilities.prepareReponse(errorCode.getCode() + "", errorCode.DESC(), strTid, nUserID);

        } catch (Exception e) {
            logger.error("Exception in addUserDetails(),ex:" + e.getMessage(), e);
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), strTid);
        }
    }

    public String getUserDetails(int min, int max, String sortBy, String orderBy, String search, String strTid) {
        try {
            return objUserDAO.getUserDetails(min, max, sortBy, orderBy, search, strTid);
        } catch (Exception e) {
            logger.error("Exception in getUserDetails(),ex:" + e.getMessage(), e);
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), strTid);
        }
    }
    public int updateUser(String imageName, String folderName) {
        try {
            return objUserDAO.updateUser(imageName,folderName);
        } catch (Exception e) {
            logger.error("Exception in updateUser(),ex:" + e.getMessage(), e);
            return -1;
        }
    }
    public int deleteUser(String imageName,String folderName) {
        try {
            return objUserDAO.deleteUser(imageName,folderName);
        } catch (Exception e) {
            logger.error("Exception in deleteUser(),ex:" + e.getMessage(), e);
            return -1;
        }
    }

    public String verifyMatchedInFromGallery(String sourceImage, String name, String strTid,int nMatchingThreshhold,String folderNames,int noOfResults) {
        try {
            return objUserDAO.verifyMatchedInFromGallery(sourceImage, name, strTid,nMatchingThreshhold,folderNames,noOfResults);
        } catch (Exception e) {
            if(e.getMessage().contains("BAD_SHARPNESS")){
                return Utilities.prepareReponse(BAD_SHARPNESS.getCode(), BAD_SHARPNESS.DESC(), strTid);
            }else{
                logger.error("Exception in verifyMatchedInFromGallery(),ex:" + e.getMessage(), e);
                return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), strTid);
                
            }
        }
    }
}
