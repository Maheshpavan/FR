/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.controller;

import com.beans.User;
import com.common.ConfigUtil;
import com.common.ResponseCodes;
import static com.common.ResponseCodes.ServiceErrorCodes.FOLDER_NAMES_ARE_MANDATORY;
import static com.common.ResponseCodes.ServiceErrorCodes.GENERIC_ERROR;
import static com.common.ResponseCodes.ServiceErrorCodes.INVALID_INPUT_IMAGE;
import static com.common.ResponseCodes.ServiceErrorCodes.INVALID_MATCHING_THRESHHOLD;
import static com.common.ResponseCodes.ServiceErrorCodes.SUCCESS;
import com.common.Utilities;
import com.service.UserService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

/**
 * REST Web Service
 *
 * @author pavankumar.g
 */
@RestController
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class);
    private static UserService objUserService = null;
    List<String> availableFolderNames = new ArrayList<>();

    UserController() {
        try {
            System.out.println("in UserController()");
            if (objUserService == null) {
                objUserService = new UserService();
            }
            String folders = ConfigUtil.getProperty("verifyface.folder.names", "accused,person_missing,person_found,dead_bodies,victims,others,facebook");
            if (StringUtils.isNotBlank(folders)) {
                for (String folder : folders.split(",")) {
                    availableFolderNames.add(folder);
                }
            }

        } catch (Exception e) {
            logger.error("Exception in PropertyController(),ex:" + e.getMessage(), e);
        }
    }

//    String FILES_DIR = ConfigUtil.getProperty("FILES_DIR", "d:\\api\\images\\");
    String VIR_DIR = ConfigUtil.getProperty("VIR_DIR", "d:\\api\\verify\\images\\");
    String IMAGE_FOLDERS_LOCATION = ConfigUtil.getProperty("FILES_DIR", "E:/xampp/htdocs/fr/public/enrolled_images/");
//    String IMAGE_FOLDERS_LOCATION = ConfigUtil.getProperty("images.folders.location", "E:/xampp/htdocs/fr/public/");
    String FILE_FOLDER_SEPERATOR = ConfigUtil.getProperty("file.folder.seperator", "^");

    @RequestMapping(value = "/user/enroll", method = RequestMethod.POST, produces = {"application/json"})
    public String enroll(@RequestParam("image") MultipartFile multipartfile,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "age", defaultValue = "0") int age,
            @RequestParam(value = "mobile", defaultValue = "") String mobile,
            @RequestParam(value = "emailid", defaultValue = "") String emailId,
            @RequestParam(value = "address", defaultValue = "") String address,
            @RequestParam(value = "psqno", defaultValue = "") String personSeqNumber,
            @RequestParam(value = "imageName") String imageName,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            @RequestParam(value = "templateFolderName", defaultValue = "") String templateFolderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        try {
            if (StringUtils.isBlank(folderNames)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }

            byte[] bytes = multipartfile.getBytes();
//            File dir = new File(FILES_DIR);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server

            List<User> users = new ArrayList<>();
            for (String folderName : folderNames.split(",")) {
                String uuid = imageName;
                File folderDir = null;
                try {
                    String prepareFolder = IMAGE_FOLDERS_LOCATION + folderName;
                    folderDir = new File(prepareFolder);
                    if (!folderDir.isDirectory()) {
                        folderDir.mkdirs();
                    }
                } catch (Exception e) {
                    logger.error(" Got Exception while verifying folder is exists or not " + Utilities.getStackTrace(e));
                    return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
                }
                File serverFile = new File(folderDir.getAbsolutePath() + File.separator + uuid + ".jpg");
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile, false));
                stream.write(bytes);
                stream.close();

//                uuid = uuid + FILE_FOLDER_SEPERATOR + folderName;
                String newFilenmae = uuid + ".jpg";
