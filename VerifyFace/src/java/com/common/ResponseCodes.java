/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.common;

/**
 *
 * @author chhavikumar.b
 */
public class ResponseCodes {

    public enum ServiceErrorCodes {

        SUCCESS(0, "SUCCESS"),
        GENERIC_ERROR(-1, "Internal error occured"),
        NO_DATA_FOUND(100, "No data found"),
        INVALID_JSON(101, "Invalid JSON"),
        INVALID_INPUT_IMAGE(102, "Invalid input image"),
        BAD_CONTRAST(102, "Invalid image due to bad contrast"),
        TOO_MANY_OBJECTS(102, "Invalid image due to too many objects found"),
        BAD_SHARPNESS(102, "Invalid image due to bad sharpness"),
        DUPLICATE_IMAGE(103, "Image name should be unique"),
        INVALID_MATCHING_THRESHHOLD(104, "Invalid matching threshhold value"),
        FOLDER_NAMES_ARE_MANDATORY(105, "Invalid folder name values");
        private final int CODE;
        private final String DESC;

        ServiceErrorCodes(int aStatus, String desc) {
            this.CODE = aStatus;
            this.DESC = desc;
        }

        public int CODE() {
            return this.CODE;
        }

        public String getCode() {
            return this.CODE + "";
        }

        public String DESC() {
            return this.DESC;
        }

        public String DESC(String param1) {
            if (null != param1 && param1.length() > 0) {
                return this.DESC + " : " + param1;
            }
            return this.DESC;
        }

        public static String getMessage(ServiceErrorCodes errorCode) {
            return errorCode.CODE() + " " + errorCode.DESC();
        }

        public static String getIdBasedmessage(int code) {
            String message = "";
            for (ServiceErrorCodes errCode : ServiceErrorCodes.values()) {
                if (errCode.CODE == code) {
                    message = errCode.DESC();
                    break;
                }
            }
            return message;
        }
    }

    public static void main(String[] args) {
        // Event rpEvent = Event.getRPEvent("MO", 1);
        //  System.out.println("rpEvent = " + rpEvent);

    }
}