//                FileUtils.copyFile(serverFile, new File(dir.getAbsolutePath() + File.separator + newFilenmae));
                serverFile.renameTo(new File(folderDir.getAbsolutePath() + File.separator + newFilenmae));
                User user = new User();
                user.setUuid(uuid);
                user.setFolderName(folderName);
                user.setName(name);
                user.setAge(age);
                user.setMobile(mobile);
                user.setEmail(emailId);
                user.setAddress(address);
                user.setImageLink("/images/" + folderName + "/" + newFilenmae);
                user.setCreatedBy("admin");
                user.setImageLocation(folderDir.getAbsolutePath() + File.separator + newFilenmae);
                user.setImageName(multipartfile.getOriginalFilename());
                user.setPersonSeqNumber(personSeqNumber);
                user.setTemplateFolderName(templateFolderName);
                users.add(user);
            }

            return objUserService.addUserDetails(users, transId);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/enrollusinglocalfile", method = RequestMethod.POST, produces = {"application/json"})
    public String enroll1(@RequestParam("image") String fileName,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "age", defaultValue = "0") int age,
            @RequestParam(value = "mobile", defaultValue = "") String mobile,
            @RequestParam(value = "emailid", defaultValue = "") String emailId,
            @RequestParam(value = "address", defaultValue = "") String address,
            @RequestParam(value = "psqno", defaultValue = "") String personSeqNumber,
            @RequestParam(value = "imageName") String imageName,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            @RequestParam(value = "templateFolderName", defaultValue = "") String templateFolderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        try {
            if (fileName == null || fileName.trim().length() == 0) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
            }
            if (StringUtils.isBlank(folderNames)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }
//            File inputFile = new File(fileName);
//            if (!inputFile.exists()) {
//                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
//            }
//            File dir = new File(FILES_DIR);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server
            InputStream inStream = null;
            OutputStream outStream = null;
            String uuid = imageName;
            List<User> users = new ArrayList<>();
            for (String folderName : folderNames.split(",")) {
                File folderDir = null;
                try {
                    String prepareFolder = IMAGE_FOLDERS_LOCATION + folderName;
                    folderDir = new File(prepareFolder);
                    if (!folderDir.isDirectory()) {
                        folderDir.mkdirs();
                    }
                } catch (Exception e) {
                    logger.error(" Got Exception while verifying folder is exists or not " + Utilities.getStackTrace(e));
                    return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
                }
//                uuid = uuid + FILE_FOLDER_SEPERATOR + folderName;
                String newFilenmae = uuid + ".jpg";
                InputStream is = null;
                OutputStream os = null;
                try {

//                public static void saveImage(String imageUrl, String destinationFile) throws IOException {
                    File serverFile = new File(folderDir.getAbsolutePath() + File.separator + newFilenmae);
                    URL url = new URL(fileName);
                    is = url.openStream();
                    os = new FileOutputStream(serverFile, false);

                    byte[] b = new byte[2048];
                    int length;

                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }

//}
//                inStream = new FileInputStream(inputFile);
//                outStream = new FileOutputStream(serverFile);
//                byte[] buffer = new byte[1024];
//                int length;
//                //copy the file content in bytes 
//                while ((length = inStream.read(buffer)) > 0) {
//                    outStream.write(buffer, 0, length);
//                }
//                serverFile.renameTo(new File(dir.getAbsolutePath() + File.separator + newFilenmae));
                } catch (Exception e) {
                    e.printStackTrace();
                    return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
                } finally {
//                inStream.close();
//                outStream.close();
                    is.close();
                    os.close();
                }

//            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
//            stream.write(bytes);
//            stream.close();
                User user = new User();
                user.setUuid(uuid);
                user.setFolderName(folderName);
                user.setName(name);
                user.setAge(age);
                user.setMobile(mobile);
                user.setEmail(emailId);
                user.setAddress(address);
                user.setImageLink("/images/" + folderName + "/" + newFilenmae);
                user.setCreatedBy("admin");
                user.setImageLocation(folderDir.getAbsolutePath() + File.separator + newFilenmae);
                user.setImageName(newFilenmae);
                user.setPersonSeqNumber(personSeqNumber);
                user.setTemplateFolderName(templateFolderName);
                users.add(user);
            }
            return objUserService.addUserDetails(users, transId);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/enrolltemplateonly", method = RequestMethod.POST, produces = {"application/json"})
    public String enrollOnlyTemplate(@RequestParam("image") String fileName,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "age", defaultValue = "0") int age,
            @RequestParam(value = "mobile", defaultValue = "") String mobile,
            @RequestParam(value = "emailid", defaultValue = "") String emailId,
            @RequestParam(value = "address", defaultValue = "") String address,
            @RequestParam(value = "psqno", defaultValue = "") String personSeqNumber,
            @RequestParam(value = "imageName") String imageName,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            @RequestParam(value = "templateFolderName", defaultValue = "") String templateFolderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        String tempFolderPath = ConfigUtil.getProperty("temp.folder.path", "E:/xampp/htdocs/fr/public/tempimages/");

        List<User> users = new ArrayList<>();
        try {
            if (fileName == null || fileName.trim().length() == 0) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
            }
            if (StringUtils.isBlank(folderNames)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }
//            File inputFile = new File(fileName);
//            if (!inputFile.exists()) {
//                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
//            }
//            File dir = new File(FILES_DIR);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server
            InputStream inStream = null;
            OutputStream outStream = null;
            String uuid = imageName;
            try {
                File tempFolder = new File(tempFolderPath);
                if (!tempFolder.isDirectory()) {
                    tempFolder.mkdirs();
                }
            } catch (Exception e) {
            }
            for (String folderName : folderNames.split(",")) {
                File folderDir = null;
                try {
                    String prepareFolder = IMAGE_FOLDERS_LOCATION + folderName;
                    folderDir = new File(prepareFolder);
                    if (!folderDir.isDirectory()) {
                        folderDir.mkdirs();
                    }
                } catch (Exception e) {
                    logger.error(" Got Exception while verifying folder is exists or not " + Utilities.getStackTrace(e));
                    return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
                }
//                uuid = uuid + FILE_FOLDER_SEPERATOR + folderName;
                String newFilenmae = uuid + ".jpg";
                InputStream is = null;
                OutputStream os = null;
                try {

//                public static void saveImage(String imageUrl, String destinationFile) throws IOException {
                    File serverFile = new File(tempFolderPath + File.separator + newFilenmae);
                    URL url = new URL(fileName);
                    is = url.openStream();
                    os = new FileOutputStream(serverFile, false);

                    byte[] b = new byte[2048];
                    int length;

                    while ((length = is.read(b)) != -1) {
                        os.write(b, 0, length);
                    }

//}
//                inStream = new FileInputStream(inputFile);
//                outStream = new FileOutputStream(serverFile);
//                byte[] buffer = new byte[1024];
//                int length;
//                //copy the file content in bytes 
//                while ((length = inStream.read(buffer)) > 0) {
//                    outStream.write(buffer, 0, length);
//                }
//                serverFile.renameTo(new File(dir.getAbsolutePath() + File.separator + newFilenmae));
                } catch (Exception e) {
                    e.printStackTrace();
                    return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
                } finally {
//                inStream.close();
//                outStream.close();
                    is.close();
                    os.close();
                }

//            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
//            stream.write(bytes);
//            stream.close();
                User user = new User();
                user.setUuid(uuid);
                user.setFolderName(folderName);
                user.setName(name);
                user.setAge(age);
                user.setMobile(mobile);
                user.setEmail(emailId);
                user.setAddress(address);
                user.setImageLink("/images/" + folderName + "/" + newFilenmae);
                user.setCreatedBy("admin");
                user.setImageLocation(tempFolderPath + File.separator + newFilenmae);
                user.setImageName(newFilenmae);
                user.setPersonSeqNumber(personSeqNumber);
                user.setTemplateFolderName(templateFolderName);
                users.add(user);
            }
            return objUserService.addUserDetails(users, transId);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
                if (users != null) {
                    for (User user : users) {
                        File tempFile = new File(user.getImageLocation());
                        if (tempFile.isFile()) {
                            if (tempFile.delete()) {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/enrollusingimagefromdrive", method = RequestMethod.POST, produces = {"application/json"})
    public String enroll2(@RequestParam("image") String fileName,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "age", defaultValue = "0") int age,
            @RequestParam(value = "mobile", defaultValue = "") String mobile,
            @RequestParam(value = "emailid", defaultValue = "") String emailId,
            @RequestParam(value = "address", defaultValue = "") String address,
            @RequestParam(value = "psqno", defaultValue = "") String personSeqNumber,
            @RequestParam(value = "imageName") String imageName,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            @RequestParam(value = "templateFolderName", defaultValue = "") String templateFolderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        try {
            if (fileName == null || fileName.trim().length() == 0) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
            }
            if (StringUtils.isBlank(folderNames)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }
            File inputFile = new File(fileName);
            if (!inputFile.exists()) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
            }
//            File dir = new File(FILES_DIR);
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server
            InputStream inStream = null;
            OutputStream outStream = null;
            String uuid = imageName;
            List<User> users = new ArrayList<>();
            for (String folderName : folderNames.split(",")) {
                File folderDir = null;
                try {
                    String prepareFolder = IMAGE_FOLDERS_LOCATION + folderName;
                    folderDir = new File(prepareFolder);
                    if (!folderDir.isDirectory()) {
                        folderDir.mkdirs();
                    }
                } catch (Exception e) {
                    logger.error(" Got Exception while verifying folder is exists or not " + Utilities.getStackTrace(e));
                    return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
                }
//                uuid = uuid + FILE_FOLDER_SEPERATOR + folderName;
                String newFilenmae = uuid + ".jpg";
//            InputStream is = null;
//            OutputStream os = null;
                try {

//                public static void saveImage(String imageUrl, String destinationFile) throws IOException {
                    File serverFile = new File(folderDir.getAbsolutePath() + File.separator + newFilenmae);
//                URL url = new URL(fileName);
//                is = url.openStream();
//                os = new FileOutputStream(serverFile);
//                
//                byte[] b = new byte[2048];
//                int length;
//                
//                while ((length = is.read(b)) != -1) {
//                    os.write(b, 0, length);
//                }

//}
                    inStream = new FileInputStream(inputFile);
                    outStream = new FileOutputStream(serverFile, false);
                    byte[] buffer = new byte[1024];
                    int length;
                    //copy the file content in bytes 
                    while ((length = inStream.read(buffer)) > 0) {
                        outStream.write(buffer, 0, length);
                    }
                    serverFile.renameTo(new File(folderDir.getAbsolutePath() + File.separator + newFilenmae));
                } catch (Exception e) {
                    e.printStackTrace();
                    return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
                } finally {
                    inStream.close();
                    outStream.close();
//                is.close();
//                os.close();
                }

//            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
//            stream.write(bytes);
//            stream.close();
                User user = new User();
                user.setUuid(uuid);
                user.setName(name);
                user.setAge(age);
                user.setMobile(mobile);
                user.setFolderName(folderName);
                user.setEmail(emailId);
                user.setAddress(address);
                user.setImageLink("/images/" + folderName + "/" + newFilenmae);
                user.setCreatedBy("admin");
                user.setImageLocation(folderDir.getAbsolutePath() + File.separator + newFilenmae);
                user.setImageName(newFilenmae);
                user.setPersonSeqNumber(personSeqNumber);
                user.setTemplateFolderName(templateFolderName);
                users.add(user);
            }
            return objUserService.addUserDetails(users, transId);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/detect", method = RequestMethod.POST, produces = {"application/json"})
    public String detect(@RequestParam("image") MultipartFile multipartfile,
            @RequestParam(value = "matchingthreshhold", defaultValue = "48") int nMatchingThreshhold,
            @RequestParam(value = "noofresults", defaultValue = "5") int noOfResults,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        long lStart = System.currentTimeMillis();
        String transId = UUID.randomUUID().toString();

        if (nMatchingThreshhold < 0 || nMatchingThreshhold > 100) {
            return Utilities.prepareReponse(INVALID_MATCHING_THRESHHOLD.getCode(), INVALID_MATCHING_THRESHHOLD.DESC(), transId);
        }
        try {
            byte[] bytes = multipartfile.getBytes();
            File dir = new File(VIR_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server

            File serverFile = new File(dir.getAbsolutePath() + File.separator + multipartfile.getOriginalFilename());
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
            stream.write(bytes);
            stream.close();
            String newFilenmae = UUID.randomUUID() + ".jpg";
            serverFile.renameTo(new File(dir.getAbsolutePath() + File.separator + newFilenmae));
            return objUserService.verifyMatchedInFromGallery(dir.getAbsolutePath() + File.separator + newFilenmae, newFilenmae, transId, nMatchingThreshhold, folderNames, noOfResults);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
            logger.error("[detect] total time taken =>" + (System.currentTimeMillis() - lStart) + " ms");
        }
    }

    @RequestMapping(value = "/user/detectusingurl", method = RequestMethod.POST, produces = {"application/json"})
    public String detectUsingUrl(@RequestParam("image") String imageURL,
            @RequestParam(value = "matchingthreshhold", defaultValue = "48") int nMatchingThreshhold,
            @RequestParam(value = "noofresults", defaultValue = "5") int noOfResults,
            @RequestParam(value = "folderNames", defaultValue = "") String folderNames,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        long lStart = System.currentTimeMillis();
        String transId = UUID.randomUUID().toString();

        if (nMatchingThreshhold < 0 || nMatchingThreshhold > 100) {
            return Utilities.prepareReponse(INVALID_MATCHING_THRESHHOLD.getCode(), INVALID_MATCHING_THRESHHOLD.DESC(), transId);
        }
        String newFilenmae = transId + ".jpg";
        InputStream is = null;
        OutputStream os = null;
        try {
            File dir = new File(VIR_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            //  String hostname=InetAddress.getLocalHost().getHostName();
            // Create the file on server

            File serverFile = new File(dir.getAbsolutePath() + File.separator + newFilenmae);
            URL url = new URL(imageURL);
            is = url.openStream();
            os = new FileOutputStream(serverFile, false);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }
            is.close();
            os.close();

            return objUserService.verifyMatchedInFromGallery(dir.getAbsolutePath() + File.separator + newFilenmae, newFilenmae, transId, nMatchingThreshhold, folderNames, noOfResults);

        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }

                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
            logger.error("[detect] total time taken =>" + (System.currentTimeMillis() - lStart) + " ms");
        }
    }

    @RequestMapping(value = "/list/users", method = RequestMethod.GET, produces = {"application/json"})
    public String getUsers(
            @RequestParam(value = "min", defaultValue = "0") int min,
            @RequestParam(value = "max", defaultValue = "20") int max,
            @RequestParam(value = "orderBy", defaultValue = "desc") String orderBy,
            @RequestParam(value = "sortBy", defaultValue = "created_on") String sortBy,
            @RequestParam(value = "search", defaultValue = "") String search,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        try {
            return objUserService.getUserDetails(min, max, sortBy, orderBy, search, transId);
        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/update", method = RequestMethod.PUT, produces = {"application/json"})
    public String moveUserToFolder(
            @RequestParam(value = "imageName", defaultValue = "") String imageName,
            @RequestParam(value = "folderName", defaultValue = "") String folderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        try {

            if (StringUtils.isBlank(imageName)) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);

            }

            if (StringUtils.isBlank(folderName)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }

            int nResult = objUserService.updateUser(imageName, folderName);

            if (nResult > 0) {
                return Utilities.prepareReponse(SUCCESS.getCode(), SUCCESS.DESC(), transId);
            } else if (nResult == 0) {
                return Utilities.prepareReponse(ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.getCode(), ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.DESC(), transId);
            } else if (nResult == -2) {
                return Utilities.prepareReponse(ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.getCode(), ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.DESC(), transId);
            } else {
                return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
            }
        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
            } catch (Exception e) {
            }
        }
    }

    @RequestMapping(value = "/user/delete", method = RequestMethod.DELETE, produces = {"application/json"})
    public String deleteUser(
            @RequestParam(value = "imageName", defaultValue = "") String imageName,
            @RequestParam(value = "folderName", defaultValue = "") String folderName,
            HttpSession httpSession,
            HttpServletResponse servletResponse) {
        String transId = UUID.randomUUID().toString();
        File file = null;
        try {

            if (StringUtils.isBlank(imageName)) {
                return Utilities.prepareReponse(INVALID_INPUT_IMAGE.getCode(), INVALID_INPUT_IMAGE.DESC(), transId);
            }

            if (StringUtils.isBlank(folderName)) {
                return Utilities.prepareReponse(FOLDER_NAMES_ARE_MANDATORY.getCode(), FOLDER_NAMES_ARE_MANDATORY.DESC(), transId);
            }

            String templatesPath = ConfigUtil.getProperty("templates.path", "E:\\xampp\\htdocs\\fr\\public\\templates\\");

            int nResult = objUserService.deleteUser(imageName, folderName);

            if (nResult > 0) {

                String strFileName = templatesPath + File.separator + imageName;
                file = new File(strFileName);
                file.deleteOnExit();
                return Utilities.prepareReponse(SUCCESS.getCode(), SUCCESS.DESC(), transId);
            } else if (nResult == 0) {
                return Utilities.prepareReponse(ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.getCode(), ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.DESC(), transId);
            } else if (nResult == -2) {
                return Utilities.prepareReponse(ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.getCode(), ResponseCodes.ServiceErrorCodes.NO_DATA_FOUND.DESC(), transId);
            } else {
                return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
            }
        } catch (Exception e) {
            return Utilities.prepareReponse(GENERIC_ERROR.getCode(), GENERIC_ERROR.DESC(), transId);
        } finally {
            try {
                if (servletResponse != null) {
                    servletResponse.setHeader("Access-Control-Allow-Origin", "*");
                }
                if (file != null) {
                    file = null;
                }
            } catch (Exception e) {
            }
        }
    }

}
